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

package jp.ad.sinet.stream.api;

import jp.ad.sinet.stream.crypto.CryptoSerializerWrapper;
import jp.ad.sinet.stream.marshal.Marshaller;
import jp.ad.sinet.stream.spi.PluginMessageWriter;
import jp.ad.sinet.stream.spi.WriterParameters;
import jp.ad.sinet.stream.utils.Timestamped;
import lombok.Getter;

import java.util.Objects;

public class SinetStreamMessageWriter<T> extends SinetStreamIO<PluginMessageWriter> implements MessageWriter<T> {

    @Getter
    private final String topic;

    @Getter
    private final Serializer<T> serializer;

    private final Serializer<Timestamped<T>> compositeSerializer;

    public SinetStreamMessageWriter(PluginMessageWriter pluginWriter, WriterParameters parameters, Serializer<T> serializer) {
        super(parameters, pluginWriter);
        this.topic = parameters.getTopic();
        this.serializer = setupSerializer(parameters, serializer);
        this.compositeSerializer = generateSerializer(parameters);
    }

    @SuppressWarnings("unchecked")
    private Serializer<T> setupSerializer(WriterParameters parameters, Serializer<T> serializer) {
        if (Objects.isNull(serializer)) {
            return parameters.getValueType().getSerializer();
        }
        return serializer;
    }

    private Serializer<Timestamped<T>> generateSerializer(WriterParameters parameters) {
        final Marshaller marshaller = new Marshaller();
        Serializer<Timestamped<T>> tsser = (data) -> {
            byte[] bytes = this.serializer.serialize(data.getValue());
            return marshaller.encode(data.getTstamp(), bytes);
        };
        return CryptoSerializerWrapper.getSerializer(parameters.getConfig(), tsser);
    }

    @Override
    public void write(T message) {
        byte[] payload = compositeSerializer.serialize(new Timestamped<>(message));
        target.write(payload);
    }
}
