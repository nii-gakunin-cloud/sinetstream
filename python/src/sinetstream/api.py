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

import binascii
import logging
import os
from pkg_resources import iter_entry_points

from Crypto.Cipher import AES
from Crypto.Hash import SHA256, SHA384, SHA512
from Crypto.Protocol.KDF import PBKDF2
from Crypto.Random import get_random_bytes
from Crypto.Util.Padding import pad, unpad


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


def hexlify(x):
    if x is None:
        return "None"
    else:
        return binascii.hexlify(x).decode()


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


def convert_params(params):
    if "consistency" in params:
        x = params.get("consistency")
        params["consistency"] = {
                "AT_MOST_ONCE": AT_MOST_ONCE,
                "AT_LEAST_ONCE": AT_LEAST_ONCE,
                "EXACTLY_ONCE": EXACTLY_ONCE
            }.get(x, x)


def load_params(service):
    config = load_config()
    params = config.get(service)
    if params is None:
        logger.error(f"invalid service: {service}")
        raise NoServiceError()

    convert_params(params)
    validate_config(params)
    return params


param_list = {
    # param_name           type                 plugin
    "service":            ([str],               []),
    "brokers":            ([list],              []),
    "topic":              ([str],               []),
    "topics":             ([str, list],         []),
    "type":               ([str],               []),
    "consistency":        ([int, str],          []),
    "value_type":         ([str],               []),
    "value_serializer":   ([type(lambda x:x)],  []),
    "value_deserializer": ([type(lambda x:x)],  []),
    "receive_timeout_ms": ([int],               []),
    "tls":                ([dict],              []),
    "crypto":             ([dict],              []),
    "data_encryption":    ([bool],              []),
}


def del_sinetstream_param(d, plugin):
    ks = param_list.keys()
    d2 = {}
    d2 = {k: v for k, v in d.items() if (k not in ks) or (plugin in param_list[k][1])}
    return d2


class Message(object):
    def __init__(self, value, topic, raw):
        self._value = value
        self._topic = topic
        self._raw = raw

    @property
    def value(self):
        return self._value

    @property
    def topic(self):
        return self._topic

    @property
    def raw(self):
        return self._raw


def make_message(reader, value, topic, raw):
    assert type(value) == bytes
    if reader.cipher is not None:
        value = reader.cipher.decrypt(value)
    assert type(value) == bytes
    if reader.params["value_deserializer"] is not None:
        value = reader.params["value_deserializer"](value)
    return Message(value, topic, raw)


# crypto:
#     algorithm:  XXX (MUST) == AES
#     key_length: 128
#     mode:       XXX (MUST)
#     padding:    none
#     password:   XXX (MUST)
#     key_derivation:
#         algorithm:  XXX (MUST)
#         salt_bytes: 8
#         iteration:  10000
#         prf:        HMAC-SHA256


class CipherNull(object):
    def __init__(self):
        pass

    def encrypt(self, data):
        assert type(data) is bytes
        return b'X' + data

    def decrypt(self, cdata):
        assert type(cdata) is bytes
        return cdata[1:]


def get_password(crypto_params):
    password = crypto_params["password"]
    typ = type(password)
    if typ is str:
        return password
    elif typ is dict:
        value = password.get("value")
        path = password.get("path")
        if value is None and path is None:
            logger.error("neither value: nor path is specified")
            raise InvalidArgumentError()
        if value is not None and path is not None:
            logger.warning("both value: and path are specified; value is used")
        if value is not None:
            return value
        return open(path, "r").read()
    else:
        logger.error("password: must be str or dict")
        raise InvalidArgumentError()


def split_bytes(x, n):
    return x[:n], x[n:]


def strlst(xs):
    return ",".join(x for x in xs)


def conv_param(s, d, kwd):
    if s not in d:
        logger.error(f"invalid {kwd} is specified")
        logger.info(f"valid {kwd} are: {strlst(d)}")
        raise InvalidArgumentError()
    return d[s]


def make_key(crypto_params, salt=None):
    kd_params = crypto_params["key_derivation"]
    salt_bytes = kd_params["salt_bytes"]
    iteration = kd_params["iteration"]
    prf = kd_params["prf"]
    password = get_password(crypto_params)
    key_length = crypto_params["key_length"]

    hmac_hash_module = conv_param(prf,
                                  {
                                      "HMAC-SHA256": SHA256,
                                      "HMAC-SHA384": SHA384,
                                      "HMAC-SHA512": SHA512,
                                  },
                                  "crypto:key_derivation:prf")

    if kd_params["algorithm"] == "pbkdf2":
        if salt is None:
            salt = get_random_bytes(salt_bytes)
        else:
            assert len(salt) == salt_bytes
        key = PBKDF2(password,
                     salt,
                     dkLen=int(key_length/8),
                     count=iteration,
                     hmac_hash_module=hmac_hash_module)
    else:
        logger.error("invalid crypto:key_derivation:algorithm")
        raise InvalidArgumentError()

    logger.error(f"XXX:make_key: salt={hexlify(salt)}")
    logger.error(f"XXX:make_key: key={hexlify(key)}")
    assert len(key) == int(key_length/8)
    return key, salt


