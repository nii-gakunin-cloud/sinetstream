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

import jp.ad.sinet.stream.api.*;
import jp.ad.sinet.stream.api.valuetype.SimpleValueType;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MessageReaderTest implements ConfigFileAware {

    private TestReporter reporter;
    private static final String SERVICE = "service-1";
    private static final String TOPIC = "test-topic-java-001";

    @Nested
    class ReaderTest {

        @Test
        void testGetReader() {
            MessageReaderFactory<String> builder =
                    MessageReaderFactory.<String>builder().service(SERVICE).topic(TOPIC).build();
            try (MessageReader<String> reader = builder.getReader()) {
                assertNotNull(reader);
            }
        }

        @ParameterizedTest
        @EnumSource(Consistency.class)
        void consistency(Consistency consistency) {
            MessageReaderFactory<String> builder =
                    MessageReaderFactory.<String>builder().service(SERVICE).topic(TOPIC)
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
        void streamTest(Consistency consistency) {
            MessageReaderFactory<String> builder =
                    MessageReaderFactory.<String>builder().service(SERVICE).topic(TOPIC)
                            .consistency(consistency)
                            .receiveTimeout(Duration.ofSeconds(10))
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
    class PropertiesTest {

        @Nested
        class TopicTest {
            @Test
            void topic() {
                MessageReaderFactory<String> builder = MessageReaderFactory.<String>builder().service(SERVICE).topic(TOPIC).build();
                try (MessageReader<String> reader = builder.getReader()) {
                    assertEquals(TOPIC, reader.getTopic());
                }
            }

            @Test
            void topics() {
                List<String> topics =
                        IntStream.range(0, 5).mapToObj(x -> String.format(TOPIC + "-%d", x)).collect(Collectors.toList());

                MessageReaderFactory<String> builder = MessageReaderFactory.<String>builder().service(SERVICE).topics(topics).build();
                try (MessageReader<String> reader = builder.getReader()) {
                    assertIterableEquals(topics, reader.getTopics());
                }
            }
        }

        @ParameterizedTest
        @EnumSource(Consistency.class)
        void consistency(Consistency consistency) {
            MessageReaderFactory<String> builder =
                    MessageReaderFactory.<String>builder().service(SERVICE).topic(TOPIC)
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
                        MessageReaderFactory.<String>builder().service(SERVICE).topic(TOPIC)
                                .clientId(clientId)
                                .build();
                try (MessageReader<String> reader = builder.getReader()) {
                    assertEquals(clientId, reader.getClientId());
                }
            }

            @Test
            void defaultClientId() {
                MessageReaderFactory<String> builder =
                        MessageReaderFactory.<String>builder().service(SERVICE).topic(TOPIC)
                                .build();
                try (MessageReader<String> reader = builder.getReader()) {
                    assertNotNull(reader.getClientId());
                }
            }

            @ParameterizedTest
            @NullAndEmptySource
            void emptyAndNull(String clientId) {
                MessageReaderFactory<String> builder =
                        MessageReaderFactory.<String>builder().service(SERVICE).topic(TOPIC)
                                .clientId(clientId)
                                .build();
                try (MessageReader<String> reader = builder.getReader()) {
                    assertNotNull(reader.getClientId());
                    assertTrue(StringUtils.isNotEmpty(reader.getClientId()));
                }
            }
        }

        /*
        @ParameterizedTest
        @EnumSource(ValueType.class)
        void valueType(ValueType valueType) {
            MessageReaderFactory builder =
                    MessageReaderFactory.builder().service(SERVICE).topic(TOPIC)
                            .valueType(valueType)
                            .build();
            try (MessageReader reader = builder.getReader()) {
                assertEquals(valueType, reader.getValueType());
            }
        }
         */

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void dataEncryption(Boolean dataEncryption) {
            MessageReaderFactory<String> builder =
                    MessageReaderFactory.<String>builder().service("service-with-encrypt-eax").topic(TOPIC)
                            .dataEncryption(dataEncryption)
                            .build();
            try (MessageReader<String> reader = builder.getReader()) {
                assertEquals(dataEncryption, reader.isDataEncryption());
            }
        }

        /*
        @Nested
        @SuppressWarnings("unchecked")
        class DeserializerTest {
            @ParameterizedTest
            @EnumSource(ValueType.class)
            void deserializer(ValueType valueType) {
                Deserializer des = valueType.getDeserializer();
                MessageReaderFactory builder =
                        MessageReaderFactory.builder().service(SERVICE).topic(TOPIC)
                                .deserializer(des)
                                .build();
                try (MessageReader reader = builder.getReader()) {
                    // @Disabled XXX FIXME Timestamp breaks this test.
                    // assertEquals(des, reader.getDeserializer());
                }
            }

            @Test
            void equality() {
                Deserializer<String> des = ValueType.TEXT.getDeserializer();
                MessageReaderFactory<String> builder =
                        MessageReaderFactory.<String>builder().service(SERVICE).topic(TOPIC)
                                .deserializer(des)
                                .build();
                try (MessageReader<String> reader = builder.getReader()) {
                    byte[] bytes = RandomStringUtils.randomAlphabetic(24).getBytes(StandardCharsets.UTF_8);
                    // @Disabled XXX FIXME Timestamp breaks this test.
                    // assertEquals(des.deserialize(bytes), reader.getDeserializer().deserialize(bytes));
                }
            }
        }
         */

        @Nested
        class ReceiveTimeoutTest {
            @Test
            void defaultTimeout() {
                MessageReaderFactory<String> builder =
                        MessageReaderFactory.<String>builder().service(SERVICE).topic(TOPIC).build();
                try (MessageReader<String> reader = builder.getReader()) {
                    assertEquals(Duration.ofNanos(Long.MAX_VALUE), reader.getReceiveTimeout());
                }
            }

            @ParameterizedTest
            @MethodSource("jp.ad.sinet.stream.plugins.mqtt.MessageReaderTest#getDurations")
            void receiveTimeout(Duration timeout) {
                MessageReaderFactory<String> builder =
                        MessageReaderFactory.<String>builder().service(SERVICE).topic(TOPIC)
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

    @Nested
    class BrokerTest {
        @Test
        void brokersWithDefaultPort() {
            MessageReaderFactory<String> builder =
                    MessageReaderFactory.<String>builder().service("service-2").topic(TOPIC)
                            .receiveTimeout(Duration.ofSeconds(10)).build();
            try (MessageReader<String> reader = builder.getReader()) {
                Message<String> msg;
                while (Objects.nonNull(msg = reader.read())) {
                    assertNotNull(msg.getValue());
                    reporter.publishEntry(msg.getValue());
                }
            }
        }

        @Test
        void brokersByString() {
            MessageReaderFactory<String> builder =
                    MessageReaderFactory.<String>builder().service("service-3").topic(TOPIC)
                            .receiveTimeout(Duration.ofSeconds(10)).build();
            try (MessageReader<String> reader = builder.getReader()) {
                Message<String> msg;
                while (Objects.nonNull(msg = reader.read())) {
                    assertNotNull(msg.getValue());
                    reporter.publishEntry(msg.getValue());
                }
            }
        }

        @Test
        void brokersByStringWithDefaultPort() {
            MessageReaderFactory<String> builder =
                    MessageReaderFactory.<String>builder().service("service-4").topic(TOPIC)
                            .receiveTimeout(Duration.ofSeconds(10)).build();
            try (MessageReader<String> reader = builder.getReader()) {
                Message<String> msg;
                while (Objects.nonNull(msg = reader.read())) {
                    assertNotNull(msg.getValue());
                    reporter.publishEntry(msg.getValue());
                }
            }
        }

        @Test
        void manyBrokers() {
            MessageReaderFactory<String> builder =
                    MessageReaderFactory.<String>builder().service("service-X-1").topic(TOPIC).build();
            assertThrows(InvalidConfigurationException.class, builder::getReader);
        }

        @Test
        void noBrokers() {
            MessageReaderFactory<String> builder =
                    MessageReaderFactory.<String>builder().service("service-X-2").topic(TOPIC).build();
            assertThrows(InvalidConfigurationException.class, builder::getReader);
        }

        @Test
        void emptyBrokers() {
            MessageReaderFactory<String> builder =
                    MessageReaderFactory.<String>builder().service("service-X-3").topic(TOPIC).build();
            assertThrows(InvalidConfigurationException.class, builder::getReader);
        }

        @Test
        void unknownHostBroker() {
            MessageReaderFactory<String> builder =
                    MessageReaderFactory.<String>builder().service("service-Z-1").topic(TOPIC).build();
            assertThrows(ConnectionException.class, builder::getReader);
        }

        @Test
        void unreachableBroker() {
            MessageReaderFactory<String> builder =
                    MessageReaderFactory.<String>builder().service("service-Z-2").topic(TOPIC).build();
            assertThrows(ConnectionException.class, builder::getReader);
        }
    }

    @BeforeEach
    void setup(TestReporter reporter) {
        this.reporter = reporter;
    }
}
