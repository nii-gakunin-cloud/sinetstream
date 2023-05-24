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

import jp.ad.sinet.stream.spi.PluginMessageWrapper;
import jp.ad.sinet.stream.utils.Timestamped;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.eclipse.paho.mqttv5.common.MqttMessage;

@EqualsAndHashCode
@ToString
class SinetMqttv5Message implements PluginMessageWrapper {
    @Getter
    private final String topic;
    @Getter
    private final MqttMessage raw;

    @SuppressWarnings("WeakerAccess")
    SinetMqttv5Message(String topic, MqttMessage message) {
        this.topic = topic;
        this.raw = message;
    }

    @Override
    public Timestamped<byte[]> getValue() {
        return new Timestamped<byte[]>(raw.getPayload(), 0);
    }
}
