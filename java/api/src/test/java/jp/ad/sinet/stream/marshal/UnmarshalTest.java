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

package jp.ad.sinet.stream.marshal;

import jp.ad.sinet.stream.ConfigFileAware;
import jp.ad.sinet.stream.api.Message;
import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.plugins.dummy.DummyMessageProvider;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import jp.ad.sinet.stream.utils.Timestamped;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UnmarshalTest implements ConfigFileAware {

    private static final String SERVICE = "service-0";
    private static final String TOPIC = "topic-002";
    private final List<String> lines = new ArrayList<>();

    @Test
    void readerStream_ok() {
        MessageReaderFactory<String> readerBuilder =
                MessageReaderFactory.<String>builder().service(SERVICE)
                        .topic(TOPIC)
                        .receiveTimeout(Duration.ofMillis(100))
                        .build();

        try(MessageReader<String> reader = readerBuilder.getReader()) {
            Message<String> msg;
            while ((msg = reader.read()) != null) {
                long ts = msg.getTimestampMicroseconds();
                assertTrue(ts != 0);
                //System.err.println(ts + " " + msg.getValue());
            }
        }
    }

    @Test
    void readerStream_broken() {
        boolean debug = false;

        // get the original bytes
        BlockingQueue<Timestamped<byte[]>> queue = DummyMessageProvider.getQueue(TOPIC);
        Timestamped<byte[]> original = queue.peek();
        byte[] original_bytes = original.getValue();
        if (debug)
            System.err.println("ORIGINAL: " + Arrays.toString(original_bytes));

        // break and try
        int[] pos_list = {
            0, 1,                       // marker
            2, 3, 4, 5, 6, 7, 8, 9,     // fingerprint
            18,                         // length of record.msg
        };
        for (int pos : pos_list) {
            if (debug)
                System.err.println("POS: " + pos);
            queue.clear();
            ByteBuffer bb = ByteBuffer.allocate(original_bytes.length);
            int i = 0;
            for (byte b : original_bytes) {
                if (i == pos)
                    b = (byte)~b;
                bb.put(b);
                i++;
            }
            try {
                queue.put(new Timestamped<byte[]>(bb.array()));
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            if (debug)
                for (Timestamped<byte[]> e : queue) {
                    System.err.println("MODIFIED: " + Arrays.toString(e.getValue()));
                }

            MessageReaderFactory<String> readerBuilder =
                    MessageReaderFactory.<String>builder().service(SERVICE)
                            .topic(TOPIC)
                            .receiveTimeout(Duration.ofMillis(100))
                            .build();
            boolean caught = false;
            try {
                try(MessageReader<String> reader = readerBuilder.getReader()) {
                    Message<String> msg;
                    while ((msg = reader.read()) != null) {
                        throw new RuntimeException("never reach");
                    }
                    throw new RuntimeException("never reach");
                }
            }
            catch (jp.ad.sinet.stream.api.InvalidMessageException ex) {
                caught = true;
            }
            assertTrue(caught);
        }

    }

    @Test
    void readerStream_short() {
        boolean debug = false;

        // get the original bytes
        BlockingQueue<Timestamped<byte[]>> queue = DummyMessageProvider.getQueue(TOPIC);
        Timestamped<byte[]> original = queue.peek();
        byte[] original_bytes = original.getValue();
        if (debug)
            System.err.println("ORIGINAL: " + Arrays.toString(original_bytes));

        // shorten
        for (int len = 0; len < (2+8+8+3); len++) {
            if (debug)
                System.err.println("LEN: " + len);
            queue.clear();
            try {
                queue.put(new Timestamped<byte[]>(Arrays.copyOf(original_bytes, len)));
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            if (debug)
                for (Timestamped<byte[]> e : queue) {
                    System.err.println("MODIFIED: " + Arrays.toString(e.getValue()));
                }

            MessageReaderFactory<String> readerBuilder =
                    MessageReaderFactory.<String>builder().service(SERVICE)
                            .topic(TOPIC)
                            .receiveTimeout(Duration.ofMillis(100))
                            .build();
            boolean caught = false;
            try {
                try(MessageReader<String> reader = readerBuilder.getReader()) {
                    Message<String> msg;
                    while ((msg = reader.read()) != null) {
                        throw new RuntimeException("never reach");
                    }
                    throw new RuntimeException("never reach");
                }
            }
            catch (jp.ad.sinet.stream.api.InvalidMessageException ex) {
                caught = true;
            }
            assertTrue(caught);
        }
    }

    @Test
    void readerStream_long() {
        boolean debug = false;

        // get the original bytes
        BlockingQueue<Timestamped<byte[]>> queue = DummyMessageProvider.getQueue(TOPIC);
        Timestamped<byte[]> original = queue.peek();
        byte[] original_bytes = original.getValue();
        if (debug)
            System.err.println("ORIGINAL: " + Arrays.toString(original_bytes));

        // too long
        queue.clear();
        int len = original_bytes.length + 1;
        try {
            queue.put(new Timestamped<byte[]>(Arrays.copyOf(original_bytes, len)));
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        if (debug)
            for (Timestamped<byte[]> e : queue) {
                System.err.println("MODIFIED: " + Arrays.toString(e.getValue()));
            }
        MessageReaderFactory<String> readerBuilder =
                MessageReaderFactory.<String>builder().service(SERVICE)
                        .topic(TOPIC)
                        .receiveTimeout(Duration.ofMillis(100))
                        .build();
        try(MessageReader<String> reader = readerBuilder.getReader()) {
            Message<String> msg;
            while ((msg = reader.read()) != null) {
                long ts = msg.getTimestampMicroseconds();
                assertTrue(ts != 0);
            }
        }
        // trailing garbage should be ignored.
    }

    @BeforeEach
    void writeMessages() {
        lines.clear();
        MessageWriterFactory<String> writerBuilder =
                MessageWriterFactory.<String>builder().service(SERVICE).topic(TOPIC).build();
        try (MessageWriter<String> writer = writerBuilder.getWriter()) {
            IntStream.range(0, 5).forEach(i -> {
                final String data = RandomStringUtils.randomAlphabetic(10);
                lines.add(data);
                writer.write(data);
            });
        }
    }
}
