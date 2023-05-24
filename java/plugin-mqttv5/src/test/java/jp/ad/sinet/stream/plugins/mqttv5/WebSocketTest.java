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

package jp.ad.sinet.stream.plugins.mqttv5;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@EnabledIfEnvironmentVariable(named="RUN_INTERGRATION_TEST", matches = "(?i)true")
class WebSocketTest {


    @Nested
    class WebSocket extends ConfigFileAware.ReaderWriterTest {
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put("transport", "websockets");
            return params;
        }

        @Override
        public Optional<Object> getBroker() {
            return Optional.of(System.getenv().getOrDefault("MQTT_WS_BROKER", "broker:1885"));
        }
    }

    @Nested
    class BadTransport extends ConfigFileAware.ErrorTest {
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put("transport", "tcp");
            return params;
        }

        @Override
        public Optional<Object> getBroker() {
            return Optional.of(System.getenv().getOrDefault("MQTT_WS_BROKER", "broker:1885"));
        }
    }

    @Nested
    class SecureWebSocket extends ConfigFileAware.ReaderWriterTest {
        @SneakyThrows
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put("transport", "websockets");
            Map<String, Object> tls = new HashMap<>();
            params.put("tls", tls);
            tls.put("trustStore", TlsTest.makeTempCert("ca.p12").toAbsolutePath().toString());
            tls.put("trustStoreType", "PKCS12");
            tls.put("trustStorePassword", System.getenv().getOrDefault("TRUSTSTORE_PASSWORD", "ca-pass"));
            tls.put("keyStore", TlsTest.makeTempCert("client0.p12").toAbsolutePath().toString());
            tls.put("keyStoreType", "PKCS12");
            tls.put("keyStorePassword", System.getenv().getOrDefault("KEYSTORE_PASSWORD", "client0-pass"));
            return params;
        }

        @Override
        public Optional<Object> getBroker() {
            return Optional.of(System.getenv().getOrDefault("MQTT_SSL_WS_BROKER", "broker:8886"));
        }
    }

    @Nested
    class SecureWebSocketWithNoHostnameCheck extends ConfigFileAware.ReaderWriterTest {
        @SneakyThrows
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put("transport", "websockets");
            Map<String, Object> tls = new HashMap<>();
            params.put("tls", tls);
            tls.put("trustStore", TlsTest.makeTempCert("ca.p12").toAbsolutePath().toString());
            tls.put("trustStoreType", "PKCS12");
            tls.put("trustStorePassword", System.getenv().getOrDefault("TRUSTSTORE_PASSWORD", "ca-pass"));
            tls.put("keyStore", TlsTest.makeTempCert("client0.p12").toAbsolutePath().toString());
            tls.put("keyStoreType", "PKCS12");
            tls.put("keyStorePassword", System.getenv().getOrDefault("KEYSTORE_PASSWORD", "client0-pass"));
            tls.put("check_hostname", false);
            return params;
        }

        @Override
        public Optional<Object> getBroker() {
            return Optional.of(
                    System.getenv().getOrDefault("MQTT_SSL_WS_BROKER_IP", "127.0.0.1:8886"));
        }
    }

    @Nested
    class SecureWebSocketWithHostnameCheck extends ConfigFileAware.ErrorTest {
        @SneakyThrows
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put("transport", "websockets");
            Map<String, Object> tls = new HashMap<>();
            params.put("tls", tls);
            tls.put("trustStore", TlsTest.makeTempCert("ca.p12").toAbsolutePath().toString());
            tls.put("trustStoreType", "PKCS12");
            tls.put("trustStorePassword", System.getenv().getOrDefault("TRUSTSTORE_PASSWORD", "ca-pass"));
            tls.put("keyStore", TlsTest.makeTempCert("client0.p12").toAbsolutePath().toString());
            tls.put("keyStoreType", "PKCS12");
            tls.put("keyStorePassword", System.getenv().getOrDefault("KEYSTORE_PASSWORD", "client0-pass"));
            tls.put("check_hostname", true);
            return params;
        }

        @Override
        public Optional<Object> getBroker() {
            return Optional.of(
                    System.getenv().getOrDefault("MQTT_SSL_WS_BROKER_IP", "127.0.0.1:8886"));
        }
    }

    @Nested
    class WebSocketWithKeyStoreMqttParams extends ConfigFileAware.ReaderWriterTest {
        @SneakyThrows
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put("transport", "websockets");
            Map<String, Object> tls = new HashMap<>();
            params.put("tls_set", tls);
            tls.put("trustStore", TlsTest.makeTempCert("ca.p12").toAbsolutePath().toString());
            tls.put("trustStoreType", "PKCS12");
            tls.put("trustStorePassword", System.getenv().getOrDefault("TRUSTSTORE_PASSWORD", "ca-pass"));
            tls.put("keyStore", TlsTest.makeTempCert("client0.p12").toAbsolutePath().toString());
            tls.put("keyStoreType", "PKCS12");
            tls.put("keyStorePassword", System.getenv().getOrDefault("KEYSTORE_PASSWORD", "client0-pass"));
            return params;
        }

        @Override
        public Optional<Object> getBroker() {
            return Optional.of(System.getenv().getOrDefault("MQTT_SSL_WS_BROKER", "broker:8886"));
        }
    }

    @Nested
    class WebSocketWithKeyStoreMqttParamsAndNoHostnameCheck extends ConfigFileAware.ReaderWriterTest {
        @SneakyThrows
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put("transport", "websockets");
            Map<String, Object> tls = new HashMap<>();
            params.put("tls_set", tls);
            tls.put("trustStore", TlsTest.makeTempCert("ca.p12").toAbsolutePath().toString());
            tls.put("trustStoreType", "PKCS12");
            tls.put("trustStorePassword", System.getenv().getOrDefault("TRUSTSTORE_PASSWORD", "ca-pass"));
            tls.put("keyStore", TlsTest.makeTempCert("client0.p12").toAbsolutePath().toString());
            tls.put("keyStoreType", "PKCS12");
            tls.put("keyStorePassword", System.getenv().getOrDefault("KEYSTORE_PASSWORD", "client0-pass"));
            Map<String, Object> insecure = new HashMap<>();
            insecure.put("value", true);
            params.put("tls_insecure_set", insecure);
            return params;
        }

        @Override
        public Optional<Object> getBroker() {
            return Optional.of(
                    System.getenv().getOrDefault("MQTT_SSL_WS_BROKER_IP", "127.0.0.1:8886"));
        }
    }

    @Nested
    class WebSocketWithKeyStoreMqttParamsAndHostnameCheck extends ConfigFileAware.ErrorTest {
        @SneakyThrows
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put("transport", "websockets");
            Map<String, Object> tls = new HashMap<>();
            params.put("tls_set", tls);
            tls.put("trustStore", TlsTest.makeTempCert("ca.p12").toAbsolutePath().toString());
            tls.put("trustStoreType", "PKCS12");
            tls.put("trustStorePassword", System.getenv().getOrDefault("TRUSTSTORE_PASSWORD", "ca-pass"));
            tls.put("keyStore", TlsTest.makeTempCert("client0.p12").toAbsolutePath().toString());
            tls.put("keyStoreType", "PKCS12");
            tls.put("keyStorePassword", System.getenv().getOrDefault("KEYSTORE_PASSWORD", "client0-pass"));
            Map<String, Object> insecure = new HashMap<>();
            insecure.put("value", false);
            params.put("tls_insecure_set", insecure);
            return params;
        }

        @Override
        public Optional<Object> getBroker() {
            return Optional.of(
                    System.getenv().getOrDefault("MQTT_SSL_WS_BROKER_IP", "127.0.0.1:8886"));
        }
    }

}
