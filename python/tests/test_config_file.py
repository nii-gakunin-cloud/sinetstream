#!/usr/local/bin/python3.6
# vim: expandtab shiftwidth=4

# Copyright (C) 2020 National Institute of Informatics
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

import os
from logging import getLogger
from pathlib import Path

import pytest
from conftest import create_config_file, SERVICE, TOPIC
from sinetstream import MessageReader, MessageWriter, AT_MOST_ONCE, BYTE_ARRAY, NoConfigError

logger = getLogger(__name__)
pytestmark = pytest.mark.usefixtures('dummy_reader_plugin', 'dummy_writer_plugin')


@pytest.mark.parametrize("io", [MessageReader, MessageWriter])
def test_config_dir(setup_config_file, config_file, io):
    ss = io(service=SERVICE, topic=TOPIC, config_file=config_file)
    assert ss.consistency == AT_MOST_ONCE
    assert ss.value_type == BYTE_ARRAY


@pytest.mark.parametrize("io", [MessageReader, MessageWriter])
def test_config_dir_not_exists(setup_config, config_file, io):
    with pytest.raises(NoConfigError):
        io(service=SERVICE, topic=TOPIC, config_file=config_file)


@pytest.fixture()
def setup_config_file(tmp_path, config_brokers, config_topic, config_params, config_file):
    cwd = Path.cwd().absolute()
    try:
        os.chdir(str(tmp_path))
        create_config_file(config_brokers, config_topic, config_params, config=config_file)
        yield
    finally:
        os.chdir(str(cwd))


@pytest.fixture()
def config_file(tmp_path):
    return Path("ss.yml")
