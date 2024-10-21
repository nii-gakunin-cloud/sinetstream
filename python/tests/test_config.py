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
from shutil import copyfile

import pytest
import sinetstream
from conftest import create_config_file, SERVICE, SERVICE_TYPE, BROKER

logging.basicConfig(level=logging.WARNING)
logger = logging.getLogger(__name__)


@pytest.mark.parametrize("service", [SERVICE, None])
def test_load_config(setup_config, service):
    svc, params = sinetstream.configs.get_config_params(service)
    logger.info(f"params={params}")
    assert svc == SERVICE
    assert params is not None
    assert params["type"] == SERVICE_TYPE
    assert set(params["brokers"]) == {BROKER}


@pytest.mark.parametrize("service", [SERVICE, None])
def test_load_home_config(setup_home_config, service):
    svc, params = sinetstream.configs.get_config_params(service)
    logger.info(f"params={params}")
    assert svc == SERVICE
    assert params is not None
    assert params["type"] == SERVICE_TYPE
    assert set(params["brokers"]) == {BROKER}


@pytest.fixture()
def setup_home_config(tmp_path, config_brokers, config_topic):
    cwd = Path.cwd().absolute()
    config = Path('~/.config/sinetstream/config.yml').expanduser()
    backup = None
    try:
        os.chdir(str(tmp_path))
        if config.exists():
            backup = tmp_path / 'config.yml'
            copyfile(config, backup)
        else:
            config.parent.mkdir(parents=True, exist_ok=True)
        create_config_file(brokers=config_brokers, topic=config_topic, config=config)
        yield
    finally:
        os.chdir(str(cwd))
        if backup:
            copyfile(backup, config)
        else:
            config.unlink()
