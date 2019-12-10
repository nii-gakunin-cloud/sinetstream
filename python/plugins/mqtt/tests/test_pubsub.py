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

import time
import logging
import pytest
import copy
from threading import Thread, Semaphore
from queue import Queue

import sinetstream

logging.basicConfig(level=logging.ERROR)
# logging.basicConfig(level=logging.DEBUG)


service = 'service-2'
topic = 'mss-test-002'
text_msgs = [
    'test message 001',
    'test message 002']
sem = Semaphore(0)


def reader(que, len_msgs, *args, **kwargs):
    with sinetstream.MessageReader(*args, **kwargs) as f:
        sem.release()
        for idx, msg in zip(range(len_msgs), f):
            logging.info(f"{msg.raw}")
            que.put(msg)


def writer(msgs, *args, **kwargs):
    with sinetstream.MessageWriter(*args, **kwargs) as f:
        for msg in msgs:
            f.publish(msg)


@pytest.fixture()
def pubsub():
    q = Queue()
    msgs = copy.copy(text_msgs)
    reader_params = {
        'service': service,
        'topics': topic,
    }
    writer_params = {
        'service': service,
        'topic': topic,
    }
    yield (msgs, reader_params, writer_params)

    th = Thread(target=reader, args=(q, len(msgs)), kwargs=reader_params)
    th.start()
    sem.acquire()
    writer(msgs, **writer_params)
    th.join()

    for expected in msgs:
        msg = q.get_nowait()
        assert msg.topic == topic
        assert msg.value == expected


def test_pubsub(pubsub):
    msgs, _, _ = pubsub
    msgs.clear()
    msgs.extend([x.encode() for x in text_msgs])


hdr = b"XXX"


def ser(x):
    return hdr + x.encode()


def des(x):
    return x[len(hdr):].decode()


def test_pubsub_serdes(pubsub):
    _, reader_params, writer_params = pubsub
    reader_params.update({'value_deserializer': des})
    writer_params.update({'value_serializer': ser})


def test_pubsub_value_type(pubsub):
    _, reader_params, writer_params = pubsub
    reader_params.update({'value_type': "text"})
    writer_params.update({'value_type': "text"})


def test_pubsub_value_type_config(pubsub):
    _, reader_params, writer_params = pubsub
    service_text = service + "-text"
    reader_params.update({'service': service_text})
    writer_params.update({'service': service_text})


def test_pubsub_value_type_config_and_arg(pubsub):
    _, reader_params, writer_params = pubsub
    service_text = service + "-image"
    reader_params.update({
        'service': service_text,
        'value_type': 'text',
    })
    writer_params.update({
        'service': service_text,
        'value_type': 'text',
    })
