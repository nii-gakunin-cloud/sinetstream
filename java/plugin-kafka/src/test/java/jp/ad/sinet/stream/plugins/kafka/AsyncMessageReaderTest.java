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
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import lombok.extern.java.Log;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Timeout(15)
@Log
@EnabledIfEnvironmentVariable(named="RUN_INTERGRATION_TEST", matches = "(?i)true")
class AsyncMessageReaderTest implements ConfigFileAware {

    @TempDir
    Path workdir;

    private List<String> lines;
    private String topic;

    @ParameterizedTest
    @EnumSource(Consistency.class)
    void readMessages(Consistency consistency) throws InterruptedException {
        final AtomicInteger count = new AtomicInteger(0);
        final CountDownLatch done = new CountDownLatch(lines.size());
        MessageReaderFactory<String> readerFactory = MessageReaderFactory.<String>builder()
                .config(getConfigFile(workdir))
                .service(getServiceName())
                .consistency(consistency)
                .topic(topic)
                .build();
        MessageWriterFactory<String> writerFactory = MessageWriterFactory.<String>builder()
                .config(getConfigFile(workdir))
                .service(getServiceName())
                .consistency(consistency)
                .topic(topic)
                .build();
        try (AsyncMessageReader<String> reader = readerFactory.getAsyncReader();
             MessageWriter<String> writer = writerFactory.getWriter()) {

            reader.addOnMessageCallback((msg) -> {
                try {
                    assertEquals(topic, msg.getTopic());
                    int idx = count.get();
                    assertEquals(lines.get(idx), msg.getValue());
                    count.getAndIncrement();
                } finally {
                    done.countDown();
                }
            }, (ex) -> {
                log.log(Level.WARNING, "reader error", ex);
                done.countDown();
            });
            for (String line : lines) {
                writer.write(line);
            }
            done.await();
        }
        assertEquals(lines.size(), count.get());
    }

    @ParameterizedTest
    @EnumSource(Consistency.class)
    void readMessages2(Consistency consistency) throws InterruptedException {
        final AtomicInteger count = new AtomicInteger(0);
        final CountDownLatch done = new CountDownLatch(lines.size());
        MessageReaderFactory<String> readerFactory = MessageReaderFactory.<String>builder()
                .config(getConfigFile(workdir))
                .service(getServiceName())
                .consistency(consistency)
                .topic(topic)
                .build();
        MessageWriterFactory<String> writerFactory = MessageWriterFactory.<String>builder()
                .config(getConfigFile(workdir))
                .service(getServiceName())
                .consistency(consistency)
                .topic(topic)
                .build();
        try (AsyncMessageReader<String> reader = readerFactory.getAsyncReader();
             AsyncMessageWriter<String> writer = writerFactory.getAsyncWriter()) {

            reader.addOnMessageCallback((msg) -> {
                try {
                    assertEquals(topic, msg.getTopic());
                    int idx = count.get();
                    assertEquals(lines.get(idx), msg.getValue());
                    count.getAndIncrement();
                } finally {
                    done.countDown();
                }
            }, (ex) -> {
                log.log(Level.WARNING, "reader error", ex);
                done.countDown();
            });
            for (String line : lines) {
                writer.write(line);
            }
            done.await();
        }
        assertEquals(lines.size(), count.get());
    }

    @Nested
    class OnMessageTest {
        @Nested
        class Noop extends NoCallback {
        }

        @Nested
        class RemoveCallback extends NoCallback {
            @Override
            protected void setupCallback(AsyncMessageReader<String> reader, Consumer<Message<String>> onMessage,
                                         Consumer<Throwable> onFailure) {
                reader.addOnMessageCallback(onMessage, onFailure);
                reader.removeOnMessageCallback(onMessage, onFailure);
            }
        }

        @Nested
        class ClearCallbacks extends NoCallback {
            @Override
            protected void setupCallback(AsyncMessageReader<String> reader, Consumer<Message<String>> onMessage,
                                         Consumer<Throwable> onFailure) {
                reader.clearOnMessageCallback();
            }
        }
    }

    class NoCallback {
        @Test
        void removeOnMessage() throws InterruptedException {
            final AtomicInteger count = new AtomicInteger(0);
            final CountDownLatch done = new CountDownLatch(lines.size());
            MessageReaderFactory<String> readerFactory = MessageReaderFactory.<String>builder()
                    .config(getConfigFile(workdir))
                    .service(getServiceName())
                    .consistency(Consistency.AT_LEAST_ONCE)
                    .topic(topic)
                    .build();
            MessageWriterFactory<String> writerFactory = MessageWriterFactory.<String>builder()
                    .config(getConfigFile(workdir))
                    .service(getServiceName())
                    .consistency(Consistency.AT_LEAST_ONCE)
                    .topic(topic)
                    .build();

            Consumer<Message<String>> onMessage = (msg) -> {
                try {
                    assertEquals(topic, msg.getTopic());
                    int idx = count.get();
                    assertEquals(lines.get(idx), msg.getValue());
                    count.getAndIncrement();
                } finally {
                    done.countDown();
                }
            };
            Consumer<Throwable> onFailure = (ex) -> {
                log.log(Level.WARNING, "reader error", ex);
                done.countDown();
            };

            try (AsyncMessageReader<String> reader = readerFactory.getAsyncReader();
                 AsyncMessageWriter<String> writer = writerFactory.getAsyncWriter()) {
                setupCallback(reader, onMessage, onFailure);
                for (String line : lines) {
                    writer.write(line);
                }
                done.await(3, TimeUnit.SECONDS);
            }
            assertEquals(0, count.get());
        }

        protected void setupCallback(AsyncMessageReader<String> reader, Consumer<Message<String>> onMessage,
                                     Consumer<Throwable> onFailure) {
        }
    }

    @BeforeEach
    void setupMessages() {
        lines = IntStream.range(0, 100)
                .mapToObj(x -> RandomStringUtils.randomAlphabetic(10))
                .collect(Collectors.toList());
    }

    @BeforeEach
    void setupTopic() {
        topic = generateTopic();
    }
}
