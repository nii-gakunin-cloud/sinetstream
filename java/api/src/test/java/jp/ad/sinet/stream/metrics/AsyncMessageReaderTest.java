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

package jp.ad.sinet.stream.metrics;

import jp.ad.sinet.stream.ConfigFileAware;
import jp.ad.sinet.stream.api.AsyncMessageReader;
import jp.ad.sinet.stream.api.AsyncMessageWriter;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.api.Metrics;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
            Metrics mr = reader.getMetrics();
            Metrics mw = writer.getMetrics();
            //System.out.println("mr=" + mr);
            //System.out.println("mw=" + mw);
            assertEquals(mr.getStartTime(), mr.getStartTimeMillis()/1000.0);
            assertEquals(mr.getEndTime(), mr.getEndTimeMillis()/1000.0);
            assertEquals(mr.getTime(), mr.getTimeMillis()/1000.0);
            assertEquals(lines.size(), mr.getMsgCountTotal());
            assertEquals(lines.size(), mr.getMsgCountTotal());
            assertEquals(lines.size(), mw.getMsgCountTotal());
            assertEquals("this is a dummy metrics", mr.getRaw());
            assertEquals("this is a dummy metrics", mw.getRaw());
            Metrics mr2 = reader.getMetrics(); reader.resetMetrics();
            Metrics mw2 = writer.getMetrics(); writer.resetMetrics();
            Metrics mr3 = reader.getMetrics(); reader.resetMetrics();
            Metrics mw3 = writer.getMetrics(); writer.resetMetrics();
            assertEquals(mr.getMsgCountTotal(), mr2.getMsgCountTotal());
            assertEquals(mw.getMsgCountTotal(), mw2.getMsgCountTotal());
            assertEquals(0, mr3.getMsgCountTotal());
            assertEquals(0, mw3.getMsgCountTotal());
        }
    }

    @Test
    void readMessagesErr() throws InterruptedException {
        final AtomicInteger count = new AtomicInteger(0);
        final CountDownLatch done = new CountDownLatch(lines.size());
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("test_dummy_reader_read_fail", true);
        MessageReaderFactory<String> readerFactory = MessageReaderFactory.<String>builder().service(SERVICE).topic(topic).parameters(parameters).build();
        MessageWriterFactory<String> writerFactory = MessageWriterFactory.<String>builder().service(SERVICE).topic(topic).build();
        try (AsyncMessageReader<String> reader = readerFactory.getAsyncReader();
             MessageWriter<String> writer = writerFactory.getWriter()) {

            reader.addOnMessageCallback((msg) -> {
                int idx = count.getAndIncrement();
                assertEquals(topic, msg.getTopic());
                assertEquals(lines.get(idx), msg.getValue());
                done.countDown();
            }, (ex) -> {
                int idx = count.getAndIncrement();
                done.countDown();
            });

            lines.forEach(writer::write);
            //done.await();
            boolean zeroed = done.await(1, TimeUnit.SECONDS);
            assertTrue(!zeroed);
            Metrics mr = reader.getMetrics();
            Metrics mw = writer.getMetrics();
            System.out.println("mr=" + mr);
            System.out.println("mw=" + mw);
            assertEquals(0, mr.getMsgCountTotal());
            assertEquals(lines.size(), mw.getMsgCountTotal());
            assertEquals("this is a dummy metrics", mr.getRaw());
            assertEquals("this is a dummy metrics", mw.getRaw());
            assertEquals(1, mr.getErrorCountTotal());
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
