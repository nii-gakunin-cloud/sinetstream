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
import jp.ad.sinet.stream.spi.PluginAsyncMessageWriter;
import jp.ad.sinet.stream.spi.WriterParameters;
import jp.ad.sinet.stream.utils.Timestamped;

import org.jdeferred2.Promise;

import lombok.Getter;
import lombok.extern.java.Log;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Log
public class S3AsyncMessageWriter implements PluginAsyncMessageWriter {

    @Getter
    private final String topic;

    S3AsyncMessageWriter(WriterParameters parameters) {
        this.topic = parameters.getTopic();
    }


    @Override
    public Promise<?, ? extends Throwable, ?> write(Timestamped<byte[]> message) {
        return null;
    }

    @Override
    public String getClientId() {
        return "XXX";
    }

    @Override
    public Consistency getConsistency() { return null; }

    @Override
    public Map<String, Object> getConfig() { return null; }

    @Override
    public void close() {}
}
