#!/usr/bin/env python3

# Copyright (C) 2023 National Institute of Informatics
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
# from concurrent.futures.thread import ThreadPoolExecutor
# from promise import Promise

from sinetstream import (
   # InvalidConfigError,
   InvalidArgumentError)
# from sinetstream.spi import PluginMessage
# from sinetstream.api import MessageIO

logger = logging.getLogger(__name__)
# logger.setLevel(logging.DEBUG)


class IOStreamMetrics(object):
    def __int__(self):
        pass

    def __str__(self):
        return ""

    def reset(self):
        pass


class IOStreamWriterMetrics(IOStreamMetrics):
    def __init__(self):
        super().__init__()

    def __str__(self):
        return super().__str__()

    def reset(self):
        super().reset()


class BaseIOStreamWriter:
    def __init__(self, params):
        logger.debug("BaseIOStreamWriter:init")
        if "iostream" not in params:
            raise InvalidArgumentError("the parameter iostream must be specified")
        ioparams = params["iostream"]
        if "iobase" not in ioparams:
            raise InvalidArgumentError("the parameter iostream.iobase must be specified")
        self._iobase = ioparams["iobase"]
        self._metrics = IOStreamWriterMetrics()

    def open(self):
        pass

    def close(self):
        pass

    def _publish(self, msg):
        assert isinstance(msg, bytes)
        self._iobase.write(msg)

    def metrics(self):
        return self._metrics

    def reset_metrics(self):
        return self._metrics.reset()

    def info(self, ipath, kwargs):
        return None


class IOStreamWriter(BaseIOStreamWriter):
    def __init__(self, params):
        logger.debug("IOStreamWriter:init")
        super().__init__(params)

    def publish(self, msg):
        return self._publish(msg)


# class IOStreamAsyncWriter(BaseIOStreamWriter):
#     def __init__(self, params):
#         logger.debug("IOStreamAsyncWriter:init")
#         super().__init__(params)
#
#     def publish(self, msg):
#         return Promise(lambda resolve, reject: [self._publish(msg), resolve(None)])


class IOStreamReaderMetrics(IOStreamMetrics):
    pass


class BaseIOStreamReader:
    def __init__(self, params):
        if "iostream" not in params:
            raise InvalidArgumentError("the parameter iostream must be specified")
        ioparams = params["iostream"]
        if "iobase" not in ioparams:
            raise InvalidArgumentError("the parameter iostream.iobase must be specified")
        self._iobase = ioparams["iobase"]
        self._metrics = IOStreamReaderMetrics()

    def open(self):
        pass

    def close(self):
        pass

    def _read(self):
        return self._iobase.read()

    def metrics(self):
        return self._metrics

    def reset_metrics(self):
        return self._metrics.reset()

    def info(self, name, kwargs):
        return None


class IOStreamReader(BaseIOStreamReader):
    def __init__(self, params):
        logger.debug("IOStreamReader:init")
        super().__init__(params)

    class Iter(object):
        def __init__(self, ioreader):
            logger.debug("IOStreamReader.Iter:__init__")
            self._ioreader = ioreader

        def __next__(self):
            logger.debug("IOStreamReader.Iter:__next__")
            ioreader = self._ioreader
            payload = ioreader._read()
            if len(payload) == 0:
                raise StopIteration()
            # topic = ioreader._topic
            topic = None
            return payload, topic, payload

    def __iter__(self):
        logger.debug("IOStreamReader:__iter__")
        return IOStreamReader.Iter(self)


# class IOStreamAsyncReader(BaseIOStreamReader):
#     def __init__(self, params):
#         logger.debug("IOStreamAsyncReader:init")
#         super().__init__(params)
#         self._reader_executor = None
#         self._on_message = None
#         self._on_failure = None
#         self._closed = True
#         self._future = None
#
#     def open(self):
#         logger.debug("IOStreamAsyncReader:open")
#         super().open()
#         self._closed = False
#         self._reader_executor = ThreadPoolExecutor(max_workers=1)
#         # becasue boto3 is sequenctial, one worker is sufficient.
#         self._start_reader()
#
#     def _start_reader(self):
#         logger.debug("IOStreamAsyncReader:_start_reader")
#         if ((self._future is None and
#              self._is_set_callback() and
#              self._reader_executor is not None
#              )):
#             logger.debug("IOStreamAsyncReader:_start_reader:submit")
#             self._future = self._reader_executor.submit(self._reader_loop)
#
#     def _reader_loop(self):
#         logger.debug("IOStreamAsyncReader:_reader_loop:start")
#         while True:
#             logger.debug("_reader_loop:loop")
#             if self._closed:
#                 logger.debug("IOStreamAsyncReader:_reader_loop:closed")
#                 break
#             payload = self._read()
#             if len(payload) == 0:
#                 logger.debug("IOStreamAsyncReader:reader_loop:eof")
#                 break
#             self._on_message(payload, self._topic, payload)
#         logger.debug("IOStreamAsyncReader:_reader_loop:end")
#
#     def close(self):
#         super().close()
#         self._closed = True
#         if self._reader_executor is not None:
#             self._reader_executor.shutdown()
#             self._reader_executor = None
#
#     def _is_set_callback(self):
#         return not (self._on_message is None and self._on_failure is None)
#
#     @property
#     def on_message(self):
#         return self._on_message
#
#     @on_message.setter
#     def on_message(self, on_message):
#         self._on_message = on_message
#         self._start_reader()
#
#     @property
#     def on_failure(self):
#         return self._on_failure
#
#     @on_failure.setter
#     def on_failure(self, on_failure):
#         self._on_failure = on_failure
#         self._start_reader()
