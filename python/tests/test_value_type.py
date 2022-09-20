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

import pytest
from sinetstream import (
    BYTE_ARRAY, TEXT, InvalidArgumentError, MessageReader, MessageWriter
)
from sinetstream.value_type import value_type_registry

from conftest import SERVICE

logging.basicConfig(level=logging.ERROR)
pytestmark = pytest.mark.usefixtures('setup_config', 'dummy_reader_plugin', 'dummy_writer_plugin')


bmsgs = [b'test message 001',
         b'test message 002']
tmsgs = ['test message 001',
         'test message 002']


def test_text(config_topic):
    with MessageWriter(SERVICE, value_type=TEXT) as fw:
        for msg in tmsgs:
            fw.publish(msg)
    with MessageReader(SERVICE, value_type=TEXT) as fr:
        for expected, msg in zip(tmsgs, fr):
            assert msg.topic == config_topic
            assert msg.value == expected


def test_byte_array(config_topic):
    with MessageWriter(SERVICE, value_type=BYTE_ARRAY) as fw:
        for msg in bmsgs:
            fw.publish(msg)
    with MessageReader(SERVICE, value_type=BYTE_ARRAY) as fr:
        for expected, msg in zip(bmsgs, fr):
            assert msg.topic == config_topic
            assert msg.value == expected


@pytest.mark.parametrize('io', [MessageReader, MessageWriter])
def test_invalid(io):
    with pytest.raises(InvalidArgumentError):
        with io(SERVICE, value_type="invalid") as _:
            pass


DEFAULT_ARG1 = "arg"
DEFAULT_ARG2 = ()
TEST_ARG1 = "abc"
TEST_ARG2 = (1, 2)


class DummyValueType:
    def __init__(self, arg1=DEFAULT_ARG1, arg2=DEFAULT_ARG2):
        self.args = dict(arg1=arg1, arg2=arg2)

    @property
    def serializer(self):
        return self.args

    @property
    def deserializer(self):
        return self.args


class DummyValueTypeEntryPoint:
    @classmethod
    def load(cls):
        return DummyValueType


value_type_registry.register("dummy", DummyValueTypeEntryPoint)


class DummyValueTypeParams:
    @property
    def name(self):
        return "dummy"

    @property
    def args(self):
        return dict(arg1=TEST_ARG1, arg2=TEST_ARG2)


IO_TUPLE = [(MessageWriter, "value_serializer"), (MessageReader, "value_deserializer")]


@pytest.mark.parametrize("io,prop", IO_TUPLE)
def test_str_type(io, prop):
    tgt = io(SERVICE, value_type="dummy")
    args = getattr(tgt, prop)
    assert args["arg1"] == DEFAULT_ARG1
    assert args["arg2"] == DEFAULT_ARG2


@pytest.mark.parametrize("io,prop", IO_TUPLE)
def test_no_args(io, prop):
    tgt = io(SERVICE, value_type={"name": "dummy"})
    args = getattr(tgt, prop)
    assert args["arg1"] == DEFAULT_ARG1
    assert args["arg2"] == DEFAULT_ARG2


@pytest.mark.parametrize("io,prop", IO_TUPLE)
def test_no_args2(io, prop):
    tgt = io(SERVICE, value_type={"name": "dummy", "arg1": TEST_ARG1})
    args = getattr(tgt, prop)
    assert args["arg1"] == DEFAULT_ARG1
    assert args["arg2"] == DEFAULT_ARG2


@pytest.mark.parametrize("io,prop", IO_TUPLE)
def test_arg1(io, prop):
    tgt = io(SERVICE, value_type={"name": "dummy", "args": {"arg1": TEST_ARG1}})
    args = getattr(tgt, prop)
    assert args["arg1"] == TEST_ARG1
    assert args["arg2"] == DEFAULT_ARG2


@pytest.mark.parametrize("io,prop", IO_TUPLE)
def test_arg2(io, prop):
    tgt = io(SERVICE, value_type={"name": "dummy", "args": {"arg2": TEST_ARG2}})
    args = getattr(tgt, prop)
    assert args["arg1"] == DEFAULT_ARG1
    assert args["arg2"] == TEST_ARG2


@pytest.mark.parametrize("io,prop", IO_TUPLE)
def test_args(io, prop):
    tgt = io(
        SERVICE,
        value_type={"name": "dummy", "args": {"arg1": TEST_ARG1, "arg2": TEST_ARG2}},
    )
    args = getattr(tgt, prop)
    assert args["arg1"] == TEST_ARG1
    assert args["arg2"] == TEST_ARG2


@pytest.mark.parametrize("io,prop", IO_TUPLE)
def test_args_obj(io, prop):
    tgt = io(SERVICE, value_type=DummyValueTypeParams())
    args = getattr(tgt, prop)
    assert args["arg1"] == TEST_ARG1
    assert args["arg2"] == TEST_ARG2
