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

import jp.ad.sinet.stream.api.AsyncMessageReader;
import jp.ad.sinet.stream.api.AsyncMessageWriter;
import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import lombok.extern.java.Log;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Timeout(60)
@Log
class AsyncMessageReaderTest implements ConfigFileAware {

    private String topic;
    private List<String> lines;
    @TempDir
    Path workdir;

    @ParameterizedTest
    @EnumSource(Consistency.class)
    void readMessages(Consistency consistency) throws InterruptedException {
        final AtomicInteger count = new AtomicInteger(0);
        final CountDownLatch done = new CountDownLatch(lines.size());
        MessageReaderFactory<String> readerFactory = MessageReaderFactory.<String>builder()
                .config(getConfigFile(workdir)).service(getServiceName())
                .topic(topic).consistency(consistency).build();
        MessageWriterFactory<String> writerFactory = MessageWriterFactory.<String>builder()
                .config(getConfigFile(workdir)).service(getServiceName())
                .topic(topic).consistency(consistency).build();

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
                .config(getConfigFile(workdir)).service(getServiceName())
                .topic(topic).consistency(consistency).build();
        MessageWriterFactory<String> writerFactory = MessageWriterFactory.<String>builder()
                .config(getConfigFile(workdir)).service(getServiceName())
                .topic(topic).consistency(consistency).build();
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

    @BeforeEach
    void setupMessages() {
        lines = IntStream.range(0, 50)
                .mapToObj(x -> RandomStringUtils.randomAlphabetic(10))
                .collect(Collectors.toList());
    }

    @BeforeEach
    void setupTopic() {
        topic = generateTopic();
    }
}
