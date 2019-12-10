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


def test_reader_1():
    with sinetstream.MessageReader(service, topic) as f:
        pass
    assert True


def test_reader_1_list():
    with sinetstream.MessageReader(service, [topic]) as f:
        pass
    assert True


def test_reader_2_list():
    with sinetstream.MessageReader(service, [topic, topic+"2"]) as f:
        pass
    assert True


def test_reader_consistency_0():
    with sinetstream.MessageReader(service, topic, consistency=sinetstream.AT_MOST_ONCE) as f:
        pass
    assert True


def test_reader_consistency_1():
    with sinetstream.MessageReader(service, topic, consistency=sinetstream.AT_LEAST_ONCE) as f:
        pass
    assert True


def test_reader_consistency_2():
    with sinetstream.MessageReader(service, topic, consistency=sinetstream.EXACTLY_ONCE) as f:
        pass
    assert True


def test_reader_consistency_X():
    try:
        with sinetstream.MessageReader(service, topic, consistency=999) as f:
            pass
    except sinetstream.InvalidArgumentError:
        assert True
    else:
        assert False


def test_reader_client_id_default():
    with sinetstream.MessageReader(service, topic) as f:
        assert f.client_id is not None and f.client_id != ""
    assert True


def test_reader_client_id_set():
    cid = "oreore"
    with sinetstream.MessageReader(service, topic, client_id=cid) as f:
        assert f.client_id == cid
    assert True


def test_reader_deser():
    with sinetstream.MessageReader(service, topic, value_deserializer=(lambda x: x)) as f:
        pass
    assert True


@pytest.mark.timeout(timeout=5, method='thread')
def test_reader_timeout():
    with sinetstream.MessageReader(service, topic, receive_timeout_ms=3000) as f:
        for msg in f:
            pass


def test_reader_kafka_opt():
    with sinetstream.MessageReader(service, topic, heartbeat_interval_ms=1000) as f:
        pass
    assert True


def test_reader_seek_to_beginning():
    with sinetstream.MessageReader(service, topic) as f:
        f.seek_to_beginning()


def test_reader_seek_to_end():
    with sinetstream.MessageReader(service, topic) as f:
        f.seek_to_end()


def test_open_close():
    f = sinetstream.MessageReader(service, topic).open()
    assert hasattr(f, '__iter__')
    f.close()


def test_open_twice():
    with sinetstream.MessageReader(service, topic) as f:
        with pytest.raises(sinetstream.AlreadyConnectedError):
            f.open()
