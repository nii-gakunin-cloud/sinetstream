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

import logging
import os
from pathlib import Path

import pytest
from sinetstream import (
    MessageWriter, MessageReader, NoServiceError, NoConfigError,
    InvalidArgumentError, UnsupportedServiceTypeError,
)
from conftest import SERVICE

logging.basicConfig(level=logging.CRITICAL)
BAD_SERVICE = 'bad-service'


@pytest.mark.parametrize('io', [MessageReader, MessageWriter])
def test_no_service_error(io, setup_config):
    with pytest.raises(NoServiceError):
        with io(BAD_SERVICE) as _:
            pass


@pytest.mark.parametrize('io', [MessageReader, MessageWriter])
def test_no_config(io, setup_no_config):
    with pytest.raises(NoConfigError):
        with io(SERVICE) as _:
            pass


@pytest.fixture()
def setup_no_config(tmp_path):
    cwd = Path.cwd().absolute()
    try:
        os.chdir(str(tmp_path))
        yield
    finally:
        os.chdir(str(cwd))


@pytest.mark.parametrize("io", [MessageWriter, MessageReader])
def test_bad_consistency(io, setup_config):
    with pytest.raises(InvalidArgumentError):
        with io(SERVICE, consistency=999) as _:
            pass


@pytest.mark.skip
@pytest.mark.parametrize("io,config_topic", [
    pytest.param(MessageWriter, None),
    pytest.param(MessageReader, None),
])
def test_no_topic(io, setup_config):
    with pytest.raises(InvalidArgumentError):
        with io(SERVICE) as _:
            pass


@pytest.mark.parametrize("io,config_params", [
    pytest.param(MessageWriter, {'type': 'unsupported'}),
    pytest.param(MessageReader, {'type': 'unsupported'}),
])
def test_unsupported_service_type(io, setup_config):
    with pytest.raises(UnsupportedServiceTypeError):
        with io(SERVICE) as _:
            pass
