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
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Map;

@Log
public class MqttAsyncBaseIO extends AbstractMqttIO<MqttAsyncClient> {

    MqttAsyncBaseIO(String service, Consistency consistency, String clientId, Map<String, ?> config, ValueType valueType, boolean dataEncryption) {
        super(service, consistency, clientId, config, valueType, dataEncryption);
    }

    @Override
    protected MqttAsyncClient newMqttClient(String realClientId) {
        try {
            return new MqttAsyncClient(getServerURI(), realClientId, getPersistence());
        } catch (MqttException e) {
            throw new SinetStreamException(e);
        }
    }

    @Override
    protected IMqttToken mqttConnect(MqttConnectOptions opts) throws MqttException {
        IMqttToken ret = client.connect(connectOptions);
        ret.waitForCompletion();
        return ret;
    }

    @Override
    @SuppressWarnings("WeakerAccess")
    public String getClientId() {
        return this.client.getClientId();
    }

    @Override
    protected void doClose() {
        try {
            if (this.client.isConnected()) {
                log.fine(() -> "Disconnect the broker: " + getClientId());
                this.client.disconnect().waitForCompletion();
            }
            log.fine(() -> "close MQTT client: " + getClientId());
            this.client.close();
        } catch (MqttException e) {
            throw new SinetStreamIOException(e);
        }
    }

    public Object getMetrics() { return null; }
    public void resetMetrics() { }

    public void debugDisconnectForcibly() throws MqttException {
        long quiesceTimeout_ms = 0;
        long disconnectTimeout_ms = 0;
        boolean sendDisconnectPacket = false;
        client.disconnectForcibly(quiesceTimeout_ms, disconnectTimeout_ms, sendDisconnectPacket);
    }
}
