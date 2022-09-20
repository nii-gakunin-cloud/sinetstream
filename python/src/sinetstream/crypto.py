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

import binascii
import logging

from Cryptodome.Cipher import AES
from Cryptodome.Hash import SHA256, SHA384, SHA512
from Cryptodome.Protocol.KDF import PBKDF2
from Cryptodome.Random import get_random_bytes
from Cryptodome.Util.Padding import pad, unpad

from .error import InvalidArgumentError

logger = logging.getLogger(__name__)


# crypto:
#     algorithm:  XXX (MUST) == AES
#     key_length: 128
#     mode:       XXX (MUST)
#     padding:    none
#     password:   XXX (MUST)
#     key_derivation:
#         algorithm:  XXX
#         salt_bytes: 8
#         iteration:  10000
#         prf:        HMAC-SHA256


def hexlify(x):
    if x is None:
        return "None"
    else:
        return binascii.hexlify(x).decode()


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


def conv_param(s, d, kwd):
    if s not in d:
        logger.error(f"invalid {kwd} is specified")
        logger.info(f"valid {kwd} are: {','.join(d.keys())}")
        raise InvalidArgumentError()
    return d[s]


def make_key(crypto_params, salt=None):
    key = crypto_params.get("key")
    key_length = crypto_params["key_length"]
    kd_params = crypto_params["key_derivation"]
    algorithm = kd_params["algorithm"]
    salt_bytes = kd_params["salt_bytes"]
    iteration = kd_params["iteration"]
    prf = kd_params["prf"]

    if salt is None:
        salt = get_random_bytes(salt_bytes)
    else:
        assert len(salt) == salt_bytes

    if key is not None:
        if crypto_params.get("password") is not None:
            raise InvalidArgumentError("only one of crypto.key or crypto.password can be specified.")
        if type(key) != bytes:
            raise InvalidArgumentError("crypto.key must be bytes")
        if len(key) * 8 != key_length:
            emsg = f"length of crypto.key (={len(key) * 8}) must be same as crypto.key_length(={key_length})"
            raise InvalidArgumentError(emsg)
    else:
        password = get_password(crypto_params)
        if algorithm == "pbkdf2" or algorithm == "PBKDF2WithHmacSHA256":
            hmac_hash_module = conv_param(prf,
                                          {
                                              "HMAC-SHA256": SHA256,
                                              "HMAC-SHA384": SHA384,
                                              "HMAC-SHA512": SHA512,
                                          },
                                          "crypto:key_derivation:prf")
            key = PBKDF2(password,
                         salt,
                         dkLen=int(key_length/8),
                         count=iteration,
                         hmac_hash_module=hmac_hash_module)
        else:
            logger.error("invalid crypto:key_derivation:algorithm")
            raise InvalidArgumentError()

    logger.debug(f"XXX:make_key: salt={hexlify(salt)}")
    logger.debug(f"XXX:make_key: key={hexlify(key)}")
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
                                 "NoPadding": None,
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
                logger.debug(f"XXX:enc: cdatalen={len(cdata)}")
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
        logger.debug(f"XXX:enc: enc_key={hexlify(self.enc_key)}")
        logger.debug(f"XXX:enc: salt={hexlify(self.enc_salt)}")
        logger.debug(f"XXX:enc: ivnonce.len={len(ivnonce)}")
        logger.debug(f"XXX:enc: ivnonce={hexlify(ivnonce)}")
        logger.debug(f"XXX:enc: cdata={hexlify(cdata)}")
        logger.debug(f"XXX:enc: tag={hexlify(tag)}")
        return self.enc_salt + ivnonce + cdata + tag

    def decrypt(self, cmsg):
        assert isinstance(cmsg, bytes)
        salt_bytes = self.crypto_params["key_derivation"]["salt_bytes"]
        dummy_key = b"0123456789abcdef"         # XXX Cryptodome/Cipher/AES.py: key_size = (16, 24, 32)

        salt, rem = split_bytes(cmsg, salt_bytes)
        logger.debug(f"XXX:dec: salt={hexlify(salt)}")
        if self.mode_type == "nonce":
            nonce_bytes = len(AES.new(dummy_key, self.mode).nonce)
            logger.debug(f"XXX:dec: nonce_bytes={nonce_bytes}")
            nonce, rem = split_bytes(rem, nonce_bytes)
            logger.debug(f"XXX:dec: nonce={hexlify(nonce)}")
        elif self.mode_type == "iv":
            iv_bytes = len(AES.new(dummy_key, self.mode).iv)
            if self.mode == AES.MODE_OPENPGP:
                iv_bytes += 2
            logger.debug(f"XXX:dec: iv_bytes={iv_bytes}")
            iv, rem = split_bytes(rem, iv_bytes)
            logger.debug(f"XXX:dec: iv={hexlify(iv)}")
        elif self.mode_type == "counter":
            counter, rem = split_bytes(rem, AES.block_size)
            logger.debug(f"XXX:dec: counter={hexlify(counter)}")
        else:
            assert False
        if self.aead:
            # tag_size = AES.block_size
            tag_size = {AES.MODE_EAX: 8, AES.MODE_GCM: 16}[self.mode]    # XXX Java Bouncycastle?
            cdata, tag = split_bytes(rem, -tag_size)
        else:
            cdata = rem
            tag = None
        logger.debug(f"XXX:dec: cdata={hexlify(cdata)}")
        logger.debug(f"XXX:dec: tag={hexlify(tag)}")

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
            logger.debug(f"XXX:dec:padding={self.padding}")
            data = unpad(data, AES.block_size, style=self.padding)

        return data


def make_cipher(crypto_params):
    algorithm = crypto_params["algorithm"]
    if algorithm == "AES":
        return CipherAES(crypto_params)
    else:
        assert False
