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

import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class TlsTest {

    @Nested
    class TlsKeyStore extends ReaderWriterTest {
        @SneakyThrows
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            Map<String, Object> tls = new HashMap<>();
            params.put("tls", tls);
            tls.put("trustStore", makeTempCert("ca.p12").toAbsolutePath().toString());
            tls.put("trustStoreType", "PKCS12");
            tls.put("trustStorePassword", System.getenv().getOrDefault("TRUSTSTORE_PASSWORD", "ca-pass"));
            tls.put("keyStore", makeTempCert("client0.p12").toAbsolutePath().toString());
            tls.put("keyStoreType", "PKCS12");
            tls.put("keyStorePassword", System.getenv().getOrDefault("KEYSTORE_PASSWORD", "client0-pass"));
            return params;
        }
    }

    @Nested
    class CaCerts extends ReaderWriterTest {
        @SneakyThrows
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            Map<String, Object> tls = new HashMap<>();
            params.put("tls", tls);
            tls.put("ca_certs", makeTempCert("cacert.pem").toAbsolutePath().toString());
            tls.put("keyStore", makeTempCert("client0.p12").toAbsolutePath().toString());
            tls.put("keyStoreType", "PKCS12");
            tls.put("keyStorePassword", System.getenv().getOrDefault("KEYSTORE_PASSWORD", "client0-pass"));
            return params;
        }
    }

    @Nested
    class PemCert extends ReaderWriterTest {
        @SneakyThrows
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            Map<String, Object> tls = new HashMap<>();
            params.put("tls", tls);
            tls.put("ca_certs", makeTempCert("cacert.pem").toAbsolutePath().toString());
            tls.put("certfile", makeTempCert("client0.crt").toAbsolutePath().toString());
            tls.put("keyfile", makeTempCert("client0.key").toAbsolutePath().toString());
            return params;
        }
    }

    @Nested
    class PemCertWithEncKey extends ReaderWriterTest {
        @SneakyThrows
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            Map<String, Object> tls = new HashMap<>();
            params.put("tls", tls);
            tls.put("ca_certs", makeTempCert("cacert.pem").toAbsolutePath().toString());
            tls.put("certfile", makeTempCert("client0.crt").toAbsolutePath().toString());
            tls.put("keyfile", makeTempCert("client0-enc.key").toAbsolutePath().toString());
            tls.put("keyfilePassword", System.getenv().getOrDefault("CERT_PASSPHRASE", "client1-pass"));
            return params;
        }
    }

    @Nested
    class KeyStoreMqttParams extends ReaderWriterTest {
        @SneakyThrows
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            Map<String, Object> tls = new HashMap<>();
            params.put("tls_set", tls);
            tls.put("trustStore", makeTempCert("ca.p12").toAbsolutePath().toString());
            tls.put("trustStoreType", "PKCS12");
            tls.put("trustStorePassword", System.getenv().getOrDefault("TRUSTSTORE_PASSWORD", "ca-pass"));
            tls.put("keyStore", makeTempCert("client0.p12").toAbsolutePath().toString());
            tls.put("keyStoreType", "PKCS12");
            tls.put("keyStorePassword", System.getenv().getOrDefault("KEYSTORE_PASSWORD", "client0-pass"));
            return params;
        }
    }

    @Nested
    class PemCertMqttParams extends ReaderWriterTest {
        @SneakyThrows
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            Map<String, Object> tls = new HashMap<>();
            params.put("tls_set", tls);
            tls.put("ca_certs", makeTempCert("cacert.pem").toAbsolutePath().toString());
            tls.put("certfile", makeTempCert("client0.crt").toAbsolutePath().toString());
            tls.put("keyfile", makeTempCert("client0.key").toAbsolutePath().toString());
            return params;
        }
    }

    @Nested
    class PemCertMqttParamsWithEncKey extends ReaderWriterTest {
        @SneakyThrows
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            Map<String, Object> tls = new HashMap<>();
            params.put("tls_set", tls);
            tls.put("ca_certs", makeTempCert("cacert.pem").toAbsolutePath().toString());
            tls.put("certfile", makeTempCert("client0.crt").toAbsolutePath().toString());
            tls.put("keyfile", makeTempCert("client0-enc.key").toAbsolutePath().toString());
            tls.put("keyfilePassword", System.getenv().getOrDefault("CERT_PASSPHRASE", "client1-pass"));
            return params;
        }
    }

    @Nested
    class PemCertMqttParamsAndCommonTlsParam extends ReaderWriterTest {
        @SneakyThrows
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            Map<String, Object> tls = new HashMap<>();
            params.put("tls_set", tls);
            tls.put("ca_certs", makeTempCert("cacert.pem").toAbsolutePath().toString());
            tls.put("certfile", makeTempCert("client0.crt").toAbsolutePath().toString());
            tls.put("keyfile", makeTempCert("client0.key").toAbsolutePath().toString());
            params.put("tls", false);
            return params;
        }
    }

    @Nested
    class CheckHostname {
        @Nested
        class NoHostnameCheck extends ReaderWriterTest {
            @SneakyThrows
            @Override
            public Map<String, Object> getParameters() {
                Map<String, Object> params = new HashMap<>();
                Map<String, Object> tls = new HashMap<>();
                params.put("tls", tls);
                tls.put("ca_certs", makeTempCert("cacert.pem").toAbsolutePath().toString());
                tls.put("keyStore", makeTempCert("client0.p12").toAbsolutePath().toString());
                tls.put("keyStoreType", "PKCS12");
                tls.put("keyStorePassword", System.getenv().getOrDefault("KEYSTORE_PASSWORD", "client0-pass"));
                tls.put("check_hostname", false);
                return params;
            }

            @Override
            public Optional<Object> getBroker() {
                return Optional.of(
                        System.getenv().getOrDefault("MQTT_SSL_CERT_AUTH_BROKER_IP", "127.0.0.1:8885"));
            }
        }

        @Nested
        class HostnameCheck extends ConfigFileAware.ErrorTest {
            @SneakyThrows
            @Override
            public Map<String, Object> getParameters() {
                Map<String, Object> params = new HashMap<>();
                Map<String, Object> tls = new HashMap<>();
                params.put("tls", tls);
                tls.put("ca_certs", makeTempCert("cacert.pem").toAbsolutePath().toString());
                tls.put("keyStore", makeTempCert("client0.p12").toAbsolutePath().toString());
                tls.put("keyStoreType", "PKCS12");
                tls.put("keyStorePassword", System.getenv().getOrDefault("KEYSTORE_PASSWORD", "client0-pass"));
                return params;
            }

            @Override
            public Optional<Object> getBroker() {
                return Optional.of(System.getenv().getOrDefault("MQTT_SSL_CERT_AUTH_BROKER_IP", "127.0.0.1:8885"));
            }
        }
    }

    @Nested
    class TlsInsecureSet {

        @Nested
        class NoHostnameCheckMqttParams extends ReaderWriterTest {
            @SneakyThrows
            @Override
            public Map<String, Object> getParameters() {
                Map<String, Object> params = new HashMap<>();
                Map<String, Object> tls = new HashMap<>();
                params.put("tls_set", tls);
                tls.put("ca_certs", makeTempCert("cacert.pem").toAbsolutePath().toString());
                tls.put("certfile", makeTempCert("client0.crt").toAbsolutePath().toString());
                tls.put("keyfile", makeTempCert("client0.key").toAbsolutePath().toString());
                Map<String, Object> insecure = new HashMap<>();
                insecure.put("value", true);
                params.put("tls_insecure_set", insecure);
                return params;
            }

            @Override
            public Optional<Object> getBroker() {
                return Optional.of(
                        System.getenv().getOrDefault("MQTT_SSL_CERT_AUTH_BROKER_IP", "127.0.0.1:8885"));
            }
        }

        @Nested
        class TlsParameters extends ReaderWriterTest {
            @SneakyThrows
            @Override
            public Map<String, Object> getParameters() {
                Map<String, Object> params = new HashMap<>();
                Map<String, Object> tls = new HashMap<>();
                params.put("tls", tls);
                tls.put("ca_certs", makeTempCert("cacert.pem").toAbsolutePath().toString());
                tls.put("certfile", makeTempCert("client0.crt").toAbsolutePath().toString());
                tls.put("keyfile", makeTempCert("client0-enc.key").toAbsolutePath().toString());
                tls.put("keyfilePassword", System.getenv().getOrDefault("CERT_PASSPHRASE", "client1-pass"));
                Map<String, Object> insecure = new HashMap<>();
                insecure.put("value", true);
                params.put("tls_insecure_set", insecure);
                return params;
            }

            @Override
            public Optional<Object> getBroker() {
                return Optional.of(
                        System.getenv().getOrDefault("MQTT_SSL_CERT_AUTH_BROKER_IP", "127.0.0.1:8885"));
            }
        }

        @Nested
        class HostnameCheckMqttParams extends ConfigFileAware.ErrorTest {
            @SneakyThrows
            @Override
            public Map<String, Object> getParameters() {
                Map<String, Object> params = new HashMap<>();
                Map<String, Object> tls = new HashMap<>();
                params.put("tls_set", tls);
                tls.put("ca_certs", makeTempCert("cacert.pem").toAbsolutePath().toString());
                tls.put("certfile", makeTempCert("client0.crt").toAbsolutePath().toString());
                tls.put("keyfile", makeTempCert("client0.key").toAbsolutePath().toString());
                Map<String, Object> insecure = new HashMap<>();
                insecure.put("value", false);
                params.put("tls_insecure_set", insecure);
                return params;
            }

            @Override
            public Optional<Object> getBroker() {
                return Optional.of(
                        System.getenv().getOrDefault("MQTT_SSL_CERT_AUTH_BROKER_IP", "127.0.0.1:8885"));
            }
        }
    }

    @Nested
    class NoTls extends ReaderWriterTest {
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put("tls", false);
            return params;
        }

        @Override
        public Optional<Object> getBroker() {
            return Optional.of(System.getenv().getOrDefault("MQTT_BROKER", "broker:1883"));
        }
    }

    @Nested
    class EmptyMapTls extends ReaderWriterTest {
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put("tls", Collections.emptyMap());
            return params;
        }

        @Override
        public Optional<Object> getBroker() {
            return Optional.of(System.getenv().getOrDefault("MQTT_BROKER", "broker:1883"));
        }
    }

    class ReaderWriterTest extends ConfigFileAware.ReaderWriterTest {
        @Override
        public Optional<Object> getBroker() {
            return Optional.of(System.getenv().getOrDefault("MQTT_SSL_CERT_AUTH_BROKER", "broker:8885"));
        }
    }

    static Path makeTempCert(String name) throws IOException {
        URL baseURL = new URL(System.getenv().getOrDefault("CERT_URL", "http://broker:18080"));
        return makeTempCert(name, baseURL);
    }

    static Path makeTempCert(String name, URL baseURL) throws IOException {
        Path path = Files.createTempFile(null, null);
        URL url = new URL(baseURL, name);
        try (InputStream in = url.openStream()) {
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        }
        path.toFile().deleteOnExit();
        return path;
    }
}
