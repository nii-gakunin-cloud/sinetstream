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

import os
import yaml
from pathlib import Path
from logging import getLogger

import pytest

SERVICE = 'service-1'
TOPIC = 'mss-test-001'
TOPIC2 = 'mss-test-002'

BROKER = os.environ.get('KAFKA_BROKER', 'broker:9092')
SSL_BROKER = os.environ.get('KAFKA_SSL_BROKER')
USER_PASSWD_BROKER = os.environ.get('KAFKA_USER_PASSWD_BROKER')
SSL_BROKER_BAD_HOSTNAME = os.environ.get('KAFKA_SSL_BROKER_BAD_HOSTNAME')
KAFKA_USER = os.environ.get('KAFKA_USER')
KAFKA_PASSWD = os.environ.get('KAFKA_PASSWD')
KAFKA_READ_USER = os.environ.get('KAFKA_READ_USER')
KAFKA_READ_PASSWD = os.environ.get('KAFKA_READ_PASSWD')
KAFKA_WRITE_USER = os.environ.get('KAFKA_WRITE_USER')
KAFKA_WRITE_PASSWD = os.environ.get('KAFKA_WRITE_PASSWD')


def absolute_path(x):
    return Path(x).absolute() if x is not None else None


CACERT_PATH = absolute_path(os.environ.get('CACERT_PATH'))
CLIENT_CERT_PATH = absolute_path(os.environ.get('CLIENT_CERT_PATH'))
CLIENT_CERT_KEY_PATH = absolute_path(os.environ.get('CLIENT_CERT_KEY_PATH'))
CLIENT_BAD_CERT_PATH = absolute_path(os.environ.get('CLIENT_BAD_CERT_PATH'))
CLIENT_BAD_CERT_KEY_PATH = absolute_path(
    os.environ.get('CLIENT_BAD_CERT_KEY_PATH'))

logger = getLogger(__name__)


@pytest.fixture()
def config_brokers():
    return [BROKER]


@pytest.fixture()
def config_topics():
    return None


@pytest.fixture()
def config_value_type():
    return None


@pytest.fixture()
def config_params():
    return None


@pytest.fixture()
def setup_config(
        tmp_path, config_brokers, config_topics, config_value_type,
        config_params):
    cwd = Path.cwd().absolute()
    try:
        os.chdir(str(tmp_path))
        create_config_file(
            config_brokers, config_topics, config_value_type, config_params)
        yield
    finally:
        os.chdir(str(cwd))


def create_config_file(
        brokers=None, topics=None, value_type=None, params=None,
        config=Path('.sinetstream_config.yml')):
    parameters = {SERVICE: {'type': 'kafka'}}
    if brokers is not None:
        parameters[SERVICE]['brokers'] = brokers
    if topics is not None:
        parameters[SERVICE]['topic'] = topics
    if value_type is not None:
        parameters[SERVICE]['value_type'] = value_type
    if params is not None:
        parameters[SERVICE].update(params)
    logger.debug(f'CONFIG: {parameters}')
    with config.open(mode='w') as f:
        yaml.safe_dump(parameters, f)
