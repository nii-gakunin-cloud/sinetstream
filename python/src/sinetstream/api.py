#!/usr/bin/env python3

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

import copy
import logging
import time
from concurrent.futures import ThreadPoolExecutor
from enum import Enum, auto
from threading import Lock, RLock
from os import unlink
from tempfile import NamedTemporaryFile

from sinetstream.configs import get_config_params
from sinetstream.crypto import CipherAES
from sinetstream.error import (
    InvalidArgumentError,
    UnsupportedServiceTypeError, InvalidMessageError, AlreadyConnectedError)
from sinetstream.marshal import Marshaller, Unmarshaller
from sinetstream.spi import (
    PluginMessageReader, PluginMessageWriter,
    PluginAsyncMessageReader, PluginAsyncMessageWriter,
    PluginMessage,
)
from sinetstream.utils import Registry
from sinetstream.value_type import BYTE_ARRAY, value_type_registry
from sinetstream.compression import compression_registry

logger = logging.getLogger(__name__)


class Consistency(Enum):
    AT_MOST_ONCE = auto()
    AT_LEAST_ONCE = auto()
    EXACTLY_ONCE = auto()


AT_MOST_ONCE = Consistency.AT_MOST_ONCE
AT_LEAST_ONCE = Consistency.AT_LEAST_ONCE
EXACTLY_ONCE = Consistency.EXACTLY_ONCE

DEFAULT_CLIENT_ID = None


def convert_params(params):
    consistency = params.get("consistency")
    if consistency is None:
        return params
    new_params = copy.copy(params)
    try:
        new_params["consistency"] = (
            consistency if isinstance(consistency, Consistency)
            else Consistency[consistency])
        return new_params
    except KeyError:
        raise InvalidArgumentError(f'invalid consistency: {consistency}')


