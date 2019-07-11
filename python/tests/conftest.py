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

import pytest
import sinetstream


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
        raise StopIteration()


class DummyKafkaEntryPoint(object):
    @classmethod
    def load(cls):
        return DummyKafkaReader


@pytest.fixture(scope='session')
def dummy_reader_plugin():
    sinetstream.MessageReader.registry.register('kafka', DummyKafkaEntryPoint)


class DummyKafkaWriter(object):
    def __init__(self, message_writer):
        self._writer = message_writer

    def open(self):
        if self._writer._client_id is None:
            self._writer._client_id = "client_id"

    def close(self):
        pass


class DummyKafkaEntryPoint(object):
    @classmethod
    def load(cls):
        return DummyKafkaWriter


@pytest.fixture(scope='session')
def dummy_writer_plugin():
    sinetstream.MessageWriter.registry.register('kafka', DummyKafkaEntryPoint)
