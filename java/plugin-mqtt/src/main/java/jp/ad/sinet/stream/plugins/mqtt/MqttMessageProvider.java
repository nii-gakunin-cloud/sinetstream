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

import jp.ad.sinet.stream.spi.*;
import lombok.extern.java.Log;

import java.util.Map;

@Log
public class MqttMessageProvider implements MessageWriterProvider, MessageReaderProvider, AsyncMessageWriterProvider, AsyncMessageReaderProvider {

    private static <T> T safeCast(Object o, Class<T> cls) {
        return o != null && cls.isInstance(o) ? cls.cast(o) : null;
    }

    @Override
    public PluginMessageWriter getWriter(WriterParameters params) {
        log.fine(() -> "MQTT getWriter: " + params.toString());
        return new MqttMessageWriter(params);
    }

    @Override
    public PluginMessageReader getReader(ReaderParameters params) {
        log.fine(() -> "MQTT getReader: " + params.toString());
        return new MqttMessageReader(params);
    }

    @Override
    public PluginAsyncMessageWriter getAsyncWriter(WriterParameters params) {
        log.fine(() -> "MQTT getAsyncWriter: " + params.toString());
        return new MqttAsyncMessageWriter(params);
    }

    @Override
    public PluginAsyncMessageReader getAsyncReader(ReaderParameters params) {
        log.fine(() -> "MQTT getAsyncReader: " + params.toString());
        return new MqttAsyncMessageReader(params);
    }

    @Override
    public String getType() {
        return "mqtt";
    }

    @Override
    public boolean isProvider(Map<String, ?> params) {
        String protocol = safeCast(params.get("protocol"), String.class);
        try {
            log.fine(() -> "MqttMessageReader.isProvider -> " + (protocol == null || MqttVersion.valueOf(protocol) != null));
            return protocol == null || MqttVersion.valueOf(protocol) != null;
        }
        catch (IllegalArgumentException e) {
            return false;
        }
    }
}
