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

import logging
from random import choices
from string import ascii_letters, digits
from threading import Condition

import pytest
from conftest import SERVICE, TOPIC

from sinetstream import (
    MessageWriter, AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE,
    TEXT, AsyncMessageReader)

logging.basicConfig(level=logging.CRITICAL)
pytestmark = pytest.mark.usefixtures('setup_config')


@pytest.mark.parametrize("consistency", [
    AT_MOST_ONCE,
    AT_LEAST_ONCE,
    EXACTLY_ONCE,
])
def test_on_message(setup_messages, consistency):
    count = len(setup_messages)
    cv = Condition()

    def assert_messages(message):
        nonlocal count
        expected = setup_messages[len(setup_messages) - count]
        assert message.topic == TOPIC
        assert message.value == expected
        with cv:
            count -= 1
            cv.notify_all()

    with AsyncMessageReader(SERVICE, value_type=TEXT, consistency=consistency) as reader:
        reader.on_message = assert_messages
        write_messages(setup_messages, consistency)
        with cv:
            while count > 0:
                cv.wait()


def write_messages(messages, consistency):
    with MessageWriter(SERVICE, value_type=TEXT, consistency=consistency) as writer:
        for msg in messages:
            writer.publish(msg)


@pytest.fixture()
def setup_messages():
    return [''.join(choices(ascii_letters + digits, k=10)) for _ in range(100)]
