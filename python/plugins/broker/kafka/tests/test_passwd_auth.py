#!/usr/bin/env python3

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
from sinetstream import MessageReader, MessageWriter, ConnectionError
import threading
import pytest
from conftest import (
    CONFVER1, CONFVER3,
    SERVICE, TOPIC, USER_PASSWD_BROKER, KAFKA_USER, KAFKA_PASSWD,
)

logging.basicConfig(level=logging.CRITICAL)
logger = logging.getLogger(__name__)
pytestmark = pytest.mark.skipif(
    USER_PASSWD_BROKER is None, reason='KAFKA_USER_PASSWD_BROKER is not set.')

sasl_plain = {
    'security_protocol': 'SASL_PLAINTEXT',
    'sasl_mechanism': 'PLAIN',
    'sasl_plain_username': KAFKA_USER,
    'sasl_plain_password': KAFKA_PASSWD,
}


@pytest.mark.parametrize("io, config_kafka_params, config_version", [
    (MessageReader, sasl_plain, CONFVER1),
    (MessageWriter, sasl_plain, CONFVER3),
    ]
)
def test_password_auth(io, setup_config):
    with io(SERVICE, TOPIC) as _:
        pass


bad_passwd_params = {
    'security_protocol': 'SASL_PLAINTEXT',
    'sasl_mechanism': 'PLAIN',
    'sasl_plain_username': KAFKA_USER,
    'sasl_plain_password': (
        KAFKA_PASSWD + 'X' if KAFKA_PASSWD is not None else None),
}


bad_user_params = {
    'security_protocol': 'SASL_PLAINTEXT',
    'sasl_mechanism': 'PLAIN',
    'sasl_plain_username': (
        'X' + KAFKA_USER if KAFKA_USER is not None else None),
    'sasl_plain_password': KAFKA_PASSWD,
}


bad_security_protocol_params = {
    'sasl_plain_username': KAFKA_USER,
    'sasl_plain_password': KAFKA_PASSWD,
}


@pytest.mark.parametrize("io,config_kafka_params,config_version", [
    (MessageReader, bad_passwd_params, CONFVER1),
    (MessageWriter, bad_passwd_params, CONFVER1),
    (MessageReader, bad_passwd_params, CONFVER3),
    (MessageWriter, bad_passwd_params, CONFVER3),
])
def test_bad_password(io, setup_config):
    count = threading.active_count()
    ss = io(SERVICE, TOPIC)
    with pytest.raises(ConnectionError):
        ss.open()
    assert count == threading.active_count()
    ss.close()


@pytest.mark.parametrize("io,config_kafka_params", [
    (MessageReader, bad_user_params),
    (MessageWriter, bad_user_params),
])
def test_bad_user(io, setup_config):
    count = threading.active_count()
    ss = io(SERVICE, TOPIC)
    with pytest.raises(ConnectionError):
        ss.open()
    assert count == threading.active_count()
    ss.close()


@pytest.mark.parametrize("io,config_kafka_params", [
    (MessageReader, bad_security_protocol_params),
    (MessageWriter, bad_security_protocol_params),
])
def test_bad_security_protocol(io, setup_config):
    with pytest.raises(ConnectionError):
        with io(SERVICE, TOPIC) as _:
            pass


@pytest.fixture()
def config_brokers():
    logger.debug(f'BROKER: {USER_PASSWD_BROKER}')
    return [USER_PASSWD_BROKER]


#@pytest.fixture()
#def config_params():
#    return {
#        'security_protocol': 'SASL_PLAINTEXT',
#        'sasl_mechanism': 'PLAIN',
#        'sasl_plain_username': KAFKA_USER,
#        'sasl_plain_password': KAFKA_PASSWD,
#    }
