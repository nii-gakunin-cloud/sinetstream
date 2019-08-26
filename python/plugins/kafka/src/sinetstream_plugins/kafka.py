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
    Message, AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE,
    ConnectionError, AlreadyConnectedError,
)
from sinetstream.api import setdict

logger = logging.getLogger(__name__)


class KafkaMessage(Message):
    def __init__(self, rec):
        self.raw = rec

    @property
    def value(self):
        return self.raw.value

    @property
    def topic(self):
        return self.raw.topic


class KafkaReaderHandleIter(object):
    def __init__(self, reader):
        logger.debug("KafkaReaderHandleIter:init")
        self.reader = reader

    def __next__(self):
        logger.debug("KafkaReaderHandleIter:next")
        rec = next(self.reader.kafka_consumer)
        return KafkaMessage(rec)


def get_tls_configs(svc):
    configs = {}
    tls = svc.get("tls")
    if tls:
        if type(tls) is bool:
            configs["security_protocol"] = "SSL"
        else:
            setdict(configs, "security_protocol", "SSL")
            setdict(configs, "ssl_cafile", tls.get("ca_certs"))
            setdict(configs, "ssl_certfile", tls.get("certfile"))
            setdict(configs, "ssl_keyfile", tls.get("keyfile"))
            setdict(configs, "ssl_ciphers", tls.get("ciphers"))
            setdict(configs, "ssl_check_hostname", tls.get("check_hostname"))
    return configs


class KafkaReader(object):
    def __init__(self, message_reader):
        logger.debug("KafkaReader:init")
        self._reader = message_reader
        self.kafka_consumer = None
        if self._reader.consistency == EXACTLY_ONCE:
            logger.warning("KafkaReader doesn't support EXACTLY_ONCE")
            logger.warning("Fallbacked into AT_LEAST_ONCE")
            self._reader.consistency = AT_LEAST_ONCE

    def open(self):
        logger.debug("KafkaReader:open")
        if self.kafka_consumer is not None:
            logger.error(f"already connected")
            raise AlreadyConnectedError()
        brokers = self._reader.svc["brokers"]
        configs = {}
        setdict(configs, "bootstrap_servers", brokers)
        setdict(configs, "client_id", self._reader._client_id)
        setdict(configs, "value_deserializer", self._reader.value_deserializer)
        consistency = self._reader.consistency
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
        if self._reader.receive_timeout_ms != float("inf"):
            configs["consumer_timeout_ms"] = int(
                self._reader.receive_timeout_ms)
        configs.update(get_tls_configs(self._reader.svc))
        tls = self._reader.kwargs.get("tls")
        if tls:
            if type(tls) is bool:
                del self._reader.kwargs["tls"]
                configs["security_protocol"] = "SSL"
            else:
                logger.error("tls must be bool")
                raise InvalidArgumentError()
        configs.update(self._reader.kwargs)
        try:
            if isinstance(self._reader.topics, list):
                topics = tuple(self._reader.topics)
            else:
                topics = (self._reader.topics,)
            self.kafka_consumer = kafka.KafkaConsumer(*topics, **configs)
        except kafka.errors.NoBrokersAvailable:
            logger.error(f"cannot connect broker: {brokers}")
            raise ConnectionError()
        self._reader._client_id = self.kafka_consumer.config["client_id"]
        logger.info(f"KafkaReader: connected to {brokers}")
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


class KafkaWriter(object):
    def __init__(self, message_writer):
        logger.debug("KafkaWriter:init")
        self._writer = message_writer
        self.kafka_producer = None
        if self._writer.consistency == EXACTLY_ONCE:
            logger.warning("KafkaWriter doesn't support EXACTLY_ONCE")
            logger.warning("Fallbacked into AT_LEAST_ONCE")
            self._writer.consistency = AT_LEAST_ONCE

    def open(self):
        logger.debug("KafkaWriter:open")
        if self.kafka_producer is not None:
            raise AlreadyConnectedError()
        brokers = self._writer.svc["brokers"]
        configs = {}
        setdict(configs, "bootstrap_servers", brokers)
        setdict(configs, "client_id", self._writer._client_id)
        setdict(configs, "value_serializer", self._writer.value_serializer)
        consistency = self._writer.consistency
        if consistency == AT_MOST_ONCE:
            # Producer will not wait for any acknowledgment from the server.
            configs["acks"] = 0
        elif consistency == AT_LEAST_ONCE:
            # Wait for leader to write the record to its local log only.
            configs["acks"] = 1
        elif consistency == EXACTLY_ONCE:
            assert False        # NOT IMPLEMENTED
        configs.update(get_tls_configs(self._writer.svc))
        configs.update(self._writer.kwargs)
        try:
            self.kafka_producer = kafka.KafkaProducer(**configs)
        except kafka.errors.NoBrokersAvailable:
            logger.error(f"cannot connect broker: {brokers}")
            raise ConnectionError()
        self._writer._client_id = self.kafka_producer.config["client_id"]
        logger.info(f"KafkaWriter: connected to {brokers}")

    def close(self):
        logger.debug("KafkaWriter:close")
        self.kafka_producer.close()
        self.kafka_producer = None

    def publish(self, msg):
        logger.debug("KafkaWriter:publish")
        self.kafka_producer.send(self._writer.topic, msg)
