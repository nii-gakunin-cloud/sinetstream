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

package jp.ad.sinet.stream.utils;

import jp.ad.sinet.stream.api.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigFileTest {

    interface ConfigTestByWriter {
        @Test
        default void getWriter() {
            String service = "service-0";
            String topic = "test-topic-java-001";
            MessageWriterFactory<String> factory =
                    MessageWriterFactory.<String>builder().service(service).topic(topic).build();
            try (MessageWriter<String> writer = factory.getWriter()) {
                assertNotNull(writer);
                assertEquals(service, writer.getService());
                assertEquals(topic, writer.getTopic());
                assertEquals("dummy", writer.getConfig().get("type"));
            }
        }

        @Test
        default void unsupportedServiceTypeByWriter() {
            MessageWriterFactory<String> factory =
                    MessageWriterFactory.<String>builder().service("service-1").topic("test-topic-java-001").build();
            assertThrows( UnsupportedServiceTypeException.class, factory::getWriter);
        }

        @Test
        default void noServiceByWriter() {
            MessageWriterFactory<String> factory =
                    MessageWriterFactory.<String>builder().service("service-X").topic("test-topic-java-001").build();
            assertThrows(NoServiceException.class, factory::getWriter);
        }

        @Test
        default void noServiceTypeByWriter() {
            MessageWriterFactory<String> factory =
                    MessageWriterFactory.<String>builder().service("service-Z").topic("test-topic-java-001").build();
            assertThrows(InvalidConfigurationException.class, factory::getWriter);
        }
    }

    interface ConfigTestByReader {
        @Test
        default void getReader() {
            String service = "service-0";
            List<String> topics = Arrays.asList("test-topic-java-001", "test-topic-java-002");
            MessageReaderFactory<String> factory = MessageReaderFactory.<String>builder().service(service).topics(topics).build();
            try (MessageReader<String> reader = factory.getReader()) {
                assertNotNull(reader);
                assertEquals(service, reader.getService());
                assertEquals(topics, reader.getTopics());
                assertEquals("dummy", reader.getConfig().get("type"));
            }
        }

        @Test
        default void unsupportedServiceTypeByReader() {
            MessageReaderFactory<String> factory =
                    MessageReaderFactory.<String>builder().service("service-1").topic("test-topic-java-001").build();
            assertThrows(UnsupportedServiceTypeException.class, factory::getReader);
        }

        @Test
        default void noServiceByReader() {
            MessageReaderFactory<String> factory =
                    MessageReaderFactory.<String>builder().service("service-X").topic("test-topic-java-001").build();
            assertThrows(NoServiceException.class, factory::getReader);
        }

        @Test
        default void noServiceTypeByReader() {
            MessageReaderFactory<String> factory =
                    MessageReaderFactory.<String>builder().service("service-Z").topic("test-topic-java-001").build();
            assertThrows(InvalidConfigurationException.class, factory::getReader);
        }
    }

    @Nested
    class CwdConfigFile {

        @Nested
        class Writer implements ConfigTestByWriter {
        }

        @Nested
        class Reader implements ConfigTestByReader {
        }

        @BeforeEach
        void makeConfigFile() throws IOException {
            try (InputStream in = ConfigFileTest.class.getResourceAsStream("/sinetstream_config.yml")) {
                Files.copy(in, Paths.get(".sinetstream_config.yml"));
            }
        }

        @AfterEach
        void cleanupConfigFile() throws IOException {
            Files.deleteIfExists(Paths.get(".sinetstream_config.yml"));
        }
    }

    @Nested
    class HomedirConfigFile {

        @Nested
        class Writer implements ConfigTestByWriter {
        }

        @Nested
        class Reader implements ConfigTestByReader {
        }

        @BeforeEach
        void makeConfigFile() throws IOException {
            try (InputStream in = ConfigFileTest.class.getResourceAsStream("/sinetstream_config.yml")) {
                Path path = Paths.get(System.getProperty("user.home"), ".config", "sinetstream", "config.yml");
                //noinspection ResultOfMethodCallIgnored
                path.getParent().toFile().mkdirs();
                Files.copy(in, path);
            }
        }

        @AfterEach
        void cleanupConfigFile() throws IOException {
            Path path = Paths.get(System.getProperty("user.home"), ".config", "sinetstream", "config.yml");
            Files.deleteIfExists(path);
        }
    }
}
