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

package jp.ad.sinet.stream.metrics;

import jp.ad.sinet.stream.ConfigFileAware;
import jp.ad.sinet.stream.api.Message;
import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.api.Metrics;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class MessageReaderTest implements ConfigFileAware {

    private static final String SERVICE = "service-0";
    private static final String TOPIC = "topic-" + RandomStringUtils.randomAlphabetic(5);
    private List<String> lines;

    @Test
    void readerStream() {
        MessageReaderFactory<String> readerBuilder =
                MessageReaderFactory.<String>builder().service(SERVICE).topic(TOPIC)
                        .receiveTimeout(Duration.ofMillis(100)).build();
        MessageWriterFactory<String> writerFactory =
                MessageWriterFactory.<String>builder().service(SERVICE).topic(TOPIC).build();

        try (MessageReader<String> reader = readerBuilder.getReader();
             MessageWriter<String> writer = writerFactory.getWriter()) {
            //System.err.println("XXX:TEST " + reader.getMetrics().toString());
            assertNotNull(reader.getMetrics().toString());
            assertNotNull(writer.getMetrics().toString());
            assertEquals(0, writer.getMetrics().getMsgCountRate());
            assertEquals(0, writer.getMetrics().getMsgBytesRate());
            assertEquals(0, writer.getMetrics().getMsgSizeAvg());
            assertEquals(0, writer.getMetrics().getErrorCountRate());
            lines.forEach(writer::write);
            assertIterableEquals(lines, reader.stream().map(Message::getValue).collect(Collectors.toList()));
            Metrics mr = reader.getMetrics();
            Metrics mw = writer.getMetrics();
            //System.out.println("mr=" + mr);
            //System.out.println("mw=" + mw);
            assert(mr.getStartTime() > 0);
            assert(mr.getEndTime() > 0);
            assert(mr.getStartTimeMillis() > 0);
            assert(mr.getEndTimeMillis() > 0);
            assertEquals(mr.getStartTime() * 1000, mr.getStartTimeMillis());
            assertEquals(mr.getEndTime() * 1000, mr.getEndTimeMillis());
            assertEquals(mr.getTime() * 1000, mr.getTimeMillis());
            assertEquals(mr.getMsgCountTotal(), lines.size());
            assertEquals(mw.getMsgCountTotal(), lines.size());
            assertEquals(mr.getRaw(), "this is a dummy metrics");
            assertEquals(mw.getRaw(), "this is a dummy metrics");
            Metrics mr2 = reader.getMetrics(); reader.resetMetrics();
            Metrics mw2 = writer.getMetrics(); writer.resetMetrics();
            Metrics mr3 = reader.getMetrics(); reader.resetMetrics();
            Metrics mw3 = writer.getMetrics(); writer.resetMetrics();
            assertEquals(mr2.getMsgCountTotal(), mr.getMsgCountTotal());
            assertEquals(mw2.getMsgCountTotal(), mw.getMsgCountTotal());
            assertEquals(mr3.getMsgCountTotal(), 0);
            assertEquals(mw3.getMsgCountTotal(), 0);
        }
    }

    @Test
    void readerStreamErr() {
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("test_dummy_reader_read_fail", true);
        MessageReaderFactory<String> readerBuilder =
                MessageReaderFactory.<String>builder().service(SERVICE).topic(TOPIC)
                        .receiveTimeout(Duration.ofMillis(100)).parameters(parameters).build();
        MessageWriterFactory<String> writerFactory =
                MessageWriterFactory.<String>builder().service(SERVICE).topic(TOPIC).build();

        try (MessageReader<String> reader = readerBuilder.getReader();
             MessageWriter<String> writer = writerFactory.getWriter()) {
            lines.forEach(writer::write);

            boolean caught = false;
            try {
                Message<String> msg;
                while ((msg = reader.read()) != null) {
                    //System.out.println(msg.getValue());
                }
            }
            catch (Exception e) {
                caught = true;
            }
            assertTrue(caught);

            Metrics mr = reader.getMetrics();
            Metrics mw = writer.getMetrics();
            //System.out.println("mr=" + mr);
            //System.out.println("mw=" + mw);
            assertEquals(mr.getMsgCountTotal(), 0);
            assertEquals(mw.getMsgCountTotal(), lines.size());
            assertEquals(mr.getRaw(), "this is a dummy metrics");
            assertEquals(mw.getRaw(), "this is a dummy metrics");
            assertEquals(mr.getErrorCountTotal(), 1);
        }
    }

    @BeforeEach
    void setupMessages() {
        lines = IntStream.range(0, 10)
                .mapToObj(x -> RandomStringUtils.randomAlphabetic(10))
                .collect(Collectors.toList());
    }
}