class Message(object):
    NOTSTAMP = 0

    def __init__(self, value, topic, tstamp, raw):
        self._value = value
        self._topic = topic
        self._tstamp = tstamp
        self._raw = raw

    def __repr__(self):
        return (
                f'Message(value={self._value!r}, topic={self._topic!r}, ' +
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
    ("padding",        False, "none", [None, "none", NoPadding", "pkcs7"]),
    ("key_derivation", False, key_derivation_params,   None),
    ("password",       True,  None,   None),
}

key_derivation_params = {
    # name             must   default available
    ("algorithm",      False, "pbkdf2",      ["pbkdf2"]),
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
    pass


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


def merge_parameter(service, kwargs, default_values, config=None, config_file=None):
    service, raw_params = get_config_params(service, config, config_file)
    svc_params = convert_params(raw_params)
    # Merge parameters
    # Priority:
    #  ctor's argument (highest)
    #  config file
    #  sinetstream's default parameter
    #  plugin's default parameter (lowest)
    params = copy.deepcopy(default_values)
    deepupdate(params, svc_params)
    deepupdate(params, convert_params(kwargs))
    normalize_params(params)
    return service, params


default_params = {
    "consistency": AT_MOST_ONCE,
    "client_id": DEFAULT_CLIENT_ID,
    "value_type": {"name": BYTE_ARRAY},
    "compression": {
        "algorithm": "gzip",
    },
    "data_compression": False,
    "crypto": {
        "key_length": 128,
        "padding": None,
        "key_derivation": {
            "algorithm": "pbkdf2",
            "salt_bytes": 8,
            "iteration": 10000,
            "prf": "HMAC-SHA256",
        },
    },
    "data_encryption": False,
}


class Metrics(object):
    def __init__(self):
        pass
        # self.start_time
        # self.end_time
        # self.msg_count_total
        # self.msg_uncompressed_bytes_total
        # self.msg_compressed_bytes_total
        # self.msg_bytes_total
        # self.msg_size_min
        # self.msg_size_max
        # self.error_count_total

    @property
    def start_time_ms(self):
        return self.start_time * 1000

    @property
    def end_time_ms(self):
        return self.end_time * 1000

    @property
    def time(self):
        return self.end_time - self.start_time

    @property
    def time_ms(self):
        return self.time * 1000

    @property
    def msg_count_rate(self):
        t = self.time
        return self.msg_count_total / t if t != 0 else 0

    @property
    def msg_compression_ratio(self):
        u = self.msg_uncompressed_bytes_total
        c = self.msg_compressed_bytes_total
        return c / u if u > 0 else 1.0

    @property
    def msg_bytes_rate(self):
        t = self.time
        return self.msg_bytes_total / t if t != 0 else 0

    @property
    def msg_size_avg(self):
        c = self.msg_count_total
        return self.msg_bytes_total / c if c != 0 else 0

    @property
    def error_count_rate(self):
        t = self.time
        return self.error_count_total / t if t != 0 else 0

    def __str__(self):
        return (f"time={self.time},"
                f"start_time={self.start_time},"
                f"end_time={self.end_time},"
                f"msg_count_total={self.msg_count_total},"
                f"msg_count_rate={self.msg_count_rate},"
                f"msg_uncompressed_bytes_total={self.msg_uncompressed_bytes_total},"
                f"msg_compressed_bytes_total={self.msg_compressed_bytes_total},"
                f"msg_compression_ratio={self.msg_compression_ratio},"
                f"msg_bytes_total={self.msg_bytes_total},"
                f"msg_bytes_rate={self.msg_bytes_rate},"
                f"msg_size_min={self.msg_size_min},"
                f"msg_size_max={self.msg_size_max},"
                f"msg_size_avg={self.msg_size_avg},"
                f"error_count_total={self.error_count_total},"
                f"error_count_rate={self.error_count_rate}")


class IOMetrics(object):
    MAXSIZE = (1 << 63) - 1

    def __init__(self):
        self._lock = Lock()
        self._metrics = Metrics()
        self.reset()

    def reset(self):
        with self._lock:
            self._metrics.start_time = time.time()
            self._metrics.end_time = self._metrics.start_time  # This is most likely unnecessary.
            self._metrics.msg_count_total = 0
            self._metrics.msg_uncompressed_bytes_total = 0
            self._metrics.msg_compressed_bytes_total = 0
            self._metrics.msg_bytes_total = 0
            self._metrics.msg_size_min = IOMetrics.MAXSIZE
            self._metrics.msg_size_max = -1
            self._metrics.error_count_total = 0

    def update(self, length, compressed_length, uncompressed_length):
        with self._lock:
            self._metrics.end_time = time.time()
            self._metrics.msg_count_total += 1
            self._metrics.msg_uncompressed_bytes_total += uncompressed_length
            self._metrics.msg_compressed_bytes_total += compressed_length
            self._metrics.msg_bytes_total += length
            self._metrics.msg_size_min = min(length, self._metrics.msg_size_min)
            self._metrics.msg_size_max = max(length, self._metrics.msg_size_max)

    def update_err(self):
        with self._lock:
            self._metrics.end_time = time.time()
            self._metrics.error_count_total += 1

    def get_metrics(self):
        with self._lock:
            r = copy.copy(self._metrics)
        if r.msg_size_min == IOMetrics.MAXSIZE:
            r.msg_size_min = None
        if r.msg_size_max == -1:
            r.msg_size_max = None
        return r


class MessageIO(object):

    def __init__(self, service, params, registry):
        validate_config(params)
        self._service = service
        self.params = params
        self._tmpfile_list = []
        self._convert_inline_data(self.params)
        self._plugin = self._find_plugin(registry, params["type"])
        self.cipher = make_cipher(params["crypto"]) if params["data_encryption"] else None
        self._opened = False
        self.iometrics = IOMetrics()
        self._lock = RLock()

    def __del__(self):
        self._cleanup_tmpfile()

    def _convert_inline_data(self, x):
        _data = "_data"
        if isinstance(x, dict):
            x2 = {}
            for k, v in x.items():
                if isinstance(v, dict):
                    self._convert_inline_data(v)
                else:
                    if k.endswith(_data):
                        k2 = k[0:-len(_data)]
                        with NamedTemporaryFile(mode=("wt" if isinstance(v, str) else "wb"),
                                                prefix="sinetstream-",
                                                suffix=".tmp",
                                                delete=False) as f:
                            fn = f.name
                            self._tmpfile_list.append(fn)
                            logger.debug(f"{k} is written to {fn}")
                            f.write(v)
                        x2[k2] = fn
                        logger.debug(f"{k2}={fn}")
            x.update(x2)
        elif isinstance(x, list):
            for e in x:
                self._convert_inline_data(e)
        else:
            pass

    def _cleanup_tmpfile(self):
        if "_tmpfile_list" in dir(self):
            for fn in self._tmpfile_list:
                logger.debug(f"unlink {fn}")
                unlink(fn)
            self._tmpfile_list = []

    def _find_plugin(self, registry, service_type):
        plugin_class = registry.get(service_type)
        if plugin_class is None:
            raise UnsupportedServiceTypeError(f"{service_type} not found")
        return plugin_class(self.params)

    def __enter__(self):
        logger.debug("MessageIO:enter")
        return self.open()

    def open(self):
        logger.debug("MessageIO:open")
        with self._lock:
            if self._opened:
                logger.error("already connected")
                raise AlreadyConnectedError()
            self._plugin.open()
            self._opened = True
            return self

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

    def _get_value_type_args(self, params):
        if isinstance(params, dict) and "args" in params:
            return params["args"]
        if hasattr(params, "args"):
            return getattr(params, "args")
        return {}

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
        vtype = self.params["value_type"]
        if isinstance(vtype, dict):
            return vtype["name"]
        if hasattr(vtype, "name"):
            return getattr(vtype, "name")
        return vtype

    @property
    def metrics(self):
        metrics = self.iometrics.get_metrics()
        metrics.raw = self._plugin.metrics() if self._plugin else None
        return metrics

    def reset_metrics(self, reset_raw=False):
        self.iometrics = IOMetrics()
        if reset_raw:
            if self._plugin is not None:
                self._plugin.reset_metrics()

    def info(self, name="", **kwargs):
        ipath = name.split(".")
        if ipath[-1] == "":
            ipath = ipath[:-1]

        lst = {
            "client_id": lambda *_: self.client_id,
            "metrics": lambda *_: self.metrics,
            self.params["type"]: lambda ipath, kwargs: self._plugin.info(ipath, kwargs),
        }

        return MessageIO._fill_info(ipath, kwargs, lst)

    def _fill_info(ipath, kwargs, lst):
        if len(ipath) == 0:
            return {k: f(ipath, kwargs) for k, f in lst.items()}
        else:
            f = lst.get(ipath[0], None)
            if f is None:
                return None
            return f(ipath[1:], kwargs)


class BaseMessageReader(MessageIO):
    default_params = {
        **default_params,
        "receive_timeout_ms": float("inf"),
    }

    def __init__(self, registry, service, topics=None, config=None, config_file=None, **kwargs):
        service, params = merge_parameter(service,
                                          kwargs,
                                          BaseMessageReader.default_params,
                                          config=config,
                                          config_file=config_file)
        params["topics"] = _setup_topics(params, topics)
        super().__init__(service, params, registry)
        self.unmarshaller = Unmarshaller() if not params.get("user_data_only") else None
        self.value_deserializer = self._setup_deserializer()
        self.decompressor = self._setup_decompressor()

    @property
    def topics(self):
        return copy.copy(self.params["topics"])

    @property
    def receive_timeout_ms(self):
        return self.params["receive_timeout_ms"]

    def _make_message(self, value, topic, raw):
        value_tstamp = value.timestamp if hasattr(value, "timestamp") else 0
        mlen = len(value)
        assert isinstance(value, bytes)
        if self.cipher is not None:
            value = self.cipher.decrypt(value)
        assert isinstance(value, bytes)
        if self.unmarshaller:
            tstamp, value = self.unmarshaller.unmarshal(value)
        else:
            tstamp = value_tstamp
        assert type(tstamp) == int
        assert isinstance(value, bytes)
        clen = len(value)
        value = self.decompressor(value)
        assert isinstance(value, bytes)
        ulen = len(value)
        self.iometrics.update(mlen, clen, ulen)
        if self.value_deserializer is not None:
            value = self.value_deserializer(value)
        return Message(value, topic, tstamp, raw)

    def _setup_deserializer(self):
        if 'value_deserializer' in self.params:
            return self.params['value_deserializer']
        value_type = self.params['value_type']
        value_type_class = value_type_registry.get(value_type)
        if value_type_class is None:
            raise InvalidArgumentError(f'invalid value_type: {value_type}')
        args = self._get_value_type_args(value_type)
        return value_type_class(**args).deserializer

    def _setup_decompressor(self):
        if not self.params.get("data_compression", False):
            return lambda x: x
        params = self.params.get("compression", {})
        algorithm = params["algorithm"]
        compression_class = compression_registry.get(algorithm)
        if compression_class is None:
            raise InvalidArgumentError(f"invalid compression algorithm: {algorithm}")
        return compression_class(level=params.get("level"),
                                 params=params.get(algorithm, {})).decompressor

    def seek_to_beginning(self):
        self._plugin.seek_to_beginning()

    def seek_to_end(self):
        self._plugin.seek_to_end()


READER_USAGE = '''MessageReader(
    service=SERVICE,                 # Service name defined in the configuration file. (REQUIRED)
    topics=TOPICS,                   # The topic to receive.
    config=CONFIG,                   # Config name on the config-server.
    consistency=AT_MOST_ONCE,        # consistency: AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE
    client_id=DEFAULT_CLIENT_ID,     # If not specified, the value is automatically generated.
    value_type=BYTE_ARRAY,           # The type of message.
    value_deserializer=None          # If not specified, use default deserializer according to valueType.
)'''

ASYNC_READER_USAGE = '''AsyncMessageReader(
    service=SERVICE,                 # Service name defined in the configuration file. (REQUIRED)
    topics=TOPICS,                   # The topic to receive.
    config=CONFIG,                   # Config name on the config-server.
    consistency=AT_MOST_ONCE,        # consistency: AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE
    client_id=DEFAULT_CLIENT_ID,     # If not specified, the value is automatically generated.
    value_type=BYTE_ARRAY,           # The type of message.
    value_deserializer=None          # If not specified, use default deserializer according to valueType.
)'''


class MessageReader(BaseMessageReader):
    registry = Registry("sinetstream.reader", PluginMessageReader)

    @staticmethod
    def usage():
        return READER_USAGE

    def __init__(self, service, topics=None, config=None, config_file=None, **kwargs):
        logger.debug("MessageReader:init")
        super().__init__(MessageReader.registry, service, topics, config, config_file, **kwargs)
        self.debug_inject_msg_bytes = None  # for injection: None or tuple (message, topic, raw)

    def __iter__(self):
        logger.debug("MessageReader:iter")
        self._iter = self._plugin.__iter__()
        return self

    def __next__(self):
        if self.debug_inject_msg_bytes is not None:
            msg_bytes = self.debug_inject_msg_bytes
            self.debug_inject_msg_bytes = None
            return self._make_message(*msg_bytes)
        try:
            message, topic, raw = next(self._iter)
            return self._make_message(message, topic, raw)
        except Exception:
            self.iometrics.update_err()
            raise


WRITER_USAGE = '''MessageWriter(
    service=SERVICE,                 # Service name defined in the configuration file. (REQUIRED)
    topic=TOPIC,                     # The topic to send.
    config=CONFIG,                   # Config name on the config-server.
    consistency=AT_MOST_ONCE,        # consistency: AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE
    client_id=DEFAULT_CLIENT_ID,     # If not specified, the value is automatically generated.
    value_type=BYTE_ARRAY,           # The type of message.
    value_serializer=None            # If not specified, use default serializer according to valueType.
)'''

ASYNC_WRITER_USAGE = '''AsyncMessageWriter(
    service=SERVICE,                 # Service name defined in the configuration file. (REQUIRED)
    topic=TOPIC,                     # The topic to send.
    config=CONFIG,                   # Config name on the config-server.
    consistency=AT_MOST_ONCE,        # consistency: AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE
    client_id=DEFAULT_CLIENT_ID,     # If not specified, the value is automatically generated.
    value_type=BYTE_ARRAY,           # The type of message.
    value_serializer=None            # If not specified, use default serializer according to valueType.
)'''


class BaseMessageWriter(MessageIO):
    default_params = {
        **default_params
    }

    def __init__(self, registry, service, topic=None, config=None, config_file=None, **kwargs):
        logger.debug("MessageWriter:init")
        service, params = merge_parameter(service,
                                          kwargs,
                                          MessageWriter.default_params,
                                          config=config,
                                          config_file=config_file)
        params["topic"] = _setup_topic(params, topic)
        super().__init__(service, params, registry)
        self.marshaller = Marshaller() if not params.get("user_data_only") else None
        self.value_serializer = self._setup_serializer()
        self.compressor = self._setup_compressor()
        self.debug_last_msg_bytes = None  # for inspection

    def _publish(self, msg):
        tstamp = int(time.time() * 1000_000)
        msg_bytes = PluginMessage(self._to_bytes(msg, tstamp))
        msg_bytes.set_timestamp(tstamp)  # for s3-broker
        if self.debug_last_msg_bytes is not None:
            self.debug_last_msg_bytes = msg_bytes
            return True
        return self._plugin.publish(msg_bytes)

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
        args = self._get_value_type_args(value_type)
        return value_type_class(**args).serializer

    def _setup_compressor(self):
        if not self.params.get("data_compression", False):
            return lambda x: x
        params = self.params.get("compression", {})
        algorithm = params["algorithm"]
        compression_class = compression_registry.get(algorithm)
        if compression_class is None:
            raise InvalidArgumentError(f"invalid compression algorithm: {algorithm}")
        return compression_class(level=params.get("level"),
                                 params=params.get(algorithm, {})).compressor

    def _invalid_message(self, msg_type):
        if self.value_serializer is not None:
            logger.error(f"value_serializer must return byte: type(msg)={msg_type}")
            return InvalidMessageError("value_serializer doesn't return bytes")
        else:
            logger.error(f"value_serializer must be specified: type(msg)={msg_type}")
            return InvalidMessageError("non-bytes message is passed to publish")

    def _to_bytes(self, msg, tstamp):
        if self.value_serializer is not None:
            msg = self.value_serializer(msg)
        if type(msg) != bytes:
            raise self._invalid_message(type(msg))
        ulen = len(msg)
        msg = self.compressor(msg)
        clen = len(msg)
        assert type(msg) == bytes
        if self.marshaller:
            msg = self.marshaller.marshal(msg, tstamp)
        if self.cipher:
            msg = self.cipher.encrypt(msg)
        assert type(msg) == bytes
        mlen = len(msg)
        self.iometrics.update(mlen, clen, ulen)
        return msg


class MessageWriter(BaseMessageWriter):
    registry = Registry("sinetstream.writer", PluginMessageWriter)

    @staticmethod
    def usage():
        return WRITER_USAGE

    def __init__(self, service, topic=None, config=None, config_file=None, **kwargs):
        super().__init__(MessageWriter.registry, service, topic, config, config_file, **kwargs)

    def publish(self, msg):
        try:
            return super()._publish(msg)
        except Exception:
            self.iometrics.update_err()
            raise


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


class AsyncMessageWriter(BaseMessageWriter):
    registry = Registry("sinetstream.async_writer", PluginAsyncMessageWriter)

    @staticmethod
    def usage():
        return ASYNC_WRITER_USAGE

    def __init__(self, service, topic=None, config=None, config_file=None, **kwargs):
        super().__init__(AsyncMessageWriter.registry, service, topic, config, config_file, **kwargs)

    def _err(self, e):
        self.iometrics.update_err()
        raise e

    def publish(self, msg):
        promise = super()._publish(msg)
        return promise.catch(lambda e: self._err(e))


class AsyncMessageReader(BaseMessageReader):
    registry = Registry("sinetstream.async_reader", PluginAsyncMessageReader)

    @staticmethod
    def usage():
        return ASYNC_READER_USAGE

    def __init__(self, service, topics=None, config=None, config_file=None, **kwargs):
        logger.debug("AsyncMessageReader:init")
        super().__init__(AsyncMessageReader.registry, service, topics, config, config_file, **kwargs)
        self._executor = None
        self._on_message = None
        self._on_failure = None

    def open(self):
        ret = super().open()
        self._executor = ThreadPoolExecutor(max_workers=1)
        return ret

    def close(self):
        super().close()
        if self._executor is not None:
            self._executor.shutdown()
        self._executor = None

    @property
    def on_message(self):
        return self._on_message

    @on_message.setter
    def on_message(self, on_message):
        self._on_message = on_message

        def callback(value, topic, raw):
            self._on_message(self._make_message(value, topic, raw))

        self._plugin.on_message = callback

    @property
    def on_failure(self):
        return self._on_failure

    @on_failure.setter
    def on_failure(self, on_failure):
        self._on_failure = on_failure

        def callback(e, traceback=None):
            self.iometrics.update_err()
            self._on_failure(e, traceback)

        self._plugin.on_failure = callback

    def debug_inject_msg_bytes(self, value, topic, raw):
        self._on_message(self._make_message(value, topic, raw))
