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
import jp.ad.sinet.stream.api.InvalidConfigurationException;
import jp.ad.sinet.stream.spi.CompressionProvider;

import java.util.*;
import java.util.stream.Collectors;

public class CompressionFactory {

    private static final Map<String, Compression> compressions;
    static {
        HashMap<String, Compression> map = new HashMap<>();
        map.putAll(Arrays.stream(BuiltinCompression.values())
                .collect(Collectors.toMap(BuiltinCompression::getName, v -> v)));

        ServiceLoader<CompressionProvider> loader = ServiceLoader.load(CompressionProvider.class);
        for (CompressionProvider provider : loader) {
            map.put(provider.getName(), provider.getCompression());
        }
        compressions = Collections.unmodifiableMap(map);
    }

    static final String DEFAULT_ALGORITHM = "gzip";

    @SuppressWarnings("unchecked")
    private static <T> T getp(Map<String, Object> map, String k, String clsT) {
        if (map == null)
            return null;
        Object o = map.get(k);
        if (o == null)
            return null;
        try {
            return (T) o;
        }
        catch (ClassCastException e) {
            //String cls = T.class.getSimpleName();
            String cls = clsT;
            throw new InvalidConfigurationException("the parameter " + k + " must be " + cls, e);
        }
    }

    private static <T> T or(T a, T b) {
        return a != null ? a : b;
    }

    public static Compressor createCompressor(Map<String, Object> parameters) {
        String algorithm = or(getp(parameters, "algorithm", "String"), DEFAULT_ALGORITHM);
        Compression compression = compressions.get(algorithm);
        if (compression == null)
            throw new InvalidConfigurationException("unsupported compression algorithm");
        Integer level = getp(parameters, "level", "Integer");
        return compression.getCompressor(level, parameters);
    }

    public static Decompressor createDecompressor(Map<String, Object> parameters) {
        String algorithm = or(getp(parameters, "algorithm", "String"), DEFAULT_ALGORITHM);
        Compression compression = compressions.get(algorithm);
        if (compression == null)
            throw new InvalidConfigurationException("unsupported compression algorithm");
        return compression.getDecompressor(parameters);
    }
}
