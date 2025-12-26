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

import pytest
from sinetstream import MessageReader, MessageWriter, InvalidArgumentError
from conftest import (
    CONFVER1, CONFVER2, CONFVER3, CONFVER,
    SERVICE, TOPIC, WS_BROKER, WSS_BROKER, CACERT_PATH,
)
from itertools import product

pytestmark = pytest.mark.usefixtures('setup_config')


@pytest.mark.parametrize("io,config_mqtt_params,config_version", product(
    [MessageReader, MessageWriter],
    [{'protocol': x} for x in ['MQTTv31', 'MQTTv311', 'MQTTv5']],
    [CONFVER1, CONFVER3],
))
def test_protocol(io):
    with io(SERVICE) as _:
        pass


@pytest.mark.parametrize("io,config_mqtt_params,config_version", [
    (MessageReader, {'protocol': 'xxx'}, CONFVER1),
    (MessageWriter, {'protocol': 'xxx'}, CONFVER1),
    (MessageReader, {'protocol': 'xxx'}, CONFVER3),
    (MessageWriter, {'protocol': 'xxx'}, CONFVER3),
])
def test_bad_protocol(io):
    with pytest.raises(InvalidArgumentError):
        with io(SERVICE) as _:
            pass


@pytest.mark.parametrize("io,config_mqtt_params,config_version", [
    (MessageReader, {'transport': 'tcp'}, CONFVER1),
    (MessageWriter, {'transport': 'tcp'}, CONFVER1),
    (MessageReader, {'transport': 'tcp'}, CONFVER3),
    (MessageWriter, {'transport': 'tcp'}, CONFVER3),
])
def test_transport_tcp(io):
    with io(SERVICE) as _:
        pass


@pytest.mark.skipif(WS_BROKER is None, reason='MQTT_WS_BROKER is not set.')
@pytest.mark.parametrize("io,config_mqtt_params,config_brokers,config_version", [
    (MessageReader, {'transport': 'websockets'}, WS_BROKER, CONFVER1),
    (MessageWriter, {'transport': 'websockets'}, WS_BROKER, CONFVER1),
    (MessageReader, {'transport': 'websockets'}, WS_BROKER, CONFVER3),
    (MessageWriter, {'transport': 'websockets'}, WS_BROKER, CONFVER3),
])
def test_transport_ws(io):
    with io(SERVICE) as _:
        pass


wss_params = {
    'transport': 'websockets',
    'tls': {
        'ca_certs': str(CACERT_PATH),
    },
}


@pytest.mark.skipif(WSS_BROKER is None, reason='MQTT_WSS_BROKER is not set.')
@pytest.mark.parametrize("io,config_mqtt_params,config_brokers,config_version", [
    (MessageReader, wss_params, WSS_BROKER, CONFVER1),
    (MessageWriter, wss_params, WSS_BROKER, CONFVER1),
    (MessageReader, wss_params, WSS_BROKER, CONFVER3),
    (MessageWriter, wss_params, WSS_BROKER, CONFVER3),
])
def test_transport_wss(io):
    with io(SERVICE) as _:
        pass


@pytest.mark.parametrize("io,config_mqtt_params,config_version", [
    (MessageReader, {'transport': 'xxx'}, CONFVER1),
    (MessageWriter, {'transport': 'xxx'}, CONFVER1),
    (MessageReader, {'transport': 'xxx'}, CONFVER3),
    (MessageWriter, {'transport': 'xxx'}, CONFVER3),
])
def test_transport_bad_value(io):
    with pytest.raises(InvalidArgumentError):
        with io(SERVICE) as _:
            pass


@pytest.mark.parametrize("io,config_mqtt_params,config_version", product(
    [MessageReader, MessageWriter],
    [{'clean_session': x} for x in [True, False]],
    [CONFVER1, CONFVER3],
))
def test_clean_session(io):
    with io(SERVICE) as _:
        pass


@pytest.mark.parametrize("io,config_mqtt_params,config_version", product(
    [MessageReader, MessageWriter],
    [{'max_inflight_messages_set': {'inflight': x}} for x in [20, 40]],
    [CONFVER1, CONFVER3],
))
def test_max_inflight_messages(io):
    with io(SERVICE) as _:
        pass


@pytest.mark.parametrize("io,config_mqtt_params,config_version", product(
    [MessageReader, MessageWriter],
    [{'max_queued_messages_set': {'queue_size': x}} for x in [0, 10]],
    [CONFVER1, CONFVER3],
))
def test_max_queued_messages(io):
    with io(SERVICE) as _:
        pass


@pytest.mark.parametrize("io,config_mqtt_params,config_version", product(
    [MessageReader, MessageWriter],
    [{'message_retry_set': {'retry': x}} for x in [5, 10]],
    [CONFVER1, CONFVER3],
))
def test_message_retry(io):
    with io(SERVICE) as _:
        pass


ws_set_options_params = {
    'transport': 'websockets',
    'ws_set_options': {
        'path': '/mqtt',
    },
}


@pytest.mark.skipif(WS_BROKER is None, reason='MQTT_WS_BROKER is not set.')
@pytest.mark.parametrize("io,config_mqtt_params,config_brokers,config_version", [
    (MessageReader, ws_set_options_params, WS_BROKER, CONFVER1),
    (MessageWriter, ws_set_options_params, WS_BROKER, CONFVER1),
    (MessageReader, ws_set_options_params, WS_BROKER, CONFVER3),
    (MessageWriter, ws_set_options_params, WS_BROKER, CONFVER3),
])
def test_ws_set_options(io):
    with io(SERVICE) as _:
        pass


will_set_params = {
    'will_set': {
        'topic': TOPIC,
        'value_type': 'text',
        'payload': 'XXX',
        'qos': 1,
        'retain': True,
    }
}


@pytest.mark.parametrize("io,config_mqtt_params,config_version", [
    (MessageReader, will_set_params, CONFVER1),
    (MessageWriter, will_set_params, CONFVER1),
    (MessageReader, will_set_params, CONFVER3),
    (MessageWriter, will_set_params, CONFVER3),
])
def test_will(io):
    with io(SERVICE) as _:
        pass


reconnect_delay_params = {
    'reconnect_delay_set': {
        'min_delay': 1,
        'max_delay': 120,
    }
}


@pytest.mark.parametrize("io,config_mqtt_params,config_version", [
    (MessageReader, reconnect_delay_params, CONFVER1),
    (MessageWriter, reconnect_delay_params, CONFVER1),
    (MessageReader, reconnect_delay_params, CONFVER3),
    (MessageWriter, reconnect_delay_params, CONFVER3),
])
def test_reconnect_delay(io):
    with io(SERVICE) as _:
        pass
