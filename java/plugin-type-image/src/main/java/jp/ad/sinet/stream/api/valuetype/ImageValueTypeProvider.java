/*
 * Copyright (C) 2020 National Institute of Informatics
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

package jp.ad.sinet.stream.api.valuetype;

import jp.ad.sinet.stream.api.Deserializer;
import jp.ad.sinet.stream.api.Serializer;
import jp.ad.sinet.stream.api.ValueType;
import jp.ad.sinet.stream.spi.ValueTypeProvider;

public class ImageValueTypeProvider implements ValueTypeProvider {

    private static final ValueType valueType = new ImageValueType();

    @Override
    public ValueType getValueType() {
        return valueType;
    }

    @Override
    public String getName() {
        return valueType.getName();
    }

    @SuppressWarnings("rawtypes")
    private static class ImageValueType implements ValueType {

        private final Serializer serializer = new ImageSerializer();
        private final Deserializer deserializer = new ImageDeserializer();

        @Override
        public String getName() {
            return "image";
        }

        @Override
        public Serializer getSerializer() {
            return serializer;
        }

        @Override
        public Deserializer getDeserializer() {
            return deserializer;
        }
    }
}
