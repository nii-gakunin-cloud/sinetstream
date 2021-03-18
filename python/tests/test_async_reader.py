#!/usr/local/bin/python3.6
# vim: expandtab shiftwidth=4

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
import time

import pytest
from conftest import SERVICE, TOPIC, TOPIC2

from sinetstream import (
    AsyncMessageReader, AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE,
    InvalidArgumentError, MessageWriter, TEXT)

logging.basicConfig(level=logging.ERROR)
pytestmark = pytest.mark.usefixtures('setup_config', 'dummy_reader_plugin', 'dummy_writer_plugin')


@pytest.mark.parametrize("topics", [
    TOPIC,
    [TOPIC],
    [TOPIC, TOPIC2],
])
def test_reader_topic(topics):
    with AsyncMessageReader(SERVICE, topics) as _:
        pass


@pytest.mark.parametrize("config_topic", [None, []])
def test_reader_bad_topics():
    with pytest.raises(InvalidArgumentError):
        with AsyncMessageReader(SERVICE) as _:
            pass


@pytest.mark.parametrize("consistency", [AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE])
def test_reader_consistency(consistency):
    with AsyncMessageReader(SERVICE, consistency=consistency) as f:
        assert consistency == f.consistency


@pytest.mark.parametrize('config_params', [
    {'consistency': 'AT_MOST_ONCE'},
    {'consistency': 'AT_LEAST_ONCE'},
    {'consistency': 'EXACTLY_ONCE'},
])
def test_reader_consistency_in_config_file(config_params):
    with AsyncMessageReader(SERVICE) as f:
        consistency = config_params['consistency']
        assert eval(consistency) == f.consistency


@pytest.mark.parametrize("consistency", [999, "XXX"])
def test_reader_bad_consistency(consistency):
    with pytest.raises(InvalidArgumentError):
        with AsyncMessageReader(SERVICE, consistency=consistency) as _:
            pass


def test_reader_client_id_default():
    with AsyncMessageReader(SERVICE) as f:
        assert f.client_id is not None and f.client_id != ""


def test_reader_client_id_empty():
    with AsyncMessageReader(SERVICE, client_id="") as f:
        assert f.client_id is not None and f.client_id != ""


def test_reader_client_id_set():
    cid = "oreore"
    with AsyncMessageReader(SERVICE, client_id=cid) as f:
        assert f.client_id == cid


def test_reader_deser():
    with AsyncMessageReader(SERVICE, value_deserializer=(lambda x: x)) as _:
        pass


def test_reader_timeout():
    with AsyncMessageReader(SERVICE, receive_timeout_ms=3000) as _:
        pass


def test_open_close():
    f = AsyncMessageReader(SERVICE).open()
    f.close()


def test_close_twice():
    f = AsyncMessageReader(SERVICE).open()
    f.close()
    f.close()


def test_reader_topic_in_config_file(config_topic):
    with AsyncMessageReader(SERVICE) as f:
        assert f.topics == config_topic


@pytest.mark.parametrize('config_topic', [[TOPIC, TOPIC2]])
def test_reader_topic_list_in_config_file():
    with AsyncMessageReader(SERVICE) as f:
        assert f.topics == [TOPIC, TOPIC2]


@pytest.mark.parametrize('config_topic', [TOPIC])
def test_reader_topic_in_config_file_and_arg():
    with AsyncMessageReader(SERVICE, TOPIC2) as f:
        assert f.topics == TOPIC2


@pytest.mark.parametrize('config_topic', [TOPIC])
def test_reader_topics_in_config_file_and_kwargs():
    with AsyncMessageReader(service=SERVICE, topic=TOPIC2) as f:
        assert f.topics == TOPIC2


@pytest.mark.parametrize('config_topic,config_params', [
    pytest.param(None, {'topics': TOPIC})
])
def test_reader_topics_in_config_file():
    with AsyncMessageReader(SERVICE) as f:
        assert f.topics == TOPIC


@pytest.mark.parametrize('config_topic,config_params', [
    pytest.param(None, {'topics': [TOPIC, TOPIC2]})
])
def test_reader_topics_list_in_config_file():
    with AsyncMessageReader(SERVICE) as f:
        assert f.topics == [TOPIC, TOPIC2]


def test_service():
    with AsyncMessageReader(SERVICE) as f:
        assert f.service == SERVICE


def test_on_message(config_topic):
    messages = ["message-1", "message-2"]
    called = []

    def assert_messages(message):
        expected = messages[len(called)]
        assert message.topic == config_topic
        assert message.value == expected
        called.append(1)

    with AsyncMessageReader(SERVICE, value_type=TEXT) as reader:
        reader.on_message = assert_messages
        with MessageWriter(SERVICE, value_type=TEXT) as writer:
            for msg in messages:
                writer.publish(msg)
        for _ in range(5):
            if len(called) == len(messages):
                break
            time.sleep(1)
        assert reader.on_message == assert_messages
    assert len(called) == len(messages)
