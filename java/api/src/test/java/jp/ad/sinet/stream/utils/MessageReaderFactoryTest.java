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

package jp.ad.sinet.stream.utils;

import jp.ad.sinet.stream.ConfigFileAware;
import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.NoServiceException;
import jp.ad.sinet.stream.api.ValueType;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static jp.ad.sinet.stream.api.Consistency.*;
import static org.junit.jupiter.api.Assertions.*;

class MessageReaderFactoryTest implements ConfigFileAware {

    private static final String DEFAULT_SERVICE = "service-0";
    private static final String SERVICE = "service-with-parameters-for-reader";
    private static final String DESERIALIZER_SERVICE = "service-with-serializer-deserializer";
    private static final String TOPIC = "test-topic-java-001";

    @Test
    void nullService() {
        MessageReaderFactory<String> factory = MessageReaderFactory.<String>builder().service(null).build();
        assertThrows(NoServiceException.class, factory::getReader);
    }

    @Test
    void noTopic() {
        MessageReaderFactory<String> factory = MessageReaderFactory.<String>builder().service(DEFAULT_SERVICE).build();
        assertThrows(IllegalStateException.class, factory::getReader);
    }

    @Test
    void rewriteService() {
        MessageReaderFactory<Object> factory =
                MessageReaderFactory.builder().service("service-1").service(DEFAULT_SERVICE).topic(TOPIC).build();
        try (MessageReader<Object> reader = factory.getReader()) {
            assertNotNull(reader);
        }
    }

    @Test
    void rewriteServiceWithNull() {
        MessageReaderFactory<String> factory =
                MessageReaderFactory.<String>builder().service(DEFAULT_SERVICE).service(null).build();
        assertThrows(NoServiceException.class, factory::getReader);
    }

    @Test
    void defaultParameters() {
        MessageReaderFactory<String> factory =
                MessageReaderFactory.<String>builder().service(DEFAULT_SERVICE).topic(TOPIC).build();
        MessageReader<String> reader = factory.getReader();
        assertEquals(AT_MOST_ONCE, reader.getConsistency());
        assertFalse(reader.isDataEncryption());
        assertEquals(ValueType.TEXT, reader.getValueType());
        assertEquals(Duration.ofNanos(Long.MAX_VALUE), reader.getReceiveTimeout());
    }

    @Test
    void configurationFileParameters() {
        MessageReaderFactory<byte[]> factory =
                MessageReaderFactory.<byte[]>builder().service(SERVICE).build();
        MessageReader<byte[]> reader = factory.getReader();
        assertEquals(AT_LEAST_ONCE, reader.getConsistency());
        assertTrue(reader.isDataEncryption());
        assertEquals(ValueType.BYTE_ARRAY, reader.getValueType());
        assertEquals(Duration.ofMillis(10000), reader.getReceiveTimeout());
        assertEquals("client-001", reader.getClientId());
        assertIterableEquals(Arrays.asList("topic-001", "topic-002"), reader.getTopics());
        assertIterableEquals(
                Arrays.asList("algorithm", "mode", "password", "provider"),
                ((Map) reader.getConfig().get("crypto")).keySet());
        assertIterableEquals(
                Arrays.asList("dummy0.example.org:1718", "dummy1.example.org"),
                (List) reader.getConfig().get("brokers"));
    }

    @Test
    void constructorParameters() {
        Map<String, String> crypto = new LinkedHashMap<>();
        crypto.put("algorithm", "AES");
        crypto.put("mode", "CBC");
        crypto.put("padding", "pkcs7");

        MessageReaderFactory<BufferedImage> factory =
                MessageReaderFactory.<BufferedImage>builder()
                        .service(SERVICE)
                        .consistency(EXACTLY_ONCE)
                        .valueType(ValueType.IMAGE)
                        .dataEncryption(false)
                        .receiveTimeout(Duration.ofSeconds(60))
                        .topics(Arrays.asList("topic-003", "topic-004"))
                        .topic("topic-005")
                        .parameter("crypto", crypto)
                        .build();
        MessageReader<BufferedImage> reader = factory.getReader();
        assertEquals(EXACTLY_ONCE, reader.getConsistency());
        assertFalse(reader.isDataEncryption());
        assertEquals(ValueType.IMAGE, reader.getValueType());
        assertEquals(Duration.ofSeconds(60), reader.getReceiveTimeout());
        assertIterableEquals(Arrays.asList("topic-003", "topic-004", "topic-005"), reader.getTopics());
        assertEquals(ValueType.IMAGE.getDeserializer(), reader.getDeserializer());

        assertIterableEquals(
                Arrays.asList("algorithm", "mode", "padding"), ((Map) reader.getConfig().get("crypto")).keySet());

        assertIterableEquals(
                Arrays.asList("dummy0.example.org:1718", "dummy1.example.org"),
                (List) reader.getConfig().get("brokers"));
    }

    @Test
    void deserializer() {
        MessageReaderFactory<String> factory =
                MessageReaderFactory.<String>builder().service(DESERIALIZER_SERVICE).build();
        MessageReader<String> reader = factory.getReader();
        assertEquals(new StringDeserializer(), reader.getDeserializer());
    }

    @Test
    void deserializerByConstructorParameter() {
        MessageReaderFactory<BufferedImage> factory =
                MessageReaderFactory.<BufferedImage>builder()
                        .service(DESERIALIZER_SERVICE)
                        .parameter("value_deserializer", ImageDeserializer.class)
                        .build();
        MessageReader<BufferedImage> reader = factory.getReader();
        assertEquals(new ImageDeserializer(), reader.getDeserializer());
    }

    @Test
    void deserializerByConstructorParameterAndDeserializer() {
        MessageReaderFactory<String> factory =
                MessageReaderFactory.<String>builder()
                        .service(DESERIALIZER_SERVICE)
                        .deserializer(new StringDeserializer())
                        .parameter("value_deserializer", ImageDeserializer.class)
                        .build();
        MessageReader<String> reader = factory.getReader();
        assertEquals(new StringDeserializer(), reader.getDeserializer());
    }
}
