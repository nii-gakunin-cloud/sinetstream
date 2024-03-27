#!/usr/bin/env python3

# Copyright (C) 2023 National Institute of Informatics
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
# import time

import pytest
# from conftest import SERVICE

# from sinetstream import MessageReader, MessageWriter, AsyncMessageReader, TEXT, BYTE_ARRAY
from sinetstream import MessageReader, MessageWriter, TEXT
from sinetstream.error import InvalidArgumentError
from sinetstream.error import InvalidMessageError

logging.basicConfig(level=logging.ERROR)
pytestmark = pytest.mark.usefixtures('setup_config', 'dummy_reader_plugin', 'dummy_writer_plugin')


def dotest2(writer_params, reader_params):
    msgs = ['',
            'test message 001',
            'test message 002',
            'test message 003' * 100,
            'test message 004' * 1000,
            ]
    with MessageWriter(**writer_params) as w:
        for msg in msgs:
            w.publish(msg)
    with MessageReader(**reader_params) as r:
        for expected, msg in zip(msgs, r):
            assert msg.topic == writer_params["topic"]
            assert msg.value == expected


def dotest1(params):
    dotest2(params, params)


def base_config():
    from random import choices
    from string import ascii_letters
    topic = ''.join(choices(ascii_letters, k=10))
    return dict(no_config=True,
                type="dummy",
                value_type=TEXT,
                topic=topic)


# --- data_encryption=off


# just normal
def test_thru(config_topic):
    dotest1(base_config())


# just normal: 2->2, 3->3
@pytest.mark.parametrize("mfmt", [2, 3])
def test_message_format(config_topic, mfmt):
    dotest1(dict(base_config(), message_format=mfmt))


# reader fallback 2->3
@pytest.mark.parametrize("mfmt", [(2, 3)])
def test_message_format_fallback_ok(mfmt):
    config = base_config()
    dotest2(dict(config, message_format=mfmt[0]),
            dict(config, message_format=mfmt[1]))


# reader fail 3->2
@pytest.mark.parametrize("mfmt", [(3, 2)])
def test_message_format_fallback_ng(mfmt):
    config = base_config()
    with pytest.raises(InvalidMessageError,
                       match=r"Unmarshaller: marker="):
        dotest2(dict(config, message_format=mfmt[0]),
                dict(config, message_format=mfmt[1]))


# --- data_encryption=on

def genkey(key_length):
    import random
    return random.getrandbits(key_length).to_bytes(int(key_length / 8), 'big')


key_length = 256
# nkey = 3
# key = [genkey(key_length) for _ in range(nkey)]
# keys = [{i+1: key[i] for i in range(n + 1)} for n in range(nkey)]
key1 = genkey(key_length)
key2 = genkey(key_length)
key3 = genkey(key_length)
keys1 = {1: key1}
keys12 = {1: key1, 2: key2}
keys2 = {2: key2}
keys123 = {1: key1, 2: key2, 3: key3}
keys23 = {2: key2, 3: key3}


def base_enc_config(wkey=None, wkeys=None, rkey=None, rkeys=None):
    crypto = dict(algorithm="AES",
                  mode="GCM",
                  key_length=key_length)
    wcrypto = dict(crypto)
    if wkey is not None:
        wcrypto["key"] = wkey
    if wkeys is not None:
        wcrypto["_keys"] = wkeys
    rcrypto = dict(crypto)
    if rkey is not None:
        rcrypto["key"] = rkey
    if rkeys is not None:
        rcrypto["_keys"] = rkeys
    config = base_config()
    return (dict(config,
                 data_encryption=True,
                 crypto=wcrypto),
            dict(config,
                 data_encryption=True,
                 crypto=rcrypto))


def test_33_enc_k1_kn1():
    dotest2(*base_enc_config(wkeys=keys1, rkeys=keys1))


def test_33_enc_k1_k12():
    dotest2(*base_enc_config(wkeys=keys1, rkeys=keys12))


def test_33_enc_k1_k123():
    dotest2(*base_enc_config(wkeys=keys1, rkeys=keys123))


