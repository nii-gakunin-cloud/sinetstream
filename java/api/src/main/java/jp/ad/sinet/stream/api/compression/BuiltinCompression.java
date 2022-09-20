/*
 * Copyright (C) 2022 National Institute of Informatics
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package jp.ad.sinet.stream.api.compression;

import jp.ad.sinet.stream.api.Compression;
import jp.ad.sinet.stream.api.Compressor;
import jp.ad.sinet.stream.api.Decompressor;

import java.util.Map;

public enum BuiltinCompression implements Compression {
    GZIP {
        public Compressor getCompressor(Integer level, Map<String, Object> parameters) {
            return new GzipCompressor(level, parameters);
        }
        public Decompressor getDecompressor(Map<String, Object> parameters) {
            return new GzipDecompressor(parameters);
        }
    },
    ZSTD {
        public Compressor getCompressor(Integer level, Map<String, Object> parameters) {
            return new ZstdCompressor(level, parameters);
        }
        public Decompressor getDecompressor(Map<String, Object> parameters) {
            return new ZstdDecompressor(parameters);
        }
    };

    @Override
    public String getName() {
        return this.name().toLowerCase();
    }
}
