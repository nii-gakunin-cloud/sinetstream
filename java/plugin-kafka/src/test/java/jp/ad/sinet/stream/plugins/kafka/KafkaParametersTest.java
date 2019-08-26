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

import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class KafkaParametersTest implements ConfigFileAware {

    private static final String SERVICE = "service-1";
    private static final String TOPIC = "test-topic-java-001";

    @Nested
    class GroupId {
        @Test
        void groupId() {
            String groupId = "group-001";
            MessageReaderFactory<String> readerBuilder =
                    MessageReaderFactory.<String>builder().service(SERVICE).topic(TOPIC)
                            .parameter("group_id", groupId).build();
            try (MessageReader<String> reader = readerBuilder.getReader()) {
                KafkaMessageReader<?, String> kafkaReader = (KafkaMessageReader<?, String>) reader;
                assertEquals(groupId, kafkaReader.getGroupId());
            }
        }

        @Test
        void defaultGroupId() {
            MessageReaderFactory<String> readerBuilder =
                    MessageReaderFactory.<String>builder().service(SERVICE).topic(TOPIC).build();
            try (MessageReader<String> reader = readerBuilder.getReader()) {
                KafkaMessageReader<?, String> kafkaReader = (KafkaMessageReader<?, String>) reader;
                assertNotNull(kafkaReader.getGroupId());
            }
        }
    }

    @Test
    void fetchMinBytes() {
        int fetchMinBytes = 1000;
        MessageReaderFactory<String> readerBuilder =
                MessageReaderFactory.<String>builder().service(SERVICE).topic(TOPIC)
                        .parameter("fetch_min_bytes", fetchMinBytes)
                        .build();
        try (MessageReader<String> reader = readerBuilder.getReader()) {
            KafkaMessageReader<?, String> kafkaReader = (KafkaMessageReader<?, String>) reader;
            assertNotNull(kafkaReader.getGroupId());
        }
    }

    @Test
    void fetchMaxBytes() {
        int fetchMaxBytes = 10000000;
        MessageReaderFactory<String> readerBuilder =
                MessageReaderFactory.<String>builder().service(SERVICE).topic(TOPIC)
                        .parameter("fetch_max_bytes", fetchMaxBytes)
                        .build();
        try (MessageReader<String> reader = readerBuilder.getReader()) {
            KafkaMessageReader<?, String> kafkaReader = (KafkaMessageReader<?, String>) reader;
            assertNotNull(kafkaReader.getGroupId());
        }
    }

    @Test
    void fetchMaxWaitMs() {
        int fetchMaxWaitMs = 10000;
        MessageReaderFactory<String> readerBuilder =
                MessageReaderFactory.<String>builder().service(SERVICE).topic(TOPIC)
                        .parameter("fetch_max_wait_ms", fetchMaxWaitMs)
                        .build();
        try (MessageReader<String> reader = readerBuilder.getReader()) {
            KafkaMessageReader<?, String> kafkaReader = (KafkaMessageReader<?, String>) reader;
            assertNotNull(kafkaReader.getGroupId());
        }
    }

    @Test
    void heartbeatIntervalMs() {
        int heartbeatIntervalMs = 1000;
        MessageReaderFactory<String> readerBuilder =
                MessageReaderFactory.<String>builder().service(SERVICE).topic(TOPIC)
                        .parameter("heartbeat_interval_ms", heartbeatIntervalMs)
                        .build();
        try (MessageReader<String> reader = readerBuilder.getReader()) {
            KafkaMessageReader<?, String> kafkaReader = (KafkaMessageReader<?, String>) reader;
            assertNotNull(kafkaReader.getGroupId());
        }
    }
}
