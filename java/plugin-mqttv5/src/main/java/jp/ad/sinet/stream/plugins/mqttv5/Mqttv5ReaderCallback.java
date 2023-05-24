/*
 * Copyright (C) 2023 National Institute of Informatics
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

package jp.ad.sinet.stream.plugins.mqttv5;

import lombok.extern.java.Log;
import org.eclipse.paho.mqttv5.client.IMqttDeliveryToken;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Log
public class Mqttv5ReaderCallback implements MqttCallback {

    private final Mqttv5Reader reader;

    public Mqttv5ReaderCallback(Mqttv5Reader reader) {
        this.reader = reader;
    }

    /* XXX なぜここまでつくりこむのか... たぶんpahoのautomaticreconnectをちゃんとつかえるようにしたらいける
    @Override
    public void connectionLost(Throwable cause) {
        log.log(Level.FINE, cause, () -> "MQTT connection lost: " + reader.getClientId());
        if (!reader.getConnectionOptions().isAutomaticReconnect()) {
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
        if (reader.getReconnectDelay() < reader.getConnectionOptions().getMaxReconnectDelay()) {
            reader.setReconnectDelay(reader.getReconnectDelay() * 2);
        }
        if (reader.getReconnectDelay() > reader.getConnectionOptions().getMaxReconnectDelay()) {
            reader.setReconnectDelay(reader.getConnectionOptions().getMaxReconnectDelay());
        }
    }
    */

    @Override
    public void disconnected(MqttDisconnectResponse disconnectResponse) {
        log.finest(() -> "MQTT disconnected: disconnectResponse=" + disconnectResponse);
        if (!reader.getConnectionOptions().isAutomaticReconnect()) {
            MqttException e = disconnectResponse.getException();
            reader.onConnectionLost(e != null ? e.getCause() : null);
            return;
        }
    }

    @Override
    public void mqttErrorOccurred(MqttException exception) {
        log.finest(() -> "MQTT error occurred: exception=" + exception);
        /*XXX*/
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        log.finer(() -> "MQTT message arrived: clientId=" + reader.getClientId() + ": message=" + message.toString());
        reader.onMessageArrived(new SinetMqttv5Message(topic, message));
    }

    @Override
    public void deliveryComplete(IMqttToken token) {
        log.finest(() -> "MQTT delivery complete: token=" + token);
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        log.finest(() -> "MQTT connect complete: reconnect=" + reconnect + " serverURI=" + serverURI);
        reader.subscribe();
    }

    // NOTE: enhanced authentication is not supported
    @Override
    public void authPacketArrived(int reasonCode, MqttProperties properties) {
        log.warning(() -> "MQTT auth packet arrived: reasonCode=" + reasonCode + " properties=" + properties);
    }
}
