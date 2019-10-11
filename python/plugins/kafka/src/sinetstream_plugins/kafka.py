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

import kafka
from sinetstream import (
    make_message, AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE,
    InvalidArgumentError, ConnectionError, AlreadyConnectedError,
)
from sinetstream.api import setdict
from sinetstream.api import del_sinetstream_param

logger = logging.getLogger(__name__)


class KafkaReaderHandleIter(object):
    def __init__(self, reader):
        logger.debug("KafkaReaderHandleIter:init")
        self.reader = reader

    def __next__(self):
        logger.debug("KafkaReaderHandleIter:next")
        rec = next(self.reader.kafka_consumer)
        return make_message(self.reader._reader, rec.value, rec.topic, rec)


"""
GOMI
# remap dict
# d = { k1: v1 }
# t = { k1: [k2, (k3, f), (k4, v4)] }
# trans_dict(d, t) => { k2: v1, k3: f(v1), k4: v4 }, {}
def trans_dict(d, t):
    d2 = {}
    d3 = {}
    for k, v in d.items():
        lst = t.get(k)
        if lst is not None:
            for x in lst:
                if type(x) is tuple:
                    k2, f = x
                    if callable(f):
                        d2[k2] = f(v)
                    else:
                        d2[k2] = f
                else:
                    d2[x] = v
        else:
            d3[k] = v
    return d2, d3
"""


def trans_dict1(d, t):
    d2 = {}
    d3 = {}
    for k, v in d.items():
        if k in t:
            d2[t[k]] = v
        else:
            d3[k] = v
    return d2, d3


def conv_tls(tls_params):
    configs = {}
    if tls_params:
        if type(tls_params) is bool:
            configs["security_protocol"] = "SSL"
        else:
            t = {
                "ca_certs":       "ssl_cafile",
                "certfile":       "ssl_certfile",
                "keyfile":        "ssl_keyfile",
                "ciphers":        "ssl_ciphers",
                "check_hostname": "ssl_check_hostname",
            }
            configs, rem = trans_dict1(tls_params, t)
            if len(rem) > 0:
                logger.warning(f"XXX INVALID PARAMTERS in tls: {rem}")
            configs["security_protocol"] = "SSL"
    return configs


def conv_consistency_for_reader(consistency):
    configs = {}
    if consistency == AT_MOST_ONCE:
        # commit asap to prevent kafka-broker resending.
        configs["enable_auto_commit"] = True
        configs["auto_commit_interval_ms"] = 101
    elif consistency == AT_LEAST_ONCE:
        # kafka-broker resends records until consumer commits.
        configs["enable_auto_commit"] = True
        # note: consumer app will finish processing within 5 secs.
        configs["auto_commit_interval_ms"] = 5000

        # another solution:
        # configs["enable_auto_commit"] = False
        # and consumer app invokes: reader.commit() explicitly
    elif consistency == EXACTLY_ONCE:
        assert False        # NOT IMPLEMENTED
    return configs


def degrade_consistency(params):
    if params["consistency"] == EXACTLY_ONCE:
        logger.warning("Kafka doesn't support EXACTLY_ONCE")
        logger.warning("Fallbacked into AT_LEAST_ONCE")
        params["consistency"] = AT_LEAST_ONCE


class KafkaReader(object):
    def __init__(self, message_reader):
        logger.debug("KafkaReader:init")
        self._reader = message_reader
        self.kafka_consumer = None
        degrade_consistency(self._reader.params)

    def open(self):
        logger.debug("KafkaReader:open")
        if self.kafka_consumer is not None:
            logger.error(f"already connected")
            raise AlreadyConnectedError()

        params = self._reader.params

        kafka_params = {
            "bootstrap_servers": params["brokers"],
            **conv_consistency_for_reader(params["consistency"]),
            **conv_tls(params.get("tls")),
        }

        rtoms = params["receive_timeout_ms"]
        if rtoms != float("inf"):
            kafka_params["consumer_timeout_ms"] = int(rtoms)

        kafka_params.update(del_sinetstream_param(params, "kafka"))

        topics = params["topics"]
        if isinstance(topics, list):
            topics = tuple(topics)
        else:
            topics = (topics,)

        try:
            logger.debug(f"kafka_params={kafka_params}")
            self.kafka_consumer = kafka.KafkaConsumer(*topics, **kafka_params)
        except kafka.errors.NoBrokersAvailable:
            logger.error(f"cannot connect broker: {params['brokers']}")
            raise ConnectionError()
        logger.info(f"KafkaReader: connected to {params['brokers']}")
        return self

    def close(self):
        logger.debug("KafkaReader:close")
        self.kafka_consumer.close()
        self.kafka_consumer = None

    def __iter__(self):
        assert self.kafka_consumer is not None
        return KafkaReaderHandleIter(self)

    def seek_to_beginning(self):
        self.kafka_consumer.seek_to_beginning()

    def seek_to_end(self):
        self.kafka_consumer.seek_to_end()


def conv_consistency_for_writer(consistency):
    configs = {}
    if consistency == AT_MOST_ONCE:
        # Producer will not wait for any acknowledgment from the server.
        configs["acks"] = 0
    elif consistency == AT_LEAST_ONCE:
        # Wait for leader to write the record to its local log only.
        configs["acks"] = 1
    elif consistency == EXACTLY_ONCE:
        assert False        # NOT IMPLEMENTED
    return configs


class KafkaWriter(object):
    def __init__(self, message_writer):
        logger.debug("KafkaWriter:init")
        self._writer = message_writer
        self.kafka_producer = None
        degrade_consistency(self._writer.params)

    def open(self):
        logger.debug("KafkaWriter:open")
        if self.kafka_producer is not None:
            raise AlreadyConnectedError()

        params = self._writer.params

        kafka_params = {
            "bootstrap_servers": params["brokers"],
            **conv_consistency_for_writer(params["consistency"]),
            **conv_tls(params.get("tls")),
        }

        kafka_params.update(del_sinetstream_param(params, "kafka"))

        try:
            logger.debug(f"kafka_params={kafka_params}")
            self.kafka_producer = kafka.KafkaProducer(**kafka_params)
        except kafka.errors.NoBrokersAvailable:
            logger.error(f"cannot connect broker: {params['brokers']}")
            raise ConnectionError()
        logger.info(f"KafkaWriter: connected to {params['brokers']}")

    def close(self):
        logger.debug("KafkaWriter:close")
        self.kafka_producer.close()
        self.kafka_producer = None

    def publish(self, msg):
        logger.debug("KafkaWriter:publish")
        self.kafka_producer.send(self._writer.params["topic"], msg)
