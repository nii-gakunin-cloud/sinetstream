#!/usr/bin/env python3

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
from math import inf

import pytest
from conftest import SERVICE, TOPIC, TOPIC2

from sinetstream import (
    MessageReader, AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE,
    InvalidArgumentError, )

logging.basicConfig(level=logging.ERROR)
pytestmark = pytest.mark.usefixtures('setup_config', 'dummy_reader_plugin')


@pytest.mark.parametrize("topics", [
    TOPIC,
    [TOPIC],
    [TOPIC, TOPIC2],
])
def test_reader_topic(topics):
    with MessageReader(SERVICE, topics) as _:
        pass


@pytest.mark.parametrize("config_topic", [None, []])
def test_reader_bad_topics():
    with pytest.raises(InvalidArgumentError):
        with MessageReader(SERVICE) as _:
            pass


@pytest.mark.parametrize("consistency", [AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE])
def test_reader_consistency(consistency):
    with MessageReader(SERVICE, consistency=consistency) as f:
        assert consistency == f.consistency


@pytest.mark.parametrize('config_params', [
    {'consistency': 'AT_MOST_ONCE'},
    {'consistency': 'AT_LEAST_ONCE'},
    {'consistency': 'EXACTLY_ONCE'},
])
def test_reader_consistency_in_config_file(config_params):
    with MessageReader(SERVICE) as f:
        consistency = config_params['consistency']
        assert eval(consistency) == f.consistency


@pytest.mark.parametrize("consistency", [999, "XXX"])
def test_reader_bad_consistency(consistency):
    with pytest.raises(InvalidArgumentError):
        with MessageReader(SERVICE, consistency=consistency) as _:
            pass


def test_reader_client_id_default():
    with MessageReader(SERVICE) as f:
        assert f.client_id is not None and f.client_id != ""


def test_reader_client_id_empty():
    with MessageReader(SERVICE, client_id="") as f:
        assert f.client_id is not None and f.client_id != ""


def test_reader_client_id_set():
    cid = "oreore"
    with MessageReader(SERVICE, client_id=cid) as f:
        assert f.client_id == cid


def test_reader_deser():
    with MessageReader(SERVICE, value_deserializer=(lambda x: x)) as _:
        pass


def test_reader_timeout():
    with MessageReader(SERVICE, receive_timeout_ms=3000) as _:
        pass


def test_open_close():
    f = MessageReader(SERVICE).open()
    assert hasattr(f, '__iter__')
    f.close()


def test_close_twice():
    f = MessageReader(SERVICE).open()
    f.close()
    f.close()


def test_reader_topic_in_config_file(config_topic):
    with MessageReader(SERVICE) as f:
        assert f.topics == config_topic


@pytest.mark.parametrize('config_topic', [[TOPIC, TOPIC2]])
def test_reader_topic_list_in_config_file():
    with MessageReader(SERVICE) as f:
        assert f.topics == [TOPIC, TOPIC2]


@pytest.mark.parametrize('config_topic', [TOPIC])
def test_reader_topic_in_config_file_and_arg():
    with MessageReader(SERVICE, TOPIC2) as f:
        assert f.topics == TOPIC2


@pytest.mark.parametrize('config_topic', [TOPIC])
def test_reader_topics_in_config_file_and_kwargs():
    with MessageReader(service=SERVICE, topic=TOPIC2) as f:
        assert f.topics == TOPIC2


@pytest.mark.parametrize('config_topic,config_params', [
    pytest.param(None, {'topics': TOPIC})
])
def test_reader_topics_in_config_file():
    with MessageReader(SERVICE) as f:
        assert f.topics == TOPIC


@pytest.mark.parametrize('config_topic,config_params', [
    pytest.param(None, {'topics': [TOPIC, TOPIC2]})
])
def test_reader_topics_list_in_config_file():
    with MessageReader(SERVICE) as f:
        assert f.topics == [TOPIC, TOPIC2]


def test_service():
    with MessageReader(SERVICE) as f:
        assert f.service == SERVICE


def test_receive_timeout_ms():
    timeout = 100
    with MessageReader(SERVICE, receive_timeout_ms=timeout) as f:
        assert f.receive_timeout_ms == timeout


def test_default_receive_timeout_ms():
    with MessageReader(SERVICE) as f:
        assert f.receive_timeout_ms == inf
