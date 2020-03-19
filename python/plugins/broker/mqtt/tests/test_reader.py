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


@pytest.mark.parametrize("topics", [
    TOPIC,
    [TOPIC],
    [TOPIC, TOPIC2],
])
def test_reader_topic(topics):
    with MessageReader(SERVICE, topics) as f:
        assert f.topics == topics


@pytest.mark.parametrize("consistency", [
    AT_MOST_ONCE,
    AT_LEAST_ONCE,
    EXACTLY_ONCE,
])
def test_reader_consistency(consistency):
    with MessageReader(SERVICE, TOPIC, consistency=consistency) as f:
        pass


def test_reader_consistency_error():
    with pytest.raises(InvalidArgumentError):
        with MessageReader(SERVICE, TOPIC, consistency=999) as f:
            pass


def test_reader_client_id_default():
    with MessageReader(SERVICE, TOPIC) as f:
        assert f.client_id is not None and f.client_id != ""


def test_reader_client_id_set():
    cid = "oreore"
    with MessageReader(SERVICE, TOPIC, client_id=cid) as f:
        assert f.client_id == cid


def test_reader_deser():
    with MessageReader(SERVICE, TOPIC, value_deserializer=(lambda x: x)) as f:
        pass


@pytest.mark.timeout(timeout=5, method='thread')
def test_reader_timeout():
    with MessageReader(SERVICE, TOPIC, receive_timeout_ms=3000) as f:
        for msg in f:
            pass


def test_open_close():
    f = MessageReader(SERVICE, TOPIC).open()
    assert hasattr(f, '__iter__')
    f.close()


def test_open_twice():
    with MessageReader(SERVICE, TOPIC) as f:
        with pytest.raises(AlreadyConnectedError):
            f.open()
