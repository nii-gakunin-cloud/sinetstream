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
import jp.ad.sinet.stream.spi.PluginMessageReader;
import jp.ad.sinet.stream.spi.PluginMessageWrapper;
import jp.ad.sinet.stream.spi.ReaderParameters;
import lombok.extern.java.Log;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Log
public class KafkaMessageReader extends KafkaBaseReader implements PluginMessageReader {

    private BlockingQueue<Future<ConsumerRecord<String, byte[]>>> queue;

    KafkaMessageReader(ReaderParameters parameters) {
        super(parameters);
    }

    @Override
    protected void submitConsumerLoop() {
        if (Objects.isNull(queue)) {
            queue = new LinkedBlockingQueue<>();
        }
        super.submitConsumerLoop();
    }


    @Override
    protected void append_consumer_records(ConsumerRecords<String, byte[]> records) {
        for (ConsumerRecord<String, byte[]> rec : records) {
            log.finer(() -> "KAFKA message poll: " + getClientId() + ": " + rec.toString());
            queue.add(CompletableFuture.completedFuture(rec));
        }
    }

    @Override
    protected void append_exception(Throwable ex) {
        CompletableFuture<ConsumerRecord<String, byte[]>> future = new CompletableFuture<>();
        future.completeExceptionally(ex);
        queue.add(future);
    }

    @Override
    public KafkaMessage read() {
        try {
            Future<ConsumerRecord<String, byte[]>> record = queue.poll(receiveTimeout.toNanos(), TimeUnit.NANOSECONDS);
            if (Objects.isNull(record)) {
                return null;
            }
            ConsumerRecord<String, byte[]> v = record.get();
            KafkaMessage ret = new KafkaMessage(v);
            if (sendOffsetsEnabled.get()) {
                sendOffsetsToTransaction(ret);
            }
            return ret;
        } catch (InterruptedException | ExecutionException e) {
            throw wrapSinetStreamException(e);
        }
    }

    @Override
    public Stream<PluginMessageWrapper> stream() {
        if (!getConsistency().equals(Consistency.EXACTLY_ONCE)) {
            return PluginMessageReader.super.stream();
        } else {
            return getExactlyOnceStream();
        }
    }

    private Stream<PluginMessageWrapper> getExactlyOnceStream() {
        sendOffsetsEnabled.set(false);
        Iterator<PluginMessageWrapper> iterator = new Iterator<PluginMessageWrapper>() {
            private PluginMessageWrapper cache = null;

            @Override
            public boolean hasNext() {
                this.cache = read();
                return Objects.nonNull(this.cache);
            }

            @Override
            public PluginMessageWrapper next() {
                PluginMessageWrapper ret = Optional.ofNullable(this.cache).orElseGet(() -> read());
                sendOffsetsToTransaction((KafkaMessage) ret);
                return ret;
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED | Spliterator.NONNULL), false);
    }

    @SuppressWarnings({"WeakerAccess"})
    public void sendOffsetsToTransaction(KafkaMessage msg) {
        Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
        TopicPartition partition = new TopicPartition(msg.getTopic(), msg.getRaw().partition());
        OffsetAndMetadata offset = new OffsetAndMetadata(msg.getRaw().offset() + 1);
        offsets.put(partition, offset);
        log.finer(() -> "KAFKA send offset: " + getClientId() + ": " + partition.topic() + ": " + offset.toString());
        producer.beginTransaction();
        producer.sendOffsetsToTransaction(offsets, groupId);
        producer.commitTransaction();
    }

    @Override
    protected void setupConsumer(Properties props) {
        super.setupConsumer(props);
        startPollingWorker();
    }
}
