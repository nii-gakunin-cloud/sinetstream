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
from ssl import SSLError
from time import sleep

from kafka import KafkaProducer, KafkaConsumer
from kafka.errors import (
    KafkaError, TopicAuthorizationFailedError, GroupAuthorizationFailedError,
)
from sinetstream import (
    AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE,
    ConnectionError, SinetError, AuthorizationError,
)

logger = logging.getLogger(__name__)


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
                "ca_certs": "ssl_cafile",
                "certfile": "ssl_certfile",
                "keyfile": "ssl_keyfile",
                "ciphers": "ssl_ciphers",
                "check_hostname": "ssl_check_hostname",
            }
            configs, rem = trans_dict1(tls_params, t)
            if len(rem) > 0:
                logger.warning(f"XXX INVALID PARAMTERS in tls: {rem}")
            configs["security_protocol"] = "SSL"
    return configs


def degrade_consistency(params):
    if params["consistency"] == EXACTLY_ONCE:
        logger.warning("Kafka doesn't support EXACTLY_ONCE")
        logger.warning("Fallbacked into AT_LEAST_ONCE")
        params["consistency"] = AT_LEAST_ONCE


SINETSTREAM_PARAM_LIST = [
    "service",
    "brokers",
    "topic",
    "topics",
    "type",
    "consistency",
    "value_type",
    "value_serializer",
    "value_deserializer",
    "receive_timeout_ms",
    "tls",
    "crypto",
    "data_encryption",
]


def del_sinetstream_param(params):
    return {
        param_name: v
        for param_name, v in params.items()
        if param_name not in SINETSTREAM_PARAM_LIST
    }


class KafkaClient(object):
    def __init__(self, params):
        self._params = params
        self._client = None
        self._brokers = params['brokers']
        degrade_consistency(self._params)
        self._kafka_params = {
            "bootstrap_servers": self._brokers,
            **self._conv_consistency(),
            **conv_tls(self._params.get("tls")),
        }
        self._kafka_params.update(del_sinetstream_param(self._params))

    def open(self):
        try:
            logger.debug(f"kafka_params={self._kafka_params}")
            self._client = self._create_client()
        except (KafkaError, SSLError):
            logger.error(f"cannot connect broker: {self._brokers}")
            self.close()
            raise ConnectionError()
        if not self._client.bootstrap_connected():
            self.close()
            raise ConnectionError()

        logger.info(f"Kafka: connected to {self._brokers}")
        return self

    def close(self):
        logger.debug("close")
        if self._client is None:
            return
        try:
            self._client.close()
            self._client = None
        except Exception:
            logger.error('kafka close() error')


class KafkaReader(KafkaClient):
    def __init__(self, params):
        logger.debug("KafkaReader:init")
        super().__init__(params)
        rtoms = self._params["receive_timeout_ms"]
        if rtoms != float("inf"):
            self._kafka_params["consumer_timeout_ms"] = int(rtoms)
        topics = self._params["topics"]
        if isinstance(topics, list):
            self._topics = tuple(topics)
        else:
            self._topics = (topics,)

    def _create_client(self):
        return KafkaConsumer(*self._topics, **self._kafka_params)

    def _conv_consistency(self):
        configs = {}
        consistency = self._params['consistency']
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
            assert False  # NOT IMPLEMENTED
        return configs

    def __iter__(self):
        assert self._client is not None
        return self

    def __next__(self):
        logger.debug("KafkaReader:next")
        try:
            rec = next(self._client)
            return rec.value, rec.topic, rec
        except (TopicAuthorizationFailedError,
                GroupAuthorizationFailedError) as ex:
            raise AuthorizationError(ex)

    def _wait_for_assignment(self, retry):
        count = 0
        while not self._client.assignment():
            self._client.poll(1)
            count += 1
            if count >= retry:
                raise SinetError(
                    'The maximum number of attempts before a consumer' +
                    ' can be assigned has been reached.')
            sleep(0.1)

    def seek_to_beginning(self, retry=200):
        self._wait_for_assignment(retry)
        self._client.seek_to_beginning()

    def seek_to_end(self, retry=200):
        self._wait_for_assignment(retry)
        self._client.seek_to_end()


class KafkaWriter(KafkaClient):
    def _create_client(self):
        return KafkaProducer(**self._kafka_params)

    def _conv_consistency(self):
        configs = {}
        consistency = self._params['consistency']
        if consistency == AT_MOST_ONCE:
            # Producer will not wait for any acknowledgment from the server.
            configs["acks"] = 0
        elif consistency == AT_LEAST_ONCE:
            # Wait for leader to write the record to its local log only.
            configs["acks"] = 1
        elif consistency == EXACTLY_ONCE:
            assert False  # NOT IMPLEMENTED
        return configs

    def publish(self, msg):
        logger.debug("KafkaWriter:publish")
        ret = self._client.send(self._params["topic"], msg)
        try:
            return ret.get()
        except TopicAuthorizationFailedError as ex:
            raise AuthorizationError(ex)
