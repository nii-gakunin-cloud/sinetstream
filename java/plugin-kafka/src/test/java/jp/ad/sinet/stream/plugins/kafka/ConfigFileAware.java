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

package jp.ad.sinet.stream.plugins.kafka;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public interface ConfigFileAware {

    @BeforeEach
    default void makeConfigFile(@TempDir Path workdir) throws IOException {
        Map<String, Map<String, ?>> config = new HashMap<>();
        Map<String, Object> params = new HashMap<>();
        config.put(getServiceName(), params);

        params.put("type", getServiceType());
        params.put("brokers", getBroker());
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

    default Object getBroker() {
        return System.getenv().getOrDefault("KAFKA_BROKER", "broker:9092");
    }

    default String getServiceName() {
        return "service-1";
    }

    default String getServiceType() {
        return "kafka";
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
}
