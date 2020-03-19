#!/usr/local/bin/python3.6
# vim: expandtab shiftwidth=4

# Copyright (C) 2019 National Institute of Informatics
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

import logging
import os
import time
from copy import copy
from enum import Enum, auto
from threading import RLock

from sinetstream.crypto import CipherAES
from sinetstream.error import (
    InvalidArgumentError, NoConfigError, NoServiceError,
    UnsupportedServiceTypeError, InvalidMessageError, AlreadyConnectedError)
from sinetstream.marshal import Marshaller, Unmarshaller
from sinetstream.spi import PluginMessageReader, PluginMessageWriter
from sinetstream.utils import Registry
from sinetstream.value_type import BYTE_ARRAY, value_type_registry

logger = logging.getLogger(__name__)


class Consistency(Enum):
    AT_MOST_ONCE = auto()
    AT_LEAST_ONCE = auto()
    EXACTLY_ONCE = auto()


AT_MOST_ONCE = Consistency.AT_MOST_ONCE
AT_LEAST_ONCE = Consistency.AT_LEAST_ONCE
EXACTLY_ONCE = Consistency.EXACTLY_ONCE

DEFAULT_CLIENT_ID = None


def yaml_load(s):
    import yaml
    try:
        from yaml import CLoader as Loader
    except ImportError:
        from yaml import Loader
    try:
        yml = yaml.load(s, Loader=Loader)
        return yml
    except yaml.scanner.ScannerError:
        return None


config_files = [
    ".sinetstream_config.yml",
    "~/.config/sinetstream/config.yml",
]


def load_config(config_file=None):
    if config_file is not None:
        ret = load_config_from_file(config_file)
        if ret is None:
            logger.error(f"No configuration file exist: {config_file}")
            raise NoConfigError(f"No configuration file exist: {config_file}")
        return ret

    url = os.environ.get("SINETSTREAM_CONFIG_URL")
    if url:
        logger.info(f"SINETSTREAM_CONFIG_URL={url}")
        ret = load_config_from_url(url)
        if ret:
            return ret

    for file in config_files:
        ret = load_config_from_file(file)
        if ret is not None:
            return ret

    logger.error(f"No configuration file exist")
    raise NoConfigError()


def load_config_from_url(url):
    import urllib.request
    try:
        with urllib.request.urlopen(url) as res:
            contents = res.read().decode("utf-8")
            return yaml_load(contents)
    except OSError as ex:
        logger.debug('load config file from URL', stack_info=True)
        logger.warning('Could not load from the specified URL.')
        return None


def load_config_from_file(file):
    try:
        with open(os.path.expanduser(file)) as fp:
            logger.debug(f"load config file from {os.path.abspath(file)}")
            yml = yaml_load(fp.read())
            if yml:
                return yml
    except FileNotFoundError:
        logger.info(f"{file}: not found")


def convert_params(params):
    consistency = params.get("consistency")
    if consistency is None:
        return params
    new_params = copy(params)
    try:
        new_params["consistency"] = (
            consistency if isinstance(consistency, Consistency)
            else Consistency[consistency])
        return new_params
    except KeyError:
        raise InvalidArgumentError(f'invalid consistency: {consistency}')


def load_params(service, config_file=None):
    config = load_config(config_file)
    params = config.get(service)
    if params is None:
        logger.error(f"invalid service: {service}")
        raise NoServiceError()

    return convert_params(params)


class Message(object):
    def __init__(self, value, topic, tstamp, raw):
        self._value = value
        self._topic = topic
        self._tstamp = tstamp
        self._raw = raw

    def __repr__(self):
        return (
            f'Message(value={self._value!r}, topic={self._topic!r},' +
            f'tstamp={self._tstamp}, raw={self._raw!r})')

    @property
    def value(self):
        return self._value

    @property
    def topic(self):
        return self._topic

    @property
    def timestamp(self):
        return float(self._tstamp) / 1000000

    @property
    def timestamp_us(self):
        return self._tstamp

    @property
    def raw(self):
        return self._raw


'''
crypto_params = {
    # name             must   default available
    ("algorithm",      True,  None,   ["AES"]),
    ("key_length",     False, 128,    None),
    ("mode",           True,  None,   ["CBC", "EAX"]),
    ("padding",        False, "none", [None, "none", "pkcs7"]),
    ("key_derivation", False, key_derivation_params,   None),
    ("password",       True,  None,   None),
}

key_derivation_params = {
    # name             must   default available
    ("algorithm",      True,  None,          ["pbkdf2"]),
    ("salt_bytes",     False, 8,             None),
    ("iteration",      False, 10000,         None),
    ("prf",            False, "HMAC-SHA256", ["HMAC-SHA256", "HMAC-SHA384", "HMAC-SHA512"]),
}
'''


