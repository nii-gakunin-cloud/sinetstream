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

package jp.ad.sinet.stream.plugins.mqtt;

import jp.ad.sinet.stream.api.*;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

class ValueTypeTest implements ConfigFileAware {

    @Nested
    class Writer {
        @Test
        void defaultValutType() {
            MessageWriterFactory<String> builder = MessageWriterFactory.<String>builder().service("service-1").topic("test-topic-java-001").build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                assertEquals(ValueType.TEXT, writer.getValueType());
                writer.write("message 1");
            }
        }

        @Test
        void textValueType() {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder().service("service-1").topic("test-topic-java-001").valueType(ValueType.TEXT).build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                assertEquals(ValueType.TEXT, writer.getValueType());
                writer.write("message 2");
            }
        }

        @Test
        void imageValueType() throws Exception {
            URL url = ValueTypeTest.class.getResource("/GakuNinCloud.png");
            BufferedImage image = ImageIO.read(url);
            MessageWriterFactory<BufferedImage> builder =
                    MessageWriterFactory.<BufferedImage>builder().service("service-1").topic("test-topic-java-001-image").valueType(ValueType.IMAGE).build();
            try (MessageWriter<BufferedImage> writer = builder.getWriter()) {
                assertEquals(ValueType.IMAGE, writer.getValueType());
                writer.write(image);
            }
        }

        @Test
        void byteArrayValueType() throws Exception {
            URL url = ValueTypeTest.class.getResource("/GakuNinCloud.png");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (BufferedInputStream in = new BufferedInputStream(url.openStream())) {
                byte[] buf = new byte[4096];
                while (in.read(buf) >= 0) {
                    out.write(buf);
                }
            }

            MessageWriterFactory<byte[]> builder =
                    MessageWriterFactory.<byte[]>builder().service("service-1").topic("test-topic-java-001-bytes")
                            .valueType(ValueType.BYTE_ARRAY).build();
            try (MessageWriter<byte[]> writer = builder.getWriter()) {
                assertEquals(ValueType.BYTE_ARRAY, writer.getValueType());
                writer.write(out.toByteArray());
            }
        }

        @Test
        void badValueType() {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder().service("service-1").topic("test-topic-java-001").valueType(ValueType.IMAGE).build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                assertEquals(ValueType.IMAGE, writer.getValueType());
                assertThrows(ClassCastException.class, () -> writer.write("message X"));
            }
        }

        @Test
        void badValueType2() throws Exception {
            URL url = ValueTypeTest.class.getResource("/GakuNinCloud.png");
            BufferedImage image = ImageIO.read(url);
            MessageWriterFactory<BufferedImage> builder =
                    MessageWriterFactory.<BufferedImage>builder().service("service-1").topic("test-topic-java-001-image").valueType(ValueType.TEXT).build();
            try (MessageWriter<BufferedImage> writer = builder.getWriter()) {
                assertEquals(ValueType.TEXT, writer.getValueType());
                assertThrows(ClassCastException.class, () -> writer.write(image));
            }
        }
    }

    @Nested
    class Reader {
        @Test
        void defaultValutType() {
            MessageReaderFactory<String> builder = MessageReaderFactory.<String>builder().service("service-1").topic("test-topic-java-001").build();
            try (MessageReader<String> reader = builder.getReader()) {
                assertEquals(ValueType.TEXT, reader.getValueType());
                assertNotNull(reader.read().getValue());
            }
        }

        @Test
        void textValueType() {
            MessageReaderFactory<String> builder =
                    MessageReaderFactory.<String>builder().service("service-1").topic("test-topic-java-001").valueType(ValueType.TEXT).build();
            try (MessageReader<String> reader = builder.getReader()) {
                assertEquals(ValueType.TEXT, reader.getValueType());
                assertNotNull(reader.read().getValue());
            }
        }

        @Test
        void imageValueType() {
            MessageReaderFactory<BufferedImage> builder =
                    MessageReaderFactory.<BufferedImage>builder().service("service-1").topic("test-topic-java-001-image").valueType(ValueType.IMAGE).build();
            try (MessageReader<BufferedImage> reader = builder.getReader()) {
                assertEquals(ValueType.IMAGE, reader.getValueType());
                assertNotNull(reader.read().getValue());
            }
        }

        @Test
        void byteArrayValueType() {
            MessageReaderFactory<byte[]> builder =
                    MessageReaderFactory.<byte[]>builder().service("service-1").topic("test-topic-java-001-bytes")
                            .valueType(ValueType.BYTE_ARRAY).build();
            try (MessageReader<byte[]> reader = builder.getReader()) {
                assertEquals(ValueType.BYTE_ARRAY, reader.getValueType());
                assertNotNull(reader.read().getValue());
            }
        }

        @Test
        void badValueType() {
            MessageReaderFactory<String> builder =
                    MessageReaderFactory.<String>builder().service("service-1").topic("test-topic-java-001").valueType(ValueType.IMAGE).build();
            try (MessageReader<String> reader = builder.getReader()) {
                assertEquals(ValueType.IMAGE, reader.getValueType());
                assertThrows(SinetStreamIOException.class, () -> reader.read().getValue());
            }
        }

        @Test
        void badValueType2() {
            MessageReaderFactory<BufferedImage> builder =
                    MessageReaderFactory.<BufferedImage>builder().service("service-1").topic("test-topic-java-001-image").valueType(ValueType.TEXT).build();
            try (MessageReader<BufferedImage> reader = builder.getReader()) {
                assertEquals(ValueType.TEXT, reader.getValueType());
                assertNotNull(reader.read().getValue());
            }
        }

        @BeforeEach
        void writeTextMessage() {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder().service("service-1").topic("test-topic-java-001")
                            .consistency(Consistency.AT_LEAST_ONCE).build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                assertEquals(ValueType.TEXT, writer.getValueType());
                writer.write("message 0");
            }
        }

        @BeforeEach
        void writeImageMessage() throws Exception {
            URL url = ValueTypeTest.class.getResource("/GakuNinCloud.png");
            BufferedImage image = ImageIO.read(url);
            MessageWriterFactory<BufferedImage> builder =
                    MessageWriterFactory.<BufferedImage>builder().service("service-1").topic("test-topic-java-001-image")
                            .valueType(ValueType.IMAGE).consistency(Consistency.AT_LEAST_ONCE).build();
            try (MessageWriter<BufferedImage> writer = builder.getWriter()) {
                assertEquals(ValueType.IMAGE, writer.getValueType());
                writer.write(image);
            }
        }
    }

    @BeforeEach
    void writeByteArrayMessage() throws Exception {
        URL url = ValueTypeTest.class.getResource("/GakuNinCloud.png");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (BufferedInputStream in = new BufferedInputStream(url.openStream())) {
            byte[] buf = new byte[4096];
            while (in.read(buf) >= 0) {
                out.write(buf);
            }
        }

        MessageWriterFactory<byte[]> builder =
                MessageWriterFactory.<byte[]>builder().service("service-1").topic("test-topic-java-001-bytes")
                        .valueType(ValueType.BYTE_ARRAY).build();
        try (MessageWriter<byte[]> writer = builder.getWriter()) {
            assertEquals(ValueType.BYTE_ARRAY, writer.getValueType());
            writer.write(out.toByteArray());
        }
    }
}
