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

package jp.ad.sinet.stream.plugins.dummy;

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.spi.*;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.Value;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

public class DummyMessageProvider implements MessageWriterProvider, MessageReaderProvider {
    @Override
    public PluginMessageWriter getWriter(WriterParameters params) {
        return new DummyWriter(params);
    }

    @Override
    public PluginMessageReader getReader(ReaderParameters params) {
        return new DummyReader(params);
    }

    @Override
    public String getType() {
        return "dummy";
    }

    private static final ConcurrentMap<String, BlockingQueue<byte[]>> topicQueue = new ConcurrentHashMap<>();

    public static BlockingQueue<byte[]> getQueue(String topic) {
        return topicQueue.get(topic);
    }
    public static void setQueue(String topic, BlockingQueue<byte[]> queue) {
        topicQueue.put(topic, queue);
    }

    @Data
    private class DummyIO {
        String service;
        Consistency consistency;
        String clientId;
        Map<String, Object> config;
        String topic;

        public void close() {
        }

        DummyIO(WriterParameters params) {
            service = params.getService();
            consistency = params.getConsistency();
            clientId = params.getClientId();
            config = Collections.unmodifiableMap(params.getConfig());
            topic = params.getTopic();
        }

        DummyIO(ReaderParameters params) {
            service = params.getService();
            consistency = params.getConsistency();
            clientId = params.getClientId();
            config = Collections.unmodifiableMap(params.getConfig());
            topic = String.join(",", params.getTopics());
        }

        public Object getMetrics(boolean reset) { return "this is a dummy metrics"; }
    }

    public class DummyWriter extends DummyIO implements PluginMessageWriter {

        private final BlockingQueue<byte[]> queue;

        public DummyWriter(WriterParameters params) {
            super(params);
            topicQueue.putIfAbsent(topic, new LinkedBlockingQueue<>());
            this.queue = topicQueue.get(topic);
        }

        @SneakyThrows
        @Override
        public void write(byte[] message) {
            queue.put(message);
        }
    }

    public class DummyReader extends DummyIO implements PluginMessageReader {

        @Getter
        private final List<String> topics;
        private final BlockingQueue<byte[]> queue;
        @Getter
        private Duration receiveTimeout;

        public DummyReader(ReaderParameters params) {
            super(params);
            topics = params.getTopics();
            receiveTimeout = params.getReceiveTimeout();
            topicQueue.putIfAbsent(topic, new LinkedBlockingQueue<>());
            queue = topicQueue.get(topic);
        }

        @SneakyThrows
        @Override
        public PluginMessageWrapper read() {
            byte[] bytes = queue.poll(receiveTimeout.toNanos(), TimeUnit.NANOSECONDS);
            if (Objects.isNull(bytes)) {
                return null;
            }
            return new DummyMessage(bytes, topics.get(0), bytes);
        }

        @Value
        private class DummyMessage implements PluginMessageWrapper {
            byte[] value;
            String topic;
            Object raw;
        }

        public String getGroupId() {
            return (String) config.get("group_id");
        }
    }
}
