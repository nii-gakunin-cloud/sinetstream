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

package jp.ad.sinet.stream.plugins.mqttv5;

import jp.ad.sinet.stream.api.ConnectionException;
import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertThrows;

public interface ConfigFileAware {

    @BeforeEach
    default void makeConfigFile(@TempDir Path workdir) throws IOException {
        Map<String, Map<String, ?>> config = new HashMap<>();
        Map<String, Object> params = new HashMap<>();
        config.put(getServiceName(), params);

        params.put("type", getServiceType());
        params.put("protocol", getProtocol());
        getBroker().ifPresent(x -> params.put("brokers", x));
        getTopic().ifPresent(x -> params.put("topic", x));
        getValueType().ifPresent(x -> params.put("value_type", x));
        params.putAll(getParameters());

        Yaml yaml = new Yaml();
        try (BufferedWriter writer = Files.newBufferedWriter(getConfigFile(workdir))) {
            yaml.dump(config, writer);
        }
    }

    default Path getConfigFile(Path workdir) {
        return workdir.resolve(".sinetstream_config.yml");
    }

    default Optional<Object> getBroker() {
        return Optional.of(System.getenv().getOrDefault("MQTT_BROKER", "broker:1883"));
    }

    default String getServiceName() {
        return "service-mqtt";
    }

    default String getServiceType() {
        return "mqtt";
    }

    default String getProtocol() {
        return "MQTTv5";
    }

    default Optional<String> getTopic() {
        return Optional.of(generateTopic());
    }

    default String generateTopic() {
        return "topic-" + RandomStringUtils.randomAlphabetic(10);
    }

    default Optional<String> getValueType() {
        return Optional.of("text");
    }

    default Map<String, Object> getParameters() {
        return Collections.emptyMap();
    }

    @Disabled
    class ReaderWriterTest implements ConfigFileAware {

        @TempDir
        Path workdir;

        @Test
        void read() {
            MessageReaderFactory<String> readerBuilder =
                    MessageReaderFactory.<String>builder()
                            .configFile(getConfigFile(workdir)).service(getServiceName())
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
                            .configFile(getConfigFile(workdir)).service(getServiceName())
                            .consistency(Consistency.AT_LEAST_ONCE)
                            .build();

            try (MessageWriter<String> writer = writerBuilder.getWriter()) {
                final String data = RandomStringUtils.randomAlphabetic(10);
                writer.write(data);
            }
        }
    }

    @Disabled
    class ErrorTest implements ConfigFileAware {

        @TempDir
        Path workdir;
        protected Class<? extends Throwable> error;

        @Test
        void read() {
            MessageReaderFactory<String> builder =
                    MessageReaderFactory.<String>builder()
                            .configFile(getConfigFile(workdir)).service(getServiceName())
                            .receiveTimeout(Duration.ofSeconds(3))
                            .consistency(Consistency.AT_LEAST_ONCE)
                            .build();

            assertThrows(error, builder::getReader);
        }

        @Test
        void asyncRead() {
            MessageReaderFactory<String> builder =
                    MessageReaderFactory.<String>builder()
                            .configFile(getConfigFile(workdir)).service(getServiceName())
                            .receiveTimeout(Duration.ofSeconds(3))
                            .consistency(Consistency.AT_LEAST_ONCE)
                            .build();

            assertThrows(error, builder::getAsyncReader);
        }

        @Test
        void write() {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder()
                            .configFile(getConfigFile(workdir)).service(getServiceName())
                            .consistency(Consistency.AT_LEAST_ONCE)
                            .build();
            assertThrows(error, builder::getWriter);
        }

        @Test
        void asyncWrite() {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder()
                            .configFile(getConfigFile(workdir)).service(getServiceName())
                            .consistency(Consistency.AT_LEAST_ONCE)
                            .build();
            assertThrows(error, builder::getAsyncWriter);
        }

        @BeforeEach
        void setupError() {
            error = ConnectionException.class;
        }
    }
}
