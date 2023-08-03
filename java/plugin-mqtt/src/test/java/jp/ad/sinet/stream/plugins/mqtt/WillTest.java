/*
 * Copyright (C) 2023 National Institute of Informatics
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

import jp.ad.sinet.stream.api.Message;
import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.api.Metrics;
import jp.ad.sinet.stream.api.SinetStreamMessageWriter;
import jp.ad.sinet.stream.api.valuetype.SimpleValueType;
import jp.ad.sinet.stream.api.ValueType;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import jp.ad.sinet.stream.utils.MessageWriterFactory;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;


import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="RUN_INTERGRATION_TEST", matches = "(?i)true")
class WillTest {

    @TempDir
    Path workdir;

    @Nested
    class ReadWriteTest implements ConfigFileAware {

        private String topic = generateTopic();

        private String genMsg() {
            return "message-" + RandomStringUtils.randomAlphabetic(10);
        }

        @Override
        public Optional<String> getTopic() {
            return Optional.of(topic);
        }

        <T> T test1(Map<String, Object> params,
                    Map<String, Object> will,
                    T willmsg) throws Exception {
            MessageWriterFactory<String> writer_builder =
                    MessageWriterFactory.<String>builder()
                            .configFile(getConfigFile(workdir)).service(getServiceName())
                            .valueType(SimpleValueType.TEXT)
                            .parameter("will_set", will)
                            .build();
            MessageReaderFactory<T> reader_builder =
                    MessageReaderFactory.<T>builder()
                            .configFile(getConfigFile(workdir)).service(getServiceName())
                            .parameters(params)
                            .receiveTimeout(Duration.ofSeconds(10))
                            .build();
            try (MessageWriter<String> writer = writer_builder.getWriter();
                 MessageReader<T> reader = reader_builder.getReader()) {

                Thread.sleep(100);  // sleep 100ms to wait for conntected...

                ((SinetStreamMessageWriter) writer).debugDisconnectForcibly();
                Message<T> msg = reader.read();
                return msg.getValue();
            }
        }

        @ParameterizedTest
        @ValueSource(strings = {"AT_MOST_ONCE", "AT_LEAST_ONCE", "EXACTLY_ONCE"})
        void testConsistency(String consistency) throws Exception {
            String willmsg = "will-" + genMsg();

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("consistency", consistency);
            params.put("value_type", "text");

            Map<String, Object> will = new HashMap<String, Object>(params);
            will.put("topic", topic);
            will.put("retain", false);
            will.put("payload", willmsg);

            String res = test1(params, will, willmsg);
            assertEquals(willmsg, res);
        }

        @Test
        void testValueTypeByteArray() throws Exception {
            byte[] willmsg = ("will-" + genMsg()).getBytes();

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("consistency", "AT_LEAST_ONCE");
            params.put("value_type", "byte_array");

            Map<String, Object> will = new HashMap<String, Object>(params);
            will.put("topic", topic);
            will.put("retain", false);
            will.put("payload", willmsg);

            byte[] res = test1(params, will, willmsg);
            assertArrayEquals(willmsg, res);
        }

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        void testRetain(boolean retain) throws Exception {
            String willmsg = "will-" + genMsg();

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("consistency", "AT_LEAST_ONCE");
            params.put("value_type", "text");

            Map<String, Object> will = new HashMap<String, Object>(params);
            will.put("topic", topic);
            will.put("retain", retain);
            will.put("payload", willmsg);

            String res = test1(params, will, willmsg);
            assertEquals(willmsg, res);
        }

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        void testComp(boolean comp) throws Exception {
            String willmsg = "will-" + genMsg();

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("consistency", "AT_LEAST_ONCE");
            params.put("value_type", "text");
            params.put("data_compression", comp);

            Map<String, Object> will = new HashMap<String, Object>(params);
            will.put("topic", topic);
            will.put("payload", willmsg);

            String res = test1(params, will, willmsg);
            assertEquals(willmsg, res);
        }

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        void testUserDataOnly(boolean comp) throws Exception {
            String willmsg = "will-" + genMsg();

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("consistency", "AT_LEAST_ONCE");
            params.put("value_type", "text");
            params.put("user_data_only", comp);

            Map<String, Object> will = new HashMap<String, Object>(params);
            will.put("topic", topic);
            will.put("payload", willmsg);

            String res = test1(params, will, willmsg);
            assertEquals(willmsg, res);
        }

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        void testEncryption(boolean enc) throws Exception {
            String willmsg = "will-" + genMsg();

            Map<String, Object> params = new HashMap<String, Object>();
            Map<String, Object> crypto = new HashMap<String, Object>();
            params.put("consistency", "AT_LEAST_ONCE");
            params.put("value_type", "text");
            params.put("data_encryption", enc);
            params.put("crypto", crypto);
                crypto.put("algorithm", "AES");
                crypto.put("mode", "EAX");
                crypto.put("password", "secret-123");

            Map<String, Object> will = new HashMap<String, Object>(params);
            will.put("topic", topic);
            will.put("payload", willmsg);

            String res = test1(params, will, willmsg);
            assertEquals(willmsg, res);
        }

    }
}
