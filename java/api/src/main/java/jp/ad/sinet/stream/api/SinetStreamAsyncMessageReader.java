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

package jp.ad.sinet.stream.api;

import jp.ad.sinet.stream.spi.PluginAsyncMessageReader;
import jp.ad.sinet.stream.spi.PluginMessageWrapper;
import jp.ad.sinet.stream.spi.ReaderParameters;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SinetStreamAsyncMessageReader<T> extends SinetStreamBaseReader<T, PluginAsyncMessageReader> implements AsyncMessageReader<T> {

    private Map<Consumer<Message<T>>, Consumer<PluginMessageWrapper>> onMessageCallbacks = new HashMap<>();
    private Map<Consumer<Throwable>, Consumer<Throwable>> onFailureCallbacks = new HashMap<>();

    public SinetStreamAsyncMessageReader(PluginAsyncMessageReader pluginReader, ReaderParameters parameters, Deserializer<T> deserializer) {
        super(pluginReader, parameters, deserializer);
    }

    @Override
    public synchronized void addOnMessageCallback(Consumer<Message<T>> onMessage, Consumer<Throwable> onFailure) {
        Consumer<PluginMessageWrapper> callback = null;
        if (!onMessageCallbacks.containsKey(onMessage)) {
            callback = (msg) -> onMessage.accept(toMessage(msg));
            onMessageCallbacks.put(onMessage, callback);
        }
        Consumer<Throwable> callback2 = null;
        if (!onFailureCallbacks.containsKey(onFailure)) {
            callback2 = (e) -> {
                this.updateMetricsErr();
                onFailure.accept(e);
            };
            onFailureCallbacks.put(onFailure, callback2);
        }
        target.addOnMessageCallback(callback, callback2);
    }

    @Override
    public synchronized void removeOnMessageCallback(Consumer<Message<T>> onMessage, Consumer<Throwable> onFailure) {
        Consumer<PluginMessageWrapper> callback = onMessageCallbacks.remove(onMessage);
        Consumer<Throwable> callback2 = onFailureCallbacks.remove(onFailure);
        target.removeOnMessageCallback(callback, callback2);
    }

    @Override
    public synchronized void clearOnMessageCallback() {
        onMessageCallbacks.clear();
        onFailureCallbacks.clear();
        target.clearOnMessageCallback();
    }
}
