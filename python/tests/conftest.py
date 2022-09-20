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

import os
from collections import defaultdict
from concurrent.futures.thread import ThreadPoolExecutor
from logging import getLogger
from math import inf
from pathlib import Path
from queue import Queue, Empty
from random import choices
from string import ascii_letters

import pytest
import yaml
from promise import Promise

import sinetstream
from sinetstream.spi import (
    PluginMessageReader, PluginMessageWriter, PluginAsyncMessageReader,
    PluginAsyncMessageWriter,
)

logger = getLogger(__name__)
que = defaultdict(Queue)
test_qwrite_failure = []
test_qread_failure = []
SERVICE = 'service-1'
TOPIC = 'mss-test-001'
TOPIC2 = 'mss-test-002'
SERVICE_TYPE = 'dummy'
BROKER = 'broker'


def qwrite(topic, value):
    if len(test_qwrite_failure) > 0:
        logger.info(f"XXX test_qwrite_failure={len(test_qwrite_failure)}")
        test_qwrite_failure.pop()
        if len(test_qwrite_failure) == 0:
            raise Exception("TEST QRITE FAILURE")
    global que
    assert isinstance(value, bytes)
    que[topic].put(value)


def qread(topic, timeout=None):
    global que
    if len(test_qread_failure) > 0:
        logger.info(f"XXX test_qread_failure={len(test_qread_failure)}")
        test_qread_failure.pop()
        if len(test_qread_failure) == 0:
            if not que[topic].empty():
                que[topic].get(block=False)
            raise Exception("TEST QREAD FAILURE")
    return que[topic].get(timeout=timeout)


def qedit(topic, nque):
    global que
    oque = que[topic]
    que[topic] = nque
    return oque


def qclear():
    global que
    que = defaultdict(Queue)


class DummyReader(PluginMessageReader):
    def __init__(self, params):
        self._params = params
        self._timeout = (
            self._params['receive_timeout_ms'] / 1000.0
            if self._params['receive_timeout_ms'] != inf
            else None)

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
            try:
                value = qread(topic, timeout=self._timeout)
                raw = {"topic": topic, "value": value}
                return value, topic, raw
            except Empty:
                raise StopIteration()


class DummyReaderEntryPoint(object):
    @classmethod
    def load(cls):
        return DummyReader


class DummyAsyncReader(PluginAsyncMessageReader):

    def __init__(self, params):
        self._params = params
        self._executor = None
        self._reader_executor = None
        self._on_message = None
        self._on_failure = None
        self._closed = True

    def open(self):
        self._closed = False
        self._reader_executor = ThreadPoolExecutor(max_workers=1)
        self._reader_executor.submit(self._read_messages)
        self._executor = ThreadPoolExecutor()

    def close(self):
        self._closed = True
        if self._reader_executor is not None:
            self._reader_executor.shutdown()
        self._reader_executor = None
        if self._executor is not None:
            self._executor.shutdown()
        self._executor = None

    def metrics(self):
        return "this is a dummy metrics"

    def reset_metrics(self):
        pass

    def _read_messages(self):
        topics = self._params.get("topics")
        if type(topics) != list:
            topics = [topics]
        while not self._closed:
            for topic in topics:
                try:
                    value = qread(topic, timeout=0.1)
                    raw = {"topic": topic, "value": value}
                    self._executor.submit(self._on_message, value, topic, raw)
                except Empty:
                    continue
                except Exception as e:
                    self._executor.submit(self._on_failure, e)

    @property
    def on_message(self):
        return self._on_message

    @on_message.setter
    def on_message(self, on_message):
        self._on_message = on_message

    @property
    def on_failure(self):
        return self._on_failure

    @on_failure.setter
    def on_failure(self, on_failure):
        self._on_failure = on_failure


class DummyAsyncReaderEntryPoint(object):
    @classmethod
    def load(cls):
        return DummyAsyncReader


@pytest.fixture(scope='session')
def dummy_reader_plugin():
    sinetstream.MessageReader.registry.register(SERVICE_TYPE, DummyReaderEntryPoint)
    sinetstream.AsyncMessageReader.registry.register(SERVICE_TYPE, DummyAsyncReaderEntryPoint)


class DummyWriter(PluginMessageWriter):
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

    def publish(self, value):
        qwrite(self._params.get("topic"), value)


class DummyWriterEntryPoint(object):
    @classmethod
    def load(cls):
        return DummyWriter


class DummyAsyncWriter(PluginAsyncMessageWriter):
    def __init__(self, params):
        self._params = params
        self._executor = None

    def open(self):
        self._executor = ThreadPoolExecutor()

    def close(self):
        if self._executor is not None:
            self._executor.shutdown()
            self._executor = None

    def metrics(self):
        return "this is a dummy metrics"

    def reset_metrics(self):
        pass

    def publish(self, value):
        future = self._executor.submit(qwrite, self._params.get("topic"), value)
        return Promise.cast(future)


class DummyAsyncWriterEntryPoint(object):
    @classmethod
    def load(cls):
        return DummyAsyncWriter


@pytest.fixture(scope='session')
def dummy_writer_plugin():
    sinetstream.MessageWriter.registry.register(SERVICE_TYPE, DummyWriterEntryPoint)
    sinetstream.AsyncMessageWriter.registry.register(SERVICE_TYPE, DummyAsyncWriterEntryPoint)


@pytest.fixture()
def config_brokers():
    return [BROKER]


@pytest.fixture()
def config_topic():
    return ''.join(choices(ascii_letters, k=10))


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


def generate_config_v1(brokers, topic, params, config, version):
    parameters = {SERVICE: {'type': SERVICE_TYPE}}
    if brokers is not None:
        parameters[SERVICE]['brokers'] = brokers
    if topic is not None:
        parameters[SERVICE]['topic'] = topic
    if params is not None:
        parameters[SERVICE].update(params)
    return parameters


def generate_config_v2(brokers, topic, params, config, version):
    contents = {
        "header": {
            "version": version,
        },
        "config": generate_config_v1(brokers, topic, params, config, version)
    }
    return contents


def create_config_file(
        brokers=None, topic=None, params=None, config=Path('.sinetstream_config.yml'),
        version=2,
):
    gen = {
        1: generate_config_v1,
        2: generate_config_v2,
        99: generate_config_v2,
    }
    parameters = gen[version](brokers, topic, params, config, version)
    with config.open(mode='w') as f:
        sinetstream.configs.setup_yaml()
        yaml.safe_dump(parameters, f)
    # with config.open() as f:
    #     logger.error(f"XXX CONFIG: {f.read()}")


@pytest.fixture()
def setup_auth(tmp_path):
    # global tmp_auth_path
    tmp_auth_path = tmp_path / "tmp_auth.json"
    with tmp_auth_path.open(mode="w") as f:
        import json
        json.dump(
            {
                "config-server": {
                    "address": "https://ss-cfg.example.org",
                    "user": "mr example",
                    "secret-key": "1234",
                    "expiration-date": "1990-01-01T00:00:00Z"
                }
            }, f)
    return tmp_auth_path


@pytest.fixture()
def setup_privkey(tmp_path):
    tmp_privkey_path = tmp_path / "private_key.pem"
    with tmp_privkey_path.open(mode="w") as f:
        f.write("""-----BEGIN RSA PRIVATE KEY-----
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
-----END RSA PRIVATE KEY-----""")
    sinetstream.configs.set_key_cache(sinetstream.configs.load_private_key(tmp_privkey_path))
