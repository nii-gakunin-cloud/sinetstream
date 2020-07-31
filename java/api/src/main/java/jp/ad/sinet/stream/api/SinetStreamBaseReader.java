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

import jp.ad.sinet.stream.crypto.CryptoDeserializerWrapper;
import jp.ad.sinet.stream.marshal.Unmarshaller;
import jp.ad.sinet.stream.spi.PluginMessageIO;
import jp.ad.sinet.stream.spi.PluginMessageWrapper;
import jp.ad.sinet.stream.spi.ReaderParameters;
import jp.ad.sinet.stream.utils.Timestamped;
import lombok.Getter;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SinetStreamBaseReader<T, U extends PluginMessageIO> extends SinetStreamIO<U> {

    @Getter
    private final List<String> topics;

    @Getter
    private final Duration receiveTimeout;

    @Getter
    private final Deserializer<T> deserializer;

    private final Deserializer<Timestamped<T>> compositeDeserializer;

    public SinetStreamBaseReader(U pluginReader, ReaderParameters parameters, Deserializer<T> deserializer) {
        super(parameters, pluginReader);
        this.topics = Collections.unmodifiableList(parameters.getTopics());
        this.receiveTimeout = parameters.getReceiveTimeout();
        this.deserializer = setupDeserializer(parameters, deserializer);
        this.compositeDeserializer = generateDeserializer(parameters);
    }

    @SuppressWarnings("unchecked")
    private Deserializer<T> setupDeserializer(ReaderParameters parameters, Deserializer<T> deserializer) {
        if (Objects.isNull(deserializer)) {
            return parameters.getValueType().getDeserializer();
        }
        return deserializer;
    }

    private Deserializer<Timestamped<T>> generateDeserializer(ReaderParameters parameters) {
        final Unmarshaller unmashaller = new Unmarshaller();
        Deserializer<Timestamped<T>> tsdes = (bytes) -> {
            Timestamped<byte[]> data = unmashaller.decode(bytes);
            T value = this.deserializer.deserialize(data.getValue());
            return new Timestamped<>(value, data.getTstamp());
        };
        return CryptoDeserializerWrapper.getDeserializer(parameters.getConfig(), tsdes);
    }

    protected Message<T> toMessage(PluginMessageWrapper pluginMessage) {
        if (Objects.isNull(pluginMessage)) {
            return null;
        }
        byte[] payload = pluginMessage.getValue();
        updateMetrics(payload.length);
        Timestamped<T> tsRecord = compositeDeserializer.deserialize(payload);
        return new Message<>(tsRecord.getValue(), pluginMessage.getTopic(), tsRecord.getTstamp(), pluginMessage.getRaw());
    }

    public String getTopic() {
        return String.join(",", topics);
    }
}
