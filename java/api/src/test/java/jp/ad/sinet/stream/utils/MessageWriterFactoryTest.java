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

package jp.ad.sinet.stream.utils;

import jp.ad.sinet.stream.ConfigFileAware;
import jp.ad.sinet.stream.api.*;
import jp.ad.sinet.stream.api.valuetype.SimpleValueType;
import jp.ad.sinet.stream.api.valuetype.ValueTypeFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static jp.ad.sinet.stream.api.Consistency.*;
import static org.junit.jupiter.api.Assertions.*;

class MessageWriterFactoryTest implements ConfigFileAware {

    private static final String DEFAULT_SERVICE = "service-00";
    private static final String SERVICE = "service-with-parameters";
    private static final String SERIALIZER_SERVICE = "service-with-serializer-deserializer";
    private static final String TOPIC = "test-topic-java-001";

    @Test
    void nullService() {
        MessageWriterFactory<String> factory = MessageWriterFactory.<String>builder().service(null).build();
        assertThrows(NoServiceException.class, factory::getWriter);
        /* XXX:FIXME: service(null)はNoServiceException()を引き起こしていたが、
         * service(null)はサービスが1つしか定義されていないときにサービス名指定を省略できる手段に変更になったので、
         * このテストはよくないものになった。
         * many services are defined in "api/src/test/resources/sinetstream_config.yml" now.
         */
    }

    @Test
    void noTopic() {
        MessageWriterFactory<String> factory = MessageWriterFactory.<String>builder().service(DEFAULT_SERVICE).build();
        assertThrows(InvalidConfigurationException.class, factory::getWriter);
    }

    @Test
    void rewriteService() {
        MessageWriterFactory<Object> factory =
                MessageWriterFactory.builder().service("service-1").service(DEFAULT_SERVICE).topic(TOPIC).build();
        try (MessageWriter<Object> writer = factory.getWriter()) {
            assertNotNull(writer);
        }
    }

    @Test
    @Disabled
    /*
        FIXME: 実サーバにつないでテストするのダメ
        FIXME: ~/.config/sinetstream/auth.jsonをつかうからダメ
    */
    void configName() {
        MessageWriterFactory<Object> factory =
                MessageWriterFactory.builder().configName("stream009").service("service-kafka-001").topic(TOPIC).build();
        try (MessageWriter<Object> writer = factory.getWriter()) {
            assertNotNull(writer);
        }
        catch (UnsupportedServiceTypeException e) {
            // ok: kafka is not supported in this test.
        }
    }
    @Test
    @Disabled
    /*
        FIXME: 実サーバにつないでテストするのダメ
        FIXME: ~/.config/sinetstream/auth.jsonをつかうからダメ
    */
    void configNameNullService() {
        MessageWriterFactory<Object> factory =
                MessageWriterFactory.builder().configName("stream009").topic(TOPIC).build();
        try (MessageWriter<Object> writer = factory.getWriter()) {
            assertNotNull(writer);
        }
        catch (NoServiceException e) {
            if (e.getMessage().equals("too many services defined")) {
                // ok
            } else {
                throw e; // NG
            }
        }
        catch (UnsupportedServiceTypeException e) {
            // ok: kafka is not supported in this test.
        }
    }

    @Test
    void rewriteServiceWithNull() {
        MessageWriterFactory<String> factory =
                MessageWriterFactory.<String>builder().service(DEFAULT_SERVICE).service(null).build();
        assertThrows(NoServiceException.class, factory::getWriter);
    }

    @Test
    void defaultParameters() {
        MessageWriterFactory<String> factory =
                MessageWriterFactory.<String>builder().service(DEFAULT_SERVICE).topic(TOPIC).build();
        MessageWriter<String> writer = factory.getWriter();
        assertEquals(AT_MOST_ONCE, writer.getConsistency());
        assertFalse(writer.isDataEncryption());
        assertEquals(SimpleValueType.BYTE_ARRAY, writer.getValueType());
    }

    @SuppressWarnings("rawtypes")
    @Test
    void configurationFileParameters() {
        MessageWriterFactory<byte[]> factory =
                MessageWriterFactory.<byte[]>builder().service(SERVICE).build();
        MessageWriter<byte[]> writer = factory.getWriter();
        assertEquals(AT_LEAST_ONCE, writer.getConsistency());
        assertTrue(writer.isDataEncryption());
        assertEquals(SimpleValueType.BYTE_ARRAY, writer.getValueType());
        assertEquals("client-001", writer.getClientId());
        assertEquals("topic-001", writer.getTopic());
        assertIterableEquals(Arrays.asList("algorithm", "mode", "password", "provider"), ((Map) writer.getConfig().get("crypto")).keySet());
        assertIterableEquals(
                Arrays.asList("dummy0.example.org:1718", "dummy1.example.org"),
                (List) writer.getConfig().get("brokers"));
    }

    @SuppressWarnings("rawtypes")
    @Test
    void constructorParameters() {
        Map<String, String> crypto = new LinkedHashMap<>();
        crypto.put("algorithm", "AES");
        crypto.put("mode", "CBC");
        crypto.put("padding", "pkcs7");
        ValueType valueType = new ValueTypeFactory().get("image");

        MessageWriterFactory<BufferedImage> factory =
                MessageWriterFactory.<BufferedImage>builder()
                        .service(SERVICE)
                        .consistency(EXACTLY_ONCE)
                        .valueType(valueType)
                        .dataEncryption(false)
                        .topic("topic-005")
                        .parameter("crypto", crypto)
                        .build();
        MessageWriter<BufferedImage> writer = factory.getWriter();
        assertEquals(EXACTLY_ONCE, writer.getConsistency());
        assertFalse(writer.isDataEncryption());
        assertEquals(valueType, writer.getValueType());
        assertEquals("topic-005", writer.getTopic());
        /* @Disabled XXX FIXME Timestamp breaks this test.
        assertEquals(ValueType.IMAGE.getSerializer(), writer.getSerializer());
        */

        assertIterableEquals(
                Arrays.asList("algorithm", "mode", "padding"), ((Map) writer.getConfig().get("crypto")).keySet());
    }
}
