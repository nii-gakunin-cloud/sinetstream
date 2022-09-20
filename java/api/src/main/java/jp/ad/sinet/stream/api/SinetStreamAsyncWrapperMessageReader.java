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

package jp.ad.sinet.stream.api;

import org.jdeferred2.Promise;
import org.jdeferred2.impl.DefaultDeferredManager;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class SinetStreamAsyncWrapperMessageReader<T> implements AsyncMessageReader<T> {
    private final MessageReader<T> syncReader;
    private final DefaultDeferredManager manager;
    private List<Consumer<Message<T>>> onMessageCallbacks = new CopyOnWriteArrayList<>();
    private List<Consumer<Throwable>> onFailureCallbacks = new CopyOnWriteArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public SinetStreamAsyncWrapperMessageReader(MessageReader<T> reader) {
        syncReader = reader;
        manager = new DefaultDeferredManager(Executors.newSingleThreadExecutor());
        Promise<Void, Throwable, Void> promise = manager.when(() -> {
            Message<T> msg;
            try {
                while (Objects.nonNull(msg = syncReader.read())) {
                    for (Consumer<Message<T>> callback : onMessageCallbacks) {
                        callback.accept(msg);
                    }
                }
            } catch (Throwable ex) {
                for (Consumer<Throwable> callback : onFailureCallbacks) {
                    callback.accept(ex);
                }
            }
        });
    }

    @Override
    public void close() {
        if (!closed.getAndSet(true)) {
            syncReader.close();
            manager.shutdown();
        }
    }

    @Override
    public void addOnMessageCallback(Consumer<Message<T>> onMessage, Consumer<Throwable> onFailure) {
        if (Objects.nonNull(onMessage)) {
            onMessageCallbacks.add(onMessage);
        }
        if (Objects.nonNull(onFailure)) {
            onFailureCallbacks.add(onFailure);
        }
    }

    @Override
    public void removeOnMessageCallback(Consumer<Message<T>> onMessage, Consumer<Throwable> onFailure) {
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
        return syncReader.getTopics();
    }

    @Override
    public Deserializer<T> getDeserializer() {
        return syncReader.getDeserializer();
    }

    @Override
    public String getService() {
        return syncReader.getService();
    }

    @Override
    public String getTopic() {
        return syncReader.getTopic();
    }

    @Override
    public Consistency getConsistency() {
        return syncReader.getConsistency();
    }

    @Override
    public String getClientId() {
        return syncReader.getClientId();
    }

    @Override
    public Map<String, Object> getConfig() {
        return syncReader.getConfig();
    }

    @Override
    public ValueType getValueType() {
        return syncReader.getValueType();
    }

    @Override
    public boolean isDataEncryption() {
        return syncReader.isDataEncryption();
    }

    @Override
    public Metrics getMetrics() {
        return syncReader.getMetrics();
    }

    @Override
    public void resetMetrics(boolean reset_raw) {
        syncReader.resetMetrics(reset_raw);
    }

    @Override
    public Object getInfo(String ipath) {
        return syncReader.getInfo(ipath);
    }
}
