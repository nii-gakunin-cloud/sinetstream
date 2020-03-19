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

package jp.ad.sinet.stream.plugins.kafka;

import jp.ad.sinet.stream.api.*;
import jp.ad.sinet.stream.api.valuetype.SimpleValueType;
import jp.ad.sinet.stream.api.valuetype.ValueTypeFactory;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.apache.kafka.common.errors.SerializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ValueTypeTest implements ConfigFileAware {

    @Disabled
    @Nested
    class Writer {
        @Test
        void defaultValueType() {
            MessageWriterFactory<byte[]> builder =
                    MessageWriterFactory.<byte[]>builder().service("service-0").topic("test-topic-java-001").build();
            try (MessageWriter<byte[]> writer = builder.getWriter()) {
                assertEquals(SimpleValueType.BYTE_ARRAY, writer.getValueType());
                writer.write("message 1".getBytes());
            }
        }

        @Test
        void textValueType() {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder().service("service-1").topic("test-topic-java-001")
                            .valueType(SimpleValueType.TEXT).build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                assertEquals(SimpleValueType.TEXT, writer.getValueType());
                writer.write("message 2");
            }
        }

        @Test
        void imageValueType() throws Exception {
            URL url = ValueTypeTest.class.getResource("/GakuNinCloud.png");
            BufferedImage image = ImageIO.read(url);
            MessageWriterFactory<BufferedImage> builder =
                    MessageWriterFactory.<BufferedImage>builder().service("service-1").topic("test-topic-java-001-image")
                            .valueType(new ValueTypeFactory().get("image")).build();
            try (MessageWriter<BufferedImage> writer = builder.getWriter()) {
                assertEquals(new ValueTypeFactory().get("image"), writer.getValueType());
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
                            .valueType(SimpleValueType.BYTE_ARRAY).build();
            try (MessageWriter<byte[]> writer = builder.getWriter()) {
                assertEquals(SimpleValueType.BYTE_ARRAY, writer.getValueType());
                writer.write(out.toByteArray());
            }
        }

        @Disabled
        @Test
        void badValueType() {
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder().service("service-1").topic("test-topic-java-001")
                            .valueType(new ValueTypeFactory().get("image")).build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                assertEquals(new ValueTypeFactory().get("image"), writer.getValueType());
                SinetStreamIOException ex = assertThrows(SinetStreamIOException.class, () -> writer.write("message X"));
                assertEquals(SerializationException.class, ex.getCause().getClass());
            }
        }

        @Disabled
        @Test
        void badValueType2() throws Exception {
            URL url = ValueTypeTest.class.getResource("/GakuNinCloud.png");
            BufferedImage image = ImageIO.read(url);
            MessageWriterFactory<BufferedImage> builder =
                    MessageWriterFactory.<BufferedImage>builder().service("service-1").topic("test-topic-java-001-image")
                            .valueType(SimpleValueType.TEXT).build();
            try (MessageWriter<BufferedImage> writer = builder.getWriter()) {
                assertEquals(SimpleValueType.TEXT, writer.getValueType());
                SinetStreamIOException ex = assertThrows(SinetStreamIOException.class, () -> writer.write(image));
                assertEquals(SerializationException.class, ex.getCause().getClass());
            }
        }
    }

    @Disabled
    @Nested
    class Reader {

        private byte[] testBytes;
        private BufferedImage testImage;

        @Test
        void defaultValueType() {
            MessageReaderFactory<byte[]> readerBuilder =
                    MessageReaderFactory.<byte[]>builder().service("service-0").topic("test-topic-java-001")
                            .receiveTimeout(Duration.ofSeconds(3)).build();
            MessageWriterFactory<byte[]> writerBuilder =
                    MessageWriterFactory.<byte[]>builder().service("service-0").topic("test-topic-java-001")
                            .consistency(Consistency.AT_LEAST_ONCE).build();
            try (MessageReader<byte[]> reader = readerBuilder.getReader();
                 MessageWriter<byte[]> writer = writerBuilder.getWriter()) {
                assertEquals(SimpleValueType.BYTE_ARRAY, reader.getValueType());
                reader.read();
                String text = "message 0";
                writer.write(text.getBytes());
                assertArrayEquals(text.getBytes(), reader.read().getValue());
            }
        }

        @Test
        void textValueType() {
            MessageReaderFactory<String> readerBuilder =
                    MessageReaderFactory.<String>builder().service("service-1").topic("test-topic-java-001")
                            .valueType(SimpleValueType.TEXT)
                            .receiveTimeout(Duration.ofSeconds(3))
                            .build();
            MessageWriterFactory<String> writerBuilder =
                    MessageWriterFactory.<String>builder().service("service-1").topic("test-topic-java-001")
                            .consistency(Consistency.AT_LEAST_ONCE).build();
            try (MessageReader<String> reader = readerBuilder.getReader();
                 MessageWriter<String> writer = writerBuilder.getWriter()) {
                assertEquals(SimpleValueType.TEXT, reader.getValueType());
                reader.read();
                String text = "message 0";
                writer.write(text);
                assertEquals(text, reader.read().getValue());
            }
        }

        @Test
        void imageValueType() {
            MessageReaderFactory<BufferedImage> readerBuilder =
                    MessageReaderFactory.<BufferedImage>builder().service("service-1").topic("test-topic-java-001-image")
                            .valueType(new ValueTypeFactory().get("image"))
                            .receiveTimeout(Duration.ofSeconds(3))
                            .build();
            MessageWriterFactory<BufferedImage> writerBuilder =
                    MessageWriterFactory.<BufferedImage>builder().service("service-1").topic("test-topic-java-001-image")
                            .valueType(new ValueTypeFactory().get("image")).consistency(Consistency.AT_LEAST_ONCE).build();
            try (MessageReader<BufferedImage> reader = readerBuilder.getReader();
                 MessageWriter<BufferedImage> writer = writerBuilder.getWriter()) {
                assertEquals(new ValueTypeFactory().get("image"), reader.getValueType());
                reader.read();
                writer.write(testImage);
                assertNotNull(reader.read().getValue());
            }
        }

        @Test
        void byteArrayValueType() {
            MessageReaderFactory<byte[]> readerBuilder =
                    MessageReaderFactory.<byte[]>builder().service("service-1").topic("test-topic-java-001-bytes")
                            .receiveTimeout(Duration.ofSeconds(3))
                            .valueType(SimpleValueType.BYTE_ARRAY).build();
            MessageWriterFactory<byte[]> writerBuilder =
                    MessageWriterFactory.<byte[]>builder().service("service-1").topic("test-topic-java-001-bytes")
                            .valueType(SimpleValueType.BYTE_ARRAY).build();
            try (MessageReader<byte[]> reader = readerBuilder.getReader();
                 MessageWriter<byte[]> writer = writerBuilder.getWriter()) {
                reader.read();
                writer.write(testBytes);
                assertEquals(SimpleValueType.BYTE_ARRAY, reader.getValueType());
                assertArrayEquals(testBytes, reader.read().getValue());
            }
        }

        @Test
        void badValueType() {
            MessageReaderFactory<String> builder =
                    MessageReaderFactory.<String>builder().service("service-1").topic("test-topic-java-001")
                            .receiveTimeout(Duration.ofSeconds(3)).valueType(new ValueTypeFactory().get("image")).build();
            try (MessageReader<String> reader = builder.getReader()) {
                assertEquals(new ValueTypeFactory().get("image"), reader.getValueType());
                Message<String> ret = assertTimeout(Duration.ofSeconds(5), reader::read);
                assertNull(ret);
            }
        }

        @Test
        void badValueType2() {
            MessageReaderFactory<BufferedImage> readerBuilder =
                    MessageReaderFactory.<BufferedImage>builder().service("service-1").topic("test-topic-java-001-image")
                            .valueType(SimpleValueType.TEXT)
                            .receiveTimeout(Duration.ofSeconds(10))
                            .build();
            MessageWriterFactory<String> writerBuilder =
                    MessageWriterFactory.<String>builder().service("service-1").topic("test-topic-java-001")
                            .consistency(Consistency.AT_LEAST_ONCE).build();
            try (MessageReader<BufferedImage> reader = readerBuilder.getReader();
                 MessageWriter<String> writer = writerBuilder.getWriter()) {

                assertEquals(SimpleValueType.TEXT, reader.getValueType());
                reader.read();
                writer.write("message-0");
                assertNull(reader.read());
            }
        }

        @BeforeEach
        void setupImageMessage() throws Exception {
            URL url = ValueTypeTest.class.getResource("/GakuNinCloud.png");
            testImage = ImageIO.read(url);
        }

        @BeforeEach
        void setupByteArrayMessage() throws Exception {
            URL url = ValueTypeTest.class.getResource("/GakuNinCloud.png");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (BufferedInputStream in = new BufferedInputStream(url.openStream())) {
                byte[] buf = new byte[4096];
                while (in.read(buf) >= 0) {
                    out.write(buf);
                }
            }
            testBytes = out.toByteArray();
        }
    }
}
