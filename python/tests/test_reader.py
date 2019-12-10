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

import time
import logging
import pytest

import sinetstream

logging.basicConfig(level=logging.ERROR)


service = 'service-1'
topic = 'mss-test-001'


def test_reader_1(dummy_reader_plugin):
    with sinetstream.MessageReader(service, topic) as f:
        pass
    assert True


def test_reader_1_list(dummy_reader_plugin):
    with sinetstream.MessageReader(service, [topic]) as f:
        pass
    assert True


def test_reader_2_list(dummy_reader_plugin):
    with sinetstream.MessageReader(service, [topic, topic+"2"]) as f:
        pass
    assert True


def test_reader_consistency_0(dummy_reader_plugin):
    with sinetstream.MessageReader(service, topic, consistency=sinetstream.AT_MOST_ONCE) as f:
        pass
    assert True


def test_reader_consistency_1(dummy_reader_plugin):
    with sinetstream.MessageReader(service, topic, consistency=sinetstream.AT_LEAST_ONCE) as f:
        pass
    assert True


def test_reader_consistency_2(dummy_reader_plugin):
    with sinetstream.MessageReader(service, topic, consistency=sinetstream.EXACTLY_ONCE) as f:
        pass
    assert True


def test_reader_consistency_X(dummy_reader_plugin):
    try:
        with sinetstream.MessageReader(service, topic, consistency=999) as f:
            pass
    except sinetstream.InvalidArgumentError:
        assert True
    else:
        assert False


def test_reader_client_id_default(dummy_reader_plugin):
    with sinetstream.MessageReader(service, topic) as f:
        assert f.client_id is not None and f.client_id != ""
    assert True


def test_reader_client_id_empty(dummy_reader_plugin):
    with sinetstream.MessageReader(service, topic, client_id="") as f:
        assert f.client_id is not None and f.client_id != ""
    assert True


def test_reader_client_id_set(dummy_reader_plugin):
    cid = "oreore"
    with sinetstream.MessageReader(service, topic, client_id=cid) as f:
        assert f.client_id == cid
    assert True


def test_reader_deser(dummy_reader_plugin):
    with sinetstream.MessageReader(service, topic, value_deserializer=(lambda x: x)) as f:
        pass
    assert True


def test_reader_timeout(dummy_reader_plugin):
    with sinetstream.MessageReader(service, topic, receive_timeout_ms=3000) as f:
        pass


def test_reader_kafka_opt(dummy_reader_plugin):
    with sinetstream.MessageReader(service, topic, heartbeat_interval_ms=1000) as f:
        pass
    assert True


@pytest.mark.skip
def test_reader_seek():
    with sinetstream.MessageReader(service, topic) as f:
        try:
            f.seek_to_beginning()
        except AssertionError:
            pass  # If any partition is not currently assigned, or if no partitions are assigned.
        try:
            f.seek_to_end()
        except AssertionError:
            pass  # If any partition is not currently assigned, or if no partitions are assigned.
    assert True


def test_reader_topics_in_config_file(dummy_reader_plugin):
    with sinetstream.MessageReader(service + "-topics") as f:
        assert f.topics == topic


def test_reader_topics_list_in_config_file(dummy_reader_plugin):
    with sinetstream.MessageReader(service + "-topics-list") as f:
        assert f.topics == [topic, "mss-test-002"]


def test_reader_topic_in_config_file(dummy_reader_plugin):
    with sinetstream.MessageReader(service + "-topic") as f:
        assert f.topics == topic


def test_reader_topic_list_in_config_file(dummy_reader_plugin):
    with sinetstream.MessageReader(service + "-topic-list") as f:
        assert f.topics == [topic, "mss-test-002"]


def test_reader_topics_in_config_file_and_arg(dummy_reader_plugin):
    topic2 = "mss-test-002"
    with sinetstream.MessageReader(service + "-topic", topic2) as f:
        assert f.topics == topic2


def test_reader_topics_in_config_file_and_kwarg(dummy_reader_plugin):
    service_name = service + "-topic"
    topic2 = "mss-test-002"
    with sinetstream.MessageReader(topics=topic2, service=service_name) as f:
        assert f.topics == topic2


def test_reader_no_topics(dummy_reader_plugin):
    with pytest.raises(sinetstream.InvalidArgumentError):
        with sinetstream.MessageReader(service) as f:
            pass


def test_reader_empty_topics_list(dummy_reader_plugin):
    with pytest.raises(sinetstream.InvalidArgumentError):
        with sinetstream.MessageReader(service, topics=[]) as f:
            pass


def test_open_close(dummy_reader_plugin):
    f = sinetstream.MessageReader(service, topic).open()
    assert hasattr(f, '__iter__')
    f.close()


def test_close_twice(dummy_reader_plugin):
    f = sinetstream.MessageReader(service, topic).open()
    f.close()
    with pytest.raises(sinetstream.SinetError):
        f.close()
