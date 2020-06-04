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

package jp.ad.sinet.stream.api;

import jp.ad.sinet.stream.ConfigFileAware;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(30)
class AsyncMessageReaderTest implements ConfigFileAware {

    private static final String SERVICE = "service-0";
    private String topic;
    private List<String> lines;

    @Test
    void readMessages() throws InterruptedException {
        final AtomicInteger count = new AtomicInteger(0);
        final CountDownLatch done = new CountDownLatch(lines.size());
        MessageReaderFactory<String> readerFactory = MessageReaderFactory.<String>builder().service(SERVICE).topic(topic).build();
        MessageWriterFactory<String> writerFactory = MessageWriterFactory.<String>builder().service(SERVICE).topic(topic).build();
        try (AsyncMessageReader<String> reader = readerFactory.getAsyncReader();
             MessageWriter<String> writer = writerFactory.getWriter()) {

            reader.addOnMessageCallback((msg) -> {
                int idx = count.getAndIncrement();
                assertEquals(topic, msg.getTopic());
                assertEquals(lines.get(idx), msg.getValue());
                done.countDown();
            });

            lines.forEach(writer::write);
            done.await();
        }
    }

    @Test
    void readMessages2() throws InterruptedException {
        final AtomicInteger count = new AtomicInteger(0);
        final CountDownLatch done = new CountDownLatch(lines.size());
        MessageReaderFactory<String> readerFactory = MessageReaderFactory.<String>builder().service(SERVICE).topic(topic).build();
        MessageWriterFactory<String> writerFactory = MessageWriterFactory.<String>builder().service(SERVICE).topic(topic).build();
        try (AsyncMessageReader<String> reader = readerFactory.getAsyncReader();
             AsyncMessageWriter<String> writer = writerFactory.getAsyncWriter()) {

            reader.addOnMessageCallback((msg) -> {
                int idx = count.getAndIncrement();
                assertEquals(topic, msg.getTopic());
                assertTrue(lines.contains(msg.getValue()));
                done.countDown();
            });
            lines.forEach(writer::write);
            done.await();
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
        topic = "topic-" + RandomStringUtils.randomAlphabetic(5);
    }
}
