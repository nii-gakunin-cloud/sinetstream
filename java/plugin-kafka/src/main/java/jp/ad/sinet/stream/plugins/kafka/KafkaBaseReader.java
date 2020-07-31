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
import jp.ad.sinet.stream.spi.ReaderParameters;
import jp.ad.sinet.stream.utils.MessageUtils;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.Serdes;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.logging.Level;

@Log
public abstract class KafkaBaseReader extends KafkaBaseIO {

    private KafkaConsumer<String, byte[]> consumer;

    final Properties consumerProperties;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    private static final AtomicInteger KAFKA_GROUP_ID_SEQUENCE = new AtomicInteger(1);

    @Getter
    private final List<String> topics;

    @Getter
    final Duration receiveTimeout;

    @Getter
    String groupId;

    AtomicBoolean sendOffsetsEnabled = new AtomicBoolean(false);

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

    KafkaProducer<byte[], byte[]> producer;
    private ExecutorService worker;

    KafkaBaseReader(ReaderParameters parameters) {
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

    protected void startPollingWorker() {
        lock.lock();
        try {
            if (Objects.isNull(worker) || worker.isShutdown()) {
                submitConsumerLoop();
                startPolling.awaitUninterruptibly();
            }
        } finally {
            lock.unlock();
        }
    }

    protected void submitConsumerLoop() {
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
                    ConsumerRecords<String, byte[]> records = this.consumer.poll(Duration.ofMillis(100));
                    startPolling.signalAll();
                    append_consumer_records(records);
                } catch (Throwable ex) {
                    startPolling.signalAll();
                    log.log(Level.FINE, ex, () -> "KAFKA poll 0: " + getClientId());
                    append_exception(ex);
                    throw ex;
                } finally {
                    lock.unlock();
                }
            }
            while (!closed.get()) {
                log.finest(() -> "KAFKA message poll: " + getClientId());
                try {
                    ConsumerRecords<String, byte[]> records = this.consumer.poll(Duration.ofMillis(100));
                    append_consumer_records(records);
                } catch (WakeupException ex) {
                    log.log(Level.FINE, "KAFKA consumer wakeup", ex);
                } catch (Throwable ex) {
                    log.log(Level.FINE, ex, () -> "KAFKA polling loop: " + getClientId());
                    append_exception(ex);
                    throw ex;
                }
            }
            log.fine(() -> "KAFKA consumer close: " + getClientId());
            consumer.close();
        });
    }

    protected abstract void append_consumer_records(ConsumerRecords<String, byte[]> records);

    protected abstract void append_exception(Throwable ex);

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

    protected void setupConsumer(Properties props) {
        log.fine(() -> "KAFKA consumer init: " + getClientId());
        consumer = new KafkaConsumer<>(props, Serdes.String().deserializer(), Serdes.ByteArray().deserializer());
        log.fine(() -> "KAFKA consumer subscribe: " + getClientId() + ": " + getTopic());
        partitionsFor();
        consumer.subscribe(topics);
        if (getConsistency().equals(Consistency.EXACTLY_ONCE)) {
            setupOffsetsProducer();
        }
    }

    private void partitionsFor() {
        try {
            for (String topic : topics) {
                consumer.partitionsFor(topic, Duration.ofSeconds(10));
            }
        } catch (org.apache.kafka.common.errors.AuthenticationException ex) {
            log.log(Level.FINER, "auth error", ex);
            throw new AuthenticationException(ex);
        } catch (TimeoutException ex) {
            log.log(Level.FINER, "timeout error", ex);
            throw new SinetStreamIOException(ex);
        }
    }

    public String getTopic() {
        return String.join(",", this.topics);
    }

    public void close() {
        if (!closed.getAndSet(true)) {
            log.fine(() -> "KAFKA consumer wakeup: " + getClientId());
            consumer.wakeup();
            stopPollingWorker();
        }
    }

    protected void stopPollingWorker() {
        lock.lock();
        try {
            if (Objects.isNull(worker) || worker.isShutdown()) {
                return;
            }
            worker.shutdown();
            try {
                worker.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.log(Level.FINER, "stop polling", e);
            }
        } finally {
            lock.unlock();
        }
    }

    private void setupOffsetsProducer() {
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

    public Object getMetrics() {
        return consumer.metrics();
    }
    public void resetMetrics() {
    }
}
