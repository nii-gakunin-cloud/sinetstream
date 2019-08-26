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

import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.api.Serializer;
import jp.ad.sinet.stream.api.SinetStreamIOException;
import jp.ad.sinet.stream.crypto.CryptoSerializerWrapper;
import jp.ad.sinet.stream.spi.WriterParameters;
import lombok.Getter;
import lombok.extern.java.Log;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;

@Log
public class MqttMessageWriter<T> extends MqttBaseIO implements MessageWriter<T> {

    @Getter
    private final String topic;

    @Getter
    private final Serializer<T> serializer;

    @SuppressWarnings("unchecked")
    MqttMessageWriter(WriterParameters<T> parameters) {
        super(parameters.getService(), parameters.getConsistency(), parameters.getClientId(), parameters.getConfig(),
                parameters.getValueType(), parameters.isDataEncryption());
        this.topic = parameters.getTopic();

        Serializer<T> ser;
        if (Objects.isNull(parameters.getSerializer())) {
            ser = valueType.getSerializer();
        } else {
            ser = parameters.getSerializer();
        }
        serializer = CryptoSerializerWrapper.getSerializer(config, ser);
        this.client.setCallback(new MqttMessageWriter.SinetMqttCallback());
        connect();
    }

    @Override
    public void write(T message) {
        try {
            byte[] payload = serializer.serialize(message);
            log.finer(() -> "MQTT publish: " + getClientId() + ": " + Arrays.toString(payload));
            client.publish(topic, payload, consistency.getQos(), retain);
        } catch (MqttException e) {
            throw new SinetStreamIOException(e);
        }
    }

    private class SinetMqttCallback implements MqttCallback {
        @Override
        public void connectionLost(Throwable cause) {
            log.log(Level.FINE, cause, () -> "MQTT connection lost: " + getClientId());
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            log.finest(() -> "MQTT delivery completed: " + token.toString());
        }
    }
}