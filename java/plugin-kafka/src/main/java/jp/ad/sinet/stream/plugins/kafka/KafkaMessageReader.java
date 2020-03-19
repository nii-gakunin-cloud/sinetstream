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

import jp.ad.sinet.stream.api.AuthenticationException;
import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.SinetStreamIOException;
import jp.ad.sinet.stream.spi.PluginMessageReader;
import jp.ad.sinet.stream.spi.PluginMessageWrapper;
import jp.ad.sinet.stream.spi.ReaderParameters;
import jp.ad.sinet.stream.utils.MessageUtils;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.serialization.Serdes;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Log
public class KafkaMessageReader extends KafkaBaseIO implements PluginMessageReader {

    private KafkaConsumer<String, byte[]> consumer;

    private final Properties consumerProperties;

    private final BlockingQueue<Future<ConsumerRecord<String, byte[]>>> queue = new LinkedBlockingQueue<>();

    private final AtomicBoolean closed = new AtomicBoolean(false);

    private static final AtomicInteger KAFKA_GROUP_ID_SEQUENCE = new AtomicInteger(1);

    @Getter
    private final List<String> topics;

    @Getter
    private final Duration receiveTimeout;

    @Getter
    private String groupId;

    private AtomicBoolean sendOffsetsEnabled = new AtomicBoolean(false);

    private final Lock lock = new ReentrantLock();
    private final Condition startPolling = lock.newCondition();

    private static final Map<String, Function<Object, Object>> CONSUMER_PARAMETER_NAMES_MAP;

    static {
        Map<String, Function<Object, Object>> params = new HashMap<>();
        params.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, MessageUtils::toInteger);
        params.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, MessageUtils::toInteger);
        params.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, MessageUtils::toInteger);
        params.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, MessageUtils::toInteger);
        params.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, MessageUtils::toBoolean);
        params.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, MessageUtils::toInteger);
        params.put(ConsumerConfig.CHECK_CRCS_CONFIG, MessageUtils::toBoolean);
        params.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, MessageUtils::toInteger);
        params.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, MessageUtils::toInteger);
        params.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, MessageUtils::toInteger);
        params.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, MessageUtils::toInteger);
        params.put(ConsumerConfig.EXCLUDE_INTERNAL_TOPICS_CONFIG, MessageUtils::toBoolean);
        params.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, MessageUtils::toBoolean);
        params.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, MessageUtils::toString);
        params.put(ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, MessageUtils::toInteger);
        params.put(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, MessageUtils::toString);
        params.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, MessageUtils::toString);
        params.put(ConsumerConfig.GROUP_ID_CONFIG, MessageUtils::toString);
        params.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, MessageUtils::toStringList);
        CONSUMER_PARAMETER_NAMES_MAP  = Collections.unmodifiableMap(params);
    }

    private KafkaProducer<byte[], byte[]> producer;
    private ExecutorService worker;

    KafkaMessageReader(ReaderParameters parameters) {
        super(parameters.getService(), parameters.getConsistency(), parameters.getClientId(), parameters.getConfig(),
                parameters.getValueType(), parameters.isDataEncryption());

        topics = Collections.unmodifiableList(parameters.getTopics());
        receiveTimeout = parameters.getReceiveTimeout();

        consumerProperties = getKafkaProperties();
        setupConsumerProperties(parameters);
        setupConsumer(consumerProperties);
    }

    private void setupConsumerProperties(ReaderParameters parameters) {
        CONSUMER_PARAMETER_NAMES_MAP.forEach((k, v) -> updateProperty(consumerProperties, k, v));
        setupGroupId(consumerProperties);
        setupSSLProperties(config, consumerProperties);
        setupConsistencyProperties();
        setupSASLProperties(config, consumerProperties);
    }

    private void setupConsistencyProperties() {
        if (this.getConsistency().equals(Consistency.EXACTLY_ONCE)) {
            consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
            consumerProperties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        }
    }

    private void startPollingWorker() {
        log.fine(() -> "KAFKA: start polling thread: " + getClientId());
        worker = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        worker.submit(() -> {
            if (!closed.get()) {
                lock.lock();
                try {
                    ConsumerRecords<String, byte[]> records = this.consumer.poll(Duration.ofMillis(1));
                    startPolling.signalAll();
                    append_consumer_records(records);
                } catch (Throwable ex) {
                    append_exception(ex);
                } finally {
                    lock.unlock();
                }
            }

            while (!closed.get()) {
                log.finest(() -> "KAFKA message poll: " + getClientId());
                try {
                    ConsumerRecords<String, byte[]> records = this.consumer.poll(Duration.ofMillis(100));
                    append_consumer_records(records);
                } catch (Throwable ex) {
                    append_exception(ex);
                }
            }
            log.fine(() -> "KAFKA consumer close: " + getClientId());
            consumer.close();
        });
    }

    private void append_consumer_records(ConsumerRecords<String, byte[]> records) {
        for (ConsumerRecord<String, byte[]> rec : records) {
            log.finer(() -> "KAFKA message poll: " + getClientId() + ": " + rec.toString());
            queue.add(CompletableFuture.completedFuture(rec));
        }
    }

    private void append_exception(Throwable ex) {
        CompletableFuture<ConsumerRecord<String, byte[]>> future = new CompletableFuture<>();
        future.completeExceptionally(ex);
        queue.add(future);
    }

    private void setupGroupId(Properties props) {
        Optional.ofNullable(props.getProperty(ConsumerConfig.GROUP_ID_CONFIG))
                .map(MessageUtils::toString).ifPresent(x -> groupId = x);
        if (Objects.isNull(groupId) || groupId.isEmpty()) {
            groupId = generateGroupId();
        }
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    }

    private String generateGroupId() {
        return service + "-group-" + KAFKA_GROUP_ID_SEQUENCE.getAndIncrement() + '-'
                + RandomStringUtils.randomAlphabetic(8);
    }

    private void setupConsumer(Properties props) {
        log.fine(() -> "KAFKA consumer init: " + getClientId());
        consumer = new KafkaConsumer<>(props, Serdes.String().deserializer(), Serdes.ByteArray().deserializer());

        log.fine(() -> "KAFKA consumer subscribe: " + getClientId() + ": " + getTopic());
        try {
            for (String topic : topics) {
                consumer.partitionsFor(topic, Duration.ofSeconds(10));
            }
        } catch (org.apache.kafka.common.errors.AuthenticationException ex) {
            throw new AuthenticationException(ex);
        } catch (TimeoutException ex) {
            throw new SinetStreamIOException(ex);
        }
        consumer.subscribe(topics);
        lock.lock();
        try {
            startPollingWorker();
            startPolling.awaitUninterruptibly();
        } finally {
            lock.unlock();
        }
        setupOffsetsProducer();
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

    private String getTopic() {
        return String.join(",", this.topics);
    }

    @Override
    public void close() {
        closed.set(true);
        log.fine(() -> "KAFKA consumer wakeup: " + getClientId());
        consumer.wakeup();
        worker.shutdownNow();
        try {
            worker.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.finer(e::getMessage);
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

    private void setupOffsetsProducer() {
        if (getConsistency().equals(Consistency.EXACTLY_ONCE)) {
            Properties props = new Properties();
            props.setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
                    consumerProperties.getProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG));
            setupTransactionId(props);
            setupSSLProperties(config, props);
            setupSASLProperties(config, props);
            sendOffsetsEnabled.set(true);
            producer = new KafkaProducer<>(props, Serdes.ByteArray().serializer(), Serdes.ByteArray().serializer());
            producer.initTransactions();
        }
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
}
