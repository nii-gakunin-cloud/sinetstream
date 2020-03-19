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
import pytest

from sinetstream import (
    MessageReader, MessageWriter, TEXT, InvalidMessageError,
)
from sinetstream.api import Message
from sinetstream.marshal import (
    avro_signle_object_format_marker,
    message_schema_fingerprint,
)

from conftest import SERVICE, TOPIC, qedit


logging.basicConfig(level=logging.ERROR)
pytestmark = pytest.mark.usefixtures(
    'setup_config', 'dummy_reader_plugin', 'dummy_writer_plugin',
)


msgs = ['test message 001',
        'test message 002',
        'test message 003']
bmsgs = [b'test message 001\x01',
         b'test message 002\x02',
         b'\x00\xff']


def test_timestamp_us():
    us = 12345678
    msg = Message(None, None, us, None)
    assert msg.timestamp_us == us
    assert msg.timestamp * 1000000 == us


crypto_params = {
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
        'value_type': TEXT,
    }


@pytest.mark.parametrize("config_params", [crypto_params])
def test_timestamp():
    with MessageWriter(SERVICE) as fw:
        for msg in msgs:
            fw.publish(msg)

    with MessageReader(SERVICE) as fr:
        for msg, orig in zip(fr, msgs):
            assert msg.topic == TOPIC
            assert msg.value == orig
            assert msg.timestamp != 0


HDRLEN = 10  # = 2-bytes marker and 8-bytes fingerprint
TSLEN = 8   # position HDRLEN+TSLEN is the place at length of msg


def test_header_size():
    assert len(avro_signle_object_format_marker) == 2
    assert len(message_schema_fingerprint) == 8


@pytest.mark.parametrize("pos", list(range(HDRLEN)) + [HDRLEN+TSLEN])
def test_brokenbit(pos, write_messages):
    break_message_one_byte(pos)

    with MessageReader(SERVICE) as fr:
        with pytest.raises(InvalidMessageError):
            for msg in fr:
                pass


@pytest.mark.parametrize("length", range(HDRLEN + TSLEN + 3))
def test_tooshort(length, write_messages):
    chop_message(length)

    with MessageReader(SERVICE) as fr:
        with pytest.raises(InvalidMessageError):
            for msg in fr:
                pass


def test_toolong(write_messages):
    expand_message()

    # trailing garbage should be ignored.
    with MessageReader(SERVICE) as fr:
        for msg, orig in zip(fr, bmsgs):
            assert msg.topic == TOPIC
            assert msg.value == orig
            assert msg.timestamp != 0


def break_message_one_byte(pos, topic=TOPIC):
    q = qedit(topic, None)
    m = q[0]
    assert type(m) == bytes
    x = bytearray(m)
    x[pos] = x[pos] ^ 0xff  # BREAK
    q[0] = bytes(x)
    qedit(topic, q)


def chop_message(length, topic=TOPIC):
    q = qedit(topic, None)
    q[0] = q[0][0:length]
    qedit(topic, q)


def expand_message(topic=TOPIC):
    q = qedit(topic, None)
    q[0] = q[0] + b"G"  # GOMI
    qedit(topic, q)


@pytest.fixture
def write_messages(setup_config):
    with MessageWriter(SERVICE) as fw:
        for msg in bmsgs:
            fw.publish(msg)
