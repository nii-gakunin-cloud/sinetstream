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

import jp.ad.sinet.stream.api.Message;
import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.api.Metrics;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="RUN_INTERGRATION_TEST", matches = "(?i)true")
class MetricsTest {

    @TempDir
    Path workdir;

    @Nested
    class ListBroker extends SimpleReaderWriterTest {
        @Override
        public Object getBroker() {
            return Collections.singletonList((String) super.getBroker());
        }
    }

    @Nested
    @DisabledIfEnvironmentVariable(named="KAFKA_BROKER_DEFAULT_PORT", matches = "false")
    class DefaultPortBroker extends SimpleReaderWriterTest {
        @Override
        public Object getBroker() {
            return System.getenv().getOrDefault("KAFKA_BROKER_HOSTNAME", "broker");
        }
    }

    @Nested
    @DisabledIfEnvironmentVariable(named="KAFKA_BROKER_DEFAULT_PORT", matches = "false")
    class DefaultPortListBroker extends DefaultPortBroker {
        @Override
        public Object getBroker() {
            return Collections.singletonList((String) super.getBroker());
        }
    }


    class SimpleReaderWriterTest implements ConfigFileAware {
        @Test
        void writer() {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder()
                            .configFile(getConfigFile(workdir)).service(getServiceName()).build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                writer.write("message-1");
                Metrics metrics = writer.getMetrics();
                //System.out.println("writer.metrics=" + metrics);
                assertEquals(metrics.getMsgCountTotal(), 1);
                Object raw_metrics = metrics.getRaw();
                assertTrue(raw_metrics instanceof java.util.Map);
            }
        }

        @Test
        void reader() {
            MessageReaderFactory<String> builder =
                    MessageReaderFactory.<String>builder()
                            .configFile(getConfigFile(workdir)).service(getServiceName())
                            .receiveTimeout(Duration.ofSeconds(3))
                            .build();
            MessageReader<String> reader0 = null;
            int count = 0;
            try (MessageReader<String> reader = builder.getReader()) {
                reader0 = reader;
                Message<String> msg;
                while (Objects.nonNull(msg = reader.read())) {
                    assertNotNull(msg.getValue());
                    count++;
                }
                Metrics metrics = reader.getMetrics();
                //System.out.println("reader.metrics=" + metrics);
                assertEquals(metrics.getMsgCountTotal(), count);
                Object raw_metrics = metrics.getRaw();
                assertTrue(raw_metrics instanceof java.util.Map);
            }
            Metrics metrics = reader0.getMetrics();
            assertEquals(metrics.getMsgCountTotal(), count);
            Object raw_metrics = metrics.getRaw();
            assertNull(raw_metrics);
        }
    }
}
