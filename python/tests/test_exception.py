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

import logging
import os

import urllib
import pytest
import sinetstream

logging.basicConfig(level=logging.ERROR)


# NoServiceError
# NoConfigError
# InvalidArgumentError
# ConnectionError
# AlreadyConnectedError


saved_url = None


def push_url(url):
    global saved_url
    assert saved_url is None
    saved_url = os.environ.get("SINETSTREAM_CONFIG_URL")
    if url:
        os.environ["SINETSTREAM_CONFIG_URL"] = url
    else:
        os.environ.pop("SINETSTREAM_CONFIG_URL", None)


def pop_url():
    global saved_url
    if saved_url:
        os.environ["SINETSTREAM_CONFIG_URL"] = saved_url
    else:
        os.environ.pop("SINETSTREAM_CONFIG_URL", None)
    saved_url = None


def test_NoConfigError_XXX():
    try:
        push_url("http://localhost:8888")
        config = sinetstream.api.load_config()
    except urllib.error.URLError:
        pass
    else:
        assert False
    finally:
        pop_url()


def test_NoConfigError():
    try:
        push_url(None)
        save = sinetstream.api.config_files
        sinetstream.api.config_files = ["./NEVER-EXIST.YML"]
        config = sinetstream.api.load_config()
    except sinetstream.NoConfigError:
        pass
    else:
        assert False
    finally:
        pop_url()
        sinetstream.api.config_files = save


@pytest.mark.skip
def test_InvalidArgumentError():
    service = 'service-1'
    topic = 'mss-test-001'

    try:
        with sinetstream.MessageReader(service, topic, consistency=999) as f:
            pass
    except sinetstream.InvalidArgumentError:
        pass
    else:
        assert False

    try:
        with sinetstream.MessageWriter(service, topic, badparam=123) as f:
            pass
    except Exception:   # XXX:TODO
        pass
    else:
        assert False


@pytest.mark.skip
def test_ConnectionError():
    service = 'bad-service-1'
    topic = 'mss-test-001'

    try:
        with sinetstream.MessageReader(service, topic) as f:
            pass
    except sinetstream.ConnectionError:
        pass
    else:
        assert False

    try:
        with sinetstream.MessageWriter(service, topic) as f:
            pass
    except sinetstream.ConnectionError:
        pass
    else:
        assert False


@pytest.mark.skip
def test_AlreadyConnectedError():
    service = 'service-1'
    topic = 'mss-test-001'

    with sinetstream.MessageReader(service, topic) as f:
        try:
            with f as f2:
                pass
        except sinetstream.AlreadyConnectedError:
            pass
        else:
            assert False

    with sinetstream.MessageWriter(service, topic) as f:
        try:
            with f as f2:
                pass
        except sinetstream.AlreadyConnectedError:
            pass
        else:
            assert False


@pytest.fixture()
def notexistent_service(tmp_path):
    config_path = tmp_path / '.sinetstream_config.yml'
    with config_path.open(mode='w') as f:
        f.write('''
notexistent-service:
  type: notexistent
  brokers:
    - kafka0.dp1.nii.ac.jp:9092''')

    cwd = os.getcwd()
    os.chdir(tmp_path)
    yield
    os.chdir(cwd)


def test_UnsupportedServiceTypeError_reader(notexistent_service):
    with pytest.raises(sinetstream.UnsupportedServiceTypeError):
        sinetstream.MessageReader('notexistent-service', 'topic')


def test_UnsupportedServiceTypeError_writer(notexistent_service):
    with pytest.raises(sinetstream.UnsupportedServiceTypeError):
        sinetstream.MessageWriter('notexistent-service', 'topic')
