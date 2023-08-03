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

import pytest
from conftest import SERVICE, TOPIC, TOPIC2

from sinetstream import (
    MessageWriter, AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE,
    InvalidArgumentError, )

logging.basicConfig(level=logging.ERROR)
pytestmark = pytest.mark.usefixtures('setup_config', 'dummy_writer_plugin')


@pytest.mark.parametrize("topics", [
    TOPIC,
    [TOPIC],
])
def test_writer_topic(topics):
    with MessageWriter(SERVICE, topics) as _:
        pass


# @pytest.mark.parametrize("config_topic", [None, [], [TOPIC, TOPIC2]])
@pytest.mark.parametrize("config_topic", [[], [TOPIC, TOPIC2]])
def test_writer_bad_topics():
    with pytest.raises(InvalidArgumentError):
        with MessageWriter(SERVICE) as _:
            pass


@pytest.mark.parametrize("consistency", [AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE])
def test_writer_consistency(consistency):
    with MessageWriter(SERVICE, consistency=consistency) as f:
        assert consistency == f.consistency


@pytest.mark.parametrize('config_params', [
    {'consistency': 'AT_MOST_ONCE'},
    {'consistency': 'AT_LEAST_ONCE'},
    {'consistency': 'EXACTLY_ONCE'},
])
def test_writer_consistency_in_config_file(config_params):
    with MessageWriter(SERVICE) as f:
        consistency = config_params['consistency']
        assert eval(consistency) == f.consistency


@pytest.mark.parametrize("consistency", [999, "XXX"])
def test_writer_bad_consistency(consistency):
    with pytest.raises(InvalidArgumentError):
        with MessageWriter(SERVICE, consistency=consistency) as _:
            pass


def test_writer_client_id_default():
    with MessageWriter(SERVICE) as f:
        assert f.client_id is not None and f.client_id != ""


def test_writer_client_id_set():
    cid = "oreore"
    with MessageWriter(SERVICE, client_id=cid) as f:
        assert f.client_id == cid


def test_writer_deser():
    with MessageWriter(SERVICE, value_serializer=(lambda x: x)) as _:
        pass


def test_open_close():
    f = MessageWriter(SERVICE).open()
    f.close()


def test_close_twice():
    f = MessageWriter(SERVICE).open()
    f.close()
    f.close()


@pytest.mark.parametrize('config_topic', [TOPIC])
def test_writer_topic_in_config_file():
    with MessageWriter(SERVICE) as f:
        assert f.topic == TOPIC


@pytest.mark.parametrize('config_topic', [[TOPIC]])
def test_writer_topic_list_one_item_in_config_file():
    with MessageWriter(SERVICE) as f:
        assert f.topic == TOPIC


@pytest.mark.parametrize('config_topic', [[TOPIC, TOPIC2]])
def test_writer_topic_list_in_config_file():
    with pytest.raises(InvalidArgumentError):
        with MessageWriter(SERVICE) as _:
            pass


@pytest.mark.parametrize('config_topic', [TOPIC])
def test_writer_topic_in_config_file_and_arg():
    with MessageWriter(SERVICE, TOPIC2) as f:
        assert f.topic == TOPIC2


@pytest.mark.parametrize('config_topic', [TOPIC])
def test_writer_topic_in_config_file_and_kwarg():
    with MessageWriter(topic=TOPIC2, service=SERVICE) as f:
        assert f.topic == TOPIC2
