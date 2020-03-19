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

import jp.ad.sinet.stream.api.*;
import jp.ad.sinet.stream.api.valuetype.SimpleValueType;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TlsTest {

    private String topic;

    @Disabled
    @ParameterizedTest
    @ValueSource(strings = {"service-connect-by-tls", "service-connect-by-tls-ca_certs",
            "service-connect-by-tls-ca_certs_cert_key", "service-connect-by-tls-no-hostname-check",
            "service-connect-by-tls_kafka", "service-connect-by-tls_kafka-no-hostname-check",
            "service-connect-by-tls_kafka-with-hostname-check", "service-connect-by-tls-encrypted-key",
            "service-ssl-256", "service-ssl-512", "service-ssl-512-no-username", "service-ssl-512-no-password" })
    void writeRead(String service) {
        String groupId = "group-java-" + RandomStringUtils.randomAlphanumeric(6);
        MessageWriterFactory<String> writerBuilder =
                MessageWriterFactory.<String>builder().service(service)
                        .topic(topic)
                        .consistency(Consistency.EXACTLY_ONCE)
                        .build();
        MessageReaderFactory<String> readerBuilder =
                MessageReaderFactory.<String>builder().service(service)
                        .topic(topic)
                        .receiveTimeout(Duration.ofSeconds(2))
                        .consistency(Consistency.EXACTLY_ONCE)
                        .parameter(ConsumerConfig.GROUP_ID_CONFIG, groupId)
                        .build();

        try (MessageWriter<String> writer = writerBuilder.getWriter();
             MessageReader<String> reader = readerBuilder.getReader()) {

            final String data = RandomStringUtils.randomAlphabetic(10);
            writer.write(data);
            Message<String> result = reader.read();
            assertEquals(data, result.getValue());
        }
    }

    @ParameterizedTest
    @DisabledIfEnvironmentVariable(named="KAFKA_BROKER_REACHABLE", matches = "false")
    @ValueSource(strings = {
            "service-connect-by-tls",
            "service-connect-by-tls-ca_certs",
            "service-connect-by-tls-ca_certs_cert_key",
            "service-connect-by-tls-no-hostname-check",
        //  "service-connect-by-tls_kafka",                     // エラー発生
            "service-connect-by-tls_kafka-no-hostname-check",
            "service-connect-by-tls_kafka-with-hostname-check"
        //  "service-connect-by-tls-encrypted-key",             // エラー発生
        //  "service-ssl-256",                                  // SecurityProtocolTest と重複
        //  "service-ssl-512",                                  // SecurityProtocolTest と重複
        //  "service-ssl-512-no-username",                      // SecurityProtocolTest と重複
        //  "service-ssl-512-no-password"                       // SecurityProtocolTest と重複
        })
    void read(String service) {
        MessageReaderFactory<String> readerBuilder =
                MessageReaderFactory.<String>builder().service(service)
                        .topic(topic)
                        .receiveTimeout(Duration.ofSeconds(3))
                        .consistency(Consistency.EXACTLY_ONCE)
                        .build();

        try (MessageReader<String> reader = readerBuilder.getReader()) {
            //noinspection StatementWithEmptyBody
            while (Objects.nonNull(reader.read())) {

            }
        }
    }

    @ParameterizedTest
    @DisabledIfEnvironmentVariable(named="KAFKA_BROKER_REACHABLE", matches = "false")
    @ValueSource(strings = {
                "service-connect-by-tls",
                "service-connect-by-tls-ca_certs",
                "service-connect-by-tls-ca_certs_cert_key",
                "service-connect-by-tls-no-hostname-check",
            //  "service-connect-by-tls_kafka",                     // エラー発生
                "service-connect-by-tls_kafka-no-hostname-check",
            //  "service-connect-by-tls-encrypted-key",             // エラー発生
            //  "service-connect-by-tls_kafka-and-tls",             // エラー発生
                "service-connect-by-tls_kafka-with-hostname-check",
            //  "service-ssl-256",                                  // SecurityProtocolTest と重複
            //  "service-ssl-512",                                  // SecurityProtocolTest と重複
            //  "service-ssl-512-no-username",                      // SecurityProtocolTest と重複
            //  "service-ssl-512-no-password"                       // SecurityProtocolTest と重複
        })
    void write(String service) {
        MessageWriterFactory<String> writerBuilder =
                MessageWriterFactory.<String>builder().service(service)
                        .topic(topic)
                        .consistency(Consistency.EXACTLY_ONCE)
                        .valueType(SimpleValueType.TEXT)
                        .build();

        try (MessageWriter<String> writer = writerBuilder.getWriter()) {
            final String data = RandomStringUtils.randomAlphabetic(10);
            writer.write(data);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"service-connect-by-no-tls", "service-connect-by-no-tls2"})
    void writeNoTls(String service) {
        MessageWriterFactory<String> writerBuilder =
                MessageWriterFactory.<String>builder().service(service)
                        .topic(topic)
                        .consistency(Consistency.EXACTLY_ONCE)
                        .build();

        try (MessageWriter<String> writer = writerBuilder.getWriter()) {
            final String data = RandomStringUtils.randomAlphabetic(10);
            writer.write(data);
        }
    }

    @Disabled   // SecurityProtocolTest と重複
    @Test
    void writeInvalidConfiguration() {
		assertThrows(InvalidConfigurationException.class, ()->{
			MessageWriterFactory<String> writerBuilder =
					MessageWriterFactory.<String>builder().service("service-ssl-exception")
							.topic(topic)
							.consistency(Consistency.EXACTLY_ONCE)
							.build();

			MessageWriter<String> writer = writerBuilder.getWriter();
			final String data = RandomStringUtils.randomAlphabetic(10);
			writer.write(data);
		});
    }

    @BeforeEach
    void setupTopic() {
        topic = "topic-ssl-" + RandomStringUtils.randomAlphabetic(5);
    }

    @BeforeEach
    void makeConfigFile() throws IOException {
        Map<String, String> vars = new HashMap<>();
        for (String name: Arrays.asList("niica", "client0")) {
            Path path = makeTempKeyStore(name + ".p12");
            vars.put(name + "KeyStore", path.toAbsolutePath().normalize().toString());
        }
        for (String name: Arrays.asList("ca.pem", "client0.crt", "client0.key", "client1.crt", "client1.key")) {
            Path path = makeTempPemFile(name);
            vars.put(name, path.toAbsolutePath().normalize().toString());
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                ConfigFileAware.class.getResourceAsStream("/sinetstream_config.yml"), StandardCharsets.UTF_8));
             BufferedWriter writer = Files.newBufferedWriter(Paths.get(".sinetstream_config.yml"))) {
            reader.lines().map(line -> {
                StringSubstitutor ss = new StringSubstitutor(vars);
                return ss.replace(line);
            }).forEach(line -> {
                try {
                    writer.write(line);
                    writer.newLine();
                } catch (IOException e) {
                    throw new SinetStreamIOException(e);
                }
            });
        }
    }

    private Path makeTempKeyStore(String name) throws IOException {
        Path path = Files.createTempFile(null, ".p12");
        try (InputStream in = ConfigFileAware.class.getResourceAsStream("/cert/" + name)) {
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        }
        path.toFile().deleteOnExit();
        return path;
    }

    private Path makeTempPemFile(String filename) throws IOException {
        Path path = Files.createTempFile(null, ".pem");
        try (InputStream in = ConfigFileAware.class.getResourceAsStream("/cert/" + filename)) {
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        }
        path.toFile().deleteOnExit();
        return path;
    }

    @AfterEach
    void cleanupConfigFile() throws IOException {
        Files.deleteIfExists(Paths.get(".sinetstream_config.yml"));
    }
}
