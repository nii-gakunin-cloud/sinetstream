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

import logging

import pytest
from conftest import SERVICE
from sinetstream import MessageReader, MessageWriter, TEXT  # , InvalidArgumentError

logging.basicConfig(level=logging.ERROR)
pytestmark = pytest.mark.usefixtures('setup_config', 'dummy_reader_plugin', 'dummy_writer_plugin')

tmsgs = ['test message 001'*3,
         'test message 002'*3]  # note: *3 means compressable


@pytest.mark.parametrize('lvl', [1, 5, 9])
def test_comp_alg_level(config_topic, lvl):
    alg = "lz4"
    compression = {
        "algorithm": alg,
        "level": lvl,
    }
    with MessageWriter(SERVICE, value_type=TEXT,
                       data_compression=True,
                       compression=compression) as fw:
        for msg in tmsgs:
            fw.publish(msg)
    with MessageReader(SERVICE, value_type=TEXT,
                       data_compression=True,
                       compression=compression) as fr:
        for expected, msg in zip(tmsgs, fr):
            assert msg.topic == config_topic
            assert msg.value == expected
