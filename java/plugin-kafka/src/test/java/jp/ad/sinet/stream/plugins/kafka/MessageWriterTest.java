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

package jp.ad.sinet.stream.plugins.kafka;

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.api.valuetype.SimpleValueType;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MessageWriterTest implements ConfigFileAware {

    @TempDir
    Path workdir;

    @Test
    void testGetWriter() {
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .config(getConfigFile(workdir)).service(getServiceName())
                        .build();
        try (MessageWriter<String> writer = builder.getWriter()) {
            assertNotNull(writer);
            writer.write("message-1");
        }
    }

    @Test
    void configIsReadonly() {
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .config(getConfigFile(workdir)).service(getServiceName()).build();
        try (MessageWriter<String> writer = builder.getWriter()) {
            assertNotNull(writer);
            assertThrows(UnsupportedOperationException.class,
                    () -> writer.getConfig().put("type", "MQTT"));
        }
    }

    @Test
    void serviceType() {
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .config(getConfigFile(workdir)).service(getServiceName()).build();
        try (MessageWriter<String> writer = builder.getWriter()) {
            assertNotNull(writer);
            assertEquals(getServiceName(), writer.getService());
            assertEquals(getServiceType(), writer.getConfig().get("type"));
        }
    }

    @Nested
    class PropertiesTest {

        @Test
        void topic() {
            String topic = generateTopic();
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .topic(topic).build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                writer.write("message-1");
                assertEquals(topic, writer.getTopic());
            }
        }

        @ParameterizedTest
        @EnumSource(Consistency.class)
        void consistency(Consistency consistency) {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
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
                        MessageWriterFactory.<String>builder()
                                .config(getConfigFile(workdir)).service(getServiceName())
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
                        MessageWriterFactory.<String>builder()
                                .config(getConfigFile(workdir)).service(getServiceName())
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
                        MessageWriterFactory.<String>builder()
                                .config(getConfigFile(workdir)).service(getServiceName())
                                .clientId(clientId)
                                .build();
                try (MessageWriter<String> writer = builder.getWriter()) {
                    writer.write("message-1");
                    assertNotNull(writer.getClientId());
                    assertTrue(StringUtils.isNotEmpty(writer.getClientId()));
                }
            }
        }

        @SuppressWarnings("rawtypes")
        @ParameterizedTest
        @EnumSource(SimpleValueType.class)
        void valueType(SimpleValueType valueType) {
            MessageWriterFactory builder =
                    MessageWriterFactory.builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .valueType(valueType)
                            .build();
            try (MessageWriter writer = builder.getWriter()) {
                assertEquals(valueType, writer.getValueType());
            }
        }
    }
}
