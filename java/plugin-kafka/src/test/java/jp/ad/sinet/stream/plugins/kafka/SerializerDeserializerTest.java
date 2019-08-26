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

package jp.ad.sinet.stream.plugins.kafka;

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.api.ValueType;
import jp.ad.sinet.stream.utils.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SerializerDeserializerTest implements ConfigFileAware {
    @Nested
    class Serializer {
        @Test
        void asciiString() {
            StringSerializer ser = new StringSerializer(StandardCharsets.US_ASCII);
            MessageWriterFactory<String> builder =
                    MessageWriterFactory.<String>builder().service("service-1").topic("test-topic-java-001")
                            .serializer(ser)
                            .build();
            try (MessageWriter<String> writer = builder.getWriter()) {
                assertEquals(ValueType.TEXT, writer.getValueType());
                writer.write("message 1");
                assertNotNull(writer.getSerializer());
            }
        }

        @Test
        void imageSerializer() throws Exception {
            URL url = ValueTypeTest.class.getResource("/GakuNinCloud.png");
            BufferedImage image = ImageIO.read(url);
            ImageSerializer<BufferedImage> ser = new ImageSerializer<>();
            MessageWriterFactory<BufferedImage> builder =
                    MessageWriterFactory.<BufferedImage>builder().service("service-1").topic("test-topic-java-001")
                            .valueType(ValueType.TEXT)
                            .serializer(ser)
                            .build();
            try (MessageWriter<BufferedImage> writer = builder.getWriter()) {
                assertEquals(ValueType.TEXT, writer.getValueType());
                writer.write(image);
                assertNotNull(writer.getSerializer());
            }
        }
    }

    @Nested
    class Deserializer {
        @Test
        void asciiString() {
            StringDeserializer des = new StringDeserializer(StandardCharsets.US_ASCII);
            MessageWriterFactory<String> writerBuilder =
                    MessageWriterFactory.<String>builder().service("service-1").topic("test-topic-java-001")
                            .consistency(Consistency.AT_LEAST_ONCE).build();
            MessageReaderFactory<String> readerBuilder =
                    MessageReaderFactory.<String>builder().service("service-1").topic("test-topic-java-001")
                            .receiveTimeout(Duration.ofSeconds(3))
                            .deserializer(des)
                            .build();
            try (MessageReader<String> reader = readerBuilder.getReader();
                 MessageWriter<String> writer = writerBuilder.getWriter()) {
                reader.read();
                String text = "message 0";
                writer.write(text);
                assertEquals(ValueType.TEXT, reader.getValueType());
                assertEquals(text, reader.read().getValue());
            }
        }
    }
}
