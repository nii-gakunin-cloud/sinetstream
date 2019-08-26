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

package jp.ad.sinet.stream.plugins.kafka.utils;

import jp.ad.sinet.stream.api.ValueType;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public class KafkaSerdeWrapper<T> implements Serde<T> {

    private final jp.ad.sinet.stream.api.Serializer<T> internalSerializer;
    private final jp.ad.sinet.stream.api.Deserializer<T> internalDeserializer;

    @SuppressWarnings("WeakerAccess")
    public KafkaSerdeWrapper(jp.ad.sinet.stream.api.Serializer<T> serializer, jp.ad.sinet.stream.api.Deserializer<T> deserializer) {
        this.internalSerializer = serializer;
        this.internalDeserializer = deserializer;
    }

    public KafkaSerdeWrapper(jp.ad.sinet.stream.api.Serializer<T> serializer) {
        this(serializer, null);
    }

    public KafkaSerdeWrapper(jp.ad.sinet.stream.api.Deserializer<T> deserializer) {
        this(null, deserializer);
    }

    @SuppressWarnings("unchecked")
    public KafkaSerdeWrapper(ValueType type) {
        this.internalSerializer = type.getSerializer();
        this.internalDeserializer = type.getDeserializer();
    }

    @Override
    public KafkaSerializer<T> serializer() {
        if (internalSerializer instanceof Serializer) {
            return (KafkaSerializer<T>) internalSerializer;
        } else {
            return new KafkaSerializerWrapper();
        }
    }

    @Override
    public KafkaDeserializer<T> deserializer() {
        if (internalDeserializer instanceof Deserializer) {
            return (KafkaDeserializer<T>) internalDeserializer;
        } else {
            return new KafkaDeserializerWrapper();
        }
    }

    public class KafkaDeserializerWrapper implements KafkaDeserializer<T> {
        @Override
        public T deserialize(String topic, byte[] data) {
            return internalDeserializer.deserialize(data);
        }

        @Override
        public T deserialize(byte[] bytes) {
            return internalDeserializer.deserialize(bytes);
        }
    }

    public class KafkaSerializerWrapper implements KafkaSerializer<T> {
        @Override
        public byte[] serialize(String topic, T data) {
            return internalSerializer.serialize(data);
        }

        @Override
        public byte[] serialize(T data) {
            return internalSerializer.serialize(data);
        }
    }
}
