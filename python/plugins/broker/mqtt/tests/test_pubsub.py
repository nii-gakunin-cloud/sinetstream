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

import copy
import logging
import os
from pathlib import Path
from queue import Queue
from tempfile import TemporaryDirectory
from threading import Thread, Event
from time import sleep

import pytest
from conftest import BROKER, SERVICE, TOPIC, create_config_file
from sinetstream import MessageReader, MessageWriter

logging.basicConfig(level=logging.CRITICAL)
logger = logging.getLogger(__name__)
pytestmark = pytest.mark.usefixtures('setup_config')


text_msgs = [
    'test message 001',
    'test message 002',
]
ev = Event()


def reader(que, len_msgs, *args, **kwargs):
    try:
        with MessageReader(*args, **kwargs) as f:
            ev.set()
            logger.debug("start message reading")
            for idx, msg in zip(range(len_msgs), f):
                logger.info(f"read message: {msg.raw}")
                que.put(msg)
    finally:
        ev.set()


def writer(msgs, *args, **kwargs):
    ev.wait()
    logger.debug("start message writing")
    with MessageWriter(*args, **kwargs) as f:
        for msg in msgs:
            logger.info(f"write message: {msg}")
            f.publish(msg)


@pytest.fixture()
def pubsub():
    q = Queue()
    msgs = copy.copy(text_msgs)
    reader_params = {
        'service': SERVICE,
        'topics': TOPIC,
    }
    writer_params = {
        'service': SERVICE,
        'topic': TOPIC,
    }
    yield msgs, reader_params, writer_params

    ev.clear()
    ths = [
        Thread(target=reader, args=(q, len(msgs)), kwargs=reader_params),
        Thread(target=writer, args=(msgs, ), kwargs=writer_params),
    ]
    for th in ths:
        th.start()
        sleep(1)    # Workaround to avoid writing until ready to read.

    for th in ths:
        th.join()

    for expected in msgs:
        msg = q.get_nowait()
        assert msg.topic == TOPIC
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


@pytest.mark.parametrize('config_value_type', ['text'])
def test_pubsub_value_type_config(pubsub):
    pass


@pytest.mark.parametrize('config_value_type', ['image'])
def test_pubsub_value_type_config_and_arg(pubsub):
    _, reader_params, writer_params = pubsub
    reader_params.update({'value_type': "text"})
    writer_params.update({'value_type': "text"})


@pytest.fixture(scope='module', autouse=True)
def create_topic():
    cwd = Path.cwd().absolute()
    with TemporaryDirectory() as work_dir:
        try:
            os.chdir(str(work_dir))
            create_config_file(brokers=[BROKER])
            with MessageWriter(SERVICE, TOPIC) as f:
                logger.debug(f"create topic: {TOPIC}")
                f.publish(b"message 000")
        finally:
            os.chdir(str(cwd))
