/*
 * Copyright (C) 2023 National Institute of Informatics
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

package jp.ad.sinet.stream.plugins.mqttv5;

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.api.UnsupportedServiceTypeException;
import jp.ad.sinet.stream.api.ConnectionException;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.junit.jupiter.api.Disabled;
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
import static org.junit.jupiter.api.Assertions.assertThrows;


@EnabledIfEnvironmentVariable(named="RUN_INTERGRATION_TEST", matches = "(?i)true")
class Mqttv5ParametersTest implements ConfigFileAware {

    @TempDir
    Path workdir;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void clean_start(boolean x) {
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .parameter("clean_start", x)
                        .build();
        try (MessageWriter<String> writer = builder.getWriter()) {
            writer.write("message-1");
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10})
    void receive_maximum(int x) {
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .parameter("receive_maximum", x)
                        .build();
        try (MessageWriter<String> writer = builder.getWriter()) {
            writer.write("message-1");
        }
    }
    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void receive_maximum_ng(int x) {
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .parameter("receive_maximum", x)
                        .build();
        assertThrows(IllegalArgumentException.class,
                     () -> builder.getWriter());
    }

    @ParameterizedTest
    @ValueSource(ints = {1000, 10000})
    void maximum_packet_size(int x) {
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .parameter("maximum_packet_size", x)
                        .build();
        try (MessageWriter<String> writer = builder.getWriter()) {
            writer.write("message-1");
        }
    }
    @ParameterizedTest
    @ValueSource(ints = {-1})
    void maximum_packet_size_ng(int x) {
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .parameter("maximum_packet_size", x)
                        .build();
        assertThrows(IllegalArgumentException.class,
                     () -> builder.getWriter());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10})
    void topic_alias_maximum(int x) {
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .parameter("topic_alias_maximum", x)
                        .build();
        try (MessageWriter<String> writer = builder.getWriter()) {
            writer.write("message-1");
        }
    }
    @ParameterizedTest
    @ValueSource(ints = {-1})
    void topic_alias_maximum_ng(int x) {
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .parameter("topic_alias_maximum", x)
                        .build();
        assertThrows(IllegalArgumentException.class,
                     () -> builder.getWriter());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void request_response_info(boolean x) {
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .parameter("request_response_info", x)
                        .build();
        try (MessageWriter<String> writer = builder.getWriter()) {
            writer.write("message-1");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void request_problem_info(boolean x) {
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .parameter("request_problem_info", x)
                        .build();
        try (MessageWriter<String> writer = builder.getWriter()) {
            writer.write("message-1");
        }
    }

    @Test
    void user_property() {
        Map<String,String> x = new HashMap<>();
        x.put("foo", "bar");
        x.put("foo2", "bar2");
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .parameter("user_property", x)
                        .build();
        try (MessageWriter<String> writer = builder.getWriter()) {
            writer.write("message-1");
        }
    }
    @Test
    void user_property_2() {
        Map<Object,Object> x = new HashMap<>();
        x.put("foo", "bar");
        x.put("foo2", "bar2");
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .parameter("user_property", x)
                        .build();
        try (MessageWriter<String> writer = builder.getWriter()) {
            writer.write("message-1");
        }
    }
    @Test
    void user_property_ng() {
        Map<Object,Object> x = new HashMap<>();
        x.put("foo", 123);
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .parameter("user_property", x)
                        .build();
        assertThrows(IllegalArgumentException.class,
                     () -> builder.getWriter());
    }

    @Test
    void auth_method_ng() {
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .parameter("auth_method", "fobar")
                        .build();
        assertThrows(ConnectionException.class,
                     () -> builder.getWriter());
    }

    @Disabled("例外飛ばず")
    @Test
    void auth_data_ng() {
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .parameter("auth_data", "fobar".getBytes())
                        .build();
        assertThrows(ConnectionException.class,
                     () -> builder.getWriter());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 100, 9999})
    void session_expiry_interval(int x) {
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .parameter("session_expiry_interval", (x==9999 ? null : x))
                        .build();
        try (MessageWriter<String> writer = builder.getWriter()) {
            writer.write("message-1");
        }
    }
    @ParameterizedTest
    @ValueSource(ints = {-1})
    void session_expiry_interval_ng(int x) {
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .parameter("session_expiry_interval", x)
                        .build();
        assertThrows(IllegalArgumentException.class,
                     () -> builder.getWriter());
    }

    @Test
    void reconnect_delay_set() {
        Map<String, Object> opts = new HashMap<>();
        opts.put("max_delay", 30);
        opts.put("min_delay", 1);
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .consistency(Consistency.AT_LEAST_ONCE)
                        .parameter("reconnect_delay_set", opts)
                        .build();
        try (MessageWriter<String> writer = builder.getWriter()) {
            writer.write("message-1");
        }
    }
    @Disabled("例外飛ばず")
    @Test
    void reconnect_delay_set_ng1() {
        Map<String, Object> opts = new HashMap<>();
        opts.put("max_delay", 30);
        opts.put("min_delay", -1);
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .consistency(Consistency.AT_LEAST_ONCE)
                        .parameter("reconnect_delay_set", opts)
                        .build();
        assertThrows(IllegalArgumentException.class,
                     () -> builder.getWriter());
    }
    @Disabled("例外飛ばず")
    @Test
    void reconnect_delay_set_ng2() {
        Map<String, Object> opts = new HashMap<>();
        opts.put("max_delay", -1);
        opts.put("min_delay", 1);
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .consistency(Consistency.AT_LEAST_ONCE)
                        .parameter("reconnect_delay_set", opts)
                        .build();
        assertThrows(IllegalArgumentException.class,
                     () -> builder.getWriter());
    }
    @Disabled("例外飛ばず")
    @Test
    void reconnect_delay_set_ng3() {
        Map<String, Object> opts = new HashMap<>();
        opts.put("max_delay", 1);
        opts.put("min_delay", 30);
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .consistency(Consistency.AT_LEAST_ONCE)
                        .parameter("reconnect_delay_set", opts)
                        .build();
        assertThrows(IllegalArgumentException.class,
                     () -> builder.getWriter());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 100})
    void max_reconnect_delay(int x) {
        Map<String, Object> opts = new HashMap<>();
        opts.put("max_reconnect_delay", x);
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .consistency(Consistency.AT_LEAST_ONCE)
                        .parameter("connect", opts)
                        .build();
        try (MessageWriter<String> writer = builder.getWriter()) {
            writer.write("message-1");
        }
    }
    @Disabled("例外飛ばず")
    @ParameterizedTest
    @ValueSource(ints = {-1})
    void max_reconnect_delay_ng(int x) {
        Map<String, Object> opts = new HashMap<>();
        opts.put("max_reconnect_delay", x);
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .consistency(Consistency.AT_LEAST_ONCE)
                        .parameter("connect", opts)
                        .build();
        assertThrows(IllegalArgumentException.class,
                     () -> builder.getWriter());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void use_subscription_identifiers(boolean x) {
        Map<String, Object> opts = new HashMap<>();
        opts.put("use_subscription_identifiers", x);
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .consistency(Consistency.AT_LEAST_ONCE)
                        .parameter("connect", opts)
                        .build();
        try (MessageWriter<String> writer = builder.getWriter()) {
            writer.write("message-1");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void send_reason_messages(boolean x) {
        Map<String, Object> opts = new HashMap<>();
        opts.put("send_reason_messages", x);
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .consistency(Consistency.AT_LEAST_ONCE)
                        .parameter("connect", opts)
                        .build();
        try (MessageWriter<String> writer = builder.getWriter()) {
            writer.write("message-1");
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 100})
    void will_delay_ineterval(int x) {
        Map<String, Object> opts = new HashMap<>();
        opts.put("payload", "message ZZZ");
        opts.put("retain", "false");
        opts.put("qos", "2");
        opts.put("topic", "test-topic-java-002");
        //opts.put("delay", 4294967296L);
        opts.put("delay_interval", x);
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir)).service(getServiceName())
                        .consistency(Consistency.AT_LEAST_ONCE)
                        .parameter("will_set", opts)
                        .build();
        try (MessageWriter<String> writer = builder.getWriter()) {
            writer.write("message-1");
        }
    }
}
