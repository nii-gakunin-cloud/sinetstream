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
import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Timeout(30)
@Log
@EnabledIfEnvironmentVariable(named="RUN_INTERGRATION_TEST", matches = "(?i)true")
class SimpleTest {

    @TempDir
    Path workdir;

    private List<String> lines;

    @Nested
    class Plaintext extends SimpleReadWrite {
    }

    @Nested
    class Ssl extends SimpleReadWrite {
        @Override
        public String getBroker() {
            return System.getenv().getOrDefault("KAFKA_SSL_BROKER", "broker:9093");
        }

        @SneakyThrows
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put("ssl_cafile", makeTempCert("cacert.pem").toAbsolutePath().toString());
            params.put("ssl_certfile", makeTempCert("client0.crt").toAbsolutePath().toString());
            params.put("ssl_keyfile", makeTempCert("client0.key").toAbsolutePath().toString());
            return params;
        }

        private Path makeTempCert(String name) throws IOException {
            URL baseURL = new URL(System.getenv().getOrDefault("KAFKA_CERT_URL", "http://broker:18080"));
            return SimpleTest.this.makeTempCert(name, baseURL);
        }
    }

    @Nested
    class Sasl extends SimpleReadWrite {
        @Override
        public String getBroker() {
            return System.getenv().getOrDefault("KAFKA_SASL_BROKER", "broker:9096");
        }

        @SneakyThrows
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put("security_protocol", "SASL_PLAINTEXT");
            params.put("sasl_mechanism", "PLAIN");
            params.put("sasl_plain_username", "user01");
            params.put("sasl_plain_password", "user01");
            return params;
        }
    }

    @Nested
    class SaslSsl extends SimpleReadWrite {
        @Override
        public String getBroker() {
            return System.getenv().getOrDefault("KAFKA_SASL_SSL_BROKER", "broker2:9097");
        }

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

        private Path makeTempCert(String name) throws IOException {
            URL baseURL = new URL(
                    System.getenv().getOrDefault("KAFKA_SASL_CERT_URL", "http://broker2:28080"));
            return SimpleTest.this.makeTempCert(name, baseURL);
        }
    }

    class SimpleReadWrite implements ConfigFileAware {
        @Test
        public void readWrite() {
            MessageWriterFactory<String> writerFactory = MessageWriterFactory.<String>builder()
                    .config(getConfigFile(workdir))
                    .service(getServiceName())
                    .consistency(Consistency.AT_LEAST_ONCE)
                    .build();
            MessageReaderFactory<String> readerFactory = MessageReaderFactory.<String>builder()
                    .config(getConfigFile(workdir))
                    .service(getServiceName())
                    .consistency(Consistency.AT_LEAST_ONCE)
                    .receiveTimeout(Duration.ofSeconds(3))
                    .build();
            try (MessageWriter<String> writer = writerFactory.getWriter();
                 MessageReader<String> reader = readerFactory.getReader()) {
                reader.read();
                for (String line : lines) {
                    writer.write(line);
                }
                reader.stream().forEach(Assertions::assertNotNull);
            }
        }
    }

    @BeforeEach
    void setupMessages() {
        lines = IntStream.range(0, 10)
                .mapToObj(x -> RandomStringUtils.randomAlphabetic(10))
                .collect(Collectors.toList());
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
