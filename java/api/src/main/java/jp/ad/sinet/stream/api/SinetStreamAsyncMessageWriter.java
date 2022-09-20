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

import jp.ad.sinet.stream.spi.PluginAsyncMessageWriter;
import jp.ad.sinet.stream.spi.WriterParameters;
import jp.ad.sinet.stream.utils.Timestamped;

import org.jdeferred2.Promise;

public class SinetStreamAsyncMessageWriter<T> extends SinetStreamBaseWriter<T, PluginAsyncMessageWriter> implements AsyncMessageWriter<T> {

    public SinetStreamAsyncMessageWriter(PluginAsyncMessageWriter pluginWriter, WriterParameters parameters, Serializer<T> serializer) {
        super(pluginWriter, parameters, serializer);
    }

    @Override
    public Promise<?, ? extends Throwable, ?> write(T message) {
        try {
            return target.write(new Timestamped<byte[]>(toPayload(message)))
                    .fail((e) -> this.updateMetricsErr());
        }
        catch (Exception e) {
            this.updateMetricsErr();
            throw e;
        }
    }
}
