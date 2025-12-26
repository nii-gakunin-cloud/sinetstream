#!/usr/bin/env python3

# Copyright (C) 2023 National Institute of Informatics
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

import pytest
from sinetstream import MessageReader, MessageWriter  # , InvalidArgumentError
from conftest import (
    CONFVER1, CONFVER2, CONFVER3, CONFVER,
    SERVICE, TOPIC,  # WS_BROKER, WSS_BROKER, CACERT_PATH,
)
# from itertools import product

from random import choices
from string import ascii_letters, digits


pytestmark = pytest.mark.usefixtures('setup_config')

rnd = ''.join(choices(ascii_letters + digits, k=10))
WILL_TOPIC = TOPIC + f"-will-{rnd}"
PAYLOAD = f'TEST-WILL-MESSAGE-{rnd}'

will_set_params_v1 = {
    'will_set': {
        'topic': WILL_TOPIC,
        'value_type': 'text',
        'payload': PAYLOAD,
        # 'qos': 1,
        'consistency': 'AT_LEAST_ONCE',
        'retain': False,
    }
}

will_set_params_v3 = {
    'will': {
        'topic': WILL_TOPIC,
        'value_type': 'text',
        'payload': PAYLOAD,
        # 'qos': 1,
        'consistency': 'AT_LEAST_ONCE',
        'retain': False,
    }
}


@pytest.mark.parametrize("config_value_type,config_mqtt_params,config_version", [
    (will_set_params_v1["will_set"]["value_type"], will_set_params_v1, CONFVER1),
    (will_set_params_v3["will"]["value_type"], will_set_params_v3, CONFVER3),
])
def test_will_payload():
    with MessageWriter(SERVICE) as w, \
         MessageReader(SERVICE, topic=WILL_TOPIC) as r:
        # from socket import SHUT_WR, SHUT_RD, SHUT_RDWR
        # XXX .shutdown(SHUT_RDWR) doesnt work.
        w._debug_get_plugin()._debug_get_socket().close()
        # NOTE: close() causes EBADF in the loop in paho
        m2 = next(iter(r))
        assert m2.value == PAYLOAD
