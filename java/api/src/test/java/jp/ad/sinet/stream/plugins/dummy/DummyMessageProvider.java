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

package jp.ad.sinet.stream.plugins.dummy;

import jp.ad.sinet.stream.api.*;
import jp.ad.sinet.stream.crypto.CryptoDeserializerWrapper;
import jp.ad.sinet.stream.crypto.CryptoSerializerWrapper;
import jp.ad.sinet.stream.spi.MessageReaderProvider;
import jp.ad.sinet.stream.spi.MessageWriterProvider;
import jp.ad.sinet.stream.spi.ReaderParameters;
import jp.ad.sinet.stream.spi.WriterParameters;
import lombok.Data;
import lombok.Getter;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

public class DummyMessageProvider<T> implements MessageWriterProvider<T>, MessageReaderProvider<T> {
    @Override
    public MessageWriter<T> getWriter(WriterParameters<T> params) {
        return new DummyWriter(params);
    }

    @Override
    public MessageReader<T> getReader(ReaderParameters<T> params) {
        return new DummyReader(params);
    }

    @Override
    public String getType() {
        return "dummy";
    }

    private static final ConcurrentMap<String, BlockingQueue<byte[]>> topicQueue = new ConcurrentHashMap<>();

    @Data
    private class DummyIO implements MessageIO {

        String service;
        Consistency consistency;
        String clientId;
        Map<String, Object> config;
        ValueType valueType;
        String topic;
        boolean dataEncryption;

        @Override
        public void close() {
        }

        @SuppressWarnings("unchecked")
        DummyIO(WriterParameters params) {
            service = params.getService();
            consistency = params.getConsistency();
            clientId = params.getClientId();
            valueType = params.getValueType();
            config = (Map<String, Object>) Collections.unmodifiableMap(params.getConfig());
            topic = params.getTopic();
            dataEncryption = params.isDataEncryption();
        }

        DummyIO(ReaderParameters<T> params) {
            service = params.getService();
            consistency = params.getConsistency();
            clientId = params.getClientId();
            valueType = params.getValueType();
            config = Collections.unmodifiableMap(params.getConfig());
            topic = String.join(",", params.getTopics());
            dataEncryption = params.isDataEncryption();
        }
    }

    private class DummyWriter extends DummyIO implements MessageWriter<T> {

        @SuppressWarnings("unchecked")
        DummyWriter(WriterParameters<T> params) {
            super(params);
            topicQueue.putIfAbsent(topic, new LinkedBlockingQueue<>());

            Serializer<T> ser;
            if (Objects.isNull(params.getSerializer())) {
                ser = valueType.getSerializer();
            } else {
                ser = params.getSerializer();
            }
            serializer = CryptoSerializerWrapper.getSerializer(config, ser);
        }

        @Getter
        private Serializer<T> serializer;

        @Override
        public void write(T message) {
            try {
                BlockingQueue<byte[]> queue = topicQueue.get(topic);
                queue.put(serializer.serialize(message));
            } catch (InterruptedException e) {
                throw new SinetStreamIOException(e);
            }
        }
    }

    private class DummyReader extends DummyIO implements MessageReader<T> {

        @Getter
        private final List<String> topics;
        @Getter
        private Duration receiveTimeout;
        @Getter
        private Deserializer<T> deserializer;

        @SuppressWarnings("unchecked")
        DummyReader(ReaderParameters<T> params) {
            super(params);
            topics = params.getTopics();
            receiveTimeout = params.getReceiveTimeout();
            topicQueue.putIfAbsent(topic, new LinkedBlockingQueue<>());

            Deserializer<T> des;
            if (Objects.isNull(params.getDeserializer())) {
                des = this.getValueType().getDeserializer();
            } else {
                des = params.getDeserializer();
            }
            deserializer = CryptoDeserializerWrapper.getDeserializer(config, des);
        }

        @Override
        public Message<T> read() {
            try {
                BlockingQueue<byte[]> queue = topicQueue.get(topic);
                byte[] bytes = queue.poll(receiveTimeout.toNanos(), TimeUnit.NANOSECONDS);
                if (Objects.isNull(bytes)) {
                    return null;
                }
                T msg = deserializer.deserialize(bytes);
                return new DummyMessage(msg);
            } catch (InterruptedException e) {
                throw new SinetStreamIOException(e);
            }
        }

        private class DummyMessage implements Message<T> {
            @Getter
            private final T value;

            DummyMessage(T msg) {
                value = msg;
            }

            @Override
            public String getTopic() {
                return topic;
            }

            @Override
            public Object getRaw() {
                return value;
            }
        }
    }
}
