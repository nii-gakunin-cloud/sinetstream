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
import jp.ad.sinet.stream.spi.CompressionProvider;

import java.util.Map;

public class Lz4CompressionProvider implements CompressionProvider {

    private static final Compression compression = new Lz4Compression();

    @Override
    public Compression getCompression() {
        return compression;
    }

    @Override
    public String getName() {
        return compression.getName();
    }

    @SuppressWarnings("rawtypes")
    private static class Lz4Compression implements Compression {
        @Override
        public String getName() {
            return "lz4";
        }

        @Override
        public Compressor getCompressor(Integer level, Map<String, Object> parameters) {
            return new Lz4Compressor(level, parameters);
        }

        @Override
        public Decompressor getDecompressor(Map<String, Object> parameters) {
            return new Lz4Decompressor(parameters);
        }
    }
}
