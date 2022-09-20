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

import jp.ad.sinet.stream.spi.PluginMessageWrapper;
import jp.ad.sinet.stream.utils.Timestamped;

public class QueueMessage implements PluginMessageWrapper {
    private final String topic;
    private final Timestamped<byte[]> value;

    public QueueMessage(String topic, Timestamped<byte[]> value) {
        this.topic = topic;
        this.value = value;
    }

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public Timestamped<byte[]> getValue() {
        return value;
    }

    @Override
    public Object getRaw() {
        return this;
    }
}
