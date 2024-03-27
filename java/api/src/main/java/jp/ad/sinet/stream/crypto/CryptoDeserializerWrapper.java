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
import jp.ad.sinet.stream.api.InvalidMessageException;
import jp.ad.sinet.stream.utils.Pair;

import lombok.EqualsAndHashCode;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

@EqualsAndHashCode
public class CryptoDeserializerWrapper<T> implements Deserializer<T> {

    private final Deserializer<Pair<byte[], Integer>> pktdes;
    private final Deserializer<T> deserializer;
    private final BiFunction<byte[], Integer, byte[]> decrypt;

    private CryptoDeserializerWrapper(Crypto crypto, Deserializer<Pair<byte[], Integer>> pktdes, Deserializer<T> deserializer, Map<String, ?> parameters) {
        this.pktdes = pktdes;
        this.deserializer = deserializer;
        this.decrypt = crypto.getDecoder(parameters);
    }

    @Override
    public T deserialize(byte[] bytes) {
        Pair<byte[], Integer> pkt = pktdes.deserialize(bytes);
        return deserializer.deserialize(decrypt.apply(pkt.getV1(), pkt.getV2()));
    }

    @SuppressWarnings("unchecked")
    public static <T> Deserializer<T> getDeserializer(Map<String, ?> config, final Deserializer<Pair<byte[], Integer>> pktdes, final Deserializer<T> deserializer) {
        Optional<CryptoDeserializerWrapper> ret = Optional.ofNullable(config.get("crypto"))
                .filter(Map.class::isInstance).map(Map.class::cast)
                .flatMap(cryptoParams ->
                        Optional.ofNullable(cryptoParams.get("provider"))
                                .filter(Crypto.class::isInstance).map(Crypto.class::cast)
                                .map(crypto -> new CryptoDeserializerWrapper<>(crypto, pktdes, deserializer, cryptoParams)));
        CryptoDeserializerWrapper des = ret.orElse(null);
        if (Objects.isNull(des)) {
            Deserializer<T> plaindes = new Deserializer<T>() {
                public T deserialize(byte[] bytes) {
                    Pair<byte[], Integer> pkt = pktdes.deserialize(bytes);
                    if (pkt.getV2() != null && pkt.getV2() != 0) {
                        throw new InvalidMessageException("unexpected: an encrypted message received when data_encryption==false");
                    }
                    return deserializer.deserialize(pkt.getV1());
                }
            };
            return plaindes;
        }
        return des;
    }
}
