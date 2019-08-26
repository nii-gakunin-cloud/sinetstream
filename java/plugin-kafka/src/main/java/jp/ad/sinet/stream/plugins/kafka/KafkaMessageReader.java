/*
 * Copyright (C) 2019 National Institute of Informatics
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

import jp.ad.sinet.stream.api.*;
import jp.ad.sinet.stream.crypto.CryptoDeserializerWrapper;
import jp.ad.sinet.stream.plugins.kafka.utils.KafkaDeserializer;
import jp.ad.sinet.stream.plugins.kafka.utils.KafkaSerdeWrapper;
import jp.ad.sinet.stream.spi.ReaderParameters;
import jp.ad.sinet.stream.utils.MessageUtils;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.Serdes;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Log
public class KafkaMessageReader<K, T> extends KafkaBaseIO implements MessageReader<T> {

    private KafkaConsumer<K, T> consumer;

    private final Properties consumerProperties;

    private final BlockingQueue<ConsumerRecord<K, T>> queue = new LinkedBlockingQueue<>();

    private final AtomicBoolean closed = new AtomicBoolean(false);

    private static final AtomicInteger KAFKA_GROUP_ID_SEQUENCE = new AtomicInteger(1);

    @Getter
    private final List<String> topics;

    @Getter
    private final Duration receiveTimeout;

    @Getter
    private String groupId;

    @Getter
    private final KafkaDeserializer<T> deserializer;

    private org.apache.kafka.common.serialization.Deserializer<K> keyDeserializer;

    private AtomicBoolean sendOffsetsEnabled = new AtomicBoolean(false);

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

    private KafkaProducer producer;

    KafkaMessageReader(ReaderParameters<T> parameters) {
        super(parameters.getService(), parameters.getConsistency(), parameters.getClientId(), parameters.getConfig(),
                parameters.getValueType(), parameters.isDataEncryption());

        topics = Collections.unmodifiableList(parameters.getTopics());
        receiveTimeout = parameters.getReceiveTimeout();

        consumerProperties = getKafkaProperties();
        setupConsumerProperties(parameters);

        setupKeyDeserializer();
        deserializer = generateDeserializer(parameters);

        setupConsumer(consumerProperties);
    }

    private void setupConsumerProperties(ReaderParameters<T> parameters) {
        CONSUMER_PARAMETER_NAMES_MAP.forEach((k, v) -> updateProperty(consumerProperties, k, v));
        setupGroupId(consumerProperties);
        setupSSLProperties(config, consumerProperties);
        setupConsistencyProperties();
    }

    private void setupConsistencyProperties() {
        if (this.getConsistency().equals(Consistency.EXACTLY_ONCE)) {
            consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
            consumerProperties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        }
    }

    private void startPollingWorker() {
        log.fine(() -> "KAFKA: start polling thread: " + getClientId());
        ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        worker.submit(() -> {
            try {
                while (!closed.get()) {
                    log.finest(() -> "KAFKA message poll: " + getClientId());
                    ConsumerRecords<K, T> records = this.consumer.poll(Duration.ofMillis(1000));
                    for (ConsumerRecord<K, T> rec : records) {
                        log.finer(() -> "KAFKA message poll: " + getClientId() + ": " + rec.toString());
                        queue.add(rec);
                    }
                }
            } catch (WakeupException e) {
                if (!closed.get()) {
                    throw e;
                }
            } finally {
                log.fine(() -> "KAFKA consumer close: " + getClientId());
                consumer.close();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private KafkaDeserializer<T> generateDeserializer(ReaderParameters<T> parameters) {
        Deserializer<T> des;
        if (Objects.isNull(parameters.getDeserializer())) {
            des = getValueType().getDeserializer();
        } else {
            des = parameters.getDeserializer();
        }
        des = CryptoDeserializerWrapper.getDeserializer(config, des);
        return new KafkaSerdeWrapper<>(des).deserializer();
    }

    @SuppressWarnings("unchecked")
    private void setupKeyDeserializer() {
        Class<org.apache.kafka.common.serialization.Deserializer> cls =
                org.apache.kafka.common.serialization.Deserializer.class;
        final String key = ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;

        @SuppressWarnings("UnnecessaryLocalVariable")
        org.apache.kafka.common.serialization.Deserializer des = Optional.ofNullable(config.get(key))
                .map(x -> MessageUtils.toClassObject(cls, x))
                .orElseGet(() -> Optional
                        .ofNullable(config.get(key.replace('.', '_')))
                        .map(x -> MessageUtils.toClassObject(cls, x))
                        .orElseGet(() -> Serdes.String().deserializer()));
        keyDeserializer = des;
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
        consumer = new KafkaConsumer<>(props, keyDeserializer, deserializer);

        log.fine(() -> "KAFKA consumer subscribe: " + getClientId() + ": " + getTopic());
        consumer.subscribe(topics);

        startPollingWorker();
        setupOffsetsProducer();
    }

    @Override
    public Message<T> read() {
        try {
            ConsumerRecord<K, T> record = queue.poll(receiveTimeout.toNanos(), TimeUnit.NANOSECONDS);
            if (Objects.isNull(record)) {
                return null;
            }
            KafkaMessage<T> ret = new KafkaMessage<>(record);
            if (sendOffsetsEnabled.get()) {
                sendOffsetsToTransaction(ret);
            }
            return ret;
        } catch (InterruptedException e) {
            throw new SinetStreamIOException(e);
        }
    }

    @Override
    public String getTopic() {
        return String.join(",", this.topics);
    }

    @Override
    public void close() {
        closed.set(true);
        log.fine(() -> "KAFKA consumer wakeup: " + getClientId());
        consumer.wakeup();
    }

    @Override
    public Stream<Message<T>> stream() {
        if (!getConsistency().equals(Consistency.EXACTLY_ONCE)) {
            return MessageReader.super.stream();
        } else {
            return getExactlyOnceStream();
        }
    }

    private Stream<Message<T>> getExactlyOnceStream() {
        sendOffsetsEnabled.set(false);
        Iterator<Message<T>> iterator = new Iterator<Message<T>>() {
            private Message<T> cache = null;

            @Override
            public boolean hasNext() {
                this.cache = read();
                return Objects.nonNull(this.cache);
            }

            @Override
            public Message<T> next() {
                Message<T> ret = Optional.ofNullable(this.cache).orElseGet(() -> read());
                sendOffsetsToTransaction((KafkaMessage<T>) ret);
                return ret;
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED | Spliterator.NONNULL), false);
    }

    @SuppressWarnings("unchecked")
    private void setupOffsetsProducer() {
        if (getConsistency().equals(Consistency.EXACTLY_ONCE)) {
            Properties props = new Properties();
            props.setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
                    consumerProperties.getProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG));
            setupTransactionId(props);
            setupSSLProperties(config, props);
            sendOffsetsEnabled.set(true);
            producer = new KafkaProducer(props, Serdes.ByteArray().serializer(), Serdes.ByteArray().serializer());
            producer.initTransactions();
        }
    }

    @SuppressWarnings({"unchecked", "WeakerAccess"})
    public void sendOffsetsToTransaction(KafkaMessage<T> msg) {
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
