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
import threading
from sinetstream import MessageReader, MessageWriter, ConnectionError
import pytest
from conftest import (
    CONFVER1, CONFVER2, CONFVER3, CONFVER,
    SERVICE, TOPIC, USER_PASSWD_BROKER, MQTT_USER, MQTT_PASSWD,
)

logging.basicConfig(level=logging.CRITICAL)
logger = logging.getLogger(__name__)
pytestmark = pytest.mark.skipif(
    USER_PASSWD_BROKER is None, reason='MQTT_USER_PASSWD_BROKER is not set.')

ok_passwd_params_v1 = {
    'username_pw_set': {
        'username': MQTT_USER,
        'password': MQTT_PASSWD,
    }
}

ok_passwd_params_v3 = {
    'username_pw': {
        'username': MQTT_USER,
        'password': MQTT_PASSWD,
    }
}


@pytest.mark.parametrize("io,config_mqtt_params,config_version", [
    (MessageReader, ok_passwd_params_v1, CONFVER1),
    (MessageWriter, ok_passwd_params_v1, CONFVER1),
    (MessageReader, ok_passwd_params_v3, CONFVER3),
    (MessageWriter, ok_passwd_params_v3, CONFVER3),
])
def test_password_password(io, setup_config):
    with io(SERVICE, TOPIC) as _:
        pass


bad_passwd_params_v1 = {
    'username_pw_set': {
        'username': MQTT_USER,
        'password': MQTT_PASSWD + 'X',
    }
}

bad_passwd_params_v3 = {
    'username_pw': {
        'username': MQTT_USER,
        'password': MQTT_PASSWD + 'X',
    }
}


@pytest.mark.parametrize("io,config_mqtt_params,config_version", [
    (MessageReader, bad_passwd_params_v1, CONFVER1),
    (MessageWriter, bad_passwd_params_v1, CONFVER1),
    (MessageReader, bad_passwd_params_v3, CONFVER3),
    (MessageWriter, bad_passwd_params_v3, CONFVER3),
])
def test_bad_password(io, setup_config):
    count = threading.active_count()
    ss = io(SERVICE, TOPIC)
    with pytest.raises(ConnectionError):
        ss.open()
    assert count == threading.active_count()
    ss.close()


bad_user_params_v1 = {
    'username_pw_set': {
        'username': MQTT_USER + 'X',
        'password': MQTT_PASSWD,
    }
}

bad_user_params_v3 = {
    'username_pw': {
        'username': MQTT_USER + 'X',
        'password': MQTT_PASSWD,
    }
}


@pytest.mark.parametrize("io,config_mqtt_params,config_version", [
    (MessageReader, bad_user_params_v1, CONFVER1),
    (MessageWriter, bad_user_params_v1, CONFVER1),
    (MessageReader, bad_user_params_v3, CONFVER3),
    (MessageWriter, bad_user_params_v3, CONFVER3),
])
def test_bad_user(io, setup_config):
    count = threading.active_count()
    ss = io(SERVICE, TOPIC)
    with pytest.raises(ConnectionError):
        ss.open()
    assert count == threading.active_count()
    ss.close()


@pytest.fixture()
def config_brokers():
    logger.debug(f'BROKER: {USER_PASSWD_BROKER}')
    return [USER_PASSWD_BROKER]
