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
import jp.ad.sinet.stream.spi.PluginMessageReader;
import jp.ad.sinet.stream.spi.PluginMessageWrapper;
import jp.ad.sinet.stream.spi.ReaderParameters;
import jp.ad.sinet.stream.utils.Timestamped;
import lombok.Getter;

import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class SinetStreamMessageReader<T> extends SinetStreamIO<PluginMessageReader> implements MessageReader<T> {

    @Getter
    private final List<String> topics;

    @Getter
    private final Duration receiveTimeout;

    @Getter
    private final Deserializer<T> deserializer;

    private final Deserializer<Timestamped<T>> compositeDeserializer;

    public SinetStreamMessageReader(PluginMessageReader pluginReader, ReaderParameters parameters, Deserializer<T> deserializer) {
        super(parameters, pluginReader);
        this.topics = parameters.getTopics();
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

    @Override
    public Message<T> read() {
        return toMessage(target.read());
    }

    private Message<T> toMessage(PluginMessageWrapper pluginMessage) {
        if (Objects.isNull(pluginMessage)) {
            return null;
        }
        Timestamped<T> tsRecord = compositeDeserializer.deserialize(pluginMessage.getValue());
        return new Message<>(tsRecord.getValue(), pluginMessage.getTopic(), tsRecord.getTstamp(), pluginMessage.getRaw());
    }

    @Override
    public Stream<Message<T>> stream() {
        Iterator<Message<T>> iterator = new Iterator<Message<T>>() {
            private Iterator<PluginMessageWrapper> it = target.stream().iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Message<T> next() {
                return toMessage(it.next());
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED | Spliterator.NONNULL), false);
    }

    @Override
    public String getTopic() {
        return String.join(",", topics);
    }
}