def test_33_enc_k1_k23():
    with pytest.raises(InvalidArgumentError,
                       match=r"not found"):
        dotest2(*base_enc_config(wkeys=keys1, rkeys=keys23))


def test_33_enc_k12_k1():
    with pytest.raises(InvalidArgumentError,
                       match=r"not found"):
        dotest2(*base_enc_config(wkeys=keys12, rkeys=keys1))


def test_33_enc_k12_k12():
    dotest2(*base_enc_config(wkeys=keys12, rkeys=keys12))


def test_33_enc_k12_k123():
    dotest2(*base_enc_config(wkeys=keys12, rkeys=keys123))


def test_33_enc_k12_k23():
    dotest2(*base_enc_config(wkeys=keys12, rkeys=keys23))


def test_33_enc_k2_k2():
    dotest2(*base_enc_config(wkeys=keys2, rkeys=keys2))


def test_22_enc_k2_k2():
    writer_params, reader_params = base_enc_config(wkey=key2, rkey=key2)
    writer_params["message_format"] = 2
    reader_params["message_format"] = 2
    dotest2(writer_params, reader_params)


def test_23_enc_k2_k1():
    writer_params, reader_params = base_enc_config(wkey=key2, rkeys=keys1)
    writer_params["message_format"] = 2
    with pytest.raises(InvalidMessageError,
                       match=r"Unmarshaller: marker="):
        dotest2(writer_params, reader_params)


def test_23_enc_k2_k2():
    writer_params, reader_params = base_enc_config(wkey=key2, rkeys=keys2)
    writer_params["message_format"] = 2
    dotest2(writer_params, reader_params)


def test_23_enc_k2_k12():
    writer_params, reader_params = base_enc_config(wkey=key2, rkeys=keys12)
    writer_params["message_format"] = 2
    dotest2(writer_params, reader_params)


def test_23_enc_k2_k123():
    writer_params, reader_params = base_enc_config(wkey=key2, rkeys=keys123)
    writer_params["message_format"] = 2
    with pytest.raises(InvalidMessageError,
                       match=r"Unmarshaller: marker="):
        dotest2(writer_params, reader_params)


def test_23_enc_k2_k23():
    writer_params, reader_params = base_enc_config(wkey=key2, rkeys=keys23)
    writer_params["message_format"] = 2
    with pytest.raises(InvalidMessageError,
                       match=r"Unmarshaller: marker="):
        dotest2(writer_params, reader_params)


# !!!TESTS!!!
# msgver={2,3}
# role={write,reader}
# enc={on,off}
# key={{1},{1,2},{1,2,3},{2,3}}
# keyver={1,2,3}
#
# udo={off,on} #user_data_only
#
#
# enc=off
#     writer=v3
#         reader=v3 -> OK
#         reader=v2 -> NG
#     writer=v2
#         reader=v2 -> OK
#         reader=v3 -> OK
#
# enc=on
#     writer=v3
#         key={1}
#             keyver=1
#                 reader=v3
#                     key={1} -> OK test_33_enc_k1_kn1
#                     key={1,2} -> OK test_33_enc_k1_k12
#                     key={1,2,3} -> OK test_33_enc_k1_k123
#                     key={2,3} -> NG test_33_enc_k1_k23
#         key={1,2}
#             keyver=2
#                 reader=v3
#                     key={1} -> NG test_33_enc_k12_k1
#                     key={1,2} -> OK test_33_enc_k12_k12
#                     key={1,2,3} -> OK test_33_enc_k12_k123
#                     key={2,3} -> OK test_33_enc_k12_k23

#         key={2}
#             keyver=2
#                 reader=v3
#                     key={2} -> OK test_33_enc_k2_k2

#     writer=v2
#         key=2
#             reader=v2
#                 key=2 -> OK test_22_enc_k2_k2
#             reader=v3
#                 key={1} -> NG test_23_enc_k2_k1
#                 key={2} -> OK test_23_enc_k2_k2
#                 key={1,2} -> OK test_23_enc_k2_k12
#                 key={1,2,3} -> NG test_23_enc_k2_k123
#                 key={2,3} -> NG test_23_enc_k2_k23
#
