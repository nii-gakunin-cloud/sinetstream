#!/usr/bin/env python3

# Copyright (C) 2021 National Institute of Informatics
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
from sinetstream import MessageReader, MessageWriter

logger = getLogger(__name__)
pytestmark = pytest.mark.usefixtures('dummy_reader_plugin', 'dummy_writer_plugin')


@pytest.mark.parametrize("io", [MessageReader, MessageWriter])
def test_config_data(setup_config_file, config_file, io):
    def readfile(fn, mode):
        with open(fn, mode=mode) as f:
            return f.read()
    with io(service=SERVICE, topic=TOPIC, config_file=config_file) as ss:
        logger.debug(f"params={ss.params}")
        kfn = {k: ss.params["tls"][k] for k in ["ca_certs", "certfile", "keyfile"]}
        for k, fn in kfn.items():
            assert readfile(fn, "rb") == _config_params()["tls"][k + "_data"]
        assert _config_params()["test_data"]["test1_data"] == readfile(ss.params["test_data"]["test1"], "r")
        assert _config_params()["test_data"]["test2_data"] == readfile(ss.params["test_data"]["test2"], "rb")
    for _, fn in kfn.items():
        # assert not os.path.exists(fn)
        pass


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


def _config_params():
    return {
        "tls": {
            "ca_certs": "tekitou",
            "ca_certs_data": b"test-ca_certs-content",
            "certfile_data": b"test-certfile-content",
            "keyfile_data": b"test-keyfile-content",
        },
        "test_data": {
            "test1_data":  "a_test1_data",
            "test2_data": b"a_test2_data",
        }
    }


@pytest.fixture()
def config_params():
    return _config_params()
