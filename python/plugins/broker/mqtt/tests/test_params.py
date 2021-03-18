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

import pytest
from sinetstream import MessageReader, MessageWriter, InvalidArgumentError
from conftest import (
    SERVICE, TOPIC, WS_BROKER, WSS_BROKER, CACERT_PATH,
)
from itertools import product

pytestmark = pytest.mark.usefixtures('setup_config')


@pytest.mark.parametrize("io,config_params", product(
    [MessageReader, MessageWriter],
    [{'protocol': x} for x in ['MQTTv31', 'MQTTv311', 'MQTTv5']],
))
def test_protocol(io):
    with io(SERVICE) as _:
        pass


@pytest.mark.parametrize("io,config_params", [
    (MessageReader, {'protocol': 'xxx'}),
    (MessageWriter, {'protocol': 'xxx'}),
])
def test_bad_protocol(io):
    with pytest.raises(InvalidArgumentError):
        with io(SERVICE) as _:
            pass


@pytest.mark.parametrize("io,config_params", [
    (MessageReader, {'transport': 'tcp'}),
    (MessageWriter, {'transport': 'tcp'}),
])
def test_transport_tcp(io):
    with io(SERVICE) as _:
        pass


@pytest.mark.skipif(WS_BROKER is None, reason='MQTT_WS_BROKER is not set.')
@pytest.mark.parametrize("io,config_params,config_brokers", [
    (MessageReader, {'transport': 'websockets'}, WS_BROKER),
    (MessageWriter, {'transport': 'websockets'}, WS_BROKER),
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
@pytest.mark.parametrize("io,config_params,config_brokers", [
    (MessageReader, wss_params, WSS_BROKER),
    (MessageWriter, wss_params, WSS_BROKER),
])
def test_transport_wss(io):
    with io(SERVICE) as _:
        pass


@pytest.mark.parametrize("io,config_params", [
    (MessageReader, {'transport': 'xxx'}),
    (MessageWriter, {'transport': 'xxx'}),
])
def test_transport_bad_value(io):
    with pytest.raises(InvalidArgumentError):
        with io(SERVICE) as _:
            pass


@pytest.mark.parametrize("io,config_params", product(
    [MessageReader, MessageWriter],
    [{'clean_session': x} for x in [True, False]],
))
def test_clean_session(io):
    with io(SERVICE) as _:
        pass


@pytest.mark.parametrize("io,config_params", product(
    [MessageReader, MessageWriter],
    [{'max_inflight_messages_set': {'inflight': x}} for x in [20, 40]],
))
def test_max_inflight_messages(io):
    with io(SERVICE) as _:
        pass


@pytest.mark.parametrize("io,config_params", product(
    [MessageReader, MessageWriter],
    [{'max_queued_messages_set': {'queue_size': x}} for x in [0, 10]],
))
def test_max_queued_messages(io):
    with io(SERVICE) as _:
        pass


@pytest.mark.parametrize("io,config_params", product(
    [MessageReader, MessageWriter],
    [{'message_retry_set': {'retry': x}} for x in [5, 10]],
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
@pytest.mark.parametrize("io,config_params,config_brokers", [
    (MessageReader, ws_set_options_params, WS_BROKER),
    (MessageWriter, ws_set_options_params, WS_BROKER),
])
def test_ws_set_options(io):
    with io(SERVICE) as _:
        pass


will_set_params = {
    'will_set': {
        'topic': TOPIC,
        'payload': 'XXX',
        'qos': 1,
        'retain': True,
    }
}


@pytest.mark.parametrize("io,config_params", [
    (MessageReader, will_set_params),
    (MessageWriter, will_set_params),
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


@pytest.mark.parametrize("io,config_params", [
    (MessageReader, reconnect_delay_params),
    (MessageWriter, reconnect_delay_params),
])
def test_reconnect_delay(io):
    with io(SERVICE) as _:
        pass
