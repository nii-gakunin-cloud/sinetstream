/*
 * Copyright (C) 2021 National Institute of Informatics
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

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RawMsgTest implements ConfigFileAware {

    //private static final String SERVICE = "service-0";
    private static final String SERVICE = "service-with-encrypt-eax";
    private static final String TOPIC = "topic-" + RandomStringUtils.randomAlphabetic(5);
    private List<String> lines;

    @Test
    void inspect_and_inject() {
        MessageReaderFactory<String> readerBuilder =
                MessageReaderFactory.<String>builder().service(SERVICE)
                        .topic(TOPIC)
                        .dataEncryption(true)
                        .receiveTimeout(Duration.ofMillis(100))
                        .build();
        MessageWriterFactory<String> writerBuilder =
                MessageWriterFactory.<String>builder().service(SERVICE)
                        .topic(TOPIC)
                        .dataEncryption(true)
                        .build();

        assertTrue(writerBuilder.getDataEncryption());
        assertTrue(readerBuilder.getDataEncryption());

        Map<String, byte[]> raws = new HashMap<String, byte[]>();
        try (MessageWriter<String> writer = writerBuilder.getWriter()) {
            for (String aLine: lines) {
                writer.write(aLine);
                raws.put(aLine, writer.getDebugLastMsgBytes());
            }
        }
        try (MessageReader<String> reader = readerBuilder.getReader()) {
            for (Map.Entry<String, byte[]> e: raws.entrySet()) {
                reader.debugInjectMsgBytes(e.getValue(), TOPIC, null);
                Message<String> msg = reader.read();
                assertEquals(e.getKey(), msg.getValue());
            }
        }
    }

    @BeforeEach
    void setupMessages() {
        lines = IntStream.range(0, 10)
                .mapToObj(x -> RandomStringUtils.randomAlphabetic(10))
                .collect(Collectors.toList());
    }
}
