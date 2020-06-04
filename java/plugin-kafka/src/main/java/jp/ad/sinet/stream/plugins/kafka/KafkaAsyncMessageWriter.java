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

package jp.ad.sinet.stream.plugins.kafka;

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.spi.PluginAsyncMessageWriter;
import jp.ad.sinet.stream.spi.WriterParameters;
import jp.ad.sinet.stream.utils.MessageUtils;
import lombok.extern.java.Log;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.jdeferred2.DeferredCallable;
import org.jdeferred2.DeferredManager;
import org.jdeferred2.Promise;
import org.jdeferred2.impl.DefaultDeferredManager;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;


@Log
public class KafkaAsyncMessageWriter extends KafkaBaseWriter implements PluginAsyncMessageWriter {

    private final DefaultDeferredManager manager;
    private final Function<ProducerRecord<String, byte[]>, Promise<?, Throwable, Void>> sender;

    KafkaAsyncMessageWriter(WriterParameters parameters) {
        super(parameters);
        this.manager = createDeferredManager();
        if (getConsistency().equals(Consistency.EXACTLY_ONCE)) {
            sender = this::transactionalSendRecord;
        } else {
            sender = this::sendRecord;
        }
    }

    private DefaultDeferredManager createDeferredManager() {
        int nthreads = MessageUtils.toInteger(getConfig().getOrDefault("thread_pool_num", 4));
        return new DefaultDeferredManager(Executors.newFixedThreadPool(nthreads));
    }

    @Override
    public void close() {
        super.close();
        manager.shutdown();
    }

    @Override
    public Promise<?, Throwable, Void> write(byte[] message) {
        return sender.apply(new ProducerRecord<>(topic, message));
    }

    private Promise<?, Throwable, Void> sendRecord(ProducerRecord<String, byte[]> record) {
        Future<RecordMetadata> future = producer.send(record);
        return manager.when(new DeferredCallable<RecordMetadata, Void>(DeferredManager.StartPolicy.AUTO) {
            @Override
            public RecordMetadata call() {
                try {
                    return future.get();
                } catch (Throwable e) {
                    throw wrapSinetStreamException(e);
                }
            }
        });
    }

    private Promise<?, Throwable, Void> transactionalSendRecord(ProducerRecord<String,byte[]> record) {
        beginTransaction();
        Future<RecordMetadata> future = producer.send(record);
        return manager.when(new DeferredCallable<RecordMetadata, Void>(DeferredManager.StartPolicy.AUTO) {
            @Override
            public RecordMetadata call() {
                try {
                    RecordMetadata ret = future.get();
                    commitTransaction();
                    return ret;
                } catch (Throwable e) {
                    abortTransaction();
                    throw wrapSinetStreamException(e);
                }
            }
        });
    }
}
