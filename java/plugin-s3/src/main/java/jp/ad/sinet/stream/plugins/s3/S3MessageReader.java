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

import jp.ad.sinet.stream.api.InvalidConfigurationException;
import jp.ad.sinet.stream.utils.Timestamped;
import jp.ad.sinet.stream.plugins.s3.S3Cli;
import jp.ad.sinet.stream.plugins.s3.S3Parameters;
import jp.ad.sinet.stream.plugins.s3.Util;

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.SinetStreamIOException;
import jp.ad.sinet.stream.spi.PluginMessageReader;
import jp.ad.sinet.stream.spi.PluginMessageWrapper;
import jp.ad.sinet.stream.spi.ReaderParameters;
import lombok.Getter;
import lombok.extern.java.Log;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.UUID;

@Log
public class S3MessageReader implements PluginMessageReader {

    @Getter
    private final List<String> topics;

    @Getter
    private final Consistency consistency;

    @Getter
    private final String clientId;

    @Getter
    private final Duration receiveTimeout;

    S3Cli s3cli;

    void connect() {
        s3cli = new S3Cli(s3parameters);
    }

    S3Parameters s3parameters;
    Predicate<String> pathFilter;
    S3Cli.ListObjRes lastListObjRes;

    S3MessageReader(ReaderParameters parameters) {
        log.fine("S3MessageReader: ctor=" + parameters);
        this.topics = Collections.unmodifiableList(parameters.getTopics());
        this.receiveTimeout = parameters.getReceiveTimeout();
        if (parameters.getConfig().get("brokers") != null)
            throw new InvalidConfigurationException("brokers: cannot be specified for s3; use s3.endpoint_url");
        this.consistency = parameters.getConsistency();
        if (consistency != Consistency.AT_MOST_ONCE)
            throw new InvalidConfigurationException("consistency must be AT_MOST_ONCE for s3");
        this.s3parameters = S3Parameters.create(parameters.getConfig());
        log.fine("S3MessageWriter: s3parameters=" + s3parameters);

        pathFilter = Util.makePathFilter(s3parameters.getPrefix(), topics, s3parameters.getName(), s3parameters.getSuffix());

        String clientId = parameters.getClientId();
        this.clientId = clientId != null && ! clientId.isEmpty() ? clientId
                                                                 : UUID.randomUUID().toString();

        connect();
        lastListObjRes = s3cli.listObj(s3parameters.getBucket(),
                                       s3parameters.getPrefix(),
                                       pathFilter,
                                       null);
    }

    @Override
    public PluginMessageWrapper read() {
        log.fine("read: lastListObjRes=" + lastListObjRes);
        if (lastListObjRes.keyList.isEmpty()) {
            if (lastListObjRes.continuationToken == null)
                return null;
            lastListObjRes = s3cli.listObj(s3parameters.getBucket(),
                                           s3parameters.getPrefix(),
                                           pathFilter,
                                           lastListObjRes.continuationToken);
            return read();
        }
        String key = lastListObjRes.keyList.get(0);
        lastListObjRes.keyList.remove(0);
        S3Cli.GetObjRes r = s3cli.getObj(s3parameters.getBucket(), key);
        String topic = Util.extractTopicFromPath(key);
        S3Message msg = new S3Message(topic, r.data, r.timestamp_us, r.res);
        return msg;
    }

    @Override
    public void close() {
        if (s3cli != null) {
            s3cli.close();
            s3cli = null;
        }
    }

    @Override
    public Map<String, Object> getConfig() { return null; }
}
