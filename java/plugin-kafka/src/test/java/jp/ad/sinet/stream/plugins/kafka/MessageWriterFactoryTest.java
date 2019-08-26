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

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static jp.ad.sinet.stream.api.Consistency.AT_MOST_ONCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MessageWriterFactoryTest implements ConfigFileAware {

    @Test
    void serviceAndTopic() {
        String service = "service-1";
        String topic = "test-topic-java-001";
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder().service(service).topic(topic).build();
        try (MessageWriter<String> writer = builder.getWriter()) {
            assertNotNull(writer);
            assertEquals(service, writer.getService());
            assertEquals(topic, writer.getTopic());
        }
    }

    @Test
    void clientId() {
        String clientId = "client-000";
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder().service("service-1").topic("test-topic-java-001")
                        .clientId(clientId).build();
        try (MessageWriter<String> writer = builder.getWriter()) {
            assertNotNull(writer);
            assertEquals(clientId, writer.getClientId());
        }
    }

    @Test
    void defaultClientId() {
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder().service("service-1").topic("test-topic-java-001").build();
        try (MessageWriter<String> writer = builder.getWriter()) {
            assertNotNull(writer);
            assertNotNull(writer.getClientId());
        }
    }

    @ParameterizedTest
    @EnumSource(Consistency.class)
    void consistency(Consistency consistency) {
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder().service("service-1").topic("test-topic-java-001")
                        .consistency(consistency).build();
        try (MessageWriter<String> writer = builder.getWriter()) {
            assertNotNull(writer);
            assertEquals(consistency, writer.getConsistency());
        }
    }

    @Test
    void defaultConsistency() {
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder().service("service-1").topic("test-topic-java-001").build();
        try (MessageWriter<String> writer = builder.getWriter()) {
            assertNotNull(writer);
            assertEquals(AT_MOST_ONCE, writer.getConsistency());
        }
    }
}
