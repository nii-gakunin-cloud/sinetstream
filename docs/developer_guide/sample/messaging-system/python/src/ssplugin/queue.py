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

from collections import defaultdict
from concurrent.futures.thread import ThreadPoolExecutor
from math import inf
from queue import Queue, Empty

from promise import Promise
from sinetstream import InvalidArgumentError
from sinetstream.spi import (
    PluginMessageReader, PluginMessageWriter, PluginAsyncMessageReader, PluginAsyncMessageWriter,
)

queues = defaultdict(Queue)


class QueueReader(PluginMessageReader):
    def __init__(self, params):
        self._queue = None
        self._topic = params.get('topic')
        if self._topic is None or not isinstance(self._topic, str):
            raise InvalidArgumentError()
        timeout_ms = params.get('receive_timeout_ms', inf)
        self._timeout = timeout_ms / 1000.0 if timeout_ms != inf else None

    def open(self):
        self._queue = queues[self._topic]

    def close(self):
        self._queue = None

    def __iter__(self):
        while True:
            try:
                value = self._queue.get(timeout=self._timeout)
                raw = value
                yield value, self._topic, raw
            except Empty:
                raise StopIteration()


class QueueWriter(PluginMessageWriter):
    def __init__(self, params):
        self._queue = None
        self._topic = params.get('topic')
        if self._topic is None or not isinstance(self._topic, str):
            raise InvalidArgumentError()

    def open(self):
        self._queue = queues[self._topic]

    def close(self):
        self._queue = None

    def publish(self, value):
        self._queue.put(value)


class QueueAsyncReader(PluginAsyncMessageReader):
    def __init__(self, params):
        self._queue = None
        self._topic = params.get('topic')
        if self._topic is None or not isinstance(self._topic, str):
            raise InvalidArgumentError()
        self._on_message = None
        self._on_failure = None
        self._reader_executor = None
        self._future = None
        self._closed = True

    def open(self):
        if self._closed:
            self._queue = queues[self._topic]
            self._reader_executor = ThreadPoolExecutor(max_workers=1)
            self._closed = False
            self._future = self._reader_executor.submit(self._reader_loop)

    def _reader_loop(self):
        while not self._closed:
            try:
                value = self._queue.get(timeout=0.1)
                raw = value
                if self._on_message is not None:
                    self._on_message(value, self._topic, raw),
            except Empty:
                pass

    def close(self):
        if not self._closed:
            self._queue = None
            self._future.cancel()
            self._reader_executor.shutdown()
            self._reader_executor = None
            self._future = None
            self._closed = True

    @property
    def on_message(self):
        return self._on_message

    @on_message.setter
    def on_message(self, on_message):
        self._on_message = on_message

    @property
    def on_failure(self):
        return self._on_failure

    @on_failure.setter
    def on_failure(self, on_failure):
        self._on_failure = on_failure


class QueueAsyncWriter(PluginAsyncMessageWriter):
    def __init__(self, params):
        self._queue = None
        self._topic = params.get('topic')
        if self._topic is None or not isinstance(self._topic, str):
            raise InvalidArgumentError()
        self._executor = None

    def open(self):
        self._executor = ThreadPoolExecutor()
        self._queue = queues[self._topic]

    def close(self):
        self._queue = None
        self._executor.shutdown()

    def publish(self, value):
        future = self._executor.submit(lambda: self._queue.put(value))
        return Promise.cast(future)
