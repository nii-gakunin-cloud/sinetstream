#!/usr/bin/env python3

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

import pytest
from conftest import SERVICE
from conftest import test_qwrite_failure, test_qread_failure  # noqa
from threading import Condition

from sinetstream import (
    MessageReader, MessageWriter,
    AsyncMessageReader, AsyncMessageWriter,
    TEXT)

logging.basicConfig(level=logging.ERROR)
pytestmark = pytest.mark.usefixtures('setup_config', 'dummy_reader_plugin', 'dummy_writer_plugin')

logger = logging.getLogger(__name__)

msgs = ['test message 1',
        'test message 02',
        'test message 003']

nmsg = len(msgs)
avro_overhead = 2 + 8 + 8 + 1  # marker + fingerprint + timestamp + body_len
pack_overhead = 4 + 2  # marker + key_version
msglens = [len(m) + avro_overhead + pack_overhead for m in msgs]
sum_len = sum(msglens)
min_len = min(msglens)
max_len = max(msglens)


def equal_metrics(a, b):
    return (a.start_time == b.start_time and
            a.end_time == b.end_time and
            a.msg_count_total == b.msg_count_total and
            a.msg_count_rate == b.msg_count_rate and
            a.msg_bytes_total == b.msg_bytes_total and
            a.msg_bytes_rate == b.msg_bytes_rate and
            a.msg_size_min == b.msg_size_min and
            a.msg_size_max == b.msg_size_max and
            a.msg_size_avg == b.msg_size_avg and
            a.msg_size_avg == b.msg_size_avg and
            a.error_count_total == b.error_count_total and
            a.error_count_rate == b.error_count_rate)


def assert_metrics(m):
    assert m.raw == "this is a dummy metrics"
    assert m.start_time > 0
    assert m.start_time_ms > 0
    assert int(m.start_time * 1000) == int(m.start_time_ms)
    assert m.end_time > 0
    assert m.end_time_ms > 0
    assert int(m.end_time * 1000) == int(m.end_time_ms)
    assert m.end_time >= m.start_time
    assert m.end_time_ms >= m.start_time_ms
    assert m.msg_count_total == nmsg
    assert m.msg_count_rate > 0
    assert m.msg_bytes_total >= sum_len
    assert m.msg_bytes_rate > 0
    assert m.msg_size_min == min_len
    assert m.msg_size_max == max_len
    assert m.msg_size_avg >= min_len
    assert m.msg_size_avg <= max_len
    assert m.error_count_total == 0
    assert m.error_count_rate == 0


def test_sync_thru(config_topic):
    with MessageWriter(SERVICE, value_type=TEXT) as fw:
        m0 = fw.metrics
        logger.info(f"writer.metrics: {m0}")
        for msg in msgs:
            fw.publish(msg)
        m = fw.metrics
        m2 = fw.metrics
        fw.reset_metrics()
        m3 = fw.metrics
        logger.info(f"writer.metrics: {m}")

        assert_metrics(m)
        assert equal_metrics(m, m2)
        assert m3.msg_count_total == 0
    with MessageReader(SERVICE, value_type=TEXT) as fr:
        for expected, msg in zip(msgs, fr):
            assert msg.topic == config_topic
            assert msg.value == expected
        m = fr.metrics
        m2 = fr.metrics
        fr.reset_metrics()
        m3 = fr.metrics
        logger.info(f"reader.metrics: {m}")

        assert_metrics(m)
        assert equal_metrics(m, m2)
        assert m3.msg_count_total == 0


def test_sync_write_err(config_topic):
    global test_qwrite_failure
    test_qwrite_failure += [1, 2]
    with MessageWriter(SERVICE, value_type=TEXT) as fw:
        for msg in msgs:
            try:
                fw.publish(msg)
            except Exception:
                logger.info("caught")
        m = fw.metrics
        logger.info(f"writer.metrics: {m}")
        assert m.error_count_total == 1


