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
from conftest import SERVICE, TOPIC, BROKER


@pytest.mark.parametrize("io,config_brokers", [
    pytest.param(MessageReader, None),
    pytest.param(MessageWriter, None),
])
def test_no_broker(io, setup_config):
    with pytest.raises(InvalidArgumentError):
        with io(SERVICE) as _:
            pass


@pytest.mark.parametrize("io,config_brokers", [
    pytest.param(MessageReader, []),
    pytest.param(MessageWriter, []),
])
def test_empty_broker_list(io, setup_config):
    with pytest.raises(InvalidArgumentError):
        with io(SERVICE) as _:
            pass


@pytest.mark.parametrize("io,config_brokers", [
    pytest.param(MessageReader, BROKER),
    pytest.param(MessageWriter, BROKER),
])
def test_brokers_str_type(io, setup_config):
    with io(SERVICE) as f:
        assert f.params['brokers'] == BROKER


@pytest.mark.parametrize("io,config_brokers", [
    pytest.param(MessageReader, [BROKER]),
    pytest.param(MessageWriter, [BROKER]),
])
def test_brokers_list(io, setup_config):
    with io(SERVICE) as f:
        assert len(f.params['brokers']) == 1
        assert f.params['brokers'][0] == BROKER


@pytest.fixture()
def config_topics():
    return [TOPIC]
