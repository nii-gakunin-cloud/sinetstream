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
import pytest

import sinetstream

logging.basicConfig(level=logging.ERROR)


service = 'service-crypto-1'
topic = 'mss-test-001'
msgs = ['test message 001',
        'test message 002']

bmsgs = [x.encode() for x in msgs]


def test_thru(dummy_reader_plugin, dummy_writer_plugin):
    with sinetstream.MessageWriter(service, topic, value_type=sinetstream.TEXT) as fw:
        for msg in msgs:
            fw.publish(msg)
    with sinetstream.MessageReader(service, topic, value_type=sinetstream.TEXT) as fr:
        i = 0
        for msg in fr:
            assert msg.topic == topic
            assert msg.value == msgs[i]
            i += 1
            if i == len(msgs):
                break
    pass


def test_enc_bin(dummy_reader_plugin, dummy_writer_plugin):
    with sinetstream.MessageWriter(service,
                                   topic,
                                   value_type=sinetstream.BYTE_ARRAY,
                                   data_encryption=True) as fw:
        for msg in bmsgs:
            fw.publish(msg)
    with sinetstream.MessageReader(service,
                                   topic,
                                   value_type=sinetstream.BYTE_ARRAY,
                                   data_encryption=True) as fr:
        i = 0
        for msg in fr:
            assert msg.topic == topic
            assert msg.value == bmsgs[i]
            i += 1
            if i == len(msgs):
                break
    pass


def test_enc_text(dummy_reader_plugin, dummy_writer_plugin):
    with sinetstream.MessageWriter(service,
                                   topic,
                                   value_type=sinetstream.TEXT,
                                   data_encryption=True) as fw:
        for msg in msgs:
            fw.publish(msg)
    with sinetstream.MessageReader(service,
                                   topic,
                                   value_type=sinetstream.TEXT,
                                   data_encryption=True) as fr:
        i = 0
        for msg in fr:
            assert msg.topic == topic
            assert msg.value == msgs[i]
            i += 1
            if i == len(msgs):
                break
    pass


@pytest.mark.parametrize("mode",
                         ["CBC",
                          "CFB",
                          "OFB",
                          "CTR",
                          "OPENPGP",
                          "OPENPGPCFB",
                          "EAX",
                          "GCM"])
def test_enc_mode(mode, dummy_reader_plugin, dummy_writer_plugin):
    with sinetstream.MessageWriter(service,
                                   topic,
                                   value_type=sinetstream.TEXT,
                                   data_encryption=True,
                                   crypto={"mode": mode}) as fw:
        for msg in msgs:
            fw.publish(msg)
    with sinetstream.MessageReader(service,
                                   topic,
                                   value_type=sinetstream.TEXT,
                                   data_encryption=True,
                                   crypto={"mode": mode}) as fr:
        i = 0
        for msg in fr:
            assert msg.topic == topic
            assert msg.value == msgs[i]
            i += 1
            if i == len(msgs):
                break
