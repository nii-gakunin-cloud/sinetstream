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
import pytest

from sinetstream import (
    MessageReader, MessageWriter, AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE,
    InvalidArgumentError, AlreadyConnectedError,
)
from conftest import SERVICE, TOPIC, TOPIC2

logging.basicConfig(level=logging.CRITICAL)
pytestmark = pytest.mark.usefixtures('setup_config')


@pytest.mark.parametrize("topic", [
    TOPIC,
    [TOPIC],
])
def test_writer_topic(topic):
    with MessageWriter(SERVICE, topic) as f:
        assert f.topic == (topic if isinstance(topic, str) else topic[0])


def test_writer_bad_topics():
    with pytest.raises(InvalidArgumentError):
        with MessageWriter(SERVICE, [TOPIC, TOPIC2]) as f:
            pass


@pytest.mark.parametrize("consistency", [
    AT_MOST_ONCE,
    AT_LEAST_ONCE,
    EXACTLY_ONCE,
])
def test_writer_consistency(consistency):
    with MessageWriter(SERVICE, TOPIC, consistency=consistency) as f:
        pass


def test_writer_bad_consistency():
    with pytest.raises(InvalidArgumentError):
        with MessageWriter(SERVICE, TOPIC, consistency=999) as f:
            pass


@pytest.mark.parametrize("qos", [0, 1, 2])
def test_writer_qos(qos):
    with MessageWriter(SERVICE, TOPIC, qos=qos) as f:
        pass


@pytest.mark.parametrize("retain", [True, False])
def test_writer_retain(retain):
    with MessageWriter(SERVICE, TOPIC, retain=retain) as f:
        pass


def test_writer_client_id_default():
    with MessageWriter(SERVICE, TOPIC) as f:
        assert f.client_id is not None and f.client_id != ""


def test_writer_client_id_set():
    cid = "oreore"
    with MessageWriter(SERVICE, TOPIC, client_id=cid) as f:
        assert f.client_id == cid


def test_writer_deser():
    with MessageWriter(SERVICE, TOPIC, value_serializer=(lambda x: x)) as f:
        pass


def test_open_twice():
    with MessageWriter(SERVICE, TOPIC) as f:
        with pytest.raises(AlreadyConnectedError):
            f.open()


def test_publish_result():
    with MessageWriter(SERVICE, TOPIC) as f:
        ret = f.publish(b'message')
        assert ret is not None
