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

import sinetstream

logging.basicConfig(level=logging.ERROR)


service = 'service-1'
topic = 'mss-test-001'
msgs = [b'test message 001',
        b'test message 002']


def test_thru(dummy_reader_plugin, dummy_writer_plugin):
    with sinetstream.MessageWriter(service, topic) as fw:
        for msg in msgs:
            fw.publish(msg)
    with sinetstream.MessageReader(service, topic) as fr:
        i = 0
        for msg in fr:
            assert msg.topic == topic
            assert msg.value == msgs[i]
            i += 1
            if i == len(msgs):
                break
    pass
