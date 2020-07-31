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
from random import choices
from string import ascii_letters, digits
from threading import Condition

import pytest
from conftest import SERVICE

from sinetstream import (
    AsyncMessageReader, MessageWriter,
    AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE, TEXT,
)

logging.basicConfig(level=logging.CRITICAL)
pytestmark = pytest.mark.usefixtures('setup_config')


@pytest.mark.timeout(30)
@pytest.mark.parametrize("consistency", [
    AT_MOST_ONCE,
    AT_LEAST_ONCE,
    EXACTLY_ONCE,
])
def test_on_message(setup_messages, consistency, config_topics):
    def reader_test(topics, cons, messages, on_message, wait_on_messages):
        reader = AsyncMessageReader(
            SERVICE, config_topics, value_type=TEXT, consistency=consistency)
        reader.on_message = on_message
        reader.open()
        write_messages(messages, topics)
        wait_on_messages()
        reader.close()

    template_on_message(setup_messages, consistency, config_topics, reader_test)


@pytest.mark.timeout(30)
@pytest.mark.parametrize("consistency", [
    AT_MOST_ONCE,
    AT_LEAST_ONCE,
    EXACTLY_ONCE,
])
def test_on_message_with_statement(setup_messages, consistency, config_topics):
    def reader_test(topics, cons, messages, on_message, wait_on_messages):
        with AsyncMessageReader(
                SERVICE, topics, value_type=TEXT, consistency=consistency) as reader:
            reader.on_message = on_message
            write_messages(messages, topics)
            wait_on_messages()

    template_on_message(setup_messages, consistency, config_topics, reader_test)


def template_on_message(setup_messages, consistency, config_topics, reader_test):
    write_messages(['message-001'], config_topics)
    count = len(setup_messages)
    check = 0
    cv = Condition()

    def assert_messages(message):
        nonlocal count, check
        # with cv:
        count, check = check_message(
            message, setup_messages, config_topics, cv, count, check)

    def wait_on_messages():
        nonlocal count
        import time
        deadline = time.time() + 20
        with cv:
            while count > 0:
                if time.time() >= deadline:
                    raise Exception("TIMEOUT")
                cv.wait(1)

    reader_test(
        config_topics, consistency, setup_messages,
        assert_messages, wait_on_messages)
    assert check == len(setup_messages)


def check_message(message, messages, topics, cv, count, check):
    with cv:
        count -= 1
        assert message.value == messages[-(count + 1)]
        assert message.topic == topics
        check += 1
        cv.notify_all()
    return count, check


def write_messages(messages, topic):
    with MessageWriter(SERVICE, topic, value_type=TEXT, consistency=AT_LEAST_ONCE) as writer:
        for msg in messages:
            writer.publish(msg)


@pytest.fixture()
def setup_messages():
    return [str(x) + ''.join(choices(ascii_letters + digits, k=10)) for x in range(100)]


@pytest.fixture()
def config_topics():
    return ''.join(choices(ascii_letters, k=10))
