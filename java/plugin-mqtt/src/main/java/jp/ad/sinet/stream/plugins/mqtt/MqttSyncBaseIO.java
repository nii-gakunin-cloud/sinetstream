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

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.SinetStreamException;
import jp.ad.sinet.stream.api.SinetStreamIOException;
import jp.ad.sinet.stream.api.ValueType;
import lombok.extern.java.Log;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Map;

@Log
public class MqttSyncBaseIO extends AbstractMqttIO<MqttClient> {

    MqttSyncBaseIO(String service, Consistency consistency, String clientId, Map<String, ?> config, ValueType valueType, boolean dataEncryption) {
        super(service, consistency, clientId, config, valueType, dataEncryption);
    }

    @Override
    protected MqttClient newMqttClient(String realClientId) {
        try {
            return new MqttClient(getServerURI(), realClientId, getPersistence());
        } catch (MqttException e) {
            throw new SinetStreamException(e);
        }
    }

    @Override
    protected IMqttToken mqttConnect(MqttConnectOptions opts) throws MqttException {
        return client.connectWithResult(opts);
    }

    @Override
    @SuppressWarnings("WeakerAccess")
    public String getClientId() {
        return this.client.getClientId();
    }

    @Override
    protected void doClose() {
        try {
            if (client.isConnected()) {
                log.fine(() -> "Disconnect the broker: " + getClientId());
                client.disconnect();
            }
            log.fine(() -> "close MQTT client: " + getClientId());
            client.close();
        } catch (MqttException e) {
            throw new SinetStreamIOException(e);
        }
    }

    public Object getMetrics() { return null; }
    public void resetMetrics() { }

    public void debugDisconnectForcibly() throws MqttException {
        System.err.println("invoking client.disconnectForcibly(0,0,false)");
        long quiesceTimeout_ms = 0;
        long disconnectTimeout_ms = 0;
        boolean sendDisconnectPacket = false;
        client.disconnectForcibly(quiesceTimeout_ms, disconnectTimeout_ms, sendDisconnectPacket);
    }
}
