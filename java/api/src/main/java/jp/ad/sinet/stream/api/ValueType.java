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

package jp.ad.sinet.stream.api;

import jp.ad.sinet.stream.utils.*;

import java.util.EnumMap;
import java.util.Optional;

public enum ValueType {
    TEXT,
    IMAGE,
    BYTE_ARRAY;

    private static final EnumMap<ValueType, Serializer> serializer;
    private static final EnumMap<ValueType, Deserializer> deserializer;

    static {
        serializer = new EnumMap<>(ValueType.class);
        deserializer = new EnumMap<>(ValueType.class);

        serializer.put(ValueType.TEXT, new StringSerializer());
        deserializer.put(ValueType.TEXT, new StringDeserializer());

        serializer.put(ValueType.IMAGE, new ImageSerializer());
        deserializer.put(ValueType.IMAGE, new ImageDeserializer());

        serializer.put(ValueType.BYTE_ARRAY, new ByteArraySerializer());
        deserializer.put(ValueType.BYTE_ARRAY, new ByteArrayDeserializer());
    }

    @SuppressWarnings("deprecation")
    public Serializer getSerializer() {
        return Optional.ofNullable(serializer.get(this)).orElseGet(SerializableSerializer::new);
    }

    @SuppressWarnings("deprecation")
    public Deserializer getDeserializer() {
        return Optional.ofNullable(deserializer.get(this)).orElseGet(SerializableDeserializer::new);
    }
}