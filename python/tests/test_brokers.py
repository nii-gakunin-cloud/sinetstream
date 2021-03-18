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

import pytest
from sinetstream import MessageReader, MessageWriter, InvalidArgumentError
from conftest import SERVICE, BROKER

pytestmark = pytest.mark.usefixtures('setup_config', 'dummy_reader_plugin', 'dummy_writer_plugin')
BROKER2 = 'broker2'
BROKER3 = 'broker3'


@pytest.mark.parametrize("io,config_brokers", [
    pytest.param(MessageReader, None),
    pytest.param(MessageWriter, None),
])
def test_no_broker(io):
    with pytest.raises(InvalidArgumentError):
        with io(SERVICE) as _:
            pass


@pytest.mark.parametrize("io,config_brokers", [
    pytest.param(MessageReader, []),
    pytest.param(MessageWriter, []),
])
def test_empty_broker_list(io):
    with pytest.raises(InvalidArgumentError):
        with io(SERVICE) as _:
            pass


@pytest.mark.parametrize("io,config_brokers", [
    pytest.param(MessageReader, BROKER),
    pytest.param(MessageWriter, BROKER),
])
def test_brokers_str_type(io):
    with io(SERVICE) as f:
        assert f.params['brokers'] == BROKER


@pytest.mark.parametrize("io,config_brokers", [
    pytest.param(MessageReader, [BROKER]),
    pytest.param(MessageWriter, [BROKER]),
])
def test_brokers_list(io):
    with io(SERVICE) as f:
        assert len(f.params['brokers']) == 1
        assert f.params['brokers'][0] == BROKER


@pytest.mark.parametrize("io,config_brokers", [
    pytest.param(MessageReader, [BROKER, BROKER2, BROKER3]),
    pytest.param(MessageWriter, [BROKER, BROKER2, BROKER3]),
])
def test_three_brokers(io):
    with io(SERVICE) as f:
        assert len(f.params['brokers']) == 3
        assert f.params['brokers'][0] == BROKER
        assert f.params['brokers'][1] == BROKER2
        assert f.params['brokers'][2] == BROKER3
