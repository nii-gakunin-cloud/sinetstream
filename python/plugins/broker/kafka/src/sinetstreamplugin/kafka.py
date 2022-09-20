#!/usr/bin/env python3

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
from concurrent.futures.thread import ThreadPoolExecutor
from ssl import SSLError
from sys import exc_info
from threading import RLock
from time import sleep

from kafka import KafkaProducer, KafkaConsumer
from kafka.errors import (
    KafkaError, TopicAuthorizationFailedError, GroupAuthorizationFailedError,
)
from promise import Promise

from sinetstream import (
    AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE,
    ConnectionError, SinetError, AuthorizationError,
)

from sinetstream.error import InvalidArgumentError

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
                logger.warning(f"unknown parameters in tls are ignored: {rem}")
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
    "compression",
    "data_compression",
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
        if 'brokers' not in params:
            raise InvalidArgumentError("You must specify several brokers.")
        self._brokers = params['brokers']
        if (type(self._brokers) != str and
            (type(self._brokers) != list or
             len(self._brokers) == 0
             )):
            raise InvalidArgumentError("You must specify several brokers.")
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

    def metrics(self):
        if self._client is None:
            return None
        return self._client.metrics(raw=True)

    def reset_metrics(self):
        pass


class BaseKafkaReader(KafkaClient):
    def __init__(self, params):
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


class KafkaReader(BaseKafkaReader):
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


class KafkaAsyncReader(BaseKafkaReader):
    def __init__(self, params):
        logger.debug("KafkaAsyncReader:init")
        super().__init__(params)
        self._executor = None
        self._reader_executor = None
        self._on_message = None
        self._on_failure = None
        self._closed = True
        self._future = None

    def open(self):
        super().open()
        self._closed = False
        self._executor = ThreadPoolExecutor()
        self._reader_executor = ThreadPoolExecutor(max_workers=1)
        if self._is_set_callback():
            self._future = self._reader_executor.submit(self._reader_loop)

    def _poll(self):
        try:
            return self._client.poll(timeout_ms=100)
        except (TopicAuthorizationFailedError, GroupAuthorizationFailedError) as ex:
            tb = exc_info()[2]
            e = AuthorizationError(ex)
            if self._on_failure is not None:
                self._executor.submit(lambda x, t: self._on_failure(x, traceback=tb), e, tb)
            raise e

    def _reader_loop(self):
        assert self._client is not None
        assert self._is_set_callback()
        while not self._closed:
            record_map = self._poll()
            for tp, records in record_map.items():
                for record in records:
                    self._executor.submit(
                        lambda r: self._on_message(r.value, r.topic, r),
                        record)

    def _is_set_callback(self):
        return not (self._on_message is None and self._on_failure is None)

    def close(self):
        super().close()
        self._closed = True
        if self._reader_executor is not None:
            self._reader_executor.shutdown()
            self._reader_executor = None
        if self._executor is not None:
            self._executor.shutdown()
            self._executor = None

    @property
    def on_message(self):
        return self._on_message

    @on_message.setter
    def on_message(self, on_message):
        self._on_message = on_message
        if self._future is None and self._reader_executor is not None:
            self._future = self._reader_executor.submit(self._reader_loop)

    @property
    def on_failure(self):
        return self._on_failure

    @on_failure.setter
    def on_failure(self, on_failure):
        self._on_failure = on_failure
        if self._future is None and self._reader_executor is not None:
            self._future = self._reader_executor.submit(self._reader_loop)


class BaseKafkaWriter(KafkaClient):
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
        return self._client.send(self._params["topic"], memoryview(msg))


class KafkaWriter(BaseKafkaWriter):
    def publish(self, msg):
        try:
            return super().publish(msg).get()
        except TopicAuthorizationFailedError as ex:
            raise AuthorizationError(ex)


class KafkaAsyncWriter(BaseKafkaWriter):
    def __init__(self, params):
        super().__init__(params)
        self._lock = RLock()

    def publish(self, msg):
        future = super().publish(msg)

        def executor(resolve, reject):
            def on_success(r):
                with self._lock:
                    resolve(r)

            def on_failure(e, traceback=None):
                with self._lock:
                    if isinstance(e, TopicAuthorizationFailedError):
                        e = AuthorizationError(e)
                    reject(e, traceback)

            future.add_callback(on_success).add_errback(on_failure)

        return KafkaAsyncWriter.AsyncWriterPromise(executor, lock=self._lock)

    class AsyncWriterPromise(Promise):

        def __init__(self, executor=None, scheduler=None, lock=None):
            super().__init__(executor=executor, scheduler=scheduler)
            self._lock = lock

        def _then(
                self,
                did_fulfill=None,
                did_reject=None,
        ):
            if self._lock is not None:
                with self._lock:
                    return super()._then(did_fulfill, did_reject)
            else:
                return super()._then(did_fulfill, did_reject)
