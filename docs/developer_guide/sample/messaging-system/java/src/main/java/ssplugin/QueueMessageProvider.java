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

package ssplugin;

import jp.ad.sinet.stream.spi.*;

import java.util.concurrent.*;

public class QueueMessageProvider implements MessageReaderProvider, MessageWriterProvider,
        AsyncMessageReaderProvider, AsyncMessageWriterProvider {

    private static final ConcurrentMap<String, BlockingQueue<QueueMessage>> queues = new ConcurrentHashMap<>();
    private static final int QUEUE_SIZE = 10;

    @Override
    public String getType() {
        return "queue";
    }

    @Override
    public PluginMessageReader getReader(ReaderParameters params) {
        String topic = params.getTopics().get(0);
        BlockingQueue<QueueMessage> queue = queues.computeIfAbsent(topic, key -> new ArrayBlockingQueue<>(QUEUE_SIZE));
        return new QueueMessageReader(params, queue);
    }

    @Override
    public PluginMessageWriter getWriter(WriterParameters params) {
        String topic = params.getTopic();
        BlockingQueue<QueueMessage> queue = queues.computeIfAbsent(topic, key -> new ArrayBlockingQueue<>(QUEUE_SIZE));
        return new QueueMessageWriter(params, queue);
    }

    @Override
    public PluginAsyncMessageReader getAsyncReader(ReaderParameters params) {
        String topic = params.getTopics().get(0);
        BlockingQueue<QueueMessage> queue = queues.computeIfAbsent(topic, key -> new ArrayBlockingQueue<>(QUEUE_SIZE));
        return new QueueAsyncMessageReader(params, queue);
    }

    @Override
    public PluginAsyncMessageWriter getAsyncWriter(WriterParameters params) {
        String topic = params.getTopic();
        BlockingQueue<QueueMessage> queue = queues.computeIfAbsent(topic, key -> new ArrayBlockingQueue<>(QUEUE_SIZE));
        return new QueueAsyncMessageWriter(params, queue);
    }
}