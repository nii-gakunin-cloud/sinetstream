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
import jp.ad.sinet.stream.spi.PluginMessageReader;
import jp.ad.sinet.stream.spi.PluginMessageWrapper;
import jp.ad.sinet.stream.spi.ReaderParameters;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Log
public class MqttMessageReader extends MqttSyncBaseIO implements PluginMessageReader, MqttReader {

    @Getter
    private final List<String> topics;
    @Getter
    private final Duration receiveTimeout;

    private final BlockingQueue<SinetMqttMessage> queue;

    MqttMessageReader(ReaderParameters parameters) {
        super(parameters.getService(), parameters.getConsistency(), parameters.getClientId(), parameters.getConfig(),
                parameters.getValueType(), parameters.isDataEncryption());
        this.topics = Collections.unmodifiableList(parameters.getTopics());
        this.receiveTimeout = parameters.getReceiveTimeout();
        this.queue = new LinkedBlockingQueue<>();
        this.client.setCallback(new MqttReaderCallback(this));
        connect();
    }

    @Override
    public void connect() {
        super.connect();
        try {
            int[] qos = new int[this.topics.size()];
            Arrays.fill(qos, this.getConsistency().getQos());
            log.fine(() -> "MQTT subscribe: " + getClientId() + ": " + getTopic());
            this.client.subscribe(this.topics.toArray(new String[0]), qos);
        } catch (MqttException e) {
            throw new SinetStreamIOException(e);
        }
        reconnectDelay = reconnectMinDelay;
    }

    @Override
    public void disconnect() throws MqttException {
        client.disconnect();
    }

    @SneakyThrows(InterruptedException.class)
    @Override
    public void onMessageArrived(SinetMqttMessage message) {
        queue.put(message);
    }

    private String getTopic() {
        return String.join(",", this.topics);
    }

    @Override
    public PluginMessageWrapper read() {
        try {
            return this.queue.poll(receiveTimeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new SinetStreamIOException(e);
        }
    }
}
