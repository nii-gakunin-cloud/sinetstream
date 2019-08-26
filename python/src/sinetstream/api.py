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
from pkg_resources import iter_entry_points

config = None

logger = logging.getLogger(__name__)


class SinetError(Exception):
    """Base class for exceptions in this module. """
    pass


class NoServiceError(SinetError):
    pass


class UnsupportedServiceTypeError(SinetError):
    pass


class NoConfigError(SinetError):
    pass


class InvalidArgumentError(SinetError):
    pass


class ConnectionError(SinetError):
    pass


class AlreadyConnectedError(SinetError):
    pass


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


def load_config():
    url = os.environ.get("SINETSTREAM_CONFIG_URL")
    if url:
        logger.info(f"SINETSTREAM_CONFIG_URL={url}")
        import urllib.request
        with urllib.request.urlopen(url) as res:
            contents = res.read().decode("utf-8")
            yml = yaml_load(contents)
            if yml:
                return yml

    for file in config_files:
        try:
            with open(os.path.expanduser(file)) as fp:
                yml = yaml_load(fp.read())
                if yml:
                    return yml
        except FileNotFoundError:
            logger.info(f"{file}: not found")
            pass

    logger.error(f"No configuration file exist")
    raise NoConfigError()


class Message(object):
    pass


# value_type
TEXT = "text"
BYTE_ARRAY = "byte_array"


# consistency
AT_MOST_ONCE = 0
AT_LEAST_ONCE = 1
EXACTLY_ONCE = 2


def check_consistency(x):
    if (
            x == AT_MOST_ONCE or
            x == AT_LEAST_ONCE or
            x == EXACTLY_ONCE
    ):
        return True
    raise InvalidArgumentError()


def setdict(d, k, v):
    if v:
        d[k] = v


def first(*args):
    for x in args:
        if x:
            return x
    return None


DEFAULT_CLIENT_ID = None


def make_client_id(cid):
    import uuid
    if cid is DEFAULT_CLIENT_ID or cid == "":
        cid = "sinetstream-" + str(uuid.uuid4())
    return cid


class Registry(object):

    def __init__(self, group):
        self.group = group
        self._plugins = {}
        self.register_entry_points()

    def register(self, name, plugin):
        self._plugins[name] = plugin

    def register_entry_points(self):
        for ep in iter_entry_points(self.group):
            logger.debug(f"entry_point.name={ep.name}")
            self._plugins[ep.name] = ep

    def get(self, name):
        if name in self._plugins:
            return self._plugins[name].load()
        else:
            logger.error(
                f"the corresponding plugin was not found: {name}")
            raise UnsupportedServiceTypeError()


# value_type -> (value_serializer, value_deserializer)
value_serdes = {
    None: (None, None),
    "raw": (None, None),
    "bytearray": (None, None),
    "byte_array": (None, None),
    "BYTEARRAY": (None, None),
    "BYTE_ARRAY": (None, None),
    "text": (lambda x: x.encode(), lambda x: x.decode())
    # "image": (xxx, yyy)
}


class MessageReader(object):

    registry = Registry("sinetstream.reader")

    def usage():
        return ('MessageReader(\n'
                '    service=SERVICE,                 '
                '# Service name defined in the configuration file. (REQUIRED)\n'
                '    topics=TOPICS,                   '
                '# The topic to receive.\n'
                '    consistency=AT_MOST_ONCE,        '
                '# consistency: AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE\n'
                '    client_id=DEFAULT_CLIENT_ID,     '
                '# If not specified, the value is automatically generated.\n'
                '    value_type=None,                 '
                '# The type of message.\n'
                '    value_deserializer=None          '
                '# If not specified, use default deserializer according to valueType.\n'
                ')')

    def __init__(self, service, topics,
                 consistency=AT_MOST_ONCE,
                 client_id=DEFAULT_CLIENT_ID,
                 value_type=None,
                 value_deserializer=None,
                 receive_timeout_ms=float("inf"),
                 **kwargs):
        logger.debug("MessageReader:init")

        check_consistency(consistency)

        config = load_config()
        self.svc = config.get(service)
        if self.svc is None:
            logger.error(f"invalid service: {service}")
            raise NoServiceError()

        self.service = service
        self.topics = topics
        self.consistency = consistency
        self._client_id = make_client_id(client_id)

        vtype = first(value_type,
                      self.svc.get("value_type"))
        if vtype not in value_serdes:
            raise InvalidArgumentError()
        self.value_deserializer = first(value_deserializer, value_serdes[vtype][1])

        self.receive_timeout_ms = receive_timeout_ms
        self.kwargs = kwargs
        self.type = self.svc["type"]
        self._reader = self._find_reader(self.type)

    def _find_reader(self, service_type):
        reader_class = MessageReader.registry.get(service_type)
        return reader_class(self)

    def __enter__(self):
        logger.debug("MessageReader:enter")
        self._reader.open()
        return self

    def __exit__(self, ex_type, ex_value, trace):
        logger.debug("MessageReader:exit")
        self._reader.close()
        self._reader = None

    def __iter__(self):
        logger.debug("MessageReader:iter")
        return self._reader.__iter__()

    @property
    def client_id(self):
        return self._client_id

    def seek_to_beginning(self):
        self._reader.seek_to_beginning()

    def seek_to_end(self):
        self._reader.seek_to_end()


class MessageWriter(object):

    registry = Registry("sinetstream.writer")

    def usage():
        return ('MessageWriter(\n'
                '    service=SERVICE,              '
                '# Service name defined in the configuration file. (REQUIRED)\n'
                '    topics=TOPICS,                '
                '# The topic to receive.\n'
                '    consistency=AT_MOST_ONCE,     '
                '# consistency: AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE\n'
                '    client_id=DEFAULT_CLIENT_ID,  '
                '# If not specified, the value is automatically generated.\n'
                '    value_type=None,              '
                '# The type of message.\n'
                '    value_serializer=None         '
                '# If not specified, use default serializer according to valueType.\n'
                ')')

    def __init__(self, service, topic,
                 consistency=AT_MOST_ONCE,
                 client_id=DEFAULT_CLIENT_ID,
                 value_type=None,
                 value_serializer=None,
                 **kwargs):
        logger.debug("MessageWriter:init")

        check_consistency(consistency)

        config = load_config()
        self.svc = config.get(service)
        if self.svc is None:
            logger.error(f"invalid service: {service}")
            raise NoServiceError()

        self.topic = topic
        self.consistency = consistency
        self._client_id = make_client_id(client_id)

        vtype = first(value_type,
                      self.svc.get("value_type"))
        if vtype not in value_serdes:
            raise InvalidArgumentError()
        self.value_serializer = first(value_serializer, value_serdes[vtype][0])

        self.kwargs = kwargs
        self.type = self.svc["type"]
        self._writer = self._find_writer(self.type)

    def _find_writer(self, service_type):
        writer_class = MessageWriter.registry.get(service_type)
        return writer_class(self)

    def __enter__(self):
        logger.debug("MessageWriter:enter")
        self._writer.open()
        return self

    def __exit__(self, ex_type, ex_value, trace):
        logger.debug("MessageWriter:exit")
        self._writer.close()
        self._writer = None

    def publish(self, msg):
        logger.debug("MessageWriter:publish")
        self._writer.publish(msg)

    @property
    def client_id(self):
        return self._client_id
