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
package ssplugin;

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.SinetStreamIOException;
import jp.ad.sinet.stream.api.ValueType;
import jp.ad.sinet.stream.spi.PluginMessageWriter;
import jp.ad.sinet.stream.spi.WriterParameters;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class QueueMessageWriter implements PluginMessageWriter {
    private final BlockingQueue<QueueMessage> queue;
    private final String topic;
    private final Map<String, Object> config;
    private final Consistency consistency;
    private final String clientId;

    public QueueMessageWriter(WriterParameters params, BlockingQueue<QueueMessage> queue) {
        this.queue = queue;
        this.topic = params.getTopic();
        this.config = Collections.unmodifiableMap(params.getConfig());
        this.consistency = params.getConsistency();
        this.clientId = params.getClientId();
    }

    @Override
    public void write(byte[] aByte) {
        QueueMessage msg = new QueueMessage(topic, aByte);
        try {
            queue.put(msg);
        } catch (InterruptedException e) {
            throw new SinetStreamIOException(e);
        }
    }

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public void close() {

    }

    @Override
    public Map<String, Object> getConfig() {
        return config;
    }

    @Override
    public Consistency getConsistency() {
        return consistency;
    }

    @Override
    public String getClientId() {
        return clientId;
    }
}
