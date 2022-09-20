#!/usr/bin/env python3

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

import pytest
from conftest import SERVICE, TOPIC
from sinetstream import MessageReader, MessageWriter, AsyncMessageReader, AsyncMessageWriter
from sinetstream import InvalidConfigError, InvalidArgumentError
from sinetstreamplugin.s3 import BaseS3Reader
from threading import Condition

# import boto3
# import os
# from moto import mock_s3
# os.environ["MOTO_S3_CUSTOM_ENDPOINTS"] = "http://a.b.c.d"


msgs = [
    b"",
    b"1",
    b"test2",
    b"test3" * 1000,
]


@pytest.mark.parametrize("prefix, topics, name, suffix, oks, ngs", [
    pytest.param("prepre", ["aaa"], "day", ".dat",
                 ["prepre/aaa/2000/01/01/aaa-8360b04f-53d4-4cdb-93f5-34b6b43a2251-123.dat",
                  "prepre/aaa/1990/12/31/aaa-8360b04f-53d4-4cdb-93f5-34b6b43a2251-123.dat",
                  ],
                 ["xprepre/aaa/2000/01/01/aaa-8360b04f-53d4-4cdb-93f5-34b6b43a2251-123.dat",
                  "prepre/xaaa/2000/01/01/aaa-8360b04f-53d4-4cdb-93f5-34b6b43a2251-123.dat",
                  "prepre/aaa/0000/01/01/aaa-8360b04f-53d4-4cdb-93f5-34b6b43a2251-123.dat",
                  "prepre/aaa/2000/00/01/aaa-8360b04f-53d4-4cdb-93f5-34b6b43a2251-123.dat",
                  "prepre/aaa/2000/13/01/aaa-8360b04f-53d4-4cdb-93f5-34b6b43a2251-123.dat",
                  "prepre/aaa/2000/20/01/aaa-8360b04f-53d4-4cdb-93f5-34b6b43a2251-123.dat",
                  "prepre/aaa/2000/01/00/aaa-8360b04f-53d4-4cdb-93f5-34b6b43a2251-123.dat",
                  "prepre/aaa/2000/01/32/aaa-8360b04f-53d4-4cdb-93f5-34b6b43a2251-123.dat",
                  "prepre/aaa/2000/01/32/aaa-8360b04f-53d4-4cdb-93f5-34b6b43a2251-123.dat",
                  "prepre/aaa/2000/01/01/xaaa-8360b04f-53d4-4cdb-93f5-34b6b43a2251-123.dat",
                  "prepre/aaa/2000/01/01/aaa-8360b04f-53d4-4cdb-93f5-34b6b43a2251-123.datx",
                  ]),
    pytest.param("prepre", ["aaa"], "hour", ".dat",
                 ["prepre/aaa/2000/01/01/00/aaa-8360b04f-53d4-4cdb-93f5-34b6b43a2251-123.dat",
                  "prepre/aaa/2000/01/01/01/aaa-8360b04f-53d4-4cdb-93f5-34b6b43a2251-123.dat",
                  "prepre/aaa/2000/01/01/11/aaa-8360b04f-53d4-4cdb-93f5-34b6b43a2251-123.dat",
                  "prepre/aaa/2000/01/01/23/aaa-8360b04f-53d4-4cdb-93f5-34b6b43a2251-123.dat",
                  ],
                 ["prepre/aaa/2000/01/01/24/aaa-8360b04f-53d4-4cdb-93f5-34b6b43a2251-123.dat",
                  ]),
    pytest.param("prepre", ["aaa"], "minute", ".dat",
                 ["prepre/aaa/2000/01/01/00/00/aaa-8360b04f-53d4-4cdb-93f5-34b6b43a2251-123.dat",
                  "prepre/aaa/2000/01/01/00/01/aaa-8360b04f-53d4-4cdb-93f5-34b6b43a2251-123.dat",
                  "prepre/aaa/2000/01/01/00/10/aaa-8360b04f-53d4-4cdb-93f5-34b6b43a2251-123.dat",
                  "prepre/aaa/2000/01/01/00/30/aaa-8360b04f-53d4-4cdb-93f5-34b6b43a2251-123.dat",
                  "prepre/aaa/2000/01/01/00/59/aaa-8360b04f-53d4-4cdb-93f5-34b6b43a2251-123.dat",
                  ],
                 ["prepre/aaa/2000/01/01/00/60/aaa-8360b04f-53d4-4cdb-93f5-34b6b43a2251-123.dat",
                  ]),
])
def test_filter(prefix, topics, name, suffix, oks, ngs):
    f = BaseS3Reader.make_filter(prefix, topics, name, suffix)
    for ok in oks:
        assert f(ok)
    for ng in ngs:
        assert not f(ng)


