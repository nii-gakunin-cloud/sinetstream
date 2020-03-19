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
from conftest import SERVICE, TOPIC, BROKER

pytestmark = pytest.mark.usefixtures('setup_config')
BROKER2 = 'mqtt2'


@pytest.mark.parametrize("io,config_brokers", [
    pytest.param(MessageReader, None),
    pytest.param(MessageWriter, None),
    pytest.param(MessageReader, []),
    pytest.param(MessageWriter, []),
    pytest.param(MessageReader, [BROKER, BROKER2]),
    pytest.param(MessageWriter, [BROKER, BROKER2]),
])
def test_bad_brokers(io):
    with pytest.raises(InvalidArgumentError):
        io(SERVICE).open()


@pytest.mark.parametrize("io,config_brokers", [
    pytest.param(MessageReader, BROKER),
    pytest.param(MessageWriter, BROKER),
])
def test_brokers_str_type(io):
    with io(SERVICE) as f:
        assert f.params['brokers'] == BROKER


broker_host = BROKER.split(':')[0]
broker_port = int(BROKER.split(':', 1)[1])


@pytest.mark.skipif(broker_port != 1883, reason='broker is not default port.')
@pytest.mark.parametrize("io,config_brokers", [
    pytest.param(MessageReader, broker_host),
    pytest.param(MessageWriter, broker_host),
])
def test_brokers_no_port(io):
    with io(SERVICE) as f:
        assert f.params['brokers'] == broker_host


@pytest.mark.parametrize("io", [MessageReader, MessageWriter])
def test_brokers_list(io):
    with io(SERVICE) as f:
        assert f.params['brokers'][0] == BROKER
