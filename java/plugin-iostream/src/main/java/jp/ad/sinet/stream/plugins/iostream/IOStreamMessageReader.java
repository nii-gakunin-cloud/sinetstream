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

package jp.ad.sinet.stream.plugins.iostream;

import jp.ad.sinet.stream.api.InvalidConfigurationException;
import jp.ad.sinet.stream.utils.Timestamped;
import jp.ad.sinet.stream.plugins.iostream.IOStreamParameters;

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.SinetStreamIOException;
import jp.ad.sinet.stream.spi.PluginMessageReader;
import jp.ad.sinet.stream.spi.PluginMessageWrapper;
import jp.ad.sinet.stream.spi.ReaderParameters;
import lombok.Getter;
import lombok.extern.java.Log;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.UUID;

@Log
public class IOStreamMessageReader implements PluginMessageReader {

    @Getter
    private final List<String> topics;

    @Getter
    private final Consistency consistency;

    @Getter
    private final String clientId;

    @Getter
    private final Duration receiveTimeout;

    void connect() {
    }

    IOStreamParameters iostreamParameters;
    InputStream ist;
    byte[] iba;

    IOStreamMessageReader(ReaderParameters parameters) {
        log.fine("IOStreamS3MessageReader: ctor=" + parameters);
        this.topics = Collections.unmodifiableList(parameters.getTopics());
        this.receiveTimeout = parameters.getReceiveTimeout();
        this.consistency = parameters.getConsistency();
        this.iostreamParameters = IOStreamParameters.create(parameters.getConfig());
        this.ist = this.iostreamParameters.getIst();
        this.iba = this.iostreamParameters.getIba();
        log.fine("IOStreamMessageReader: iostreamParameters=" + iostreamParameters);
        if (ist == null && iba == null)
            throw new InvalidConfigurationException("input_stream or input_byte_array must be specified");

        this.clientId = parameters.getClientId();
        connect();
    }

    @Override
    public PluginMessageWrapper read() {
        byte[] data = null;
        if (iba != null) {
            data = iba;
            iba = null;
        } else if (ist != null) {
            try {
                ByteArrayOutputStream baost = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];  // XXX tune me
                while (true) {
                    int len = ist.read(buf);
                    if (len < 0)
                        break;
                    baost.write(buf, 0, len);
                }
                if (baost.size() > 0)
                    data = baost.toByteArray();
            }
            catch (IOException e) {
                log.info("IOStreamMessageReader caught an exception: " + e);
                return null;
            }
        }
        if (data == null)
            return null;
        IOStreamMessage msg = new IOStreamMessage(data);
        //System.err.println("IOStreamMessageReader:read:IOStreamMessage=" + msg);
        return msg;
    }

    @Override
    public void close() {
    }

    @Override
    public Map<String, Object> getConfig() { return null; }
}
