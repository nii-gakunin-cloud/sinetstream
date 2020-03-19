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

import jp.ad.sinet.stream.api.Message;
import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.api.valuetype.SimpleValueType;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncryptTest implements ConfigFileAware {


    @ParameterizedTest
    @ValueSource(strings = {"service-with-encrypt-cbc", "service-with-encrypt-eax"})
    void encrypt(String service) {
        String topic = "topic-aes/" + RandomStringUtils.randomAlphabetic(5);
        MessageWriterFactory<String> writerBuilder =
                MessageWriterFactory.<String>builder().service(service)
                        .topic(topic)
                        .dataEncryption(true)
                        .valueType(SimpleValueType.TEXT)
                        .build();
        MessageReaderFactory<String> readerBuilder =
                MessageReaderFactory.<String>builder().service(service)
                        .topic(topic)
                        .dataEncryption(true)
                        .valueType(SimpleValueType.TEXT)
                        .build();

        assertTrue(writerBuilder.getDataEncryption());
        assertTrue(readerBuilder.getDataEncryption());

        try (MessageWriter<String> writer = writerBuilder.getWriter();
             MessageReader<String> reader = readerBuilder.getReader()) {

            final String data = RandomStringUtils.randomAlphabetic(10);
            writer.write(data);
            Message<String> result = reader.read();
            assertEquals(data, result.getValue());
        }
    }
}
