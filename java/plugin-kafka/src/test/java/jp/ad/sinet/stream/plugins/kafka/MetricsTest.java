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
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                            .config(getConfigFile(workdir)).service(getServiceName()).build();
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
                            .config(getConfigFile(workdir)).service(getServiceName())
                            .receiveTimeout(Duration.ofSeconds(3))
                            .build();
            try (MessageReader<String> reader = builder.getReader()) {
                Message<String> msg;
                int count = 0;
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
        }
    }
}
