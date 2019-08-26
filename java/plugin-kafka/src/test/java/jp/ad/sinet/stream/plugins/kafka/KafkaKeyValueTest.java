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

package jp.ad.sinet.stream.plugins.kafka;

import jp.ad.sinet.stream.api.Message;
import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KafkaKeyValueTest implements ConfigFileAware {

    private static final String SERVICE = "service-1";
    private static final String TOPIC = "test-topic-java-" + RandomStringUtils.randomAlphanumeric(4);
    private static final String GROUP = "group-java-" + RandomStringUtils.randomAlphanumeric(4);

    @Test
    @SuppressWarnings("unchecked")
    void DefaultKeyType() {
        MessageReaderFactory<String> readerFactory =
                MessageReaderFactory.<String>builder().service(SERVICE).topic(TOPIC)
                        .receiveTimeout(Duration.ofSeconds(2))
                        .parameter("group_id", GROUP).build();
        MessageWriterFactory<String> writerFactory =
                MessageWriterFactory.<String>builder().service(SERVICE).topic(TOPIC)
                        .build();
        try (MessageReader<String> reader = readerFactory.getReader();
             MessageWriter<String> writer = writerFactory.getWriter()) {
            reader.read();

            KafkaMessageReader<String, String> kafkaReader = (KafkaMessageReader<String, String>) reader;
            KafkaMessageWriter<String, String> kafkaWriter = (KafkaMessageWriter<String, String>) writer;

            IntStream.range(0, 10).forEach(i -> {
                String key = RandomStringUtils.randomAlphabetic(10);
                String value = RandomStringUtils.randomAlphabetic(100);
                kafkaWriter.write(key, value);
                Message<String> msg = kafkaReader.read();
                assertEquals(value, msg.getValue());
            });
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void ByteArrayKey() {
        MessageReaderFactory<String> readerFactory =
                MessageReaderFactory.<String>builder().service(SERVICE).topic(TOPIC)
                        .receiveTimeout(Duration.ofSeconds(2))
                        .parameter("group_id", GROUP)
                        .parameter("key_deserializer", ByteArrayDeserializer.class)
                        .build();
        MessageWriterFactory<String> writerFactory =
                MessageWriterFactory.<String>builder().service(SERVICE).topic(TOPIC)
                        .parameter("key_serializer", ByteArraySerializer.class)
                        .build();
        try (MessageReader<String> reader = readerFactory.getReader();
             MessageWriter<String> writer = writerFactory.getWriter()) {
            reader.read();

            KafkaMessageReader<byte[], String> kafkaReader = (KafkaMessageReader<byte[], String>) reader;
            KafkaMessageWriter<byte[], String> kafkaWriter = (KafkaMessageWriter<byte[], String>) writer;

            IntStream.range(0, 10).forEach(i -> {
                byte[] key = RandomStringUtils.randomAlphabetic(10).getBytes(StandardCharsets.UTF_8);
                String value = RandomStringUtils.randomAlphabetic(100);
                kafkaWriter.write(key, value);
                Message<String> msg = kafkaReader.read();
                assertEquals(value, msg.getValue());
            });
        }
    }
}
