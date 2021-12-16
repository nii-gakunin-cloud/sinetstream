#!/usr/local/bin/python3.6

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
    MessageReader, MessageWriter, AlreadyConnectedError, ConnectionError)
from conftest import SERVICE, TOPIC

logging.basicConfig(level=logging.CRITICAL)
pytestmark = pytest.mark.usefixtures('setup_config')


@pytest.mark.parametrize("io,config_brokers", [
    pytest.param(MessageReader, ["localhost:9000"]),
    pytest.param(MessageWriter, ["localhost:9000"]),
])
def test_connection_error(io):
    with pytest.raises(ConnectionError):
        with io(SERVICE, TOPIC) as _:
            pass


@pytest.mark.parametrize("io", [MessageReader, MessageWriter])
def test_already_connected_error(io):
    with io(SERVICE, TOPIC) as f:
        with pytest.raises(AlreadyConnectedError):
            f.open()
