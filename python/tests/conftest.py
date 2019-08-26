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

import collections

import pytest
import sinetstream


que = collections.defaultdict(collections.deque)


def qwrite(topic, value):
    assert type(value) is bytes
    que[topic].append(value)


def qread(topic):
    if topic not in que:
        return None
    q = que[topic]
    if len(q) == 0:
        return None
    return q.popleft()


class DummyMessage(sinetstream.Message):
    def __init__(self, topic, value):
        self.topic = topic
        self.value = value
        self.raw = {"topic": topic, "value": value}


class DummyKafkaReader(object):
    def __init__(self, message_reader):
        self._reader = message_reader

    def open(self):
        if self._reader._client_id is None:
            self._reader._client_id = "client_id"

    def close(self):
        pass

    def __iter__(self):
        return self

    def __next__(self):
        topics = self._reader.topics
        if type(topics) != list:
            topics = [topics]
        for topic in topics:
            value = qread(topic)
            if value:
                if self._reader.value_deserializer:
                    value = self._reader.value_deserializer(value)
                return DummyMessage(topic, value)
        raise StopIteration()


class DummyKafkaReaderEntryPoint(object):
    @classmethod
    def load(cls):
        return DummyKafkaReader


@pytest.fixture(scope='session')
def dummy_reader_plugin():
    sinetstream.MessageReader.registry.register('kafka', DummyKafkaReaderEntryPoint)


class DummyKafkaWriter(object):
    def __init__(self, message_writer):
        self._writer = message_writer

    def open(self):
        if self._writer._client_id is None:
            self._writer._client_id = "client_id"

    def close(self):
        pass

    def publish(self, value):
        if self._writer.value_serializer:
            value = self._writer.value_serializer(value)
        qwrite(self._writer.topic, value)


class DummyKafkaWriterEntryPoint(object):
    @classmethod
    def load(cls):
        return DummyKafkaWriter


@pytest.fixture(scope='session')
def dummy_writer_plugin():
    sinetstream.MessageWriter.registry.register('kafka', DummyKafkaWriterEntryPoint)
