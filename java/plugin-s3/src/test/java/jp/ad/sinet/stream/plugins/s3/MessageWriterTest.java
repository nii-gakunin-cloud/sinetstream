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
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.api.ValueType;
import jp.ad.sinet.stream.api.valuetype.SimpleValueType;
import jp.ad.sinet.stream.api.InvalidConfigurationException;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="RUN_INTERGRATION_TEST", matches = "(?i)true")
class MessageWriterTest implements ConfigFileAware {

    @TempDir
    Path workdir;

    /*
    @Override
    public Map<String, Object> getS3Parameters() {
    }
    */

    /*
    @Test
    void testHoge() {
        assert(false);
    }
    */

    @Nested
    class WriterTest {

        @Test
        void testGetWriter() {
            MessageWriterFactory<String> builder = MessageWriterFactory.<String>builder()
                    .configFile(getConfigFile(workdir))
                    .service(getServiceName())
                    .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                assertNotNull(writer);
            }
        }

        @Test
        void testConsistency() {
            MessageWriterFactory<String> builder = MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir))
                        .service(getServiceName())
                        .consistency(Consistency.AT_MOST_ONCE)
                        .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                assertEquals(Consistency.AT_MOST_ONCE, writer.getConsistency());
                writer.write("message-1");
            }
        }

        @ParameterizedTest
        @EnumSource(value = Consistency.class, mode = EnumSource.Mode.EXCLUDE, names = "AT_MOST_ONCE")
        void testConsistency2(Consistency consistency) {
            try {
                MessageWriterFactory<String> builder = MessageWriterFactory.<String>builder()
                            .configFile(getConfigFile(workdir))
                            .service(getServiceName())
                            .consistency(consistency)
                            .build();
                try (MessageWriter<String> writer = builder.getWriter()) {
                    fail();
                    writer.write("message-1");
                }
            } catch (InvalidConfigurationException e) {
            }
        }

        @Test
        void configIsReadonly() {
            MessageWriterFactory<String> builder = MessageWriterFactory.<String>builder()
                    .configFile(getConfigFile(workdir))
                    .service(getServiceName())
                    .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                assertNotNull(writer);
                assertThrows(UnsupportedOperationException.class, () -> writer.getConfig().put("type", "kafka"));
            }
        }

        @Test
        void serviceType() {
            String topic = generateTopic();
            MessageWriterFactory<String> builder = MessageWriterFactory.<String>builder()
                    .configFile(getConfigFile(workdir))
                    .service(getServiceName())
                    .topic(topic)
                    .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                assertNotNull(writer);
                assertEquals(getServiceName(), writer.getService());
                assertEquals(topic, writer.getTopic());
                assertEquals("s3", writer.getConfig().get("type"));
            }
        }
    }

    @Nested
    class PropertiesTest {

        @Test
        void topic() {
            String topic = generateTopic();
            MessageWriterFactory<String> builder = MessageWriterFactory.<String>builder()
                    .configFile(getConfigFile(workdir))
                    .service(getServiceName())
                    .topic(topic)
                    .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                writer.write("message-1");
                assertEquals(topic, writer.getTopic());
            }
        }

        @Test
        void consistency() {
            Consistency consistency = Consistency.AT_MOST_ONCE;
            MessageWriterFactory<String> builder = MessageWriterFactory.<String>builder()
                    .configFile(getConfigFile(workdir))
                    .service(getServiceName())
                    .consistency(consistency)
                    .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                writer.write("message-1");
                assertEquals(consistency, writer.getConsistency());
            }
        }

        @Nested
        class ClientIdTest {
            @Test
            void clientId() {
                String clientId = RandomStringUtils.randomAlphabetic(10);
                MessageWriterFactory<String> builder = MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir))
                        .service(getServiceName())
                        .clientId(clientId)
                        .build();
                try (MessageWriter<String> writer = builder.getWriter()) {
                    writer.write("message-1");
                    assertEquals(clientId, writer.getClientId());
                }
            }

            @Test
            void defaultClientId() {
                MessageWriterFactory<String> builder = MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir))
                        .service(getServiceName())
                        .build();
                try (MessageWriter<String> writer = builder.getWriter()) {
                    writer.write("message-1");
                    assertNotNull(writer.getClientId());
                }
            }

            @ParameterizedTest
            @NullAndEmptySource
            void emptyAndNull(String clientId) {
                MessageWriterFactory<String> builder = MessageWriterFactory.<String>builder()
                        .configFile(getConfigFile(workdir))
                        .service(getServiceName())
                        .clientId(clientId)
                        .build();
                try (MessageWriter<String> writer = builder.getWriter()) {
                    writer.write("message-1");
                    assertNotNull(writer.getClientId());
                    assertTrue(StringUtils.isNotEmpty(writer.getClientId()));
                }
            }
        }

        @SuppressWarnings("rawtypes")
        @ParameterizedTest
        @EnumSource(SimpleValueType.class)
        void valueType(ValueType valueType) {
            MessageWriterFactory builder = MessageWriterFactory.builder()
                        .configFile(getConfigFile(workdir))
                        .service(getServiceName())
                        .valueType(valueType)
                        .build();
            try (MessageWriter writer = builder.getWriter()) {
                assertEquals(valueType, writer.getValueType());
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void getInfo() {
        MessageWriterFactory<String> builder = MessageWriterFactory.<String>builder()
                .configFile(getConfigFile(workdir))
                .service(getServiceName())
                .build();
        try (MessageWriter<String> writer = builder.getWriter()) {
            String ans = null;
            {
                Map<String, Object> info = (Map<String, Object>) writer.getInfo();
                Map<String, Object> info_s3 = (Map<String, Object>) info.get("s3");
                Map<String, Object> info_s3_writer = (Map<String, Object>) info_s3.get("writer");
                String info_s3_writer_uuid = (String) info_s3_writer.get("uuid");
                assert(info_s3_writer_uuid != null && !info_s3_writer_uuid.equals(""));
                ans = info_s3_writer_uuid;
            }

            {
                Map<String, Object> info_s3 = (Map<String, Object>) writer.getInfo("s3");
                Map<String, Object> info_s3_writer = (Map<String, Object>) info_s3.get("writer");
                String info_s3_writer_uuid = (String) info_s3_writer.get("uuid");
                assert(info_s3_writer_uuid != null && info_s3_writer_uuid.equals(ans));
            }

            {
                Map<String, Object> info_s3_writer = (Map<String, Object>) writer.getInfo("s3.writer");
                String info_s3_writer_uuid = (String) info_s3_writer.get("uuid");
                assert(info_s3_writer_uuid != null && info_s3_writer_uuid.equals(ans));
            }

            {
                String info_s3_writer_uuid = (String) writer.getInfo("s3.writer.uuid");
                assert(info_s3_writer_uuid != null && info_s3_writer_uuid.equals(ans));
            }
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
            MessageWriterFactory<String> wbuilder = MessageWriterFactory.<String>builder()
                    .configFile(getConfigFile(workdir))
                    .service(getServiceName())
                    .topic(topic)
                    .build();
            try {
                MessageWriter<String> writer = wbuilder.getWriter();
                assert(false);
            }
            catch (InvalidConfigurationException e) {
                // ok
            }
        }
    }
}
