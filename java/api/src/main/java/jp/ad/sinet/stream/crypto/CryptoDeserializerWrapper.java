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
import jp.ad.sinet.stream.api.Deserializer;
import lombok.EqualsAndHashCode;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@EqualsAndHashCode
public class CryptoDeserializerWrapper<T> implements Deserializer<T> {

    private final Deserializer<T> deserializer;
    private final Function<byte[], byte[]> decrypt;

    private CryptoDeserializerWrapper(Crypto crypto, Deserializer<T> deserializer, Map<String, ?> parameters) {
        this.deserializer = deserializer;
        decrypt = crypto.getDecoder(parameters);
    }

    @Override
    public T deserialize(byte[] bytes) {
        return deserializer.deserialize(decrypt.apply(bytes));
    }

    @SuppressWarnings("unchecked")
    public static <T> Deserializer<T> getDeserializer(Map<String, ?> config, final Deserializer<T> deserializer) {
        Optional<CryptoDeserializerWrapper> ret = Optional.ofNullable(config.get("crypto"))
                .filter(Map.class::isInstance).map(Map.class::cast)
                .flatMap(cryptoParams ->
                        Optional.ofNullable(cryptoParams.get("provider"))
                                .filter(Crypto.class::isInstance).map(Crypto.class::cast)
                                .map(crypto -> new CryptoDeserializerWrapper<>(crypto, deserializer, cryptoParams)));
        CryptoDeserializerWrapper des = ret.orElse(null);
        if (Objects.isNull(des)) {
            return deserializer;
        }
        return des;
    }
}
