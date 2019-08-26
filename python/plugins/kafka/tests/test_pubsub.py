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
import threading
import logging

import sinetstream

logging.basicConfig(level=logging.ERROR)


service = 'service-1'
topic = 'mss-test-001'
msgs = ['test message 001',
        'test message 002']
sem = threading.Semaphore(0)


def reader(msgs, *args, **kwargs):
    with sinetstream.MessageReader(*args, **kwargs) as f:
        sem.release()
        i = 0
        for msg in f:
            logging.info(f"{msg.raw}")
            assert msg.topic == topic
            assert msg.value == msgs[i]
            i += 1
            if i == len(msgs):
                break


def writer(msgs, *args, **kwargs):
    with sinetstream.MessageWriter(*args, **kwargs) as f:
        for msg in msgs:
            f.publish(msg)


def test_pubsub():
    binmsgs = [x.encode() for x in msgs]
    t1 = threading.Thread(target=reader, args=(binmsgs, service, topic))
    t2 = threading.Thread(target=writer, args=(binmsgs, service, topic))
    t1.start()
    sem.acquire()
    time.sleep(1)       # XXX
    t2.start()


hdr = b"XXX"


def ser(x):
    return hdr + x.encode()


def des(x):
    return x[len(hdr):].decode()


def test_pubsub_serdes():
    t1 = threading.Thread(target=reader,
                          args=(msgs, service, topic),
                          kwargs={"value_deserializer": des})
    t2 = threading.Thread(target=writer,
                          args=(msgs, service, topic),
                          kwargs={"value_serializer": ser})
    t1.start()
    sem.acquire()
    time.sleep(1)       # XXX
    t2.start()


def test_pubsub_value_type():
    t1 = threading.Thread(target=reader,
                          args=(msgs, service, topic),
                          kwargs={"value_type": "text"})
    t2 = threading.Thread(target=writer,
                          args=(msgs, service, topic),
                          kwargs={"value_type": "text"})
    t1.start()
    sem.acquire()
    time.sleep(1)       # XXX
    t2.start()


def test_pubsub_value_type_config():
    service_text = service + "-text"
    t1 = threading.Thread(target=reader,
                          args=(msgs, service_text, topic))
    t2 = threading.Thread(target=writer,
                          args=(msgs, service_text, topic))
    t1.start()
    sem.acquire()
    time.sleep(1)       # XXX
    t2.start()


def test_pubsub_value_type_config_and_arg():
    service_text = service + "-image"
    t1 = threading.Thread(target=reader,
                          args=(msgs, service_text, topic),
                          kwargs={"value_type": "text"})
    t2 = threading.Thread(target=writer,
                          args=(msgs, service_text, topic),
                          kwargs={"value_type": "text"})
    t1.start()
    sem.acquire()
    time.sleep(1)       # XXX
    t2.start()
