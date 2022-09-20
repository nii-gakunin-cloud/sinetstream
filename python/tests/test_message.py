#!/usr/bin/env python3

# Copyright (C) 2022 National Institute of Informatics
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

from sinetstream import MessageReader, MessageWriter, TEXT, BYTE_ARRAY
# from sinetstream.error import InvalidArgumentError

logging.basicConfig(level=logging.ERROR)
pytestmark = pytest.mark.usefixtures('setup_config', 'dummy_reader_plugin', 'dummy_writer_plugin')


msgs = ['test message 001',
        'test message 002']


def test_thru_11(config_topic):
    t1 = time.time()
    with MessageWriter(SERVICE, value_type=TEXT, user_data_only=True) as fw:
        for msg in msgs:
            fw.publish(msg)
    t2 = time.time()
    with MessageReader(SERVICE, value_type=TEXT, user_data_only=True) as fr:
        for expected, msg in zip(msgs, fr):
            assert msg.topic == config_topic
            assert msg.value == expected
            # assert msg.timestamp_us == msg.NOTSTAMP
            assert msg.timestamp >= t1 and msg.timestamp <= t2


def test_thru_00(config_topic):
    t1 = time.time()
    with MessageWriter(SERVICE, value_type=TEXT, user_data_only=False) as fw:
        for msg in msgs:
            fw.publish(msg)
    t2 = time.time()
    with MessageReader(SERVICE, value_type=TEXT, user_data_only=False) as fr:
        for expected, msg in zip(msgs, fr):
            assert msg.topic == config_topic
            assert msg.value == expected
            assert msg.timestamp >= t1 and msg.timestamp <= t2


def test_thru_01(config_topic):
    t1 = time.time()
    with MessageWriter(SERVICE, value_type=TEXT, user_data_only=False) as fw:
        for msg in msgs:
            fw.publish(msg)
    t2 = time.time()
    with MessageReader(SERVICE, value_type=BYTE_ARRAY, user_data_only=True) as fr:
        for expected, msg in zip(msgs, fr):
            assert msg.topic == config_topic
            assert msg.value != expected.encode()
            # assert msg.timestamp_us == 0
            assert msg.timestamp >= t1 and msg.timestamp <= t2
