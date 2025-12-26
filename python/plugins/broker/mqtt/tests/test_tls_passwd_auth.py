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
import pytest
from conftest import (
    CONFVER1, CONFVER2, CONFVER3, CONFVER,
    SERVICE, TOPIC, SSL_USER_PASSWD_BROKER, MQTT_USER, MQTT_PASSWD,
    CACERT_PATH,
)

logging.basicConfig(level=logging.CRITICAL)
logger = logging.getLogger(__name__)
pytestmark = pytest.mark.skipif(
    SSL_USER_PASSWD_BROKER is None, reason='MQTT_SSL_USER_PASSWD_BROKER is not set.')


_TLS_PARAMS = {'ca_certs': str(CACERT_PATH)} if CACERT_PATH is not None else True

ok_passwd_params_v1 = {
    'username_pw_set': {
        'username': MQTT_USER,
        'password': MQTT_PASSWD,
    },
    'tls': _TLS_PARAMS,
}

ok_passwd_params_v3 = {
    'type_spec': {
        'username_pw': {
            'username': MQTT_USER,
            'password': MQTT_PASSWD,
        },
    },
    'tls': _TLS_PARAMS,
}


@pytest.mark.parametrize("io,config_comm_params,config_version", [
    (MessageReader, ok_passwd_params_v1, CONFVER1),
    (MessageWriter, ok_passwd_params_v1, CONFVER1),
    (MessageReader, ok_passwd_params_v3, CONFVER3),
    (MessageWriter, ok_passwd_params_v3, CONFVER3),
])
def test_password_auth(io, setup_config):
    with io(SERVICE, TOPIC) as _:
        pass


bad_passwd_params_v1 = {
    'username_pw_set': {
        'username': MQTT_USER,
        'password': MQTT_PASSWD + 'X',
    },
    'tls': _TLS_PARAMS,
}

bad_passwd_params_v3 = {
    'type_spec': {
        'username_pw': {
            'username': MQTT_USER,
            'password': MQTT_PASSWD + 'X',
        },
    },
    'tls': _TLS_PARAMS,
}


@pytest.mark.parametrize("io,config_comm_params,config_version", [
    (MessageReader, bad_passwd_params_v1, CONFVER1),
    (MessageWriter, bad_passwd_params_v1, CONFVER1),
    (MessageReader, bad_passwd_params_v3, CONFVER3),
    (MessageWriter, bad_passwd_params_v3, CONFVER3),
])
def test_bad_password(io, setup_config):
    with pytest.raises(ConnectionError):
        with io(SERVICE, TOPIC) as _:
            pass


bad_user_params_v1 = {
    'username_pw_set': {
        'username': MQTT_USER + 'X',
        'password': MQTT_PASSWD,
    },
    'tls': _TLS_PARAMS,
}

bad_user_params_v3 = {
    'type_spec': {
        'username_pw': {
            'username': MQTT_USER + 'X',
            'password': MQTT_PASSWD,
        },
    },
    'tls': _TLS_PARAMS,
}


@pytest.mark.parametrize("io,config_comm_params,config_version", [
    (MessageReader, bad_user_params_v1, CONFVER1),
    (MessageWriter, bad_user_params_v1, CONFVER1),
    (MessageReader, bad_user_params_v3, CONFVER3),
    (MessageWriter, bad_user_params_v3, CONFVER3),
])
def test_bad_user(io, setup_config):
    with pytest.raises(ConnectionError):
        with io(SERVICE, TOPIC) as _:
            pass


@pytest.fixture()
def config_brokers():
    logger.debug(f'BROKER: {SSL_USER_PASSWD_BROKER}')
    return [SSL_USER_PASSWD_BROKER]
