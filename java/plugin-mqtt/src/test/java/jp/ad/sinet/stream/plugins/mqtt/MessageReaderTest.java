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

package jp.ad.sinet.stream.plugins.mqtt;

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.Message;
import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.ValueType;
import jp.ad.sinet.stream.api.valuetype.SimpleValueType;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="RUN_INTERGRATION_TEST", matches = "(?i)true")
class MessageReaderTest {

    @TempDir
    Path workdir;

    @Nested
    class ReaderTest implements ConfigFileAware {

        @Test
        void testGetReader() {
            MessageReaderFactory<String> builder =
                    MessageReaderFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .receiveTimeout(Duration.ofSeconds(10)).build();
            try (MessageReader<String> reader = builder.getReader()) {
                assertNotNull(reader);
            }
        }

        @ParameterizedTest
        @EnumSource(Consistency.class)
        void consistency(Consistency consistency, TestReporter reporter) {
            MessageReaderFactory<String> builder =
                    MessageReaderFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .consistency(consistency)
                            .receiveTimeout(Duration.ofSeconds(10))
                            .valueType(SimpleValueType.TEXT)
                            .build();
            try (MessageReader<String> reader = builder.getReader()) {
                Message<String> msg;
                while (Objects.nonNull(msg = reader.read())) {
                    assertNotNull(msg.getValue());
                    reporter.publishEntry(msg.getValue());
                }
            }
        }

        @ParameterizedTest
        @EnumSource(Consistency.class)
        void streamTest(Consistency consistency, TestReporter reporter) {
            MessageReaderFactory<String> builder =
                    MessageReaderFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .consistency(consistency)
                            .receiveTimeout(Duration.ofSeconds(3))
                            .valueType(SimpleValueType.TEXT)
                            .build();
            try (MessageReader<String> reader = builder.getReader()) {
                reader.stream().forEach((msg) -> {
                    assertNotNull(msg.getValue());
                    reporter.publishEntry(msg.getValue());
                });
            }
        }
    }

    @Nested
    class TopicTest {

        @Nested
        class Topic implements ConfigFileAware {
            private String topic;

            @Test
            void topic() {
                MessageReaderFactory<String> builder = MessageReaderFactory.<String>builder()
                        .config(getConfigFile(workdir)).service(getServiceName())
                        .topic(topic)
                        .build();
                try (MessageReader<String> reader = builder.getReader()) {
                    assertEquals(topic, reader.getTopic());
                }
            }

            @BeforeEach
            void setupTopic() {
                topic = generateTopic();
            }
        }

        @Nested
        class Topics implements ConfigFileAware {
            private List<String> topics;

            @Test
            void topics() {
                MessageReaderFactory<String> builder = MessageReaderFactory.<String>builder()
                        .config(getConfigFile(workdir)).service(getServiceName())
                        .topics(topics)
                        .build();
                try (MessageReader<String> reader = builder.getReader()) {
                    assertIterableEquals(topics, reader.getTopics());
                }
            }

            @BeforeEach
            void setupTopic() {
                topics = IntStream.range(0, 5).mapToObj(x -> generateTopic()).collect(Collectors.toList());
            }
        }
    }

    @Nested
    class PropertiesTest implements ConfigFileAware {

        @ParameterizedTest
        @EnumSource(Consistency.class)
        void consistency(Consistency consistency) {
            MessageReaderFactory<String> builder =
                    MessageReaderFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .consistency(consistency)
                            .build();
            try (MessageReader<String> reader = builder.getReader()) {
                assertEquals(consistency, reader.getConsistency());
            }
        }

        @Nested
        class ClientIdTest {
            @Test
            void clientId() {
                String clientId = RandomStringUtils.randomAlphabetic(10);
                MessageReaderFactory<String> builder =
                        MessageReaderFactory.<String>builder()
                                .config(getConfigFile(workdir)).service(getServiceName())
                                .clientId(clientId)
                                .build();
                try (MessageReader<String> reader = builder.getReader()) {
                    assertEquals(clientId, reader.getClientId());
                }
            }

            @Test
            void defaultClientId() {
                MessageReaderFactory<String> builder =
                        MessageReaderFactory.<String>builder()
                                .config(getConfigFile(workdir)).service(getServiceName())
                                .build();
                try (MessageReader<String> reader = builder.getReader()) {
                    assertNotNull(reader.getClientId());
                }
            }

            @ParameterizedTest
            @NullAndEmptySource
            void emptyAndNull(String clientId) {
                MessageReaderFactory<String> builder =
                        MessageReaderFactory.<String>builder()
                                .config(getConfigFile(workdir)).service(getServiceName())
                                .clientId(clientId)
                                .build();
                try (MessageReader<String> reader = builder.getReader()) {
                    assertNotNull(reader.getClientId());
                    assertTrue(StringUtils.isNotEmpty(reader.getClientId()));
                }
            }
        }

        @SuppressWarnings("rawtypes")
        @ParameterizedTest
        @EnumSource(SimpleValueType.class)
        void valueType(ValueType valueType) {
            MessageReaderFactory builder =
                    MessageReaderFactory.builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .valueType(valueType)
                            .build();
            try (MessageReader reader = builder.getReader()) {
                assertEquals(valueType, reader.getValueType());
            }
        }

        @Nested
        class ReceiveTimeoutTest {
            @Test
            void defaultTimeout() {
                MessageReaderFactory<String> builder =
                        MessageReaderFactory.<String>builder()
                                .config(getConfigFile(workdir)).service(getServiceName())
                                .build();
                try (MessageReader<String> reader = builder.getReader()) {
                    assertEquals(Duration.ofNanos(Long.MAX_VALUE), reader.getReceiveTimeout());
                }
            }

            @ParameterizedTest
            @MethodSource("jp.ad.sinet.stream.plugins.mqtt.MessageReaderTest#getDurations")
            void receiveTimeout(Duration timeout) {
                MessageReaderFactory<String> builder =
                        MessageReaderFactory.<String>builder()
                                .config(getConfigFile(workdir)).service(getServiceName())
                                .receiveTimeout(timeout)
                                .build();
                try (MessageReader<String> reader = builder.getReader()) {
                    assertEquals(timeout, reader.getReceiveTimeout());
                }
            }
        }
    }

    static Stream<Duration> getDurations() {
        return Stream.of(
                Duration.ofSeconds(10), Duration.ofHours(3), Duration.ofDays(7), Duration.ZERO,
                Duration.ofMillis(100), Duration.ofNanos(123456789));
    }
}
