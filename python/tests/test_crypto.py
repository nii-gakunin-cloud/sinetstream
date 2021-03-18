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
import time

import pytest
from conftest import SERVICE

from sinetstream import MessageReader, MessageWriter, AsyncMessageReader, TEXT, BYTE_ARRAY

logging.basicConfig(level=logging.ERROR)
pytestmark = pytest.mark.usefixtures('setup_config', 'dummy_reader_plugin', 'dummy_writer_plugin')


msgs = ['test message 001',
        'test message 002']

bmsgs = [x.encode() for x in msgs]


def test_thru(config_topic):
    with MessageWriter(SERVICE, value_type=TEXT) as fw:
        for msg in msgs:
            fw.publish(msg)
    with MessageReader(SERVICE, value_type=TEXT) as fr:
        for expected, msg in zip(msgs, fr):
            assert msg.topic == config_topic
            assert msg.value == expected


def test_enc_bin(config_topic):
    with MessageWriter(SERVICE, value_type=BYTE_ARRAY, data_encryption=True) as fw:
        for msg in bmsgs:
            fw.publish(msg)
    with MessageReader(SERVICE, value_type=BYTE_ARRAY, data_encryption=True) as fr:
        for expected, msg in zip(bmsgs, fr):
            assert msg.topic == config_topic
            assert msg.value == expected


def test_enc_text(config_topic):
    with MessageWriter(SERVICE, value_type=TEXT, data_encryption=True) as fw:
        for msg in msgs:
            fw.publish(msg)

    with MessageReader(SERVICE, value_type=TEXT, data_encryption=True) as fr:
        for expected, msg in zip(msgs, fr):
            assert msg.topic == config_topic
            assert msg.value == expected


def test_injection(config_topic):
    injected = []
    with MessageWriter(SERVICE, topic=config_topic+"-W", value_type=TEXT, data_encryption=True) as fw:
        fw.debug_last_msg_bytes = True
        for msg in msgs:
            fw.publish(msg)
            injected.append((msg, fw.debug_last_msg_bytes))

    with MessageReader(SERVICE, topic=config_topic+"-R", value_type=TEXT, data_encryption=True) as fr:
        for expected, b in injected:
            fr.debug_inject_msg_bytes = (b, config_topic, None)
            msg = next(fr)
            assert msg.topic == config_topic
            assert msg.value == expected


def test_injection_async(config_topic):
    injected = {}
    recved = []

    with MessageWriter(SERVICE, topic=config_topic+"-W", value_type=TEXT, data_encryption=True) as fw:
        fw.debug_last_msg_bytes = True
        for msg in msgs:
            fw.publish(msg)
            injected[msg] = fw.debug_last_msg_bytes

    def recv_msg(msg):
        recved.append(msg)
    with AsyncMessageReader(SERVICE, topic=config_topic+"-R", value_type=TEXT) as fr:
        fr.on_message = recv_msg
        for b in injected.values():
            fr.debug_inject_msg_bytes(b, config_topic+"-R", None)
    time.sleep(1)  # wait to receive.
    assert len(recved) == len(msgs)
    for rec in recved:
        assert rec.value in injected


@pytest.mark.parametrize(
    "crypto", [
        {"mode": "CBC",        "padding": "pkcs7"},
        {"mode": "CFB",        "padding": "none"},
        {"mode": "OFB",        "padding": "none"},
        {"mode": "CTR",        "padding": "none"},
        {"mode": "OPENPGP",    "padding": "none"},
        {"mode": "OPENPGPCFB", "padding": "none"},
        {"mode": "EAX",        "padding": "none"},
        {"mode": "GCM",        "padding": "none"},
    ])
def test_enc_mode(crypto, config_topic):
    with MessageWriter(SERVICE, value_type=TEXT, data_encryption=True, crypto=crypto) as fw:
        for msg in msgs:
            fw.publish(msg)
    with MessageReader(SERVICE, value_type=TEXT, data_encryption=True, crypto=crypto) as fr:
        for expected, msg in zip(msgs, fr):
            assert msg.topic == config_topic
            assert msg.value == expected


@pytest.fixture()
def config_params():
    return {
        'crypto': {
            'algorithm': 'AES',
            'key_length': 128,
            'mode': 'GCM',
            'padding': 'none',
            'key_derivation': {
                'algorithm': 'pbkdf2',
            },
            'salt_bytes': 16,
            'iteration': 10000,
            'password': {
                'value': 'secret-000',
            },
        },
        'data_encryption': True,
    }
