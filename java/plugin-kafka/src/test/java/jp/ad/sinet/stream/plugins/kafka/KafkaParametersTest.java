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
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@EnabledIfEnvironmentVariable(named="RUN_INTERGRATION_TEST", matches = "(?i)true")
class KafkaParametersTest implements ConfigFileAware {

    @TempDir
    Path workdir;

    @Nested
    class GroupId {
        @Test
        void groupId() {
            String groupId = "group-001";
            MessageReaderFactory<String> readerBuilder =
                    MessageReaderFactory.<String>builder()
                            .configFile(getConfigFile(workdir)).service(getServiceName())
                            .parameter("group_id", groupId)
                            .build();
            try (MessageReader<String> reader = readerBuilder.getReader()) {
                assertEquals(groupId, reader.getConfig().get("groupId"));
            }
        }

        @Test
        void defaultGroupId() {
            MessageReaderFactory<String> readerBuilder =
                    MessageReaderFactory.<String>builder()
                            .configFile(getConfigFile(workdir)).service(getServiceName()).build();
            try (MessageReader<String> reader = readerBuilder.getReader()) {
                assertNotNull(reader.getConfig().get("groupId"));
            }
        }
    }

    @Test
    void fetchMinBytes() {
        int fetchMinBytes = 1000;
        MessageReaderFactory<String> readerBuilder =
                MessageReaderFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .parameter("fetch_min_bytes", fetchMinBytes)
                        .build();
        try (MessageReader<String> reader = readerBuilder.getReader()) {
            assertNotNull(reader.getConfig().get("groupId"));
        }
    }

    @Test
    void fetchMaxBytes() {
        int fetchMaxBytes = 10000000;
        MessageReaderFactory<String> readerBuilder =
                MessageReaderFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .parameter("fetch_max_bytes", fetchMaxBytes)
                        .build();
        try (MessageReader<String> reader = readerBuilder.getReader()) {
            assertNotNull(reader.getConfig().get("groupId"));
        }
    }

    @Test
    void fetchMaxWaitMs() {
        int fetchMaxWaitMs = 10000;
        MessageReaderFactory<String> readerBuilder =
                MessageReaderFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .parameter("fetch_max_wait_ms", fetchMaxWaitMs)
                        .build();
        try (MessageReader<String> reader = readerBuilder.getReader()) {
            assertNotNull(reader.getConfig().get("groupId"));
        }
    }

    @Test
    void heartbeatIntervalMs() {
        int heartbeatIntervalMs = 1000;
        MessageReaderFactory<String> readerBuilder =
                MessageReaderFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .parameter("heartbeat_interval_ms", heartbeatIntervalMs)
                        .build();
        try (MessageReader<String> reader = readerBuilder.getReader()) {
            assertNotNull(reader.getConfig().get("groupId"));
        }
    }
}
