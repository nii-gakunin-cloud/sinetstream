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

import jp.ad.sinet.stream.api.AuthenticationException;
import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Log
@EnabledIfEnvironmentVariable(named="RUN_INTERGRATION_TEST", matches = "(?i)true")
class TlsTest {

    @TempDir
    Path workdir;

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
    class PemCertKafkaParams extends ReaderWriterTest {
        @SneakyThrows
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put("ssl_cafile", makeTempCert("cacert.pem").toAbsolutePath().toString());
            params.put("ssl_certfile", makeTempCert("client0.crt").toAbsolutePath().toString());
            params.put("ssl_keyfile", makeTempCert("client0.key").toAbsolutePath().toString());
            return params;
        }
    }

    @Nested
    class PemCertKafkaParamsWithEncKey extends ReaderWriterTest {
        @SneakyThrows
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put("ssl_cafile", makeTempCert("cacert.pem").toAbsolutePath().toString());
            params.put("ssl_certfile", makeTempCert("client0.crt").toAbsolutePath().toString());
            params.put("ssl_keyfile", makeTempCert("client0-enc.key").toAbsolutePath().toString());
            params.put("ssl_password", System.getenv().getOrDefault("CERT_PASSPHRASE", "client1-pass"));
            return params;
        }
    }

    @Nested
    class PemCertKafkaParamsAndCommonTlsParam extends ReaderWriterTest {
        @SneakyThrows
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put("ssl_cafile", makeTempCert("cacert.pem").toAbsolutePath().toString());
            params.put("ssl_certfile", makeTempCert("client0.crt").toAbsolutePath().toString());
            params.put("ssl_keyfile", makeTempCert("client0.key").toAbsolutePath().toString());
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
            public String getBroker() {
                return System.getenv().getOrDefault("KAFKA_SSL_BROKER_IP", "127.0.0.1:9093");
            }
        }

        @Nested
        class HostnameCheck extends AuthenticationErrorTest {
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
    }

    @Nested
    class SslCheckHostname {

        @Nested
        class NoHostnameCheckKafkaParams extends ReaderWriterTest {
            @SneakyThrows
            @Override
            public Map<String, Object> getParameters() {
                Map<String, Object> params = new HashMap<>();
                params.put("ssl_cafile", makeTempCert("cacert.pem").toAbsolutePath().toString());
                params.put("ssl_certfile", makeTempCert("client0.crt").toAbsolutePath().toString());
                params.put("ssl_keyfile", makeTempCert("client0.key").toAbsolutePath().toString());
                params.put("ssl_check_hostname", false);
                return params;
            }

            @Override
            public String getBroker() {
                return System.getenv().getOrDefault("KAFKA_SSL_BROKER_IP", "127.0.0.1:9093");
            }
        }

        @Nested
        class HostnameCheckKafkaParams extends AuthenticationErrorTest {
            @SneakyThrows
            @Override
            public Map<String, Object> getParameters() {
                Map<String, Object> params = new HashMap<>();
                params.put("ssl_cafile", makeTempCert("cacert.pem").toAbsolutePath().toString());
                params.put("ssl_certfile", makeTempCert("client0.crt").toAbsolutePath().toString());
                params.put("ssl_keyfile", makeTempCert("client0.key").toAbsolutePath().toString());
                return params;
            }

            @Override
            public String getBroker() {
                return System.getenv().getOrDefault("KAFKA_SSL_BROKER_IP", "127.0.0.1:9093");
            }
        }

        @Nested
        class SaslNoHostnameCheckKafkaParams extends ReaderWriterTest {
            @SneakyThrows
            @Override
            public Map<String, Object> getParameters() {
                Map<String, Object> params = new HashMap<>();
                params.put("ssl_cafile", makeTempCert("cacert.pem").toAbsolutePath().toString());
                params.put("security_protocol", "SASL_SSL");
                params.put("sasl_mechanism", "PLAIN");
                params.put("sasl_plain_username", "user01");
                params.put("sasl_plain_password", "user01");
                params.put("ssl_check_hostname", false);
                return params;
            }

            @Override
            public String getBroker() {
                return System.getenv().getOrDefault("KAFKA_SASL_SSL_BROKER_IP", "127.0.0.1:9097");
            }

            private Path makeTempCert(String name) throws IOException {
                URL baseURL = new URL(
                        System.getenv().getOrDefault("KAFKA_SASL_CERT_URL", "http://broker2:28080"));
                return TlsTest.this.makeTempCert(name, baseURL);
            }
        }

        @Nested
        class SaslHostnameCheckKafkaParams extends AuthenticationErrorTest {
            @SneakyThrows
            @Override
            public Map<String, Object> getParameters() {
                Map<String, Object> params = new HashMap<>();
                params.put("ssl_cafile", makeTempCert("cacert.pem").toAbsolutePath().toString());
                params.put("security_protocol", "SASL_SSL");
                params.put("sasl_mechanism", "PLAIN");
                params.put("sasl_plain_username", "user01");
                params.put("sasl_plain_password", "user01");
                return params;
            }

            @Override
            public String getBroker() {
                return System.getenv().getOrDefault("KAFKA_SASL_SSL_BROKER_IP", "127.0.0.1:9097");
            }

            private Path makeTempCert(String name) throws IOException {
                URL baseURL = new URL(
                        System.getenv().getOrDefault("KAFKA_SASL_CERT_URL", "http://broker2:28080"));
                return TlsTest.this.makeTempCert(name, baseURL);
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
        public String getBroker() {
            return System.getenv().getOrDefault("KAFKA_BROKER", "broker:9092");
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
        public String getBroker() {
            return System.getenv().getOrDefault("KAFKA_BROKER", "broker:9092");
        }
    }


    private String generateGroupId() {
        return "group-java-" + RandomStringUtils.randomAlphanumeric(6);
    }

    class ReaderWriterTest implements ConfigFileAware {
        @Test
        void read() {
            MessageReaderFactory<String> readerBuilder =
                    MessageReaderFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .receiveTimeout(Duration.ofSeconds(3))
                            .consistency(Consistency.AT_LEAST_ONCE)
                            .build();

            try (MessageReader<String> reader = readerBuilder.getReader()) {
                //noinspection StatementWithEmptyBody
                while (Objects.nonNull(reader.read())) {

                }
            }
        }

        @Test
        void write() {
            MessageWriterFactory<String> writerBuilder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .consistency(Consistency.AT_LEAST_ONCE)
                            .build();

            try (MessageWriter<String> writer = writerBuilder.getWriter()) {
                final String data = RandomStringUtils.randomAlphabetic(10);
                writer.write(data);
            }
        }

        @Override
        public String getBroker() {
            return System.getenv().getOrDefault("KAFKA_SSL_BROKER", "broker:9093");
        }
    }

    class AuthenticationErrorTest implements ConfigFileAware {

        @Test
        void read() {
            MessageReaderFactory<String> readerBuilder =
                    MessageReaderFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .receiveTimeout(Duration.ofSeconds(3))
                            .consistency(Consistency.AT_LEAST_ONCE)
                            .build();

            assertThrows(AuthenticationException.class, () -> {
                //noinspection EmptyTryBlock
                try (MessageReader<String> reader = readerBuilder.getReader()) {
                }
            });
        }

        @Test
        void write() {
            MessageWriterFactory<String> writerBuilder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .consistency(Consistency.AT_LEAST_ONCE)
                            .build();
            assertThrows(AuthenticationException.class, () -> {
                try (MessageWriter<String> writer = writerBuilder.getWriter()) {
                    writer.write("xxx");
                }
            });
        }

        @Override
        public String getBroker() {
            return System.getenv().getOrDefault("KAFKA_SSL_BROKER_IP", "127.0.0.1:9093");
        }

    }

    private Path makeTempCert(String name) throws IOException {
        URL baseURL = new URL(System.getenv().getOrDefault("KAFKA_CERT_URL", "http://broker:18080"));
        return makeTempCert(name, baseURL);
    }

    private Path makeTempCert(String name, URL baseURL) throws IOException {
        Path path = Files.createTempFile(workdir, null, null);
        URL url = new URL(baseURL, name);
        try (InputStream in = url.openStream()) {
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        }
        path.toFile().deleteOnExit();
        return path;
    }
}
