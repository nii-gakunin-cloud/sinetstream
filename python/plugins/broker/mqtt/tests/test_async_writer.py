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
    AT_LEAST_ONCE, TEXT,
    AsyncMessageWriter, AT_MOST_ONCE, EXACTLY_ONCE)

logging.basicConfig(level=logging.CRITICAL)
logger = logging.getLogger(__name__)
pytestmark = pytest.mark.usefixtures('setup_config')


@pytest.mark.timeout(60)
@pytest.mark.parametrize("consistency", [
    AT_MOST_ONCE,
    AT_LEAST_ONCE,
    EXACTLY_ONCE,
])
def test_write_message(setup_messages, consistency, config_topic):
    count = len(setup_messages)
    check = 0
    cv = Condition()

    def wait_on_messages():
        nonlocal count
        with cv:
            while count > 0:
                cv.wait()

    def on_success(r):
        nonlocal count, check
        with cv:
            count -= 1
            check += 1
            cv.notify_all()

    def on_failure(e):
        nonlocal count
        logger.error(f"failure: {e}")
        with cv:
            count -= 1
            cv.notify_all()

    with AsyncMessageWriter(
            service=SERVICE, topic=config_topic, value_type=TEXT,
            consistency=consistency) as writer:
        for msg in setup_messages:
            writer.publish(msg).then(on_success).catch(on_failure)
        wait_on_messages()
    assert check == len(setup_messages)


@pytest.fixture()
def setup_messages():
    return [str(x) + '-' + ''.join(choices(ascii_letters + digits, k=10)) for x in range(10000)]


@pytest.fixture()
def config_topic():
    return ''.join(choices(ascii_letters, k=10))
