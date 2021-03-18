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
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static jp.ad.sinet.stream.api.Consistency.AT_MOST_ONCE;
import static jp.ad.sinet.stream.api.Consistency.EXACTLY_ONCE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfEnvironmentVariable(named="RUN_INTERGRATION_TEST", matches = "(?i)true")
class MqttParametersTest implements ConfigFileAware {

    @TempDir
    Path workdir;

    @Nested
    class CleanSession {
        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void cleanSession(boolean cleanSession) {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .parameter("clean_session", cleanSession)
                            .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                writer.write("message-1");
            }
        }
    }

    @Nested
    class Qos {
        @ParameterizedTest
        @EnumSource(Consistency.class)
        void qos(Consistency consistency) {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .parameter("qos", Integer.toString(consistency.getQos()))
                            .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                assertEquals(consistency, writer.getConsistency());
                writer.write("message-1");
            }
        }

        @ParameterizedTest
        @EnumSource(Consistency.class)
        void intValueQos(Consistency consistency) {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .parameter("qos", consistency.getQos())
                            .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                assertEquals(consistency, writer.getConsistency());
                writer.write("message-1");
            }
        }

        @Test
        void badQos() {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .parameter("qos", "xxx")
                            .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                assertEquals(AT_MOST_ONCE, writer.getConsistency());
                writer.write("message-1");
            }
        }

        @Test
        void badValueQos() {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .parameter("qos", "10")
                            .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                assertEquals(AT_MOST_ONCE, writer.getConsistency());
                writer.write("message-1");
            }
        }

        @ParameterizedTest
        @EnumSource(Consistency.class)
        void qosAndConsistency(Consistency consistency) {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .parameter("qos", consistency.getQos())
                            .consistency(consistency.equals(EXACTLY_ONCE) ? AT_MOST_ONCE : EXACTLY_ONCE)
                            .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                assertEquals(consistency, writer.getConsistency());
                writer.write("message-1");
            }
        }
    }

    @Nested
    class Retain {
        @Test
        void defaultRetain() {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .consistency(Consistency.AT_LEAST_ONCE)
                            .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                assertEquals(false, writer.getConfig().get("retain"));
                writer.write("message-1");
            }
        }

        @Test
        void badRetain() {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .consistency(Consistency.AT_LEAST_ONCE)
                            .parameter("retain", "xxx")
                            .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                assertEquals(false, writer.getConfig().get("retain"));
                writer.write("message-1");
            }
        }

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void retain(boolean retain) {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .consistency(Consistency.AT_LEAST_ONCE)
                            .parameter("retain", Boolean.toString(retain))
                            .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                assertEquals(retain, writer.getConfig().get("retain"));
                writer.write("message-1");
            }
        }
    }

    @Nested
    class Protocol {
        @ParameterizedTest
        @EnumSource(MqttVersion.class)
        void protocol(MqttVersion version) {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .consistency(Consistency.AT_LEAST_ONCE)
                            .parameter("protocol", version.name())
                            .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                writer.write("message-1");
            }
        }

        @Test
        void badProtocol() {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .consistency(Consistency.AT_LEAST_ONCE)
                            .parameter("protocol", "xxx")
                            .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                writer.write("message-1");
            }
        }

    }

    @Nested
    class MaxInflightMessages {
        @Test
        void maxInflightMessages() {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .consistency(Consistency.AT_LEAST_ONCE)
                            .parameter("max_inflight_messages_set", "50")
                            .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                writer.write("message-1");
            }
        }

        @Test
        void badValueMaxInflightMessages() {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .consistency(Consistency.AT_LEAST_ONCE)
                            .parameter("max_inflight_messages_set", "xxx")
                            .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                writer.write("message-1");
            }
        }

        @Test
        void intValueMaxInflightMessages() {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .consistency(Consistency.AT_LEAST_ONCE)
                            .parameter("max_inflight_messages_set", 30)
                            .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                writer.write("message-1");
            }
        }
    }

    @Nested
    class Will {
        @Test
        void setWilldefaultParams() {
            Map<String, Object> will = new HashMap<>();
            will.put("payload", "message ZZZ");
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .consistency(Consistency.AT_LEAST_ONCE)
                            .parameter("will_set", will)
                            .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                writer.write("message-1");
            }
        }

        @Test
        void setWill() {
            Map<String, Object> will = new HashMap<>();
            will.put("payload", "message ZZZ");
            will.put("retain", "false");
            will.put("qos", "2");
            will.put("topic", "test-topic-java-002");

            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .consistency(Consistency.AT_LEAST_ONCE)
                            .parameter("will_set", will)
                            .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                writer.write("message-1");
            }
        }
    }

    @Nested
    class ReconnectDelay {
        @Test
        void maxDelay() {
            Map<String, Object> opts = new HashMap<>();
            opts.put("max_delay", 30);

            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .consistency(Consistency.AT_LEAST_ONCE)
                            .parameter("reconnect_delay_set", opts)
                            .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                writer.write("message-1");
            }
        }
    }

    @Nested
    class Connect {
        @Test
        void keepalive() {
            Map<String, Object> opts = new HashMap<>();
            opts.put("keepalive", 60);

            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .consistency(Consistency.AT_LEAST_ONCE)
                            .parameter("connect", opts)
                            .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                writer.write("message-1");
            }
        }

        @Test
        void automaticReconnect() {
            Map<String, Object> opts = new HashMap<>();
            opts.put("automatic_reconnect", true);

            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .consistency(Consistency.AT_LEAST_ONCE)
                            .parameter("connect", opts)
                            .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                writer.write("message-1");
            }
        }

        @Test
        void connectionTimeout() {
            Map<String, Object> opts = new HashMap<>();
            opts.put("connection_timeout", 60);

            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .consistency(Consistency.AT_LEAST_ONCE)
                            .parameter("connect", opts)
                            .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                writer.write("message-1");
            }
        }

        @Test
        void executorServiceTimeout() {
            Map<String, Object> opts = new HashMap<>();
            opts.put("executor_service_timeout", 60);

            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .consistency(Consistency.AT_LEAST_ONCE)
                            .parameter("connect", opts)
                            .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                writer.write("message-1");
            }
        }
    }
}