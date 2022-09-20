/*
 * Copyright (C) 2022 National Institute of Informatics
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

package jp.ad.sinet.stream.plugins.s3;

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.Message;
import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.api.ValueType;
import jp.ad.sinet.stream.api.valuetype.SimpleValueType;
import jp.ad.sinet.stream.api.InvalidConfigurationException;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="RUN_INTERGRATION_TEST", matches = "(?i)true")
class MessageReaderTest {

    @TempDir
    Path workdir;

    /*
    @Override
    public Map<String, Object> getS3Parameters() {
    }
    */

    @Nested
    class ReaderTest implements ConfigFileAware {

        @Test
        void testGetReader() {
            MessageReaderFactory<String> builder = MessageReaderFactory.<String>builder()
                    .configFile(getConfigFile(workdir))
                    .service(getServiceName())
                    .receiveTimeout(Duration.ofSeconds(10))
                    .build();
            try (MessageReader<String> reader = builder.getReader()) {
                assertNotNull(reader);
            }
        }

        @ParameterizedTest
        //@EnumSource(Consistency.class)
        @EnumSource(value = Consistency.class, mode = EnumSource.Mode.INCLUDE, names = "AT_MOST_ONCE")
        void consistency(Consistency consistency, TestReporter reporter) {
            MessageReaderFactory<String> builder = MessageReaderFactory.<String>builder()
                    .configFile(getConfigFile(workdir))
                    .service(getServiceName())
                    .consistency(consistency)
                    .receiveTimeout(Duration.ofSeconds(10))
                    .valueType(SimpleValueType.TEXT)
                    .build();
            try (MessageReader<String> reader = builder.getReader()) {
                Message<String> msg;
                while (Objects.nonNull(msg = reader.read())) {
                    assertNotNull(msg.getValue());
                    reporter.publishEntry(msg.getValue());
                }
            }
        }

        @ParameterizedTest
        //@EnumSource(Consistency.class)
        @EnumSource(value = Consistency.class, mode = EnumSource.Mode.INCLUDE, names = "AT_MOST_ONCE")
        void streamTest(Consistency consistency, TestReporter reporter) {
            MessageReaderFactory<String> builder = MessageReaderFactory.<String>builder()
                    .configFile(getConfigFile(workdir))
                    .service(getServiceName())
                    .consistency(consistency)
                    .receiveTimeout(Duration.ofSeconds(3))
                    .valueType(SimpleValueType.TEXT)
                    .build();
            try (MessageReader<String> reader = builder.getReader()) {
                reader.stream().forEach((msg) -> {
                    assertNotNull(msg.getValue());
                    reporter.publishEntry(msg.getValue());
                });
            }
        }

        @Test
        @SuppressWarnings("unchecked")
        void getInfo() {
            MessageReaderFactory<String> builder = MessageReaderFactory.<String>builder()
                    .configFile(getConfigFile(workdir))
                    .service(getServiceName())
                    .receiveTimeout(Duration.ofSeconds(10))
                    .build();
            try (MessageReader<String> reader = builder.getReader()) {
                {
                    Map<String, Object> info = (Map<String, Object>) reader.getInfo();
                    assert(info == null);
                    /*
                    Map<String, Object> info_s3 = (Map<String, Object>) info.get("s3");
                    Map<String, Object> info_s3_writer = (Map<String, Object>) info_s3.get("writer");
                    assert(info_s3_writer == null);
                    Map<String, Object> info_s3_reader = (Map<String, Object>) info_s3.get("reader");
                    assert(info_s3_reader == null);
                    */
                }

                {
                    Map<String, Object> info_s3 = (Map<String, Object>) reader.getInfo("s3");
                    assert(info_s3 == null);
                    /*
                    Map<String, Object> info_s3_writer = (Map<String, Object>) info_s3.get("writer");
                    assert(info_s3_writer == null);
                    Map<String, Object> info_s3_reader = (Map<String, Object>) info_s3.get("reader");
                    assert(info_s3_reader == null);
                    */
                }

                {
                    Map<String, Object> info_s3_writer = (Map<String, Object>) reader.getInfo("s3.writer");
                    assert(info_s3_writer == null);
                    Map<String, Object> info_s3_reader = (Map<String, Object>) reader.getInfo("s3.reader");
                    assert(info_s3_reader == null);
                }

                {
                    String info_s3_writer_uuid = (String) reader.getInfo("s3.writer.uuid");
                    assert(info_s3_writer_uuid == null);
                    String info_s3_reader = (String) reader.getInfo("s3.reader.uuid");
                    assert(info_s3_reader == null);
                }
            }
        }
    }

    @Nested
    class TopicTest {

        @Nested
        class Topic implements ConfigFileAware {
            private String topic;

            @Test
            void topic() {
                MessageReaderFactory<String> builder = MessageReaderFactory.<String>builder()
                        .configFile(getConfigFile(workdir))
                        .service(getServiceName())
                        .topic(topic)
                        .build();
                try (MessageReader<String> reader = builder.getReader()) {
                    assertEquals(topic, reader.getTopic());
                }
            }

            @BeforeEach
            void setupTopic() {
                topic = generateTopic();
            }
        }

        @Nested
        class Topics implements ConfigFileAware {
            private List<String> topics;

            @Test
            void topics() {
                MessageReaderFactory<String> builder = MessageReaderFactory.<String>builder()
                        .configFile(getConfigFile(workdir))
                        .service(getServiceName())
                        .topics(topics)
                        .build();
                try (MessageReader<String> reader = builder.getReader()) {
                    assertIterableEquals(topics, reader.getTopics());
                }
            }

            @BeforeEach
            void setupTopic() {
                topics = IntStream.range(0, 5).mapToObj(x -> generateTopic()).collect(Collectors.toList());
            }
        }
    }

    @Nested
    class PropertiesTest implements ConfigFileAware {

        @Test
        void testConsistency() {
            MessageReaderFactory<String> builder = MessageReaderFactory.<String>builder()
                    .configFile(getConfigFile(workdir))
                    .service(getServiceName())
                    .consistency(Consistency.AT_MOST_ONCE)
                    .build();
            try (MessageReader<String> reader = builder.getReader()) {
                assertEquals(Consistency.AT_MOST_ONCE, reader.getConsistency());
            }
        }

        @ParameterizedTest
        @EnumSource(value =  Consistency.class, mode = EnumSource.Mode.EXCLUDE, names = "AT_MOST_ONCE")
        void testConsistency2(Consistency consistency) {
            try {
                MessageReaderFactory<String> builder = MessageReaderFactory.<String>builder()
                            .configFile(getConfigFile(workdir))
                            .service(getServiceName())
                            .consistency(consistency)
                            .build();
                try (MessageReader<String> reader = builder.getReader()) {
                    fail();
                }
            } catch (InvalidConfigurationException e) {
            }
        }

        @Nested
        class ClientIdTest {
            @Test
            void clientId() {
                String clientId = RandomStringUtils.randomAlphabetic(10);
                MessageReaderFactory<String> builder = MessageReaderFactory.<String>builder()
                        .configFile(getConfigFile(workdir))
                        .service(getServiceName())
                        .clientId(clientId)
                        .build();
                try (MessageReader<String> reader = builder.getReader()) {
                    assertEquals(clientId, reader.getClientId());
                }
            }

            @Test
            void defaultClientId() {
                MessageReaderFactory<String> builder = MessageReaderFactory.<String>builder()
                        .configFile(getConfigFile(workdir))
                        .service(getServiceName())
                        .build();
                try (MessageReader<String> reader = builder.getReader()) {
                    assertNotNull(reader.getClientId());
                }
            }

            @ParameterizedTest
            @NullAndEmptySource
            void emptyAndNull(String clientId) {
                MessageReaderFactory<String> builder = MessageReaderFactory.<String>builder()
                        .configFile(getConfigFile(workdir))
                        .service(getServiceName())
                        .clientId(clientId)
                        .build();
                try (MessageReader<String> reader = builder.getReader()) {
                    assertNotNull(reader.getClientId());
                    assertTrue(StringUtils.isNotEmpty(reader.getClientId()));
                }
            }
        }

        @SuppressWarnings("rawtypes")
        @ParameterizedTest
        @EnumSource(SimpleValueType.class)
        void valueType(ValueType valueType) {
            MessageReaderFactory builder = MessageReaderFactory.builder()
                    .configFile(getConfigFile(workdir))
                    .service(getServiceName())
                    .valueType(valueType)
                    .build();
            try (MessageReader reader = builder.getReader()) {
                assertEquals(valueType, reader.getValueType());
            }
        }

        @Nested
        class ReceiveTimeoutTest {
            @Test
            void defaultTimeout() {
                MessageReaderFactory<String> builder = MessageReaderFactory.<String>builder()
                        .configFile(getConfigFile(workdir))
                        .service(getServiceName())
                        .build();
                try (MessageReader<String> reader = builder.getReader()) {
                    assertEquals(Duration.ofNanos(Long.MAX_VALUE), reader.getReceiveTimeout());
                }
            }

            @ParameterizedTest
            @MethodSource("jp.ad.sinet.stream.plugins.s3.MessageReaderTest#getDurations")
            void receiveTimeout(Duration timeout) {
                MessageReaderFactory<String> builder = MessageReaderFactory.<String>builder()
                        .configFile(getConfigFile(workdir))
                        .service(getServiceName())
                        .receiveTimeout(timeout)
                        .build();
                try (MessageReader<String> reader = builder.getReader()) {
                    assertEquals(timeout, reader.getReceiveTimeout());
                }
            }
        }
    }

    static Stream<Duration> getDurations() {
        return Stream.of(
                Duration.ofSeconds(10), Duration.ofHours(3), Duration.ofDays(7), Duration.ZERO,
                Duration.ofMillis(100), Duration.ofNanos(123456789));
    }

    @Nested
    class ReadAfterWriteTest implements ConfigFileAware {
        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        void readAfterWrite(boolean userDataOnly) {
            String topic = generateTopic();
            System.err.println("XXX userDataOnly=" + userDataOnly);

            //int n = 10;
            int n = 3;
            ArrayList<String> msgs = new ArrayList<>(n);
            for (int i = 0; i < n; i++)
                msgs.add(RandomStringUtils.randomAlphabetic(i*i*i+1));

            MessageWriterFactory<String> wbuilder = MessageWriterFactory.<String>builder()
                    .configFile(getConfigFile(workdir))
                    .service(getServiceName())
                    .topic(topic)
                    .userDataOnly(userDataOnly)
                    .build();
            try (MessageWriter<String> writer = wbuilder.getWriter()) {
                for (String msg : msgs) {
                    writer.write(msg);
                }
            }
            MessageReaderFactory<String> rbuilder = MessageReaderFactory.<String>builder()
                    .configFile(getConfigFile(workdir))
                    .service(getServiceName())
                    .topic(topic)
                    .userDataOnly(userDataOnly)
                    .receiveTimeout(Duration.ofSeconds(10))
                    .build();
            try (MessageReader<String> reader = rbuilder.getReader()) {
                Message<String> m;
                while (Objects.nonNull(m = reader.read())) {
                    System.err.println("XXX m=" + m);
                    String msg = m.getValue();
                    boolean ok = msgs.remove(msg);
                    assert(ok);
                }
            }
            assert(msgs.isEmpty());
        }
    }

    @Nested
    class UtcOffsetTest implements ConfigFileAware {
        @Override
        public String getUtcOffset() {
            return "+0900";
        }
        @Test
        void readAfterWrite() {
            String topic = generateTopic();

            //int n = 10;
            int n = 3;
            ArrayList<String> msgs = new ArrayList<>(n);
            for (int i = 0; i < n; i++)
                msgs.add(RandomStringUtils.randomAlphabetic(i*i*i+1));

            MessageWriterFactory<String> wbuilder = MessageWriterFactory.<String>builder()
                    .configFile(getConfigFile(workdir))
                    .service(getServiceName())
                    .topic(topic)
                    .build();
            try (MessageWriter<String> writer = wbuilder.getWriter()) {
                for (String msg : msgs) {
                    writer.write(msg);
                }
            }
            MessageReaderFactory<String> rbuilder = MessageReaderFactory.<String>builder()
                    .configFile(getConfigFile(workdir))
                    .service(getServiceName())
                    .topic(topic)
                    .receiveTimeout(Duration.ofSeconds(10))
                    .build();
            try (MessageReader<String> reader = rbuilder.getReader()) {
                Message<String> m;
                while (Objects.nonNull(m = reader.read())) {
                    System.err.println("XXX m=" + m);
                    String msg = m.getValue();
                    boolean ok = msgs.remove(msg);
                    assert(ok);
                }
            }
            assert(msgs.isEmpty());
        }
    }

    @Nested
    class UtcOffsetNGTest implements ConfigFileAware {
        @Override
        public String getUtcOffset() {
            return "hoge";
        }
        //@Test(expected = InvalidConfigurationException.class)
        @Test
        void test() {
            String topic = generateTopic();
            MessageReaderFactory<String> rbuilder = MessageReaderFactory.<String>builder()
                    .configFile(getConfigFile(workdir))
                    .service(getServiceName())
                    .topic(topic)
                    .receiveTimeout(Duration.ofSeconds(10))
                    .build();
            try {
                MessageReader<String> reader = rbuilder.getReader();
                /* NOTE: because Reader doesn't refer utcOffset, reach here.
                assert(false);
                */
            }
            catch (InvalidConfigurationException e) {
                // ok
                assert(false); // NOTE: never reach.
            }
        }
    }
}
