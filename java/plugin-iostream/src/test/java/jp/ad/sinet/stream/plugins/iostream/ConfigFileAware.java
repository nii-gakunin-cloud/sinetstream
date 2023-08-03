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

package jp.ad.sinet.stream.plugins.iostream;

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
        //getBroker().ifPresent(x -> params.put("brokers", x));
        getTopic().ifPresent(x -> params.put("topic", x));
        getValueType().ifPresent(x -> params.put("value_type", x));
        params.putAll(getParameters());

        Yaml yaml = new Yaml();
        try (BufferedWriter writer = Files.newBufferedWriter(getConfigFile(workdir))) {
            //System.err.println("config=" + config);
            yaml.dump(config, writer);
        }
    }

    default Path getConfigFile(Path workdir) {
        return workdir.resolve(".sinetstream_config.yml");
    }

    default Optional<Object> getBroker() {
        return Optional.of(System.getenv().get("MQTT_BROKER"));
    }

    default String getServiceName() {
        return "service-iostream";
    }

    default String getServiceType() {
        return "iostream";
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
        Map<String, Object> iostreamparams = getIOStreamParameters();
        if (iostreamparams != null) {
            Map<String, Object> params = new HashMap<>();
            params.put("iostream", iostreamparams);
            return params;
        } else {
            return Collections.emptyMap();
        }
    }

    default String getUtcOffset() {
        return null;
    }

    default Map<String, Object> getIOStreamParameters() {
        Map<String, Object> params = new HashMap<>();
        //params.put("input_stream", xxx);
        //params.put("input_byte_array", xxx);
        //params.put("output_stream", xxx);
        return params;
    }
}
