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

import jp.ad.sinet.stream.spi.PluginMessageReader;
import jp.ad.sinet.stream.spi.PluginMessageWrapper;
import jp.ad.sinet.stream.spi.ReaderParameters;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import lombok.Getter;

public class SinetStreamMessageReader<T> extends SinetStreamBaseReader<T, PluginMessageReader> implements MessageReader<T> {

    public SinetStreamMessageReader(PluginMessageReader pluginReader, ReaderParameters parameters, Deserializer<T> deserializer) {
        super(pluginReader, parameters, deserializer);
    }

    private class InjectedMessage implements PluginMessageWrapper {
        @Getter
        private final byte[] value;
        @Getter
        private final String topic;
        @Getter
        private final Object raw;

        public InjectedMessage(byte[] value, String topic, Object raw) {
            this.value = value;
            this.topic = topic;
            this.raw = raw;
        }
    }
    private InjectedMessage injectMsg;
    public void debugInjectMsgBytes(byte[] value, String topic, Object raw) {
        injectMsg = new InjectedMessage(value, topic, raw);
    }

    @Override
    public Message<T> read() {
        if (injectMsg != null) {
            PluginMessageWrapper msg = injectMsg;
            injectMsg = null;
            return toMessage(msg);
        }
        try {
            return toMessage(target.read());
        }
        catch (Exception e) {
            updateMetricsErr();
            throw e;
        }
    }

    @Override
    public Stream<Message<T>> stream() {
        SinetStreamMessageReader<T> reader = this;
        Iterator<Message<T>> iterator = new Iterator<Message<T>>() {
            private Iterator<PluginMessageWrapper> it = target.stream().iterator();

            @Override
            public boolean hasNext() {
                try {
                    return it.hasNext();
                }
                catch (Exception e) {
                    reader.updateMetricsErr();
                    throw e;
                }
            }

            @Override
            public Message<T> next() {
                try {
                    return toMessage(it.next());
                }
                catch (Exception e) {
                    reader.updateMetricsErr();
                    throw e;
                }
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED | Spliterator.NONNULL), false);
    }
}