def test_sync_read_err(config_topic):
    with MessageWriter(SERVICE, value_type=TEXT) as fw:
        for msg in msgs:
            fw.publish(msg)

    global test_qread_failure
    test_qread_failure += [1, 2, 3]
    with MessageReader(SERVICE, value_type=TEXT) as fr:
        try:
            for expected, msg in zip(msgs, fr):
                assert msg.topic == config_topic
                assert msg.value == expected
        except Exception:
            logger.info("caught")
        m = fr.metrics
        logger.info(f"reader.metrics: {m}")
        assert m.msg_count_total == 2
        assert m.error_count_total == 1


def test_async_thru(config_topic):
    cv = Condition()
    expected = set(msgs)
    called = 0

    def assert_messages(message):
        nonlocal expected
        nonlocal called
        with cv:
            assert message.topic == config_topic
            assert message.value in expected
            expected.remove(message.value)
            called += 1
            cv.notify_all()
            assert False  # XXX assertion failure in callback is ignored...

    with AsyncMessageReader(SERVICE, value_type=TEXT) as reader:
        reader.on_message = assert_messages

        with MessageWriter(SERVICE, value_type=TEXT) as writer:
            for msg in msgs:
                writer.publish(msg)
            m = writer.metrics
            m2 = writer.metrics
            writer.reset_metrics()
            m3 = writer.metrics
            logger.info(f"writer.metrics: {m}")

            assert_metrics(m)
            assert equal_metrics(m, m2)
            assert m3.msg_count_total == 0

        with cv:
            while called != nmsg:
                cv.wait(1)
            assert called == nmsg

        m = reader.metrics
        m2 = reader.metrics
        reader.reset_metrics()
        m3 = reader.metrics
        logger.info(f"reader.metrics: {m}")

        assert_metrics(m)
        assert equal_metrics(m, m2)
        assert m3.msg_count_total == 0


def test_async_write_err(config_topic):
    test_err = [1, 2]
    global test_qwrite_failure
    test_qwrite_failure += test_err
    count = []
    err = []
    with AsyncMessageWriter(SERVICE, value_type=TEXT) as fw:
        for msg in msgs:
            ret = fw.publish(msg).then(lambda _: count.append(1), lambda _: err.append(1))
            from promise import Promise
            assert isinstance(ret, Promise)
        m = fw.metrics
        logger.info(f"writer.metrics: {m}")
        assert len(count) == len(test_err)
        assert len(err) == 1
        assert m.error_count_total == 1


def test_async_read_err(config_topic):
    cv = Condition()
    expected = set(msgs)
    called = 0
    err = 0

    def assert_messages(message):
        nonlocal expected
        nonlocal called
        with cv:
            assert message.topic == config_topic
            assert message.value in expected
            expected.remove(message.value)
            called += 1
            # print(f"XXX assert_messages: called={called} ++, message={message}")
            cv.notify_all()

    def failed(e, traceback=None):
        nonlocal called
        nonlocal err
        with cv:
            called += 1
            err += 1
            # print(f"XXX failed: called={called} ++, e={e}, traceback={traceback}")
            cv.notify_all()

    global test_qread_failure
    test_qread_failure += [1, 2, 3]

    with AsyncMessageReader(SERVICE, value_type=TEXT) as reader:
        reader.on_message = assert_messages
        reader.on_failure = failed

        with MessageWriter(SERVICE, value_type=TEXT) as writer:
            for msg in msgs:
                writer.publish(msg)

        import time
        deadline = time.time() + 5
        with cv:
            while called != nmsg:
                # print(f"XXX AsyncMessageReader: called={called} nmsg={nmsg}")
                if time.time() >= deadline:
                    raise Exception("TIMEOUT")
                cv.wait(1)
            # print(f"XXX AsyncMessageReader: FIN called={called} nmsg={nmsg}")
            assert called == nmsg

        m = reader.metrics
        logger.info(f"reader.metrics: {m}")
        assert err == 1
        assert m.msg_count_total == 2
        assert m.error_count_total == 1


@pytest.fixture()
def config_params():
    return {'data_encryption': False}
    return {
        'crypto': {
            'algorithm': 'AES',
            'key_length': 128,
            'mode': 'GCM',
            'padding': 'none',
            'key_derivation': {
                'algorithm': 'pbkdf2',
            },
            'salt_bytes': 16,
            'iteration': 10000,
            'password': {
                'value': 'secret-000',
            },
        },
        'data_encryption': True,
    }
