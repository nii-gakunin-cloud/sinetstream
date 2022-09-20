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

import collections
import os
from logging import getLogger
from pathlib import Path

import cv2
import pytest
import yaml

import sinetstream

que = None
logger = getLogger(__name__)
SERVICE = 'service-1'
TOPIC = 'mss-test-001'
SERVICE_TYPE = 'dummy'
BROKER = 'broker'
TEST_IMAGE = Path('tests/test-00.png').absolute()


def qwrite(topic, value):
    assert isinstance(value, bytes)
    que[topic].append(value)


def qread(topic):
    if topic not in que:
        return None
    q = que[topic]
    if len(q) == 0:
        return None
    return q.popleft()


def qedit(topic, nque):
    global que
    oque = que[topic]
    que[topic] = nque
    return oque


class DummyReader(object):
    def __init__(self, params):
        self._params = params

    def open(self):
        pass

    def close(self):
        pass

    def metrics(self):
        return "this is a dummy metrics"

    def reset_metrics(self):
        pass

    def __iter__(self):
        return self

    def __next__(self):
        topics = self._params.get("topics")
        if type(topics) != list:
            topics = [topics]
        for topic in topics:
            value = qread(topic)
            if value is not None:
                raw = {"topic": topic, "value": value}
                return value, topic, raw
        raise StopIteration()


class DummyReaderEntryPoint(object):
    @classmethod
    def load(cls):
        return DummyReader


@pytest.fixture(scope='session')
def dummy_reader_plugin():
    sinetstream.MessageReader.registry.register(SERVICE_TYPE, DummyReaderEntryPoint)


class DummyWriter(object):
    def __init__(self, params):
        global que
        que = collections.defaultdict(collections.deque)
        self._params = params

    def open(self):
        pass

    def close(self):
        pass

    def metrics(self):
        return "this is a dummy metrics"

    def reset_metrics(self):
        pass

    def publish(self, value):
        qwrite(self._params.get("topic"), value)


class DummyWriterEntryPoint(object):
    @classmethod
    def load(cls):
        return DummyWriter


@pytest.fixture(scope='session')
def dummy_writer_plugin():
    sinetstream.MessageWriter.registry.register(SERVICE_TYPE, DummyWriterEntryPoint)


@pytest.fixture()
def config_brokers():
    return [BROKER]


@pytest.fixture()
def config_topic():
    return TOPIC


@pytest.fixture()
def config_params():
    return None


@pytest.fixture()
def setup_config(tmp_path, config_brokers, config_topic, config_params):
    cwd = Path.cwd().absolute()
    try:
        os.chdir(str(tmp_path))
        create_config_file(config_brokers, config_topic, config_params)
        yield
    finally:
        os.chdir(str(cwd))


def create_config_file(
        brokers=None, topic=None, params=None, config=Path('.sinetstream_config.yml'),
):
    parameters = {SERVICE: {'type': SERVICE_TYPE}}
    if brokers is not None:
        parameters[SERVICE]['brokers'] = brokers
    if topic is not None:
        parameters[SERVICE]['topic'] = topic
    if params is not None:
        parameters[SERVICE].update(params)
    with config.open(mode='w') as f:
        yaml.safe_dump(parameters, f)


@pytest.fixture
def image_object():
    img = cv2.imread(str(TEST_IMAGE))
    if img is None:
        raise RuntimeError()
    return img


@pytest.fixture
def image_bytes():
    with TEST_IMAGE.open(mode='rb') as f:
        return f.read()
