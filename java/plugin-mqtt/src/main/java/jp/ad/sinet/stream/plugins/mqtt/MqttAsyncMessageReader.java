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

package jp.ad.sinet.stream.plugins.mqtt;

import jp.ad.sinet.stream.api.SinetStreamIOException;
import jp.ad.sinet.stream.spi.PluginAsyncMessageReader;
import jp.ad.sinet.stream.spi.PluginMessageWrapper;
import jp.ad.sinet.stream.spi.ReaderParameters;
import lombok.Getter;
import lombok.extern.java.Log;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

@Log
public class MqttAsyncMessageReader extends MqttAsyncBaseIO implements PluginAsyncMessageReader, MqttReader {
    private final Lock lock = new ReentrantLock();
    private final Condition connectionChanged = lock.newCondition();
    private boolean connected = false;

    @Getter
    private final List<String> topics;
    @Getter
    private final Duration receiveTimeout;
    private final ExecutorService executor;

    private List<Consumer<PluginMessageWrapper>> onMessageCallbacks = new CopyOnWriteArrayList<>();
    private List<Consumer<Throwable>> onFailureCallbacks = new CopyOnWriteArrayList<>();

    MqttAsyncMessageReader(ReaderParameters parameters) {
        super(parameters.getService(), parameters.getConsistency(), parameters.getClientId(), parameters.getConfig(),
                parameters.getValueType(), parameters.isDataEncryption());
        this.topics = Collections.unmodifiableList(parameters.getTopics());
        this.receiveTimeout = parameters.getReceiveTimeout();
        this.executor = Executors.newSingleThreadExecutor();
        this.client.setCallback(new MqttReaderCallback(this));
        connect();
    }

    @Override
    protected void doClose() {
        super.doClose();
        executor.shutdown();
    }

    @Override
    public void onMessageArrived(SinetMqttMessage message) {
        for (Consumer<PluginMessageWrapper> action : onMessageCallbacks) {
            executor.submit(() -> action.accept(message));
        }
    }

    @Override
    public void onConnectionLost(Throwable cause) {
        for (Consumer<Throwable> action : onFailureCallbacks) {
            executor.submit(() -> action.accept(cause));
        }
    }

    @Override
    public void connect() {
        super.connect();

        lock.lock();
        try {
            while (connected != true)
                connectionChanged.awaitUninterruptibly();
                //connectionChanged.await();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void onConnectionComplete(boolean reconnect, String serverURI) {
        subscribe();

        lock.lock();
        try {
            connected = true;
            connectionChanged.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private void subscribe() {
        try {
            int[] qos = new int[this.topics.size()];
            Arrays.fill(qos, this.getConsistency().getQos());
            log.fine(() -> "MQTT subscribe: " + getClientId() + ": " + getTopic());
            this.client.subscribe(this.topics.toArray(new String[0]), qos).waitForCompletion();
        } catch (MqttException e) {
            throw new SinetStreamIOException(e);
        }
    }

    @Override
    public void disconnect() throws MqttException {
        client.disconnect().waitForCompletion();

        lock.lock();
        try {
            connected = false;
            connectionChanged.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public String getTopic() {
        return String.join(",", this.topics);
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
}
