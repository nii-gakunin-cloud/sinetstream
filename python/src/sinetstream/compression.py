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

from sinetstream.spi import PluginCompression
from sinetstream.utils import Registry

import copy
import zlib
import zstandard as zstd

GZIP = "gzip"
ZSTD = "zstd"
compression_registry = Registry('sinetstream.compression', PluginCompression)


class GzipCompression(object):
    def __init__(self, level=None, params={}):
        self._level = level
        self._params = params

    def compress(self, data, params):
        cctx = zlib.compressobj(**params)
        return cctx.compress(data) + cctx.flush()

    @property
    def compressor(self):
        params = copy.deepcopy(self._params)
        if self._level is not None and "level" not in params:
            params["level"] = self._level
        return lambda data: self.compress(data, params)

    def decompress(self, data, params):
        dctx = zlib.decompressobj(**params)
        return dctx.decompress(data) + dctx.flush()

    @property
    def decompressor(self):
        return lambda data: self.decompress(data, self._params)


class ZstdCompression(object):
    def __init__(self, level=None, params={}):
        self._level = level
        self._params = params

    @property
    def compressor(self):
        params = copy.deepcopy(self._params)
        if self._level is not None and "level" not in params:
            params["level"] = self._level
        cctx = zstd.ZstdCompressor(**params)
        return lambda data: cctx.compress(data)

    @property
    def decompressor(self):
        dctx = zstd.ZstdDecompressor(**self._params)
        return lambda data: dctx.decompress(data)


class GzipCompressionEntryPoint(object):
    @classmethod
    def load(cls):
        return GzipCompression


class ZstdCompressionEntryPoint(object):
    @classmethod
    def load(cls):
        return ZstdCompression


compression_registry.register(GZIP, GzipCompressionEntryPoint)
compression_registry.register(ZSTD, ZstdCompressionEntryPoint)
