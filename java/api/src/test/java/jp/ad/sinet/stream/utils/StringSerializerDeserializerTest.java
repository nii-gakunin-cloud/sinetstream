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

package jp.ad.sinet.stream.utils;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class StringSerializerDeserializerTest {

    @Nested
    class Serializer {

        @Test
        void alphaNumeric() {
            String text = RandomStringUtils.randomAlphanumeric(20);
            StringSerializer ser = new StringSerializer();
            StringDeserializer des = new StringDeserializer();
            byte[] bytes = ser.serialize(text);
            assertTrue(bytes.length > 0);
            assertEquals(text, des.deserialize(bytes));
        }

        @Test
        void random() {
            String text = RandomStringUtils.random(20);
            StringSerializer ser = new StringSerializer();
            StringDeserializer des = new StringDeserializer();
            byte[] bytes = ser.serialize(text);
            assertTrue(bytes.length > 0);
            assertEquals(text, des.deserialize(bytes));
        }

        @Test
        void ascii() {
            String text = RandomStringUtils.randomAscii(20);
            StringSerializer ser = new StringSerializer(StandardCharsets.US_ASCII);
            StringDeserializer des = new StringDeserializer();
            byte[] bytes = ser.serialize(text);
            assertTrue(bytes.length > 0);
            assertEquals(text, des.deserialize(bytes));
        }

        @Test
        void nullData() {
            StringSerializer ser = new StringSerializer();
            assertNull(ser.serialize(null));
        }
    }

    @Nested
    class Deserializer {

        @Test
        void alphaNumeric() {
            String text = RandomStringUtils.randomAlphanumeric(20);
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            StringDeserializer des = new StringDeserializer();
            assertEquals(text, des.deserialize(bytes));
        }

        @Test
        void random() {
            String text = RandomStringUtils.random(20);
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            StringDeserializer des = new StringDeserializer();
            assertEquals(text, des.deserialize(bytes));
        }

        @Test
        void ascii() {
            String text = RandomStringUtils.randomAscii(20);
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            StringDeserializer des = new StringDeserializer(StandardCharsets.US_ASCII);
            assertEquals(text, des.deserialize(bytes));
        }

        @Test
        void nullData() {
            StringDeserializer des = new StringDeserializer();
            assertNull(des.deserialize(null));
        }
    }
}