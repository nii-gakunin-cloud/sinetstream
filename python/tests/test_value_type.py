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
from conftest import SERVICE

from sinetstream import MessageReader, MessageWriter, BYTE_ARRAY, TEXT, InvalidArgumentError

logging.basicConfig(level=logging.ERROR)
pytestmark = pytest.mark.usefixtures('setup_config', 'dummy_reader_plugin', 'dummy_writer_plugin')


bmsgs = [b'test message 001',
         b'test message 002']
tmsgs = ['test message 001',
         'test message 002']


def test_text(config_topic):
    with MessageWriter(SERVICE, value_type=TEXT) as fw:
        for msg in tmsgs:
            fw.publish(msg)
    with MessageReader(SERVICE, value_type=TEXT) as fr:
        for expected, msg in zip(tmsgs, fr):
            assert msg.topic == config_topic
            assert msg.value == expected


def test_byte_array(config_topic):
    with MessageWriter(SERVICE, value_type=BYTE_ARRAY) as fw:
        for msg in bmsgs:
            fw.publish(msg)
    with MessageReader(SERVICE, value_type=BYTE_ARRAY) as fr:
        for expected, msg in zip(bmsgs, fr):
            assert msg.topic == config_topic
            assert msg.value == expected


@pytest.mark.parametrize('io', [MessageReader, MessageWriter])
def test_invalid(io):
    with pytest.raises(InvalidArgumentError):
        with io(SERVICE, value_type="invalid") as _:
            pass