class CipherAES(object):
    def __init__(self, crypto_params):
        assert crypto_params["algorithm"] == "AES"

        # key_length = crypto_params["key_length"]

        mode = conv_param(crypto_params["mode"],
                          {
                              # "ECB": AES.MODE_ECB,
                              "CBC": AES.MODE_CBC,
                              "CFB": AES.MODE_CFB,
                              "OFB": AES.MODE_OFB,
                              "CTR": AES.MODE_CTR,
                              "OPENPGP": AES.MODE_OPENPGP,
                              "OPENPGPCFB": AES.MODE_OPENPGP,
                              # "CCM": AES.MODE_CCM,
                              "EAX": AES.MODE_EAX,
                              # "SIV": AES.MODE_SIV,
                              "GCM": AES.MODE_GCM,
                              # "OCB": AES.MODE_OCB,
                          },
                          "crypto:mode")

        mode_type = {
            AES.MODE_ECB: "",
            AES.MODE_CBC: "iv",
            AES.MODE_CFB: "iv",
            AES.MODE_OFB: "iv",
            # AES.MODE_CTR: "nonce",
            AES.MODE_CTR: "counter",
            AES.MODE_OPENPGP: "iv",
            AES.MODE_CCM: "nonce",
            AES.MODE_EAX: "nonce",
            AES.MODE_SIV: "nonce",
            AES.MODE_GCM: "nonce",
            AES.MODE_OCB: "nonce",
        }[mode]

        padding = conv_param(crypto_params["padding"],
                             {
                                 None: None,
                                 "none": None,
                                 "pkcs7": "pkcs7",
                             },
                             "crypto:padding")

        self.crypto_params = crypto_params
        self.mode = mode
        self.mode_type = mode_type
        self.aead = (crypto_params["mode"] in ["CCM", "EAX", "SIV", "GCM", "OCB"])
        self.mac_len = {AES.MODE_EAX: 8, AES.MODE_GCM: 16}.get(self.mode)  # XXX Bouncycastle
        self.padding = padding
        self.enc_key = None
        self.enc_salt = None
        self.counter = None     # for CTR
        self.dec_keys = {}

    def setup_enc(self):
        key, salt = make_key(self.crypto_params)
        self.enc_key = key
        self.enc_salt = salt

    def encrypt(self, data):
        assert type(data) is bytes

        if self.enc_key is None:
            self.setup_enc()

        if self.padding:
            data = pad(data, AES.block_size, style=self.padding)

        if self.mode_type == "counter":
            self.counter = get_random_bytes(AES.block_size)     # XXX
            enc_cipher = AES.new(self.enc_key, self.mode, nonce=b'', initial_value=self.counter)
        else:
            if self.aead:
                enc_cipher = AES.new(self.enc_key, self.mode, mac_len=self.mac_len)
            else:
                enc_cipher = AES.new(self.enc_key, self.mode)

        if self.mode_type == "nonce":
            if self.aead:
                enc_cipher.update(self.enc_salt)
                enc_cipher.update(enc_cipher.nonce)
                cdata, tag = enc_cipher.encrypt_and_digest(data)
                assert len(tag) == self.mac_len
            else:
                cdata = enc_cipher.encrypt(data)
                tag = b''
            ivnonce = enc_cipher.nonce
        elif self.mode_type == "iv":
            cdata = enc_cipher.encrypt(data)
            if self.mode == AES.MODE_OPENPGP:
                logger.error(f"XXX:enc: cdatalen={len(cdata)}")
                ivnonce, cdata = split_bytes(cdata, AES.block_size + 2)
            else:
                ivnonce = enc_cipher.iv
            tag = b''
        elif self.mode_type == "counter":
            cdata = enc_cipher.encrypt(data)
            ivnonce = self.counter
            tag = b''
        else:
            assert False
        logger.error(f"XXX:enc: salt={hexlify(self.enc_salt)}")
        logger.error(f"XXX:enc: ivnonce.len={len(ivnonce)}")
        logger.error(f"XXX:enc: ivnonce={hexlify(ivnonce)}")
        logger.error(f"XXX:enc: cdata={hexlify(cdata)}")
        logger.error(f"XXX:enc: tag={hexlify(tag)}")
        return self.enc_salt + ivnonce + cdata + tag

    def decrypt(self, cmsg):
        assert type(cmsg) is bytes
        salt_bytes = self.crypto_params["key_derivation"]["salt_bytes"]
        dummy_key = b"0123456789abcdef"         # XXX Crypto/Cipher/AES.py: key_size = (16, 24, 32)

        salt, rem = split_bytes(cmsg, salt_bytes)
        logger.error(f"XXX:dec: salt={hexlify(salt)}")
        if self.mode_type == "nonce":
            nonce_bytes = len(AES.new(dummy_key, self.mode).nonce)
            logger.error(f"XXX:dec: nonce_bytes={nonce_bytes}")
            nonce, rem = split_bytes(rem, nonce_bytes)
            logger.error(f"XXX:dec: nonce={hexlify(nonce)}")
        elif self.mode_type == "iv":
            iv_bytes = len(AES.new(dummy_key, self.mode).iv)
            if self.mode == AES.MODE_OPENPGP:
                iv_bytes += 2
            logger.error(f"XXX:dec: iv_bytes={iv_bytes}")
            iv, rem = split_bytes(rem, iv_bytes)
            logger.error(f"XXX:dec: iv={hexlify(iv)}")
        elif self.mode_type == "counter":
            counter, rem = split_bytes(rem, AES.block_size)
            logger.error(f"XXX:dec: counter={hexlify(counter)}")
        else:
            assert False
        if self.aead:
            # tag_size = AES.block_size
            tag_size = {AES.MODE_EAX: 8, AES.MODE_GCM: 16}[self.mode]    # XXX Java Bouncycastle?
            cdata, tag = split_bytes(rem, -tag_size)
        else:
            cdata = rem
            tag = None
        logger.error(f"XXX:dec: cdata={hexlify(cdata)}")
        logger.error(f"XXX:dec: tag={hexlify(tag)}")

        key = self.dec_keys.get(salt)
        if key is None:
            key, _salt = make_key(self.crypto_params, salt=salt)
            self.dec_keys[salt] = key

        if self.mode_type == "nonce":
            if self.aead:
                cipher = AES.new(key, self.mode, nonce=nonce, mac_len=self.mac_len)
            else:
                cipher = AES.new(key, self.mode, nonce=nonce)
        elif self.mode_type == "iv":
            cipher = AES.new(key, self.mode, iv=iv)
        elif self.mode_type == "counter":
            cipher = AES.new(key, self.mode, nonce=b'', initial_value=counter)
        else:
            assert False

        data = cipher.decrypt(cdata)

        if self.padding:
            logger.error(f"XXX:dec:padding={self.padding}")
            data = unpad(data, AES.block_size, style=self.padding)

        return data


