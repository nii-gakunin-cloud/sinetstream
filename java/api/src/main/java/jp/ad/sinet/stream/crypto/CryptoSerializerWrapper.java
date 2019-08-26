/*
 * Copyright (C) 2019 National Institute of Informatics
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

package jp.ad.sinet.stream.crypto;

import jp.ad.sinet.stream.api.Crypto;
import jp.ad.sinet.stream.api.Serializer;
import lombok.EqualsAndHashCode;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@EqualsAndHashCode
public class CryptoSerializerWrapper<T> implements Serializer<T> {

    private final Serializer<T> serializer;
    private final Function<byte[], byte[]> encrypt;

    private CryptoSerializerWrapper(Crypto crypto, Serializer<T> serializer, Map<String, ?> parameters) {
        this.serializer = serializer;
        encrypt = crypto.getEncoder(parameters);
    }

    @Override
    public byte[] serialize(T data) {
        return encrypt.apply(serializer.serialize(data));
    }

    @SuppressWarnings("unchecked")
    public static <T> Serializer<T> getSerializer(Map<String, ?> config, final Serializer<T> serializer) {
        Optional<CryptoSerializerWrapper> ret = Optional.ofNullable(config.get("crypto"))
                .filter(Map.class::isInstance).map(Map.class::cast)
                .flatMap(cryptoParams ->
                        Optional.ofNullable(cryptoParams.get("provider"))
                                .filter(Crypto.class::isInstance).map(Crypto.class::cast)
                                .map(crypto -> new CryptoSerializerWrapper<>(crypto, serializer, cryptoParams)));
        CryptoSerializerWrapper ser = ret.orElse(null);
        if (Objects.isNull(ser)) {
            return serializer;
        }
        return ser;
    }
}