def make_cipher(crypto_params):
    algorithm = crypto_params["algorithm"]
    if algorithm == "AES":
        return CipherAES(crypto_params)
    else:
        assert False


def make_client_id():
    import uuid
    return "sinetstream-" + str(uuid.uuid4())


def validate_config(params):
    if 'brokers' not in params or not params['brokers']:
        raise InvalidArgumentError("You must specify several brokers.")


def deepupdate(d1, d2):
    for k, v in d2.items():
        if isinstance(v, dict) and k in d1:
            deepupdate(d1[k], v)
        else:
            d1[k] = v


def normalize_params(params):
    # client_id: None/"" -> generate
    cid = params["client_id"]
    if cid is DEFAULT_CLIENT_ID or cid == "":
        params["client_id"] = make_client_id()

    # crypto.password: STR -> value: STR
    # XXX crypto.password.value vs path
    crypto = params["crypto"]
    if "password" in crypto:
        password = crypto["password"]
        if type(password) is str:
            crypto["password"] = {"value": password}


def merge_parameter(service, kwargs, default_values, config_file=None):
    svc_params = load_params(service, config_file)
    # Merge parameters
    # Priority:
    #  ctor's argument (highest)
    #  config file
    #  sinetstream's default parameter
    #  plugin's default parameter (lowest)
    params = default_values.copy()
    deepupdate(params, svc_params)
    deepupdate(params, convert_params(kwargs))
    normalize_params(params)
    return params


default_params = {
    "consistency": AT_MOST_ONCE,
    "client_id": DEFAULT_CLIENT_ID,
    "value_type": BYTE_ARRAY,
    "crypto": {
        "key_length": 128,
        "padding": None,
        "key_derivation": {
            "salt_bytes": 8,
            "iteration": 10000,
            "prf": "HMAC-SHA256",
        },
    },
    "data_encryption": False
}


class MessageIO(object):

    def __init__(self, service, params, registry):
        validate_config(params)
        self._service = service
        self.params = params
        self._plugin = self._find_plugin(registry, params["type"])
        self.cipher = make_cipher(params["crypto"]) if params["data_encryption"] else None
        self._opened = False
        self._lock = RLock()

    def _find_plugin(self, registry, service_type):
        plugin_class = registry.get(service_type)
        if plugin_class is None:
            raise UnsupportedServiceTypeError(f"{service_type} not found")
        return plugin_class(self.params)

    def __enter__(self):
        logger.debug("MessageIO:enter")
        with self._lock:
            if self._opened:
                logger.error(f"already connected")
                raise AlreadyConnectedError()
            self._plugin.open()
            self._opened = True
            return self

    open = __enter__

    def __exit__(self, ex_type, ex_value, trace):
        logger.debug("MessageIO:exit")
        self.close()

    def close(self):
        logger.debug("MessageIO:close")
        with self._lock:
            if not self._opened:
                return
            self._plugin.close()
            self._plugin = None
            self._opened = False

    @property
    def service(self):
        return self._service

    @property
    def client_id(self):
        return self.params["client_id"]

    @property
    def consistency(self):
        return self.params["consistency"]

    @property
    def value_type(self):
        return self.params["value_type"]


READER_USAGE = '''MessageReader(
    service=SERVICE,                 # Service name defined in the configuration file. (REQUIRED)
    topics=TOPICS,                   # The topic to receive.
    consistency=AT_MOST_ONCE,        # consistency: AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE
    client_id=DEFAULT_CLIENT_ID,     # If not specified, the value is automatically generated.
    value_type=byte_array,           # The type of message.
    value_deserializer=None          # If not specified, use default deserializer according to valueType.
)'''


