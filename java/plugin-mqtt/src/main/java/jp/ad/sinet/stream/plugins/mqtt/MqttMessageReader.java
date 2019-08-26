/*
 * Copyright (C) 2019 National Institute of Informatics
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

import jp.ad.sinet.stream.api.Deserializer;
import jp.ad.sinet.stream.api.Message;
import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.SinetStreamIOException;
import jp.ad.sinet.stream.crypto.CryptoDeserializerWrapper;
import jp.ad.sinet.stream.spi.ReaderParameters;
import lombok.Getter;
import lombok.extern.java.Log;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Log
public class MqttMessageReader<T> extends MqttBaseIO implements MessageReader<T> {

    @Getter
    private final List<String> topics;
    @Getter
    private final Duration receiveTimeout;

    private final BlockingQueue<MqttMessageTuple> queue;

    @Getter
    private final Deserializer<T> deserializer;

    @SuppressWarnings("unchecked")
    MqttMessageReader(ReaderParameters<T> parameters) {
        super(parameters.getService(), parameters.getConsistency(), parameters.getClientId(), parameters.getConfig(),
                parameters.getValueType(), parameters.isDataEncryption());
        this.topics = Collections.unmodifiableList(parameters.getTopics());
        this.receiveTimeout = parameters.getReceiveTimeout();
        this.queue = new LinkedBlockingQueue<>();

        Deserializer<T> des;
        if (Objects.isNull(parameters.getDeserializer())) {
            des = this.getValueType().getDeserializer();
        } else {
            des = parameters.getDeserializer();
        }
        deserializer = CryptoDeserializerWrapper.getDeserializer(config, des);

        this.client.setCallback(new SinetMqttCallback());
        connect();
    }

    @Override
    void connect() {
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
    public String getTopic() {
        return String.join(",", this.topics);
    }

    @Override
    public Message<T> read() {
        try {
            MqttMessageTuple tp = this.queue.poll(receiveTimeout.toNanos(), TimeUnit.NANOSECONDS);
            if (Objects.isNull(tp)) {
                return null;
            }
            return new SinetMqttMessage<>(tp.getTopic(), tp.getMessage(), this.deserializer::deserialize);
        } catch (InterruptedException e) {
            throw new SinetStreamIOException(e);
        }
    }

    private class SinetMqttCallback implements MqttCallback {
        @Override
        public void connectionLost(Throwable cause) {
            log.log(Level.FINE, cause, () -> "MQTT connection lost: " + getClientId());
            if (!connectOptions.isAutomaticReconnect()) {
                return;
            }
            Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                updateReconnectDelay();
                disconnectClient();
                connect();
            }, reconnectDelay, TimeUnit.SECONDS);
        }

        private void disconnectClient() {
            try {
                log.fine(() -> "Disconnect the broker: " + getClientId());
                MqttMessageReader.this.client.disconnect();
            } catch (MqttException e) {
                log.log(Level.FINER, e, () -> "MQTT disconnect ERROR: " + getClientId());
            }
        }

        private void updateReconnectDelay() {
            if (reconnectDelay < connectOptions.getMaxReconnectDelay()) {
                reconnectDelay *= 2;
            }
            if (reconnectDelay > connectOptions.getMaxReconnectDelay()) {
                reconnectDelay = connectOptions.getMaxReconnectDelay();
            }
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            log.finer(() -> "MQTT message arrived: " + getClientId() + ": " + message.toString());
            queue.put(new MqttMessageTuple(topic, message));
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            log.finest(() -> "MQTT delivery completed: " + token.toString());
        }
    }

    private static class MqttMessageTuple {
        @Getter
        private final String topic;
        @Getter
        private final MqttMessage message;
        MqttMessageTuple(String topic, MqttMessage message) {
            this.topic = topic;
            this.message = message;
        }
    }
}