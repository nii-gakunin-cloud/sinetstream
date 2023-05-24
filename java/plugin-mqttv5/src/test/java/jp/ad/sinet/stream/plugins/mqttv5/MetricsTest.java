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

package jp.ad.sinet.stream.plugins.mqttv5;

import jp.ad.sinet.stream.api.Message;
import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.api.Metrics;
import jp.ad.sinet.stream.api.valuetype.SimpleValueType;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="RUN_INTERGRATION_TEST", matches = "(?i)true")
class MetricsTest {

    @TempDir
    Path workdir;

    @Nested
    class ReadWriteTest implements ConfigFileAware {

        @Test
        void readwrite() {
            MessageWriterFactory<String> writer_builder =
                    MessageWriterFactory.<String>builder()
                            .configFile(getConfigFile(workdir)).service(getServiceName())
                            .valueType(SimpleValueType.TEXT)
                            .build();
            MessageReaderFactory<String> reader_builder =
                    MessageReaderFactory.<String>builder()
                            .configFile(getConfigFile(workdir)).service(getServiceName())
                            .valueType(SimpleValueType.TEXT)
                            .receiveTimeout(Duration.ofSeconds(10))
                            .build();
            MessageWriter<String> writer0 = null;
            MessageReader<String> reader0 = null;
            try (MessageWriter<String> writer = writer_builder.getWriter();
                 MessageReader<String> reader = reader_builder.getReader()) {
                writer0 = writer;
                reader0 = reader;

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
            Metrics writer_metrics = writer0.getMetrics();
            assertEquals(writer_metrics.getMsgCountTotal(), 1);
            assertNull(writer_metrics.getRaw());

            Metrics reader_metrics = reader0.getMetrics();
            assertEquals(reader_metrics.getMsgCountTotal(), 1);
            assertNull(reader_metrics.getRaw());
        }
    }
}