class MessageReader(MessageIO):
    registry = Registry("sinetstream.reader", PluginMessageReader)

    @staticmethod
    def usage():
        return READER_USAGE

    default_params = {
        **default_params,
        "receive_timeout_ms": float("inf"),
    }

    def __init__(self, service, topics=None, config_file=None, **kwargs):
        logger.debug("MessageReader:init")
        params = merge_parameter(service, kwargs, MessageReader.default_params, config_file=config_file)
        params["topics"] = _setup_topics(params, topics)
        super().__init__(service, params, MessageReader.registry)
        self.unmarshaller = Unmarshaller()
        self.value_deserializer = self._setup_deserializer()

    def __iter__(self):
        logger.debug("MessageReader:iter")
        self._iter = self._plugin.__iter__()
        return self

    def __next__(self):
        message, topic, raw = next(self._iter)
        return self._make_message(message, topic, raw)

    def _make_message(self, value, topic, raw):
        assert type(value) == bytes
        if self.cipher is not None:
            value = self.cipher.decrypt(value)
        assert type(value) == bytes
        tstamp, value = self.unmarshaller.unmarshal(value)
        assert type(tstamp) == int
        assert type(value) == bytes
        if self.value_deserializer is not None:
            value = self.value_deserializer(value)
        return Message(value, topic, tstamp, raw)

    def seek_to_beginning(self):
        self._plugin.seek_to_beginning()

    def seek_to_end(self):
        self._plugin.seek_to_end()

    def _setup_deserializer(self):
        if 'value_deserializer' in self.params:
            return self.params['value_deserializer']
        value_type = self.params['value_type']
        value_type_class = value_type_registry.get(value_type)
        if value_type_class is None:
            raise InvalidArgumentError(f'invalid value_type: {value_type}')
        return value_type_class().deserializer

    @property
    def topics(self):
        return copy(self.params["topics"])

    @property
    def receive_timeout_ms(self):
        return self.params["receive_timeout_ms"]


WRITER_USAGE = '''MessageWriter(
    service=SERVICE,                 # Service name defined in the configuration file. (REQUIRED)
    topic=TOPIC,                     # The topic to send.
    consistency=AT_MOST_ONCE,        # consistency: AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE
    client_id=DEFAULT_CLIENT_ID,     # If not specified, the value is automatically generated.
    value_type=byte_array,           # The type of message.
    value_serializer=None            # If not specified, use default serializer according to valueType.
)'''


class MessageWriter(MessageIO):

    registry = Registry("sinetstream.writer", PluginMessageWriter)

    @staticmethod
    def usage():
        return WRITER_USAGE

    default_params = {
        **default_params
    }

    def __init__(self, service, topic=None, config_file=None, **kwargs):
        logger.debug("MessageWriter:init")
        params = merge_parameter(service, kwargs, MessageWriter.default_params, config_file=config_file)
        params["topic"] = _setup_topic(params, topic)
        super().__init__(service, params, MessageWriter.registry)
        self.marshaller = Marshaller()
        self.value_serializer = self._setup_serializer()

    def publish(self, msg):
        tstamp = int(time.time() * 1000000)
        logger.debug(f"MessageWriter:publish:type(msg)={type(msg)}")
        if self.value_serializer is not None:
            msg = self.value_serializer(msg)
            if type(msg) != bytes:
                logger.error(f"value_serializer must return byte: type(msg)={type(msg)}")
                raise InvalidMessageError("value_serializer doesn't return bytes")
        else:
            if type(msg) != bytes:
                logger.error(f"value_serializer must be specified: type(msg)={type(msg)}")
                raise InvalidMessageError("non-bytes message is passed to publish")
        msg = self.marshaller.marshal(msg, tstamp)
        if self.cipher:
            msg = self.cipher.encrypt(msg)
        assert type(msg) == bytes
        return self._plugin.publish(msg)

    @property
    def topic(self):
        return self.params["topic"]

    def _setup_serializer(self):
        if 'value_serializer' in self.params:
            return self.params['value_serializer']
        value_type = self.params['value_type']
        value_type_class = value_type_registry.get(value_type)
        if value_type_class is None:
            raise InvalidArgumentError(f'invalid value_type: {value_type}')
        return value_type_class().serializer


def _setup_topic(params, topic):
    if topic is not None:
        ret = topic
    elif "topic" in params:
        ret = params["topic"]
    else:
        raise InvalidArgumentError("You must specify a topic.")

    if isinstance(ret, list):
        num_topic = len(ret)
        if num_topic > 1:
            raise InvalidArgumentError("You cannot specify multiple topics.")
        elif num_topic == 0:
            raise InvalidArgumentError("You must specify a topic.")
        return ret[0]
    else:
        return ret


def _setup_topics(params, topics):
    if topics is not None:
        ret = topics
    elif "topics" in params:
        ret = params["topics"]
    elif "topic" in params:
        ret = params["topic"]
    else:
        raise InvalidArgumentError("You must specify several topics.")

    if isinstance(ret, list) and len(ret) == 0:
        raise InvalidArgumentError("You must specify several topics.")
    return ret
