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

import lombok.extern.java.Log;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Log
public class MqttReaderCallback implements MqttCallback {

    private final MqttReader reader;

    public MqttReaderCallback(MqttReader reader) {
        this.reader = reader;
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.log(Level.FINE, cause, () -> "MQTT connection lost: " + reader.getClientId());
        if (!reader.getConnectOptions().isAutomaticReconnect()) {
            reader.onConnectionLost(cause);
            return;
        }
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            updateReconnectDelay();
            disconnectClient();
            reader.connect();
        }, reader.getReconnectDelay(), TimeUnit.SECONDS);
    }

    private void disconnectClient() {
        try {
            log.fine(() -> "Disconnect the broker: " + reader.getClientId());
            reader.disconnect();
        } catch (MqttException e) {
            log.log(Level.FINER, e, () -> "MQTT disconnect ERROR: " + reader.getClientId());
        }
    }

    private void updateReconnectDelay() {
        if (reader.getReconnectDelay() < reader.getConnectOptions().getMaxReconnectDelay()) {
            reader.setReconnectDelay(reader.getReconnectDelay() * 2);
        }
        if (reader.getReconnectDelay() > reader.getConnectOptions().getMaxReconnectDelay()) {
            reader.setReconnectDelay(reader.getConnectOptions().getMaxReconnectDelay());
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        log.finer(() -> "MQTT message arrived: " + reader.getClientId() + ": " + message.toString());
        reader.onMessageArrived(new SinetMqttMessage(topic, message));
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        log.finest(() -> "MQTT delivery completed: " + token.toString());
    }
}
