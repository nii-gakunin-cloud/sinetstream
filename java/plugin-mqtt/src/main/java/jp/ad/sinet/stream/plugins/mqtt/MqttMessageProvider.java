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

import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.spi.MessageReaderProvider;
import jp.ad.sinet.stream.spi.MessageWriterProvider;
import jp.ad.sinet.stream.spi.ReaderParameters;
import jp.ad.sinet.stream.spi.WriterParameters;
import lombok.extern.java.Log;

@Log
public class MqttMessageProvider<T> implements MessageWriterProvider<T>, MessageReaderProvider<T> {

    @Override
    public MessageWriter<T> getWriter(WriterParameters<T> params) {
        log.fine(() -> "MQTT getWriter: " + params.toString());
        return new MqttMessageWriter<>(params);
    }

    @Override
    public MessageReader<T> getReader(ReaderParameters<T> params) {
        log.fine(() -> "MQTT getReader: " + params.toString());
        return new MqttMessageReader<>(params);
    }

    @Override
    public String getType() {
        return "mqtt";
    }
}
