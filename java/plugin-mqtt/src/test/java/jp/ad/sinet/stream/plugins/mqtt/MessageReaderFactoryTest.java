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

package jp.ad.sinet.stream.plugins.mqtt;

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static jp.ad.sinet.stream.api.Consistency.AT_MOST_ONCE;
import static org.junit.jupiter.api.Assertions.*;

class MessageReaderFactoryTest implements ConfigFileAware {

    @Test
    void serviceAndTopic() {
        String service = "service-1";
        String topic = "test-topic-java-001";
        MessageReaderFactory<String> builder = MessageReaderFactory.<String>builder().service(service).topic(topic).build();
        try (MessageReader<String> reader = builder.getReader()) {
            assertNotNull(reader);
            assertEquals(service, reader.getService());
            assertEquals(topic, reader.getTopic());
            assertIterableEquals(Collections.singletonList(topic), reader.getTopics());
        }
    }

    @Test
    void topics() {
        List<String> topics = Arrays.asList("test-topic-java-001", "test-topic-java-002");
        MessageReaderFactory<String> builder = MessageReaderFactory.<String>builder().service("service-1").topics(topics).build();
        try (MessageReader<String> reader = builder.getReader()) {
            assertNotNull(reader);
            assertEquals(String.join(",", topics), reader.getTopic());
            assertIterableEquals(topics, reader.getTopics());
        }
    }

    @Test
    void clientId() {
        String clientId = "client-000";
        MessageReaderFactory<String> builder =
                MessageReaderFactory.<String>builder().service("service-1").topic("test-topic-java-001")
                        .clientId(clientId).build();
        try (MessageReader<String> reader = builder.getReader()) {
            assertNotNull(reader);
            assertEquals(clientId, reader.getClientId());
        }
    }

    @ParameterizedTest
    @EnumSource(Consistency.class)
    void consistency(Consistency consistency) {
        MessageReaderFactory<String> builder =
                MessageReaderFactory.<String>builder().service("service-1").topic("test-topic-java-001")
                        .consistency(consistency).build();
        try (MessageReader<String> reader = builder.getReader()) {
            assertNotNull(reader);
            assertEquals(consistency, reader.getConsistency());
        }
    }

    @Test
    void defaultConsistency() {
        MessageReaderFactory<String> builder =
                MessageReaderFactory.<String>builder().service("service-1").topic("test-topic-java-001").build();
        try (MessageReader<String> reader = builder.getReader()) {
            assertNotNull(reader);
            assertEquals(AT_MOST_ONCE, reader.getConsistency());
        }
    }

    @Test
    void receiveTimeout() {
        Duration timeout = Duration.ofSeconds(30);
        MessageReaderFactory<String> builder =
                MessageReaderFactory.<String>builder().service("service-1").topic("test-topic-java-001")
                        .receiveTimeout(timeout).build();
        try (MessageReader<String> reader = builder.getReader()) {
            assertNotNull(reader);
            assertEquals(timeout, reader.getReceiveTimeout());
        }
    }

    @Test
    void defaultReceiveTimeout() {
        MessageReaderFactory<String> builder =
                MessageReaderFactory.<String>builder().service("service-1").topic("test-topic-java-001").build();
        try (MessageReader<String> reader = builder.getReader()) {
            assertNotNull(reader);
            assertEquals(Duration.ofNanos(Long.MAX_VALUE), reader.getReceiveTimeout());
        }
    }
}
