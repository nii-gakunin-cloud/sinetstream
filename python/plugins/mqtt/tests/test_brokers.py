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


@pytest.mark.parametrize("io", [MessageReader, MessageWriter])
def test_two_brokers(io):
    with pytest.raises(InvalidArgumentError):
        io('service-2-two-brokers').open()


@pytest.mark.parametrize("io", [MessageReader, MessageWriter])
def test_no_broker(io):
    with pytest.raises(InvalidArgumentError):
        io('service-2-no-broker').open()


@pytest.mark.parametrize("io", [MessageReader, MessageWriter])
def test_empty_broker_list(io):
    with pytest.raises(InvalidArgumentError):
        io('service-2-no-broker', brokers=[]).open()


@pytest.mark.parametrize("io", [MessageReader, MessageWriter])
def test_brokers_str_type(io):
    with io('service-2-str-brokers') as f:
        assert f.params['brokers'] == '192.168.2.105:1883'


@pytest.mark.parametrize("io", [MessageReader, MessageWriter])
def test_brokers_list(io):
    with io('service-2-list-brokers') as f:
        assert f.params['brokers'][0] == '192.168.2.105:1883'
