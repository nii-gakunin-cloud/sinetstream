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
import jp.ad.sinet.stream.spi.PluginMessageWriter;
import jp.ad.sinet.stream.spi.WriterParameters;
import jp.ad.sinet.stream.utils.Timestamped;
import lombok.Getter;
import lombok.extern.java.Log;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Arrays;

@Log
public class MqttMessageWriter extends MqttSyncBaseIO implements PluginMessageWriter {

    @Getter
    private final String topic;

    MqttMessageWriter(WriterParameters parameters) {
        super(parameters.getService(), parameters.getConsistency(), parameters.getClientId(), parameters.getConfig(),
                parameters.getValueType(), parameters.isDataEncryption());
        this.topic = parameters.getTopic();
        connect();
    }

    @Override
    public void write(Timestamped<byte[]> message) {
        try {
            log.finer(() -> "MQTT publish: " + getClientId() + ": " + Arrays.toString(message.getValue()));
            client.publish(topic, message.getValue(), consistency.getQos(), retain);
        } catch (MqttException e) {
            throw new SinetStreamIOException(e);
        }
    }
}
