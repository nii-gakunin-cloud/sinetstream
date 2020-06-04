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

import jp.ad.sinet.stream.utils.MessageUtils;
import org.jdeferred2.Promise;
import org.jdeferred2.impl.DefaultDeferredManager;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class SinetStreamAsyncWrapperMessageWriter<T> implements AsyncMessageWriter<T> {

    private final MessageWriter<T> syncWriter;
    private final DefaultDeferredManager manager;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public SinetStreamAsyncWrapperMessageWriter(MessageWriter<T> writer) {
        syncWriter = writer;
        int nthreads = MessageUtils.toInteger(writer.getConfig().getOrDefault("thread_pool_num", 10));
        ExecutorService executor =
                syncWriter.isThreadSafe() ?
                        Executors.newFixedThreadPool(nthreads) :
                        Executors.newSingleThreadExecutor();
        manager = new DefaultDeferredManager(executor);
    }

    @Override
    public Promise<Void, Throwable, Void> write(T message) {
        return manager.when(() -> syncWriter.write(message));
    }

    @Override
    public void close() {
        if (!closed.getAndSet(true)) {
            manager.shutdown();
            syncWriter.close();
        }
    }

    @Override
    public Serializer<T> getSerializer() {
        return syncWriter.getSerializer();
    }

    @Override
    public String getService() {
        return syncWriter.getService();
    }

    @Override
    public String getTopic() {
        return syncWriter.getTopic();
    }

    @Override
    public Consistency getConsistency() {
        return syncWriter.getConsistency();
    }

    @Override
    public String getClientId() {
        return syncWriter.getClientId();
    }

    @Override
    public Map<String, Object> getConfig() {
        return syncWriter.getConfig();
    }

    @Override
    public ValueType getValueType() {
        return syncWriter.getValueType();
    }

    @Override
    public boolean isDataEncryption() {
        return syncWriter.isDataEncryption();
    }
}
