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

from io import BytesIO, SEEK_SET
from .api import (MessageWriter, MessageReader)

logger = logging.getLogger(__name__)
# logger.setLevel(logging.DEBUG)


class SINETStreamMessageEncoder:
    def __init__(self, **writer_params):
        self._bio = BytesIO()
        self._wr = MessageWriter(**dict(writer_params,
                                        no_config=True,
                                        type="iostream",
                                        iostream={"iobase": self._bio}))
        self._opened = False

    def open(self):
        if not self._opened:
            self._bio.__enter__()
            self._wr.__enter__()
            self._opened = True

    def close(self, ex_type=None, ex_value=None, trace=None):
        if self._opened:
            self._wr.__exit__(ex_type, ex_value, trace)
            self._bio.__exit__(ex_type, ex_value, trace)
            self._opened = False

    def __enter__(self):
        self.open()
        return self

    def __exit__(self, ex_type, ex_value, trace):
        self.close(ex_type, ex_value, trace)

    def encode(self, msg, timestamp=None):
        self.open()
        self._wr.publish(msg, timestamp=timestamp)
        bs = self._bio.getvalue()
        self._bio.seek(0, SEEK_SET)
        self._bio.truncate(0)
        return bs


class SINETStreamMessageDecoder:
    def __init__(self, **reader_params):
        self._bio = BytesIO()
        self._rd = MessageReader(**dict(reader_params,
                                        no_config=True,
                                        type="iostream",
                                        iostream={"iobase": self._bio}))
        self._opened = False

    def open(self):
        if not self._opened:
            self._bio.__enter__()
            self._rd.__enter__()
            self._opened = True

    def close(self, ex_type=None, ex_value=None, trace=None):
        if self._opened:
            self._rd.__exit__(ex_type, ex_value, trace)
            self._bio.__exit__(ex_type, ex_value, trace)
            self._opened = False

    def __enter__(self):
        self.open()
        return self

    def __exit__(self, ex_type, ex_value, trace):
        self.close(ex_type, ex_value, trace)

    def decode(self, bs):
        self.open()
        self._bio.write(bs)
        self._bio.seek(0, SEEK_SET)
        msg = next(iter(self._rd))
        self._bio.seek(0, SEEK_SET)
        self._bio.truncate(0)
        return msg
