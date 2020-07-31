#!usr/local/bin/python3.6
# vim: expandtab shiftwidth=4

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
from random import choices
from string import ascii_letters, digits
# from threading import Condition

import pytest
from conftest import SERVICE, TOPIC

from sinetstream import (
    MessageWriter, MessageReader,
    # AsyncMessageWriter, AsyncMessageReader,
    # AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE,
    AT_LEAST_ONCE,
    TEXT, Metrics
    )

logging.basicConfig(level=logging.CRITICAL)
logger = logging.getLogger(__name__)
pytestmark = pytest.mark.usefixtures('setup_config')

consistency = AT_LEAST_ONCE


def test_metrics0_reader():
    with MessageReader(SERVICE, TOPIC) as f:
        m = f.metrics()
        assert type(m) == Metrics
        assert type(m.raw) is dict
        assert m.start_time > 0
        assert m.end_time > 0
        assert m.end_time >= m.start_time
        assert m.msg_count_total == 0
        assert m.msg_bytes_total >= 0
        assert m.msg_size_min is None
        assert m.msg_size_max is None
        assert m.error_count_total == 0


def test_metrics0_writer():
    with MessageWriter(SERVICE, TOPIC) as f:
        m = f.metrics()
        assert type(m) == Metrics
        assert type(m.raw) is dict
        assert m.start_time > 0
        assert m.end_time > 0
        assert m.end_time >= m.start_time
        assert m.msg_count_total == 0
        assert m.msg_bytes_total >= 0
        assert m.msg_size_min is None
        assert m.msg_size_max is None
        assert m.error_count_total == 0


def test_metrics(setup_messages, config_topic):
    config_topic = TOPIC
    nmsg = len(setup_messages)
    with MessageReader(SERVICE, config_topic, value_type=TEXT) as reader:
        with MessageWriter(SERVICE, config_topic, value_type=TEXT) as writer:
            for msg in setup_messages:
                writer.publish(msg)
            writer_metrics = writer.metrics()
            logger.info(f"writer.metrics: {writer_metrics}")
        count = 0
        logger.info(f"OK")
        reader.seek_to_beginning()
        for msg in reader:
            count += 1
            if count >= nmsg:
                break
        reader_metrics = reader.metrics()
        logger.info(f"reader.metrics: {reader_metrics}")

    min_avro_overhead = 2 + 8 + 8 + 1  # marker + fingerprint + timestamp + body_len
    msg_sizes = [len(msg) + min_avro_overhead for msg in setup_messages]
    msg_bytes_total = sum(msg_sizes)
    msg_size_min = min(msg_sizes)
    msg_size_max = max(msg_sizes)

    assert type(writer_metrics) == Metrics
    assert type(writer_metrics.raw) is dict
    assert writer_metrics.start_time > 0
    assert writer_metrics.end_time > 0
    assert writer_metrics.end_time >= writer_metrics.start_time
    assert writer_metrics.msg_count_total == nmsg
    assert writer_metrics.msg_bytes_total >= msg_bytes_total
    assert writer_metrics.msg_size_min >= msg_size_min
    assert writer_metrics.msg_size_max >= msg_size_max
    assert writer_metrics.error_count_total == 0

    assert type(reader_metrics) == Metrics
    assert type(reader_metrics.raw) is dict
    assert reader_metrics.start_time > 0
    assert reader_metrics.end_time > 0
    assert reader_metrics.end_time >= reader_metrics.start_time
    assert reader_metrics.msg_count_total == nmsg
    assert reader_metrics.msg_bytes_total >= msg_bytes_total
    assert reader_metrics.msg_size_min >= msg_size_min
    assert reader_metrics.msg_size_max >= msg_size_max
    assert reader_metrics.error_count_total == 0


@pytest.fixture()
def setup_messages():
    return [''.join(choices(ascii_letters + digits, k=10)) for _ in range(10)]


@pytest.fixture()
def config_topic():
    return ''.join(choices(ascii_letters, k=10))
