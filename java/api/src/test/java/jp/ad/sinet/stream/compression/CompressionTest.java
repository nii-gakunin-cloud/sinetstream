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

package jp.ad.sinet.stream.compression;

import jp.ad.sinet.stream.ConfigFileAware;
import jp.ad.sinet.stream.api.*;
import jp.ad.sinet.stream.api.compression.CompressionFactory;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CompressionTest implements ConfigFileAware {

    @Nested
    class CompressionAlgorithmTest {

        @ParameterizedTest
        @ValueSource(strings = {"hogecomp", ""})
        void testCompAlgorithm(String alg) {
            Map<String, Object> param = new HashMap<>();
            param.put("algorithm", alg);
            assertThrows(InvalidConfigurationException.class,
                    () -> CompressionFactory.createCompressor(param));
            assertThrows(InvalidConfigurationException.class,
                    () -> CompressionFactory.createDecompressor(param));
        }

        @ParameterizedTest
        @CsvSource({
            "gzip,null",
            "null,gzip",
            "null,null",
            })
        void testCompDefault(String walg, String ralg) {
            Map<String, Object> wparam = new HashMap<>();
            Map<String, Object> rparam = new HashMap<>();
            wparam.put("algorithm", walg.equals("null") ? null : walg);
            rparam.put("algorithm", ralg.equals("null") ? null : ralg);
            Compressor comp = CompressionFactory.createCompressor(wparam);
            Decompressor decomp = CompressionFactory.createDecompressor(rparam);
            int cnt = 10;
            for (int i = 0; i < cnt; i++) {
                int sz = 100;
                byte[] data = new byte[sz];
                new Random().nextBytes(data);
                assert(Arrays.equals(data, decomp.decompress(comp.compress(data))));
            }
        }

        @ParameterizedTest
        @CsvSource({
            "gzip,1,10",
            "gzip,10,10",
            "gzip,100,10",
            "gzip,1000,10",
            "gzip,10000,10",
            "gzip,100000,10",
            "gzip,1000000,10",
            "zstd,1,10",
            "zstd,10,10",
            "zstd,100,10",
            "zstd,1000,10",
            "zstd,10000,10",
            "zstd,100000,10",
            "zstd,1000000,10",
            })
        void testCompDecompSize(String alg, int sz, int cnt) {
            Map<String, Object> param = new HashMap<>();
            param.put("algorithm", alg);
            Compressor comp = CompressionFactory.createCompressor(param);
            Decompressor decomp = CompressionFactory.createDecompressor(param);
            for (int i = 0; i < cnt; i++) {
                byte[] data = new byte[sz];
                new Random().nextBytes(data);
                assert(Arrays.equals(data, decomp.decompress(comp.compress(data))));
            }
        }

        @ParameterizedTest
        @CsvSource({
            //NG "gzip,-2",
            "gzip,-1", // -1 means default
            "gzip,0", // no comp
            "gzip,1", // min
            "gzip,6", // default
            "gzip,9", // max
            //NG "gzip,10",
            "zstd,-2",
            "zstd,-1",
            "zstd,0", // 0 means default
            "zstd,1", // min
            "zstd,3", // default
            "zstd,19",
            "zstd,20", // max
            "zstd,22", // ultra max
            "zstd,99", // ok...
            })
        void testCompDecompLevel(String alg, int lvl) {
            Map<String, Object> param = new HashMap<>();
            param.put("algorithm", alg);
            param.put("level", lvl);
            Compressor comp = CompressionFactory.createCompressor(param);
            Decompressor decomp = CompressionFactory.createDecompressor(param);
            int cnt = 10;
            int sz = 1000;
            for (int i = 0; i < cnt; i++) {
                byte[] data = new byte[sz];
                new Random().nextBytes(data);
                assert(Arrays.equals(data, decomp.decompress(comp.compress(data))));
            }
        }

        /*
        @ParameterizedTest
        @ValueSource(ints = {0, 1, 2, 10, 11, 21, 20, 21, 22, 0, 1, 2, 10, 11, 21, 20, 21, 22})
        void testCompDecomp(int imp) {
            Map<String, Object> param = new HashMap<>();
            param.put("algorithm", "zstd");
            param.put("_zstd_imp", 0);
            Compressor comp = CompressionFactory.createCompressor(param);
            Decompressor decomp = CompressionFactory.createDecompressor(param);
            long start1 = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                byte[] data = new byte[1000];
                new Random().nextBytes(data);
                assert(Arrays.equals(data, decomp.decompress(comp.compress(data))));
            }
            long end1 = System.nanoTime();
            System.err.println("imp=" + imp + " time=" + (end1 - start1));
        }
        */
    }

    private static final String SERVICE = "service-0";
    private static final String SERVICE_COMP_ON = "service-with-compression-on";
    private static final String SERVICE_COMP_OFF = "service-with-compression-off";
    private static final String TOPIC = "topic-" + RandomStringUtils.randomAlphabetic(5);

    @Nested
    class CompressionIOTest {

        private List<String> lines;

        @Test
        void testCompressionOn() {
            MessageReaderFactory<String> readerBuilder = MessageReaderFactory.<String>builder()
                    .service(SERVICE_COMP_ON)
                    .topic(TOPIC)
                    .receiveTimeout(Duration.ofMillis(100))
                    .build();
            MessageWriterFactory<String> writerFactory = MessageWriterFactory.<String>builder()
                    .service(SERVICE_COMP_ON)
                    .topic(TOPIC)
                    .build();

            try (MessageReader<String> reader = readerBuilder.getReader();
                 MessageWriter<String> writer = writerFactory.getWriter()) {
                lines.forEach(writer::write);
                assertIterableEquals(lines, reader.stream().map(Message::getValue).collect(Collectors.toList()));
            }
        }

        @Test
        void testCompressionOff() {
            MessageReaderFactory<String> readerBuilder = MessageReaderFactory.<String>builder()
                    .service(SERVICE_COMP_OFF)
                    .topic(TOPIC)
                    .receiveTimeout(Duration.ofMillis(100))
                    .build();
            MessageWriterFactory<String> writerFactory = MessageWriterFactory.<String>builder()
                    .service(SERVICE_COMP_OFF)
                    .topic(TOPIC)
                    .build();

            try (MessageReader<String> reader = readerBuilder.getReader();
                 MessageWriter<String> writer = writerFactory.getWriter()) {
                lines.forEach(writer::write);
                assertIterableEquals(lines, reader.stream().map(Message::getValue).collect(Collectors.toList()));
            }
        }

        @Test
        void testCompressionNG_OFFtoON() {
            MessageReaderFactory<String> readerBuilder = MessageReaderFactory.<String>builder()
                    .service(SERVICE_COMP_ON)
                    .topic(TOPIC)
                    .receiveTimeout(Duration.ofMillis(100))
                    .build();
            MessageWriterFactory<String> writerFactory = MessageWriterFactory.<String>builder()
                    .service(SERVICE_COMP_OFF)
                    .topic(TOPIC)
                    .build();

            try (MessageReader<String> reader = readerBuilder.getReader();
                 MessageWriter<String> writer = writerFactory.getWriter()) {
                String x = lines.get(0);
                writer.write(x);
                assertThrows(
                        SinetStreamIOException.class,
                        () -> reader.read());
                //assertEquals(x, y.getValue());
            }
        }

        @Test
        void testEnableCompressionNG_ONtoOFF() {
            MessageReaderFactory<String> readerBuilder = MessageReaderFactory.<String>builder()
                    .service(SERVICE_COMP_OFF)
                    .topic(TOPIC)
                    .receiveTimeout(Duration.ofMillis(100))
                    .build();
            MessageWriterFactory<String> writerFactory = MessageWriterFactory.<String>builder()
                    .service(SERVICE_COMP_ON)
                    .topic(TOPIC)
                    .build();

            try (MessageReader<String> reader = readerBuilder.getReader();
                 MessageWriter<String> writer = writerFactory.getWriter()) {
                String x = lines.get(0);
                writer.write(x);
                Message<String> y = reader.read();
                assert(! x.equals(y.getValue()));
            }
        }

        @ParameterizedTest
        @CsvSource({
            "gzip,gzip", //good
            "zstd,zstd", //good
            "gzip,zstd",
            "zstd,gzip",
        })
        void testEnableCompressionNG_alg(String writerAlg, String readerAlg) {
            Map<String, Map<String, Object>> reader_config = new HashMap<>();
            Map<String, Object> reader_config_compresion = new HashMap<>();
            reader_config.put("compression", reader_config_compresion);
            reader_config_compresion.put("algorithm", readerAlg);
            MessageReaderFactory<String> readerBuilder = MessageReaderFactory.<String>builder()
                    .service(SERVICE_COMP_ON)
                    .topic(TOPIC)
                    .receiveTimeout(Duration.ofMillis(100))
                    .parameters(reader_config)
                    .build();
            Map<String, Map<String, Object>> writer_config = new HashMap<>();
            Map<String, Object> writer_config_compresion = new HashMap<>();
            writer_config.put("compression", writer_config_compresion);
            writer_config_compresion.put("algorithm", writerAlg);
            MessageWriterFactory<String> writerFactory = MessageWriterFactory.<String>builder()
                    .service(SERVICE_COMP_ON)
                    .topic(TOPIC)
                    .parameters(writer_config)
                    .build();

            try (MessageReader<String> reader = readerBuilder.getReader();
                 MessageWriter<String> writer = writerFactory.getWriter()) {
                if (readerAlg.equals(writerAlg)) {
                    // good case
                    lines.forEach(writer::write);
                    assertIterableEquals(lines, reader.stream().map(Message::getValue).collect(Collectors.toList()));
                } else {
                    // bad case
                    String x = lines.get(0);
                    writer.write(x);
                    assertThrows(
                            SinetStreamIOException.class,
                            () -> reader.read());
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

    @Nested
    class CompressionMetricsTest {
        private List<String> lines;

        @Test
        void testCompressionOffMetrics() {
            MessageReaderFactory<String> readerBuilder = MessageReaderFactory.<String>builder()
                    .service(SERVICE_COMP_OFF)
                    .topic(TOPIC)
                    .receiveTimeout(Duration.ofMillis(100))
                    .build();
            MessageWriterFactory<String> writerFactory = MessageWriterFactory.<String>builder()
                    .service(SERVICE_COMP_OFF)
                    .topic(TOPIC)
                    .build();

            try (MessageReader<String> reader = readerBuilder.getReader();
                 MessageWriter<String> writer = writerFactory.getWriter()) {
                lines.forEach(writer::write);
                assertIterableEquals(lines, reader.stream().map(Message::getValue).collect(Collectors.toList()));
                Metrics readerMetrics = reader.getMetrics();
                Metrics writerMetrics = writer.getMetrics();
                assert(readerMetrics.getMsgCompressedBytesTotal() == readerMetrics.getMsgUncompressedBytesTotal());
                assert(writerMetrics.getMsgCompressedBytesTotal() == writerMetrics.getMsgUncompressedBytesTotal());
                assert(readerMetrics.getMsgCompressionRatio() == 1);
                assert(writerMetrics.getMsgCompressionRatio() == 1);
                assert(writerMetrics.getMsgCompressedBytesTotal() == readerMetrics.getMsgUncompressedBytesTotal());
            }
        }

        @Test
        void testCompressionOnMetrics() {
            MessageReaderFactory<String> readerBuilder = MessageReaderFactory.<String>builder()
                    .service(SERVICE_COMP_ON)
                    .topic(TOPIC)
                    .receiveTimeout(Duration.ofMillis(100))
                    .build();
            MessageWriterFactory<String> writerFactory = MessageWriterFactory.<String>builder()
                    .service(SERVICE_COMP_ON)
                    .topic(TOPIC)
                    .build();

            try (MessageReader<String> reader = readerBuilder.getReader();
                 MessageWriter<String> writer = writerFactory.getWriter()) {
                lines.forEach(writer::write);
                assertIterableEquals(lines, reader.stream().map(Message::getValue).collect(Collectors.toList()));
                Metrics readerMetrics = reader.getMetrics();
                Metrics writerMetrics = writer.getMetrics();
                assert(readerMetrics.getMsgCompressedBytesTotal() < readerMetrics.getMsgUncompressedBytesTotal());
                assert(writerMetrics.getMsgCompressedBytesTotal() < writerMetrics.getMsgUncompressedBytesTotal());
                assert(readerMetrics.getMsgCompressionRatio() < 1);
                assert(writerMetrics.getMsgCompressionRatio() < 1);
                assert(readerMetrics.getMsgCompressedBytesTotal() == writerMetrics.getMsgCompressedBytesTotal());
                assert(readerMetrics.getMsgUncompressedBytesTotal() == writerMetrics.getMsgUncompressedBytesTotal());
            }
        }

        @BeforeEach
        void setupMessages() {
            lines = IntStream.range(0, 10)
                    .mapToObj(x -> StringUtils.repeat("abc", 10))  // gen compressable
                    .collect(Collectors.toList());
        }
    }
}
