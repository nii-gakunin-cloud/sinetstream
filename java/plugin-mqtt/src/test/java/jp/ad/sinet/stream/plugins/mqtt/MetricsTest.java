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

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.Message;
import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.api.Metrics;
import jp.ad.sinet.stream.api.ValueType;
import jp.ad.sinet.stream.api.valuetype.SimpleValueType;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MetricsTest {

    @TempDir
    Path workdir;

    @Nested
    class ReadWriteTest implements ConfigFileAware {

        @Test
        void readwrite() {
            MessageWriterFactory<String> writer_builder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .valueType(SimpleValueType.TEXT)
                            .build();
            MessageReaderFactory<String> reader_builder =
                    MessageReaderFactory.<String>builder()
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .valueType(SimpleValueType.TEXT)
                            .receiveTimeout(Duration.ofSeconds(10))
                            .build();
            try (MessageWriter<String> writer = writer_builder.getWriter();
                 MessageReader<String> reader = reader_builder.getReader()) {
                writer.write("message-1");

                Message<String> msg;
                msg = reader.read();
                assertNotNull(msg);
                assertNotNull(msg.getValue());

                Metrics writer_metrics = writer.getMetrics();
                assertEquals(writer_metrics.getMsgCountTotal(), 1);
                assertNull(writer_metrics.getRaw());

                Metrics reader_metrics = reader.getMetrics();
                assertEquals(reader_metrics.getMsgCountTotal(), 1);
                assertNull(reader_metrics.getRaw());
            }
        }
    }
}
