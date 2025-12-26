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

import logging
import pytest

from sinetstream import (
    MessageWriter,
    # AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE,
    InvalidArgumentError,
    ConnectionError,
    # AlreadyConnectedError,
)
from conftest import SERVICE, TOPIC, CONFVER

logging.basicConfig(level=logging.CRITICAL)
pytestmark = pytest.mark.usefixtures('setup_config')


mqttv3 = "MQTTv311"
mqttv5 = "MQTTv5"


def make_type_spec(**kwargs):
    if CONFVER == 3:
        return { "type_spec": kwargs }
    if CONFVER == 1 or CONFVER == 2:
        return kwargs


def show_props(f):
    # print(f"_connect_properties={f._plugin._mqttc._connect_properties}")
    pass


@pytest.mark.parametrize("x", [mqttv3, mqttv5])
def test_writer_mqttv5_protocol(x):
    with MessageWriter(SERVICE, TOPIC, **make_type_spec(protocol=x)) as _:
        pass


@pytest.mark.parametrize("x", ["MQTTv51"])
def test_writer_mqttv5_protocol_NG(x):
    with pytest.raises(InvalidArgumentError):
        with MessageWriter(SERVICE, TOPIC, **make_type_spec(protocol=x)) as _:
            pass


@pytest.mark.parametrize("x", [1, 1 << 15, 0, -1])
def test_writer_mqttv5_receive_maximum(x):
    with MessageWriter(SERVICE, TOPIC, **make_type_spec(protocol=mqttv5,
                       receive_maximum=x)) as _:
        show_props(_)
        pass


@pytest.mark.parametrize("x", [100, 1 << 20])
def test_writer_mqttv5_maximum_packet_size(x):
    with MessageWriter(SERVICE, TOPIC, **make_type_spec(protocol=mqttv5,
                       maximum_packet_size=x)) as _:
        show_props(_)
        pass


@pytest.mark.parametrize("x", [100, 1 << 20])
def test_writer_mqttv5_topic_alias_maximum(x):
    with MessageWriter(SERVICE, TOPIC, **make_type_spec(protocol=mqttv5,
                       topic_alias_maximum=x)) as _:
        show_props(_)
        pass


@pytest.mark.parametrize("x", [False, True])
def test_writer_mqttv5_request_response_info(x):
    with MessageWriter(SERVICE, TOPIC, **make_type_spec(protocol=mqttv5,
                       request_response_info=x)) as _:
        show_props(_)
        pass


@pytest.mark.parametrize("x", [False, True])
def test_writer_mqttv5_request_problem_info(x):
    with MessageWriter(SERVICE, TOPIC, **make_type_spec(protocol=mqttv5,
                       request_problem_info=x)) as _:
        show_props(_)
        pass


@pytest.mark.parametrize("x", [{},
                               {"foo": "bar"},
                               {"foo": "bar", "foo2": "bar2"}])
def test_writer_mqttv5_user_property(x):
    with MessageWriter(SERVICE, TOPIC, **make_type_spec(protocol=mqttv5,
                       user_property=x)) as _:
        show_props(_)
        pass


@pytest.mark.parametrize("x", ["foo"])
def test_writer_mqttv5_auth_method_NG(x):
    with pytest.raises(ConnectionError):
        with MessageWriter(SERVICE, TOPIC, **make_type_spec(protocol=mqttv5,
                           auth_method=x)) as _:
            show_props(_)
            pass


@pytest.mark.parametrize("x", [b"foo"])
def test_writer_mqttv5_auth_data(x):
    with MessageWriter(SERVICE, TOPIC, **make_type_spec(protocol=mqttv5,
                       auth_data=x)) as _:
        show_props(_)
        pass


@pytest.mark.parametrize("x", [False, True])
def test_writer_mqttv5_clean_session_NG(x):
    with pytest.raises(InvalidArgumentError):
        with MessageWriter(SERVICE, TOPIC, **make_type_spec(protocol=mqttv5,
                           clean_session=x)) as _:
            show_props(_)
            pass


@pytest.mark.parametrize("x", [False, True])
def test_writer_mqttv5_clean_start(x):
    with MessageWriter(SERVICE, TOPIC, **make_type_spec(protocol=mqttv5,
                       clean_start=x)) as _:
        show_props(_)
        pass


@pytest.mark.parametrize("x", [False, True])
def test_writer_mqttv3_clean_start_NG(x):
    with pytest.raises(ConnectionError):
        with MessageWriter(SERVICE, TOPIC, **make_type_spec(protocol=mqttv3,
                           clean_start=x)) as _:
            show_props(_)
        pass


@pytest.mark.parametrize("x", [0, 1, 1000])
def test_writer_mqttv5_session_expiry_interval(x):
    with MessageWriter(SERVICE, TOPIC, **make_type_spec(protocol=mqttv5,
                       session_expiry_interval=x)) as _:
        show_props(_)
        pass


@pytest.mark.parametrize("x", [-1, 0, 1, 100])
def test_writer_mqttv5_max_reconnect_delay(x):
    with MessageWriter(SERVICE, TOPIC, **make_type_spec(protocol=mqttv5,
                       max_reconnect_delay=x)) as _:
        show_props(_)
        pass


@pytest.mark.parametrize("x", [False, True])
def test_writer_mqttv5_use_subscription_identifiers(x):
    with MessageWriter(SERVICE, TOPIC, **make_type_spec(protocol=mqttv5,
                       use_subscription_identifiers=x)) as _:
        show_props(_)
        pass


@pytest.mark.parametrize("x", [False, True])
def test_writer_mqttv5_send_reason_messages(x):
    with MessageWriter(SERVICE, TOPIC, **make_type_spec(protocol=mqttv5,
                       send_reason_messages=x)) as _:
        show_props(_)
        pass


def test_writer_mqttv5_will_delay_interval():
    with MessageWriter(SERVICE, TOPIC, **make_type_spec(protocol=mqttv5,
                       will_set={
                           'topic': TOPIC,
                           'payload': b'XXX',
                           'qos': 1,
                           'delay_interval': 10
                       })) as _:
        show_props(_)
        pass
