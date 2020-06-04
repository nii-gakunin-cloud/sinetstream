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

import logging
from threading import Condition

import pytest
from conftest import (
    SERVICE, USER_PASSWD_BROKER, KAFKA_READ_USER, KAFKA_READ_PASSWD,
    KAFKA_WRITE_USER, KAFKA_WRITE_PASSWD,
)

from sinetstream import (
    MessageReader, MessageWriter, AuthorizationError,
    AT_LEAST_ONCE, AT_MOST_ONCE,
    AsyncMessageWriter, AsyncMessageReader)

logging.basicConfig(level=logging.CRITICAL)
logger = logging.getLogger(__name__)
pytestmark = pytest.mark.skipif(
    USER_PASSWD_BROKER is None, reason='KAFKA_USER_PASSWD_BROKER is not set.')


@pytest.mark.skipif(
    KAFKA_READ_USER is None or KAFKA_READ_PASSWD is None,
    reason='USER/PASSWD is not set.')
def test_no_auth_write(setup_config):
    params = {
        'sasl_plain_username': KAFKA_READ_USER,
        'sasl_plain_password': KAFKA_READ_PASSWD,
    }
    with pytest.raises(AuthorizationError):
        with MessageWriter(SERVICE, consistency=AT_LEAST_ONCE, **params) as f:
            f.publish(b'message-001')


@pytest.mark.skipif(
    KAFKA_READ_USER is None or KAFKA_READ_PASSWD is None,
    reason='USER/PASSWD is not set.')
def test_no_auth_write_no_ack(setup_config):
    params = {
        'sasl_plain_username': KAFKA_READ_USER,
        'sasl_plain_password': KAFKA_READ_PASSWD,
    }
    with MessageWriter(SERVICE, consistency=AT_MOST_ONCE, **params) as f:
        f.publish(b'message-001')


@pytest.mark.skipif(
    KAFKA_READ_USER is None or KAFKA_READ_PASSWD is None,
    reason='USER/PASSWD is not set.')
def test_no_auth_async_write(setup_config):
    params = {
        'sasl_plain_username': KAFKA_READ_USER,
        'sasl_plain_password': KAFKA_READ_PASSWD,
    }
    error = 0
    countdown = 1
    cv = Condition()

    def on_error(e):
        nonlocal countdown, error
        with cv:
            countdown -= 1
            if isinstance(e, AuthorizationError):
                error += 1
            cv.notify_all()

    def on_success(_):
        nonlocal countdown
        with cv:
            countdown -= 1
            cv.notify_all()

    def wait_done():
        with cv:
            while countdown > 0:
                cv.wait()

    with AsyncMessageWriter(SERVICE, consistency=AT_LEAST_ONCE, **params) as f:
        f.publish(b'message-001').catch(on_error).then(on_success)
        wait_done()
    assert error == 1


@pytest.mark.skipif(
    KAFKA_READ_USER is None or KAFKA_READ_PASSWD is None,
    reason='USER/PASSWD is not set.')
def test_no_auth_async_write_no_ack(setup_config):
    params = {
        'sasl_plain_username': KAFKA_READ_USER,
        'sasl_plain_password': KAFKA_READ_PASSWD,
    }
    error = 0
    countdown = 1
    cv = Condition()

    def on_error(e):
        nonlocal countdown, error
        with cv:
            countdown -= 1
            error += 1
            cv.notify_all()

    def on_success(_):
        nonlocal countdown
        with cv:
            countdown -= 1
            cv.notify_all()

    def wait_done():
        with cv:
            while countdown > 0:
                cv.wait()

    with AsyncMessageWriter(SERVICE, consistency=AT_MOST_ONCE, **params) as f:
        f.publish(b'message-001').catch(on_error).then(on_success)
        wait_done()
    assert error == 0


@pytest.mark.skipif(
    KAFKA_WRITE_USER is None or KAFKA_WRITE_PASSWD is None,
    reason='USER/PASSWD is not set.')
def test_no_auth_read(setup_config):
    params = {
        'sasl_plain_username': KAFKA_WRITE_USER,
        'sasl_plain_password': KAFKA_WRITE_PASSWD,
    }
    with pytest.raises(AuthorizationError):
        with MessageReader(SERVICE, **params) as f:
            for _ in f:
                pass


@pytest.mark.skipif(
    KAFKA_WRITE_USER is None or KAFKA_WRITE_PASSWD is None,
    reason='USER/PASSWD is not set.')
def test_no_auth_async_read(setup_config):
    params = {
        'sasl_plain_username': KAFKA_WRITE_USER,
        'sasl_plain_password': KAFKA_WRITE_PASSWD,
    }
    error = 0
    countdown = 1
    cv = Condition()

    def error_handler(e, traceback=None):
        nonlocal countdown, error
        with cv:
            countdown -= 1
            if isinstance(e, AuthorizationError):
                error += 1
            cv.notify_all()

    reader = AsyncMessageReader(SERVICE, **params)
    reader.on_failure = error_handler
    reader.open()
    with cv:
        cv.wait(10)
    reader.close()
    assert error == 1


@pytest.mark.skipif(
    KAFKA_WRITE_USER is None or KAFKA_WRITE_PASSWD is None,
    reason='USER/PASSWD is not set.')
def test_no_auth_async_read_with_stmt(setup_config):
    params = {
        'sasl_plain_username': KAFKA_WRITE_USER,
        'sasl_plain_password': KAFKA_WRITE_PASSWD,
    }
    error = 0
    countdown = 1
    cv = Condition()

    def error_handler(e, traceback=None):
        nonlocal countdown, error
        with cv:
            countdown -= 1
            if isinstance(e, AuthorizationError):
                error += 1
            cv.notify_all()

    with AsyncMessageReader(SERVICE, **params) as reader:
        reader.on_failure = error_handler
        with cv:
            cv.wait(10)
    assert error == 1


@pytest.fixture()
def config_brokers():
    logger.debug(f'BROKER: {USER_PASSWD_BROKER}')
    return [USER_PASSWD_BROKER]


@pytest.fixture()
def config_topics():
    return 'mss-test-003'


@pytest.fixture()
def config_params():
    return {
        'security_protocol': 'SASL_PLAINTEXT',
        'sasl_mechanism': 'PLAIN',
    }
