/*
 * Copyright (C) 2019 National Institute of Informatics
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

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.Message;
import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

class DisconnectTest implements ConfigFileAware {

    private static final String SERVICE = "service-1";
    private static final String TOPIC = "test-topic-java-001";

    @Disabled
    @Test
    void testGetReader() {
        Logger logger = Logger.getLogger("");
        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(Level.FINEST));

        Logger.getLogger("jp.ad.sinet").setLevel(Level.FINER);

        MessageWriterFactory<String> factory =
                MessageWriterFactory.<String>builder()
                        .service(SERVICE).topic(TOPIC)
                        .clientId("writer-000")
                        .consistency(Consistency.EXACTLY_ONCE)
                        .build();

        MessageReaderFactory<String> readerFactory =
                MessageReaderFactory.<String>builder()
                        .service(SERVICE).topic(TOPIC)
                        .receiveTimeout(Duration.ofSeconds(5))
                        .clientId("reader-000")
                        .consistency(Consistency.EXACTLY_ONCE)
                        .build();
        try (MessageWriter<String> writer = factory.getWriter();
             MessageReader<String> reader = readerFactory.getReader()) {

            IntStream.range(0, 20).forEach(i -> {
                Message<String> msg;
                boolean done = false;
                while (!done) {
                    try {
                        writer.write("message: " + i);
                        done = true;
                    } catch (Throwable e) {
                        e.printStackTrace();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
                try {
                    msg = reader.read();
                    System.out.println(msg);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            });
        }
    }

    @Disabled
    @Test
    void readerStream() {
        Logger logger = Logger.getLogger("");
        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(Level.FINEST));

        Logger.getLogger("jp.ad.sinet").setLevel(Level.FINER);

        MessageWriterFactory<String> factory =
                MessageWriterFactory.<String>builder()
                        .service(SERVICE).topic(TOPIC)
                        .clientId("writer-000")
                        .consistency(Consistency.EXACTLY_ONCE)
                        .build();

        MessageReaderFactory<String> readerFactory =
                MessageReaderFactory.<String>builder()
                        .service(SERVICE).topic(TOPIC)
                        .receiveTimeout(Duration.ofSeconds(1))
                        .clientId("reader-000")
                        .consistency(Consistency.EXACTLY_ONCE)
                        .build();
        try (MessageWriter<String> writer = factory.getWriter();
             MessageReader<String> reader = readerFactory.getReader()) {

            IntStream.range(0, 20).forEach(i -> {
                boolean done = false;
                while (!done) {
                    try {
                        writer.write("message: " + i);
                        done = true;
                    } catch (Throwable e) {
                        e.printStackTrace();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
            });
            reader.stream().forEach(System.out::println);
        }
    }
}
