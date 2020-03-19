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

import jp.ad.sinet.stream.api.*;
import jp.ad.sinet.stream.api.valuetype.SimpleValueType;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthTest implements ConfigFileAware {

    private String topic;
    private TestReporter reporter;

    @ParameterizedTest
    @EnumSource(Consistency.class)
    void passwordAuthWrite(Consistency consistency) {
        MessageWriterFactory<String> writerBuilder =
                MessageWriterFactory.<String>builder()
                        .service("auth-password")
                        .topic(topic)
                        .consistency(consistency)
                        .valueType(SimpleValueType.TEXT)
                        .build();

        try (MessageWriter<String> writer = writerBuilder.getWriter()) {
            final String data = RandomStringUtils.randomAlphabetic(10);
            writer.write(data);
        }
    }

    @ParameterizedTest
    @EnumSource(Consistency.class)
    void passwordBadAuthWrite(Consistency consistency) {
        MessageWriterFactory<String> writerBuilder =
                MessageWriterFactory.<String>builder()
                        .service("auth-bad-password")
                        .topic(topic)
                        .consistency(consistency)
                        .valueType(SimpleValueType.TEXT)
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
    void passwordAuthRead(Consistency consistency) {
        final String data = RandomStringUtils.randomAlphabetic(10);

        MessageWriterFactory<String> writerBuilder =
                MessageWriterFactory.<String>builder()
                        .service("auth-password")
                        .topic(topic)
                        .consistency(consistency)
                        .valueType(SimpleValueType.TEXT)
                        .build();

        MessageReaderFactory<String> readerBuilder =
                MessageReaderFactory.<String>builder()
                        .service("auth-password")
                        .topic(topic)
                        .consistency(consistency)
                        .valueType(SimpleValueType.TEXT)
                        .receiveTimeout(Duration.ofSeconds(5))
                        .build();

        try (MessageReader<String> reader = readerBuilder.getReader();
             MessageWriter<String> writer = writerBuilder.getWriter()) {
            writer.write(data);
            Message<String> msg = reader.read();
            assertEquals(data, msg.getValue());
        }
    }

    @ParameterizedTest
    @EnumSource(Consistency.class)
    void passwordBadAuthRead(Consistency consistency) {
        final String data = RandomStringUtils.randomAlphabetic(10);

        MessageWriterFactory<String> writerBuilder =
                MessageWriterFactory.<String>builder()
                        .service("auth-password")
                        .topic(topic)
                        .consistency(consistency)
                        .valueType(SimpleValueType.TEXT)
                        .build();

        MessageReaderFactory<String> readerBuilder =
                MessageReaderFactory.<String>builder()
                        .service("auth-bad-password")
                        .topic(topic)
                        .consistency(consistency)
                        .valueType(SimpleValueType.TEXT)
                        .receiveTimeout(Duration.ofSeconds(10))
                        .build();

        assertThrows(AuthenticationException.class, () -> {
            try (MessageReader<String> reader = readerBuilder.getReader();
                 MessageWriter<String> writer = writerBuilder.getWriter()) {
                writer.write(data);
                reader.stream().forEach((msg) -> assertEquals(data, msg.getValue()));
            }
        });
    }

    @BeforeEach
    void setupTopic() {
        topic = "mss-test/" + RandomStringUtils.randomAlphabetic(5);
    }

    @BeforeEach
    void setupReporter(TestReporter reporter) {
        this.reporter = reporter;
    }
}
