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

package jp.ad.sinet.stream.plugins.iostream;

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
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.*;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

//@EnabledIfEnvironmentVariable(named="RUN_INTERGRATION_TEST", matches = "(?i)true")
class MessageReaderTest {

    @Nested
    class ReadAfterWriteTest implements ConfigFileAware {
        @ParameterizedTest
        @CsvSource({
            "false, 1, 0",
            "false, 10, 0",
            "false, 100, 0",
            "false, 1000, 0",
            "false, 1000, 1",
            "true, 1, 0",
            //"true, 100, 1",
            "true, 1000, 0",
        })
        void readAfterWrite(boolean userDataOnly, int msglen, long tstamp) {
            String topic = generateTopic();

            String msg = RandomStringUtils.randomAlphabetic(msglen);

            Map<String, Object> iop = new HashMap<String, Object>();
            Map<String, Object> iop2 = new HashMap<String, Object>();
            ByteArrayOutputStream ost = new ByteArrayOutputStream();
            iop2.put("output_stream", ost);
            iop.put("iostream", iop2);

            MessageWriterFactory<String> wbuilder = MessageWriterFactory.<String>builder()
                    .noConfig(true)
                    .type("iostream")
                    .valueType(SimpleValueType.TEXT)
                    .userDataOnly(userDataOnly)
                    .parameters(iop)
                    .build();
            try (MessageWriter<String> writer = wbuilder.getWriter()) {
                writer.write(msg, tstamp);
            }

            //Map<String, Map<String, Object>> iop = new HashMap<String, Map<String, Object>>();
            //Map<String, Object> iop2 = new HashMap<String, Object>();
            InputStream ist = new ByteArrayInputStream(ost.toByteArray());
            //System.err.println("XXX len=" + ost.toByteArray().length);
            iop2.put("input_stream", ist);
            iop2.remove("output_stream");
            //iop.put("iostream", iop2);

            MessageReaderFactory<String> rbuilder = MessageReaderFactory.<String>builder()
                    .noConfig(true)
                    .type("iostream")
                    .valueType(SimpleValueType.TEXT)
                    .userDataOnly(userDataOnly)
                    .parameters(iop)
                    .build();
            try (MessageReader<String> reader = rbuilder.getReader()) {
                int n = 0;
                Message<String> m;
                while (Objects.nonNull(m = reader.read())) {
                    //System.err.println("XXX m=" + m);
                    assert(m.getValue().equals(msg));
                    assert(m.getTimestampMicroseconds() == tstamp);
                    n++;
                }
                assert(n == 1);
            }
        }

        @ParameterizedTest
        @CsvSource({
            "false, 1, 0",
            "false, 10, 0",
            "false, 100, 0",
            "false, 1000, 0",
            "false, 1000, 1",
            "true, 1, 0",
            //"true, 100, 1",
            "true, 1000, 0",
        })
        void readAfterWriteParameters(boolean userDataOnly, int msglen, long tstamp) {
            String topic = generateTopic();

            String msg = RandomStringUtils.randomAlphabetic(msglen);

            Map<String, Object> iop = new HashMap<String, Object>();
            iop.put("no_config", true);
            iop.put("type", "iostream");
            iop.put("value_type", "text");
            iop.put("user_data_only", userDataOnly);
            Map<String, Object> iop2 = new HashMap<String, Object>();
            ByteArrayOutputStream ost = new ByteArrayOutputStream();
            iop2.put("output_stream", ost);
            iop.put("iostream", iop2);

            MessageWriterFactory<String> wbuilder = MessageWriterFactory.<String>builder()
                    //.noConfig(true)
                    //.type("iostream")
                    //.valueType(SimpleValueType.TEXT)
                    //.userDataOnly(userDataOnly)
                    .parameters(iop)
                    .build();
            try (MessageWriter<String> writer = wbuilder.getWriter()) {
                writer.write(msg, tstamp);
            }

            //Map<String, Map<String, Object>> iop = new HashMap<String, Map<String, Object>>();
            //Map<String, Object> iop2 = new HashMap<String, Object>();
            InputStream ist = new ByteArrayInputStream(ost.toByteArray());
            //System.err.println("XXX len=" + ost.toByteArray().length);
            iop2.put("input_stream", ist);
            iop2.remove("output_stream");
            //iop.put("iostream", iop2);

            MessageReaderFactory<String> rbuilder = MessageReaderFactory.<String>builder()
                    //.noConfig(true)
                    //.type("iostream")
                    //.valueType(SimpleValueType.TEXT)
                    //.userDataOnly(userDataOnly)
                    .parameters(iop)
                    .build();
            try (MessageReader<String> reader = rbuilder.getReader()) {
                int n = 0;
                Message<String> m;
                while (Objects.nonNull(m = reader.read())) {
                    //System.err.println("XXX m=" + m);
                    assert(m.getValue().equals(msg));
                    assert(m.getTimestampMicroseconds() == tstamp);
                    n++;
                }
                assert(n == 1);
            }
        }
    }
}
