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
    assert type(value) is bytes
    que[topic].put(value)


def qread(topic, timeout=None):
    if len(test_qread_failure) > 0:
        logger.info(f"XXX test_qread_failure={len(test_qread_failure)}")
        test_qread_failure.pop()
        if len(test_qread_failure) == 0:
            raise Exception("TEST QREAD FAILURE")
    global que
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
