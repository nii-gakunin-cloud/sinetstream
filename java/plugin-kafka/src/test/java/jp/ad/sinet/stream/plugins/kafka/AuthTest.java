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
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AuthTest {

    @TempDir
    Path workdir;

    @Nested
    class Sasl implements ConfigFileAware {

        @ParameterizedTest
        @EnumSource(Consistency.class)
        void passwordAuthWrite(Consistency consistency) {
            MessageWriterFactory<String> writerBuilder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir))
                            .service(getServiceName())
                            .consistency(consistency)
                            .build();

            try (MessageWriter<String> writer = writerBuilder.getWriter()) {
                final String data = RandomStringUtils.randomAlphabetic(10);
                writer.write(data);
            }
        }

        @ParameterizedTest
        @EnumSource(Consistency.class)
        void passwordAuthRead(Consistency consistency) throws InterruptedException {
            final String data = RandomStringUtils.randomAlphabetic(10);

            MessageWriterFactory<String> writerBuilder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir))
                            .service(getServiceName())
                            .consistency(Consistency.AT_LEAST_ONCE)
                            .build();

            MessageReaderFactory<String> readerBuilder =
                    MessageReaderFactory.<String>builder()
                            .config(getConfigFile(workdir))
                            .service(getServiceName())
                            .consistency(consistency)
                            .receiveTimeout(Duration.ofSeconds(3))
                            .build();

            try (MessageReader<String> reader = readerBuilder.getReader()) {
                Thread.sleep(1000);
                try (MessageWriter<String> writer = writerBuilder.getWriter()) {
                    writer.write(data);
                }
                Message<String> msg = reader.read();
                assertEquals(data, msg.getValue());
            }
        }

        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put("security_protocol", "SASL_PLAINTEXT");
            params.put("sasl_mechanism", "PLAIN");
            params.put("sasl_plain_username", "user01");
            params.put("sasl_plain_password", "user01");
            return params;
        }

        @Override
        public Object getBroker() {
            return System.getenv().getOrDefault("KAFKA_SASL_BROKER", "broker2:9096");
        }
    }

    @Nested
    class BadPassword implements ConfigFileAware {

        @ParameterizedTest
        @EnumSource(Consistency.class)
        void passwordBadAuthWrite(Consistency consistency) {
            MessageWriterFactory<String> writerBuilder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir))
                            .service(getServiceName())
                            .consistency(consistency)
                            .build();

            assertThrows(AuthenticationException.class, () -> {
                try (MessageWriter<String> writer = writerBuilder.getWriter()) {
                    final String data = RandomStringUtils.randomAlphabetic(10);
                    writer.write(data);
                }
            });
        }

        @ParameterizedTest
        @EnumSource(Consistency.class)
        void passwordBadAuthRead(Consistency consistency) {
            MessageReaderFactory<String> readerBuilder =
                    MessageReaderFactory.<String>builder()
                            .config(getConfigFile(workdir))
                            .service(getServiceName())
                            .consistency(consistency)
                            .receiveTimeout(Duration.ofSeconds(3))
                            .build();

            assertThrows(AuthenticationException.class, () -> {
                //noinspection EmptyTryBlock
                try (MessageReader<String> reader = readerBuilder.getReader()) {

                }
            });
        }


        @ParameterizedTest
        @EnumSource(value=Consistency.class, names = {"AT_MOST_ONCE", "AT_LEAST_ONCE"})
        void passwordBadAuthAsyncWrite(Consistency consistency) throws InterruptedException {
            MessageWriterFactory<String> writerBuilder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir))
                            .service(getServiceName())
                            .consistency(consistency)
                            .build();

            final AtomicInteger count = new AtomicInteger(0);
            CountDownLatch done = new CountDownLatch(1);
            try (AsyncMessageWriter<String> writer = writerBuilder.getAsyncWriter()) {
                final String data = RandomStringUtils.randomAlphabetic(10);
                writer.write(data).fail(e -> {
                    if (e instanceof AuthenticationException) {
                        count.getAndIncrement();
                    }
                }).always((s, r, e) -> done.countDown());
                boolean ret = done.await(10, TimeUnit.SECONDS);
                assertTrue(ret);
            }
            assertEquals(1, count.get());
        }

        @ParameterizedTest
        @EnumSource(value=Consistency.class, names = {"EXACTLY_ONCE"})
        void passwordBadAuthAsyncWriteEOS(Consistency consistency) {
            MessageWriterFactory<String> writerBuilder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir))
                            .service(getServiceName())
                            .consistency(consistency)
                            .build();

            assertThrows(AuthenticationException.class, () -> {
                //noinspection EmptyTryBlock
                try (AsyncMessageWriter<String> writer = writerBuilder.getAsyncWriter()) {
                }
            });
        }

        @ParameterizedTest
        @EnumSource(Consistency.class)
        void passwordBadAuthAsyncRead(Consistency consistency) {
            MessageReaderFactory<String> readerBuilder =
                    MessageReaderFactory.<String>builder()
                            .config(getConfigFile(workdir))
                            .service(getServiceName())
                            .consistency(consistency)
                            .build();

            assertThrows(AuthenticationException.class, () -> {
                //noinspection EmptyTryBlock
                try (AsyncMessageReader<String> ignored = readerBuilder.getAsyncReader()) {
                }
            });
        }

        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put("security_protocol", "SASL_PLAINTEXT");
            params.put("sasl_mechanism", "PLAIN");
            params.put("sasl_plain_username", "user01");
            params.put("sasl_plain_password", "xxxx");
            return params;
        }

        @Override
        public Object getBroker() {
            return System.getenv().getOrDefault("KAFKA_SASL_BROKER", "broker2:9096");
        }
    }

    @Nested
    class ReadUser implements ConfigFileAware {

        @ParameterizedTest
        @EnumSource(Consistency.class)
        void readAuthorizeUserWrite(Consistency consistency) throws Throwable {
            MessageWriterFactory<String> writerBuilder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir))
                            .service(getServiceName())
                            .consistency(consistency)
                            .build();
            Executable doWrite = () -> {
                try (MessageWriter<String> writer = writerBuilder.getWriter()) {
                    final String data = RandomStringUtils.randomAlphabetic(10);
                    writer.write(data);
                }
            };
            if (consistency.equals(Consistency.AT_MOST_ONCE)) {
                doWrite.execute();
            } else {
                assertThrows(AuthorizationException.class, doWrite);
            }
        }

        @ParameterizedTest
        @EnumSource(Consistency.class)
        void readAuthorizeUserAsyncWrite(Consistency consistency) throws Throwable {
            MessageWriterFactory<String> writerBuilder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir))
                            .service(getServiceName())
                            .consistency(consistency)
                            .build();
            final AtomicInteger count = new AtomicInteger(0);
            final CountDownLatch done = new CountDownLatch(1);
            try (AsyncMessageWriter<String> writer = writerBuilder.getAsyncWriter()) {
                final String data = RandomStringUtils.randomAlphabetic(10);
                writer.write(data).fail(e -> {
                    if (e instanceof AuthorizationException) {
                        count.getAndIncrement();
                    }
                }).always((s, r, e) -> done.countDown());
                boolean ret = done.await(10, TimeUnit.SECONDS);
                assertTrue(ret);
            }
            if (consistency.equals(Consistency.AT_MOST_ONCE)) {
                assertEquals(0, count.get());
            } else {
                assertEquals(1, count.get());
            }
        }


        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put("security_protocol", "SASL_PLAINTEXT");
            params.put("sasl_mechanism", "PLAIN");
            params.put("sasl_plain_username", "user02");
            params.put("sasl_plain_password", "user02");
            return params;
        }

        @Override
        public Object getBroker() {
            return System.getenv().getOrDefault("KAFKA_SASL_BROKER", "broker2:9096");
        }

        @Override
        public Optional<String> getTopic() {
            return Optional.of("mss-test-003");
        }
    }

    @Nested
    class WriteUser implements ConfigFileAware {

        @ParameterizedTest
        @EnumSource(value=Consistency.class)
        void writeAuthorizeUserRead(Consistency consistency) {
            final String data = RandomStringUtils.randomAlphabetic(10);

            MessageWriterFactory<String> writerBuilder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir))
                            .service(getServiceName())
                            .consistency(consistency)
                            .build();

            MessageReaderFactory<String> readerBuilder =
                    MessageReaderFactory.<String>builder()
                            .config(getConfigFile(workdir))
                            .service(getServiceName())
                            .consistency(consistency)
                            .receiveTimeout(Duration.ofSeconds(3))
                            .build();

            assertThrows(AuthorizationException.class, () -> {
                try (MessageReader<String> reader = readerBuilder.getReader();
                     MessageWriter<String> writer = writerBuilder.getWriter()) {
                    writer.write(data);
                    reader.read();
                }
            });
        }

        @ParameterizedTest
        @EnumSource(Consistency.class)
        void writeAuthorizeUserAsyncRead(Consistency consistency) throws InterruptedException {
            final String data = RandomStringUtils.randomAlphabetic(10);

            MessageWriterFactory<String> writerBuilder =
                    MessageWriterFactory.<String>builder()
                            .config(getConfigFile(workdir))
                            .service(getServiceName())
                            .consistency(consistency)
                            .build();

            MessageReaderFactory<String> readerBuilder =
                    MessageReaderFactory.<String>builder()
                            .config(getConfigFile(workdir))
                            .service(getServiceName())
                            .consistency(consistency)
                            .receiveTimeout(Duration.ofSeconds(3))
                            .build();

            final AtomicInteger count = new AtomicInteger(0);
            final CountDownLatch done = new CountDownLatch(1);
            try (AsyncMessageReader<String> reader = readerBuilder.getAsyncReader();
                 MessageWriter<String> writer = writerBuilder.getWriter()) {

                reader.addOnMessageCallback(
                        msg -> done.countDown(),
                        e -> {
                            done.countDown();
                            if (e instanceof AuthorizationException) {
                                count.getAndIncrement();
                            }
                        });
                writer.write(data);
                boolean ret = done.await(10, TimeUnit.SECONDS);
                assertTrue(ret);
                assertEquals(1, count.get());
            }
        }


        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put("security_protocol", "SASL_PLAINTEXT");
            params.put("sasl_mechanism", "PLAIN");
            params.put("sasl_plain_username", "user03");
            params.put("sasl_plain_password", "user03");
            return params;
        }

        @Override
        public Object getBroker() {
            return System.getenv().getOrDefault("KAFKA_SASL_BROKER", "broker2:9096");
        }

        @Override
        public Optional<String> getTopic() {
            return Optional.of("mss-test-003");
        }
    }
}
