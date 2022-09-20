/*
 * Copyright (C) 2022 National Institute of Informatics
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

package jp.ad.sinet.stream.utils;

import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.api.SinetStreamIOException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class Lz4CompressionTest {

    private static final String SERVICE_COMP = "service-0-comp";
    private static final String SERVICE_NOCOMP = "service-0-nocomp";
    private static Path configFile;
    private String topic;

    @ParameterizedTest
    @ValueSource(strings = { SERVICE_NOCOMP, SERVICE_COMP })
    void normal(String service) {
        MessageReaderFactory<byte[]> readerBuilder =
                MessageReaderFactory.<byte[]>builder()
                        .service(service)
                        .topic(topic)
                        .configFile(configFile)
                        .receiveTimeout(Duration.ofSeconds(3))
                        .build();
        MessageWriterFactory<byte[]> writerBuilder =
                MessageWriterFactory.<byte[]>builder()
                        .service(service)
                        .topic(topic)
                        .configFile(configFile)
                        .build();
        try (MessageReader<byte[]> reader = readerBuilder.getReader();
             MessageWriter<byte[]> writer = writerBuilder.getWriter()) {
            String text = RandomStringUtils.randomAlphabetic(100);
            writer.write(text.getBytes());
            assertArrayEquals(text.getBytes(), reader.read().getValue());
        }
    }

    @Test
    void nocomp_to_comp() {
        MessageReaderFactory<byte[]> readerBuilder =
                MessageReaderFactory.<byte[]>builder()
                        .service(SERVICE_COMP)
                        .topic(topic)
                        .configFile(configFile)
                        .receiveTimeout(Duration.ofSeconds(3))
                        .build();
        MessageWriterFactory<byte[]> writerBuilder =
                MessageWriterFactory.<byte[]>builder()
                        .service(SERVICE_NOCOMP)
                        .topic(topic)
                        .configFile(configFile)
                        .build();
        try (MessageReader<byte[]> reader = readerBuilder.getReader();
             MessageWriter<byte[]> writer = writerBuilder.getWriter()) {
            String text = RandomStringUtils.randomAlphabetic(100);
            writer.write(text.getBytes());
            assertThrows(
                    SinetStreamIOException.class,
                    () -> reader.read());
        }
    }

    @Test
    void comp_to_nocomp() {
        MessageReaderFactory<byte[]> readerBuilder =
                MessageReaderFactory.<byte[]>builder()
                        .service(SERVICE_NOCOMP)
                        .topic(topic)
                        .configFile(configFile)
                        .receiveTimeout(Duration.ofSeconds(3))
                        .build();
        MessageWriterFactory<byte[]> writerBuilder =
                MessageWriterFactory.<byte[]>builder()
                        .service(SERVICE_COMP)
                        .topic(topic)
                        .configFile(configFile)
                        .build();
        try (MessageReader<byte[]> reader = readerBuilder.getReader();
             MessageWriter<byte[]> writer = writerBuilder.getWriter()) {
            String text = RandomStringUtils.randomAlphabetic(100);
            writer.write(text.getBytes());
            assertNotEquals(text.getBytes().length, reader.read().getValue().length);
        }
    }

    @BeforeAll
    static void setupConfig() throws IOException {
        configFile = Files.createTempFile(null, ".yml");
        try (BufferedWriter writer = Files.newBufferedWriter(configFile)) {
            writer.append(SERVICE_COMP).append(':').append(System.lineSeparator());
            writer.append("  ").append("type: dummy").append(System.lineSeparator());
            writer.append("  ").append("brokers: broker").append(System.lineSeparator());
            writer.append("  ").append("data_compression: yes").append(System.lineSeparator());
            writer.append("  ").append("compression:").append(System.lineSeparator());
            writer.append("    ").append("algorithm: lz4").append(System.lineSeparator());
            writer.append(SERVICE_NOCOMP).append(':').append(System.lineSeparator());
            writer.append("  ").append("type: dummy").append(System.lineSeparator());
            writer.append("  ").append("brokers: broker").append(System.lineSeparator());
            writer.append("  ").append("data_compression: no").append(System.lineSeparator());
            writer.append("  ").append("compression:").append(System.lineSeparator());
            writer.append("    ").append("algorithm: lz4").append(System.lineSeparator());
        }
    }

    @AfterAll
    static void cleanupConfig() throws IOException {
        Files.delete(configFile);
    }

    @BeforeEach
    void setupTopic() {
        topic = "topic-" + RandomStringUtils.randomAlphabetic(10);
    }
}
