#!/usr/local/bin/python3.6
# vim: expandtab shiftwidth=4

# Copyright (C) 2020 National Institute of Informatics
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

from time import time

import pytest

from sinetstream import MessageReader, MessageWriter, AsyncMessageReader, AsyncMessageWriter
from sinetstream.api import Message


def test_usage_prog():
    pass


@pytest.mark.parametrize(
    "io", [MessageReader, MessageWriter, AsyncMessageReader, AsyncMessageWriter])
def test_usage(io):
    io.usage()


def test_message():
    raw = {
        'value': 'message',
        'topic': 'topic-1',
    }
    ts = int(time() * 1000_000)
    msg = Message(raw['value'], raw['topic'], ts, raw)
    assert msg.raw == raw
    assert msg.value == raw['value']
    assert msg.topic == raw['topic']
    assert msg.timestamp_us == ts
    assert msg.timestamp == ts / 1000_000.0
    assert repr(msg) is not None