def make_cipher(crypto_params):
    algorithm = crypto_params["algorithm"]
    if algorithm == "AES":
        return CipherAES(crypto_params)
    else:
        assert False


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


def make_client_id():
    import uuid
    return "sinetstream-" + str(uuid.uuid4())


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


def validate_config(params):
    pass        # XXX:FIXME


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


def merge_parameter(service, kwargs, default_params, role):
    svc_params = load_params(service)

    # Merge parameters
    # Priority:
    #  ctor's argument (highest)
    #  config file
    #  sinetstream's default parameter
    #  plugin's default parameter (lowest)
    params = default_params.copy()
    validate_config(params)
    deepupdate(params, svc_params)
    deepupdate(params, kwargs)
    validate_config(params)

    # derive: value_type -> value_serdes/value_deserializer
    vtype = params.get("value_type")
    vserdes = value_serdes.get(vtype)
    if vserdes is None:
        raise InvalidArgumentError()
    vser, vdes = vserdes
    if role == "reader":
        params.setdefault("value_deserializer", vdes)
    elif role == "writer":
        params.setdefault("value_serializer", vser)
    else:
        assert False

    normalize_params(params)

    return params


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


default_params = {
    "consistency": AT_MOST_ONCE,
    "client_id": DEFAULT_CLIENT_ID,
    "value_type": None,
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

    default_params = {
        **default_params,
        "receive_timeout_ms": float("inf"),
    }

    def __init__(self, service, topics, **kwargs):
        logger.debug("MessageReader:init")

        validate_config(kwargs)
        params = merge_parameter(service, kwargs, MessageReader.default_params, "reader")

        check_consistency(params["consistency"])  # XXX
        params["topics"] = topics

        self.kwargs = kwargs
        self.params = params

        self._reader = self._find_reader(params["type"])

        if params["data_encryption"]:
            self.cipher = make_cipher(params["crypto"])
        else:
            self.cipher = None

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
        return self.params["client_id"]

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
                '    topic=TOPIC,                '
                '# The topic to send.\n'
                '    consistency=AT_MOST_ONCE,     '
                '# consistency: AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE\n'
                '    client_id=DEFAULT_CLIENT_ID,  '
                '# If not specified, the value is automatically generated.\n'
                '    value_type=None,              '
                '# The type of message.\n'
                '    value_serializer=None         '
                '# If not specified, use default serializer according to valueType.\n'
                ')')

    default_params = {
        **default_params
    }

    def __init__(self, service, topic, **kwargs):
        logger.debug("MessageWriter:init")

        validate_config(kwargs)
        params = merge_parameter(service, kwargs, MessageWriter.default_params, "writer")

        check_consistency(params["consistency"])        # XXX
        params["topic"] = topic

        self.kwargs = kwargs
        self.params = params

        self._writer = self._find_writer(params["type"])

        if params["data_encryption"]:
            self.cipher = make_cipher(params["crypto"])
        else:
            self.cipher = None

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
        logger.debug(f"MessageWriter:publish:type(msg)={type(msg)}")
        if self.params["value_serializer"] is not None:
            msg = self.params["value_serializer"](msg)
        assert type(msg) == bytes
        if self.cipher:
            msg = self.cipher.encrypt(msg)
        assert type(msg) == bytes
        self._writer.publish(msg)

    @property
    def client_id(self):
        return self.params["client_id"]
