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

import logging

import numpy as np
import pytest
from conftest import SERVICE, TOPIC
from sinetstream import MessageReader, MessageWriter, InvalidArgumentError

logging.basicConfig(level=logging.ERROR)
pytestmark = pytest.mark.usefixtures('setup_config', 'dummy_reader_plugin', 'dummy_writer_plugin')


def test_image(image_object):
    msgs = [image_object, image_object]

    with MessageWriter(SERVICE, value_type='image') as fw:
        for msg in msgs:
            fw.publish(msg)

    with MessageReader(SERVICE, value_type='image') as fr:
        for expected, msg in zip(msgs, fr):
            assert msg.topic == TOPIC
            assert np.array_equal(msg.value, expected)


def test_publish_bad_type():
    with MessageWriter(SERVICE, value_type='image') as f:
        with pytest.raises(InvalidArgumentError):
            f.publish(b'abc')


def test_read_bad_type():
    with MessageWriter(SERVICE, value_type='text') as fw:
        fw.publish('messsage-001')

    with MessageReader(SERVICE, value_type='image') as fr:
        with pytest.raises(InvalidArgumentError):
            for _ in fr:
                pass