def test_pub(setup_config):
    with MessageWriter(service=SERVICE, topic=TOPIC) as w:
        for m in msgs:
            w.publish(m)


def test_sub(setup_config):
    with MessageReader(service=SERVICE, topics=TOPIC) as r:
        for m in r:
            # print(m)
            assert m is not None


@pytest.mark.parametrize("ctor", [MessageReader, MessageWriter])
def test_broker(setup_config, ctor):
    with pytest.raises(InvalidConfigError):
        with ctor(service=SERVICE, topic=TOPIC, brokers=["1.2.3.4"]) as _:
            pass


# @pytest.mark.skip
def test_pub_failure(setup_config):
    with MessageWriter(service=SERVICE,
                       topic=TOPIC,
                       s3={"endpoint_url": "http://127.0.0.99:999"}) as w:
        with pytest.raises(ConnectionError):
            for m in msgs:
                w.publish(m)


@pytest.mark.parametrize("utc_offset", ["+0900", "-0900", "+0000"])
def test_utc_offset(setup_config, utc_offset):
    with MessageWriter(service=SERVICE,
                       topic=TOPIC,
                       s3={"utc_offset": utc_offset}) as w:
        for m in msgs:
            w.publish(m)


@pytest.mark.parametrize("utc_offset", ["hoge", 1234])
def test_utc_offset_ng(setup_config, utc_offset):
    with pytest.raises(InvalidArgumentError):
        with MessageWriter(service=SERVICE,
                           topic=TOPIC,
                           s3={"utc_offset": utc_offset}) as w:
            for m in msgs:
                w.publish(m)


def test_uuid(setup_config):
    with MessageWriter(service=SERVICE, topic=TOPIC) as w:
        i = w.info("s3.writer.uuid")
        assert i is not None and type(i) == str
        j = w.info()
        assert j["s3"]["writer"]["uuid"] == i
        j = w.info("s3")
        assert j["writer"]["uuid"] == i
        j = w.info("s3.writer")
        assert j["uuid"] == i
    with MessageWriter(service=SERVICE, topic=TOPIC) as w2:
        i2 = w2.info("s3.writer.uuid")
        assert i2 is not None and type(i2) == str
        assert i2 != i
    with MessageReader(service=SERVICE, topics=TOPIC) as r:
        k = r.info("s3.writer.uuid")
        assert k is None
        k = r.info("s3.reader.uuid")
        assert k is None


def test_async(setup_config):
    topic = TOPIC + "-async"
    cv = Condition()
    count = len(msgs)
    check = 0

    def wait_on_message():
        nonlocal count
        with cv:
            cv.wait_for(lambda: count == 0, timeout=15)

    # write phase

    count = len(msgs)
    check = 0

    def on_success(r):
        nonlocal count, check
        with cv:
            count -= 1
            check += 1
            cv.notify_all()

    def on_failure(e):
        nonlocal count
        print(f"failure: e={e}")
        with cv:
            count -= 1
            cv.notify_all()

    with AsyncMessageWriter(service=SERVICE, topic=topic) as w:
        for m in msgs:
            w.publish(m).then(on_success).catch(on_failure)
        wait_on_message()
    assert count == 0
    assert check == len(msgs)

    # read phase

    count = len(msgs)
    check = 0

    def recved(msg):
        nonlocal count, check
        with cv:
            count -= 1
            check += 1
            cv.notify_all()

    with AsyncMessageReader(service=SERVICE, topic=topic) as r:
        r.on_message = recved
        wait_on_message()
    assert count == 0
    assert check == len(msgs)


# @mock_s3
# def test_moto():
#     s3 = boto3.client(service_name="s3", endpoint_url="http://a.b.c.d")
#     s3.create_bucket(Bucket="testbucket")
#     data = b'testtest'
#     s3.put_object(Bucket="testbucket", Key="testput.bin", Body=data)
#     r = s3.get_object(Bucket="testbucket", Key="testput.bin")
#     # print(r)
#     assert r.get("Body").read() == data
