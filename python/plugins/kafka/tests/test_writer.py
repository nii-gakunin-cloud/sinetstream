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
import threading
import logging
import pytest

import sinetstream

logging.basicConfig(level=logging.ERROR)


service = 'service-1'
topic = 'mss-test-001'


def test_writer_1():
    with sinetstream.MessageWriter(service, topic) as f:
        pass
    assert True


def test_writer_1_list():
    with sinetstream.MessageWriter(service, [topic]) as f:
        pass
    assert True


def test_writer_2_list():
    with pytest.raises(sinetstream.InvalidArgumentError):
        with sinetstream.MessageWriter(service, [topic, topic+"2"]) as f:
            pass


def test_writer_consistency_0():
    with sinetstream.MessageWriter(service, topic, consistency=sinetstream.AT_MOST_ONCE) as f:
        pass
    assert True


def test_writer_consistency_1():
    with sinetstream.MessageWriter(service, topic, consistency=sinetstream.AT_LEAST_ONCE) as f:
        pass
    assert True


def test_writer_consistency_2():
    with sinetstream.MessageWriter(service, topic, consistency=sinetstream.EXACTLY_ONCE) as f:
        pass
    assert True


def test_writer_consistency_X():
    try:
        with sinetstream.MessageWriter(service, topic, consistency=999) as f:
            pass
    except sinetstream.InvalidArgumentError:
        assert True
    else:
        assert False


def test_writer_client_id_default():
    with sinetstream.MessageWriter(service, topic) as f:
        assert f.client_id is not None and f.client_id != ""
    assert True


def test_writer_client_id_set():
    cid = "oreore"
    with sinetstream.MessageWriter(service, topic, client_id=cid) as f:
        assert f.client_id == cid
    assert True


def test_writer_deser():
    with sinetstream.MessageWriter(service, topic, value_serializer=(lambda x: x)) as f:
        pass
    assert True


def test_writer_kafka_opt():
    with sinetstream.MessageWriter(service, topic, batch_size=1000) as f:
        pass
    assert True


def test_open_twice():
    with sinetstream.MessageWriter(service, topic) as f:
        with pytest.raises(sinetstream.AlreadyConnectedError):
            f.open()


def test_publish_result():
    with sinetstream.MessageWriter(service, topic) as f:
        ret = f.publish(b'message')
        assert ret is not None
