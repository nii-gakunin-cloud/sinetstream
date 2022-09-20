# Copyright (C) 2022 National Institute of Informatics
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

import copy
import lz4.frame


class LZ4Compression(object):
    def __init__(self, level=None, params={}):
        self._level = level
        self._params = params

    def compress(self, data, params):
        return lz4.frame.compress(data, **params)

    @property
    def compressor(self):
        params = copy.deepcopy(self._params)
        if self._level is not None and "compression_level" not in params:
            params["compression_level"] = self._level
        return lambda data: self.compress(data, params)

    def decompress(self, data):
        return lz4.frame.decompress(data)

    @property
    def decompressor(self):
        return self.decompress
