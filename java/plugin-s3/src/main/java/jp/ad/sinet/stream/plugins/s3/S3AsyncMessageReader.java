/*
 * Copyright (C) 2022 National Institute of Informatics
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

package jp.ad.sinet.stream.plugins.s3;

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.SinetStreamIOException;
import jp.ad.sinet.stream.spi.PluginAsyncMessageReader;
import jp.ad.sinet.stream.spi.PluginMessageWrapper;
import jp.ad.sinet.stream.spi.ReaderParameters;
import lombok.Getter;
import lombok.extern.java.Log;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Log
public class S3AsyncMessageReader implements PluginAsyncMessageReader {

    @Getter
    private final List<String> topics;
    @Getter
    private final Duration receiveTimeout;
    private final ExecutorService executor;

    S3AsyncMessageReader(ReaderParameters parameters) {
        this.topics = Collections.unmodifiableList(parameters.getTopics());
        this.receiveTimeout = parameters.getReceiveTimeout();
        this.executor = null;
    }

    @Override
    public void addOnMessageCallback(Consumer<PluginMessageWrapper> onMessage, Consumer<Throwable> onFailure) {
/*
        if (Objects.nonNull(onMessage)) {
            onMessageCallbacks.add(onMessage);
        }
        if (Objects.nonNull(onFailure)) {
            onFailureCallbacks.add(onFailure);
        }
*/
    }

    @Override
    public void removeOnMessageCallback(Consumer<PluginMessageWrapper> onMessage, Consumer<Throwable> onFailure) {
/*
        if (Objects.nonNull(onMessage)) {
            onMessageCallbacks.remove(onMessage);
        }
        if (Objects.nonNull(onFailure)) {
            onFailureCallbacks.remove(onFailure);
        }
*/
    }

    @Override
    public void clearOnMessageCallback() {
/*
        onMessageCallbacks.clear();
        onFailureCallbacks.clear();
*/
    }

    @Override
    public String getClientId() { return null; }

    @Override
    public Consistency getConsistency() { return null; }
    @Override
    public Map<String, Object> getConfig() { return null; }

    @Override
    public void close() {}
}
