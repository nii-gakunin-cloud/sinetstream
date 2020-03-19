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

import jp.ad.sinet.stream.api.ConnectionException;
import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.InvalidConfigurationException;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.api.valuetype.SimpleValueType;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class MessageWriterTest implements ConfigFileAware {

    private static final String SERVICE = "service-1";
    private static final String TOPIC = "test-topic-java-001";

    @Nested
    class WriterTest {

        @Test
        void testGetWriter() {
            MessageWriterFactory<String> builder = MessageWriterFactory.<String>builder().service(SERVICE).topic(TOPIC).build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                assertNotNull(writer);
            }
        }

        @ParameterizedTest
        @EnumSource(Consistency.class)
        void consistency(Consistency consistency) {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder().service(SERVICE).topic(TOPIC)
                            .consistency(consistency)
                            .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                writer.write("message-1");
            }
        }

        @Test
        void configIsReadonly() {
            MessageWriterFactory<String> builder = MessageWriterFactory.<String>builder().service(SERVICE).topic(TOPIC).build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                assertNotNull(writer);
                assertThrows(UnsupportedOperationException.class, () -> writer.getConfig().put("type", "kafka"));
            }
        }

        @Test
        void serviceType() {
            MessageWriterFactory<String> builder = MessageWriterFactory.<String>builder().service(SERVICE).topic(TOPIC).build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                assertNotNull(writer);
                assertEquals(SERVICE, writer.getService());
                assertEquals(TOPIC, writer.getTopic());
                assertEquals("mqtt", writer.getConfig().get("type"));
            }
        }
    }


    @Nested
    class PropertiesTest {

        @Test
        void topic() {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder().service(SERVICE).topic(TOPIC).build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                writer.write("message-1");
                assertEquals(TOPIC, writer.getTopic());
            }
        }

        @ParameterizedTest
        @EnumSource(Consistency.class)
        void consistency(Consistency consistency) {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder().service(SERVICE).topic(TOPIC)
                            .consistency(consistency)
                            .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                writer.write("message-1");
                assertEquals(consistency, writer.getConsistency());
            }
        }

        @Nested
        class ClientIdTest {
            @Test
            void clientId() {
                String clientId = RandomStringUtils.randomAlphabetic(10);
                MessageWriterFactory<String> builder =
                        MessageWriterFactory.<String>builder().service(SERVICE).topic(TOPIC)
                                .clientId(clientId)
                                .build();
                try (MessageWriter<String> writer = builder.getWriter()) {
                    writer.write("message-1");
                    assertEquals(clientId, writer.getClientId());
                }
            }

            @Test
            void defaultClientId() {
                MessageWriterFactory<String> builder =
                        MessageWriterFactory.<String>builder().service(SERVICE).topic(TOPIC)
                                .build();
                try (MessageWriter<String> writer = builder.getWriter()) {
                    writer.write("message-1");
                    assertNotNull(writer.getClientId());
                }
            }

            @ParameterizedTest
            @NullAndEmptySource
            void emptyAndNull(String clientId) {
                MessageWriterFactory<String> builder =
                        MessageWriterFactory.<String>builder().service(SERVICE).topic(TOPIC)
                                .clientId(clientId)
                                .build();
                try (MessageWriter<String> writer = builder.getWriter()) {
                    writer.write("message-1");
                    assertNotNull(writer.getClientId());
                    assertTrue(StringUtils.isNotEmpty(writer.getClientId()));
                }
            }
        }

        /*
        @ParameterizedTest
        @EnumSource(ValueType.class)
        void valueType(ValueType valueType) {
            MessageWriterFactory builder =
                    MessageWriterFactory.builder().service(SERVICE).topic(TOPIC)
                            .valueType(valueType)
                            .build();
            try (MessageWriter writer = builder.getWriter()) {
                assertEquals(valueType, writer.getValueType());
            }
        }
         */

        /*
        @Nested
        @SuppressWarnings("unchecked")
        class SerializerTest {
            @ParameterizedTest
            @EnumSource(ValueType.class)
            void serializer(ValueType valueType) {
                Serializer ser = valueType.getSerializer();
                MessageWriterFactory builder =
                        MessageWriterFactory.builder().service(SERVICE).topic(TOPIC)
                                .serializer(ser)
                                .build();
                try (MessageWriter writer = builder.getWriter()) {
                    // @Disabled XXX FIXME Timestamp breaks this test.
                    // assertEquals(ser, writer.getSerializer());
                }
            }

            @Test
            void equality() {
                Serializer<String> ser = ValueType.TEXT.getSerializer();
                MessageWriterFactory<String> builder =
                        MessageWriterFactory.<String>builder().service(SERVICE).topic(TOPIC)
                                .serializer(ser)
                                .build();
                try (MessageWriter<String> writer = builder.getWriter()) {
                    String text = RandomStringUtils.randomAlphabetic(24);
                    // @Disabled XXX FIXME Timestamp breaks this test.
                    // assertArrayEquals(ser.serialize(text), writer.getSerializer().serialize(text));
                }
            }
        }
         */

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void dataEncryption(Boolean dataEncryption) {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder().service("service-with-encrypt-eax").topic(TOPIC)
                            .dataEncryption(dataEncryption)
                            .valueType(SimpleValueType.TEXT)
                            .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                writer.write("message-1");
                assertEquals(dataEncryption, writer.isDataEncryption());
            }
        }
    }

    @Nested
    class BrokerTest {
        @Test
        void brokersWithDefaultPort() {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder().service("service-2").topic(TOPIC).build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                writer.write("message-1");
            }
        }

        @Test
        void brokersByString() {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder().service("service-3").topic(TOPIC).build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                writer.write("message-1");
            }
        }

        @Test
        void brokersByStringWithDefaultPort() {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder().service("service-4").topic(TOPIC).build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                writer.write("message-1");
            }
        }

        @Test
        void manyBrokers() {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder().service("service-X-1").topic(TOPIC).build();
            assertThrows(InvalidConfigurationException.class, builder::getWriter);
        }

        @Test
        void noBrokers() {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder().service("service-X-2").topic(TOPIC).build();
            assertThrows(InvalidConfigurationException.class, builder::getWriter);
        }

        @Test
        void emptyBrokers() {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder().service("service-X-3").topic(TOPIC).build();
            assertThrows(InvalidConfigurationException.class, builder::getWriter);
        }

        @Test
        void unknownHostBroker() {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder().service("service-Z-1").topic(TOPIC).build();
            assertThrows(ConnectionException.class, builder::getWriter);
        }

        @Test
        void unreachableBroker() {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder().service("service-Z-2").topic(TOPIC).build();
            assertThrows(ConnectionException.class, builder::getWriter);
        }
    }
}
