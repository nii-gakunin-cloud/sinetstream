#!/usr/bin/env python3

# Copyright (C) 2022 National Institute of Informatics
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
from logging import getLogger, DEBUG

import pytest

SERVICE = 'service-1'
TOPIC = 'mss-test-001'
TOPIC2 = 'mss-test-002'

logger = getLogger(__name__)
logger.setLevel(DEBUG)


@pytest.fixture()
def setup_config(tmp_path):
    cwd = Path.cwd().absolute()
    try:
        os.chdir(str(tmp_path))
        create_config_file()
        yield
    finally:
        os.chdir(str(cwd))


def getenv(k, d):
    from os import environ
    return environ.get(k, d)


S3_ENDPOINT_URL = getenv("S3_ENDPOINT_URL", "http://127.0.0.1:9000")
S3_BUCKET = getenv("S3_BUCKET", "sstest")
S3_PREFIX = getenv("S3_PREFIX", "testprefix")
S3_SUFFIX = getenv("S3_SUFFIX", ".binbin")
S3_NAME = getenv("S3_NAME", "day")
S3_AWS_ACCESS_KEY_ID = getenv("S3_AWS_ACCESS_KEY_ID", "minioadmin")
S3_AWS_SECRET_ACCESS_KEY = getenv("S3_AWS_SECRET_ACCESS_KEY", "minioadmin")


def create_config_file(config=Path('.sinetstream_config.yml')):
    parameters = {
        "header": {
            "versoin": 3
        },
        "config": {
            SERVICE: {
                "type": "s3",
                "topic": TOPIC,
                # "brokers": XXX,
                # "user_data_only": False,
                # consistency: AT_MOST_ONCE,
                "type_spec": {
                    "endpoint_url": S3_ENDPOINT_URL,
                    "bucket": S3_BUCKET,
                    "prefix": S3_PREFIX,
                    "suffix": S3_SUFFIX,
                    "name": S3_NAME,
                    "aws_access_key_id": S3_AWS_ACCESS_KEY_ID,
                    "aws_secret_access_key": S3_AWS_SECRET_ACCESS_KEY,
                    # "utc_offset": "+0900",
                },
            }
        }
    }
    logger.debug(f'CONFIG: {parameters}')
    with config.open(mode='w') as f:
        yaml.safe_dump(parameters, f)
