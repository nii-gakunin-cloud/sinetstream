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


def test_writer_1(dummy_writer_plugin):
    with sinetstream.MessageWriter(service, topic) as f:
        assert topic == f.topic


def test_writer_1_list(dummy_writer_plugin):
    with sinetstream.MessageWriter(service, [topic]) as f:
        assert topic == f.topic


def test_writer_2_list(dummy_writer_plugin):
    with pytest.raises(sinetstream.InvalidArgumentError):
        with sinetstream.MessageWriter(service, [topic, topic+"2"]) as f:
            pass


def test_writer_consistency_0(dummy_writer_plugin):
    with sinetstream.MessageWriter(service, topic, consistency=sinetstream.AT_MOST_ONCE) as f:
        pass
    assert True


def test_writer_consistency_1(dummy_writer_plugin):
    with sinetstream.MessageWriter(service, topic, consistency=sinetstream.AT_LEAST_ONCE) as f:
        pass
    assert True


def test_writer_consistency_2(dummy_writer_plugin):
    with sinetstream.MessageWriter(service, topic, consistency=sinetstream.EXACTLY_ONCE) as f:
        pass
    assert True


def test_writer_consistency_X(dummy_writer_plugin):
    try:
        with sinetstream.MessageWriter(service, topic, consistency=999) as f:
            pass
    except sinetstream.InvalidArgumentError:
        assert True
    else:
        assert False


def test_writer_client_id_default(dummy_writer_plugin):
    with sinetstream.MessageWriter(service, topic) as f:
        assert f.client_id is not None and f.client_id != ""
    assert True


def test_writer_client_id_set(dummy_writer_plugin):
    cid = "oreore"
    with sinetstream.MessageWriter(service, topic, client_id=cid) as f:
        assert f.client_id == cid
    assert True


def test_writer_deser(dummy_writer_plugin):
    with sinetstream.MessageWriter(service, topic, value_serializer=(lambda x: x)) as f:
        pass
    assert True


def test_writer_kafka_opt(dummy_writer_plugin):
    with sinetstream.MessageWriter(service, topic, batch_size=1000) as f:
        pass
    assert True


def test_writer_topic_in_config_file(dummy_writer_plugin):
    with sinetstream.MessageWriter(service + "-topic") as f:
        assert f.topic == topic


def test_writer_topic_list_one_item_in_config_file(dummy_writer_plugin):
    with sinetstream.MessageWriter(service + "-topic-list-one-item") as f:
        assert f.topic == topic


def test_writer_topic_list_in_config_file(dummy_writer_plugin):
    with pytest.raises(sinetstream.InvalidArgumentError):
        with sinetstream.MessageWriter(service + "-topic-list") as f:
            pass


def test_writer_topic_in_config_file_and_arg(dummy_writer_plugin):
    topic2 = "mss-test-002"
    with sinetstream.MessageWriter(service + "-topic", topic2) as f:
        assert f.topic == topic2


def test_writer_topic_in_config_file_and_kwarg(dummy_writer_plugin):
    service_name = service + "-topic"
    topic2 = "mss-test-002"
    with sinetstream.MessageWriter(topic=topic2, service=service_name) as f:
        assert f.topic == topic2


def test_writer_no_topic(dummy_writer_plugin):
    with pytest.raises(sinetstream.InvalidArgumentError):
        with sinetstream.MessageWriter(service) as f:
            pass


def test_writer_empty_topic_list(dummy_writer_plugin):
    with pytest.raises(sinetstream.InvalidArgumentError):
        with sinetstream.MessageWriter(service, topic=[]) as f:
            pass


def test_open_close(dummy_writer_plugin):
    f = sinetstream.MessageWriter(service, topic).open()
    f.close()


def test_close_twice(dummy_writer_plugin):
    f = sinetstream.MessageWriter(service, topic).open()
    f.close()
    with pytest.raises(sinetstream.SinetError):
        f.close()
