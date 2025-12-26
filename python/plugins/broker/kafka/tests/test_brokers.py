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

import pytest
from sinetstream import MessageReader, MessageWriter, InvalidArgumentError
from conftest import SERVICE, TOPIC, BROKER, CONFVER1, CONFVER3


@pytest.mark.parametrize("io,config_brokers,config_version", [
    pytest.param(MessageReader, None, CONFVER1),
    pytest.param(MessageWriter, None, CONFVER1),
    pytest.param(MessageReader, None, CONFVER3),
    pytest.param(MessageWriter, None, CONFVER3),
])
def test_no_broker(io, setup_config):
    with pytest.raises(InvalidArgumentError):
        with io(SERVICE) as _:
            pass


@pytest.mark.parametrize("io,config_brokers,config_version", [
    pytest.param(MessageReader, [], CONFVER1),
    pytest.param(MessageWriter, [], CONFVER1),
    pytest.param(MessageReader, [], CONFVER3),
    pytest.param(MessageWriter, [], CONFVER3),
])
def test_empty_broker_list(io, setup_config):
    with pytest.raises(InvalidArgumentError):
        with io(SERVICE) as _:
            pass


@pytest.mark.parametrize("io,config_brokers,config_version", [
    pytest.param(MessageReader, BROKER, CONFVER1),
    pytest.param(MessageWriter, BROKER, CONFVER1),
    pytest.param(MessageReader, BROKER, CONFVER3),
    pytest.param(MessageWriter, BROKER, CONFVER3),
])
def test_brokers_str_type(io, setup_config):
    with io(SERVICE) as f:
        assert f.params['brokers'] == BROKER


@pytest.mark.parametrize("io,config_brokers,config_version", [
    pytest.param(MessageReader, [BROKER], CONFVER1),
    pytest.param(MessageWriter, [BROKER], CONFVER1),
    pytest.param(MessageReader, [BROKER], CONFVER3),
    pytest.param(MessageWriter, [BROKER], CONFVER3),
])
def test_brokers_list(io, setup_config):
    with io(SERVICE) as f:
        assert len(f.params['brokers']) == 1
        assert f.params['brokers'][0] == BROKER


@pytest.fixture()
def config_topics():
    return [TOPIC]
