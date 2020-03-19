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
from math import inf
from queue import Queue, Empty

from sinetstream import InvalidArgumentError
from sinetstream.spi import PluginMessageReader, PluginMessageWriter

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
