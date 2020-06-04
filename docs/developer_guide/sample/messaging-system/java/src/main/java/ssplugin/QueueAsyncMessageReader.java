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

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.spi.PluginAsyncMessageReader;
import jp.ad.sinet.stream.spi.PluginMessageWrapper;
import jp.ad.sinet.stream.spi.ReaderParameters;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class QueueAsyncMessageReader implements PluginAsyncMessageReader {

    private final BlockingQueue<QueueMessage> queue;
    private final List<String> topics;
    private final Map<String, Object> config;
    private final Consistency consistency;
    private final String clientId;
    private final Future<?> future;
    private ExecutorService executor;
    private List<Consumer<PluginMessageWrapper>> onMessageCallbacks = new CopyOnWriteArrayList<>();
    private List<Consumer<Throwable>> onFailureCallbacks = new CopyOnWriteArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public QueueAsyncMessageReader(ReaderParameters params, BlockingQueue<QueueMessage> queue) {
        this.queue = queue;
        this.topics = Collections.unmodifiableList(params.getTopics());
        this.config = Collections.unmodifiableMap(params.getConfig());
        this.consistency = params.getConsistency();
        this.clientId = params.getClientId();
        executor = Executors.newSingleThreadExecutor();
        future = executor.submit(this::pollingTask);
    }

    private void pollingTask() {
        try {
            while (!closed.get()) {
                onMessage(queue.take());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void onMessage(PluginMessageWrapper message) {
        for (Consumer<PluginMessageWrapper> callback : onMessageCallbacks) {
            try {
                callback.accept(message);
            } catch (Throwable ex) {
                onFailure(ex);
            }
        }
    }

    private void onFailure(Throwable ex) {
        for (Consumer<Throwable> callback : onFailureCallbacks) {
            try {
                callback.accept(ex);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() {
        if (!closed.getAndSet(true)) {
            future.cancel(true);
            executor.shutdown();
        }
    }

    @Override
    public void addOnMessageCallback(Consumer<PluginMessageWrapper> onMessage, Consumer<Throwable> onFailure) {
        if (Objects.nonNull(onMessage)) {
            onMessageCallbacks.add(onMessage);
        }
        if (Objects.nonNull(onFailure)) {
            onFailureCallbacks.add(onFailure);
        }
    }

    @Override
    public void removeOnMessageCallback(Consumer<PluginMessageWrapper> onMessage, Consumer<Throwable> onFailure) {
        if (Objects.nonNull(onMessage)) {
            onMessageCallbacks.remove(onMessage);
        }
        if (Objects.nonNull(onFailure)) {
            onFailureCallbacks.remove(onFailure);
        }
    }

    @Override
    public void clearOnMessageCallback() {
        onMessageCallbacks.clear();
        onFailureCallbacks.clear();
    }

    @Override
    public List<String> getTopics() {
        return topics;
    }

    @Override
    public Map<String, Object> getConfig() {
        return config;
    }

    @Override
    public Consistency getConsistency() {
        return consistency;
    }

    @Override
    public String getClientId() {
        return clientId;
    }
}
