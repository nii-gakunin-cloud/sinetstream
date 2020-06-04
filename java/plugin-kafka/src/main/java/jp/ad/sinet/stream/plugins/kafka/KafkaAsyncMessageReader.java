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

import jp.ad.sinet.stream.api.SinetStreamException;
import jp.ad.sinet.stream.spi.PluginAsyncMessageReader;
import jp.ad.sinet.stream.spi.PluginMessageWrapper;
import jp.ad.sinet.stream.spi.ReaderParameters;
import lombok.extern.java.Log;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;

@Log
public class KafkaAsyncMessageReader extends KafkaBaseReader implements PluginAsyncMessageReader {

    private ExecutorService callbackExecutor;
    private List<Consumer<PluginMessageWrapper>> onMessageCallbacks = new CopyOnWriteArrayList<>();
    private List<Consumer<Throwable>> onFailureCallbacks = new CopyOnWriteArrayList<>();

    KafkaAsyncMessageReader(ReaderParameters parameters) {
        super(parameters);
    }

    @Override
    protected void submitConsumerLoop() {
        if (Objects.isNull(callbackExecutor) || callbackExecutor.isShutdown()) {
            callbackExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            });
        }
        super.submitConsumerLoop();
    }

    @Override
    protected void append_consumer_records(ConsumerRecords<String, byte[]> records) {
        for (ConsumerRecord<String, byte[]> rec : records) {
            log.finer(() -> "KAFKA message poll: " + getClientId() + ": " + rec.toString());
            final KafkaMessage msg = new KafkaMessage(rec);
            for (Consumer<PluginMessageWrapper> action : onMessageCallbacks) {
                callbackExecutor.submit(() -> action.accept(msg));
            }
        }
    }

    @Override
    protected void append_exception(Throwable ex) {
        log.log(Level.FINE, "Kafka reader error!", ex);
        SinetStreamException e = wrapSinetStreamException(ex);
        for (Consumer<Throwable> action : onFailureCallbacks) {
            callbackExecutor.submit(() -> action.accept(e));
        }
    }

    @Override
    protected void stopPollingWorker() {
        super.stopPollingWorker();
        if (Objects.nonNull(callbackExecutor) && !callbackExecutor.isShutdown()) {
            callbackExecutor.shutdownNow();
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
        if (!(onMessageCallbacks.isEmpty() && onFailureCallbacks.isEmpty())) {
            startPollingWorker();
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
        if (onMessageCallbacks.isEmpty() && onFailureCallbacks.isEmpty()) {
            stopPollingWorker();
        }
    }

    @Override
    public void clearOnMessageCallback() {
        onMessageCallbacks.clear();
        onFailureCallbacks.clear();
        stopPollingWorker();
    }
}
