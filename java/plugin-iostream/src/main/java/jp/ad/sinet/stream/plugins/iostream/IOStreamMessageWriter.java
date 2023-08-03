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

import jp.ad.sinet.stream.utils.Timestamped;
import jp.ad.sinet.stream.plugins.iostream.IOStreamParameters;

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.SinetStreamIOException;
import jp.ad.sinet.stream.api.InvalidConfigurationException;
import jp.ad.sinet.stream.spi.PluginMessageWriter;
import jp.ad.sinet.stream.spi.WriterParameters;

import lombok.Getter;
import lombok.extern.java.Log;
import lombok.Value;

import java.util.Map;
import java.util.HashMap;

import java.util.List;
import java.util.LinkedList;

import java.net.URI;
import java.net.URISyntaxException;
import java.lang.RuntimeException;

import java.util.UUID;

import java.time.OffsetDateTime;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

import java.lang.StringBuilder;

/*
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.Level;
*/

import java.io.OutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.function.Predicate;

@Log
public class IOStreamMessageWriter implements PluginMessageWriter {

    @Getter
    private final String topic;

    @Getter
    private final Consistency consistency;

    @Getter
    private final String clientId;

    private OutputStream ost;

    void connect() {
    }

    IOStreamParameters iostreamParameters;

    @SuppressWarnings("unchecked")
    IOStreamMessageWriter(WriterParameters parameters) {
        log.fine("IOStreamMessageWriter: ctor=" + parameters);
        this.topic = parameters.getTopic();
        this.consistency = parameters.getConsistency();
        this.iostreamParameters = IOStreamParameters.create(parameters.getConfig());
        log.fine("IOStreamMessageWriter: iostreamParameters=" + iostreamParameters);
        this.clientId = parameters.getClientId();
        Map<String, Object> config_iostream = (Map<String, Object>)parameters.getConfig().get("iostream");
        this.ost = (OutputStream) config_iostream.get("output_stream");
        connect();
    }

    @Override
    public void write(Timestamped<byte[]> message) {
        log.fine("IOStreamMessageWriter: write");
        try {
            ost.write(message.getValue());
        }
        catch (IOException e) {
            log.info("IOStreamMessageWriter caught an exception: " + e);
        }
    }

    @Override
    public void close() {
    }

    @Override
    public Map<String, Object> getConfig() { return null; }

    @Override
    public Object getInfo(List<String> ipath) {
        return null;
    }
}
