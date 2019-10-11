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

import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.api.MessageWriter;
import jp.ad.sinet.stream.api.Serializer;
import jp.ad.sinet.stream.api.SinetStreamIOException;
import jp.ad.sinet.stream.crypto.CryptoSerializerWrapper;
import jp.ad.sinet.stream.plugins.kafka.utils.KafkaSerdeWrapper;
import jp.ad.sinet.stream.plugins.kafka.utils.KafkaSerializer;
import jp.ad.sinet.stream.spi.WriterParameters;
import jp.ad.sinet.stream.utils.MessageUtils;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

@Log
public class KafkaMessageWriter<K, V> extends KafkaBaseIO implements MessageWriter<V> {

    private KafkaProducer<K, V> producer;

    @Getter
    private final String topic;

    @Getter
    private final KafkaSerializer<V> serializer;

    private final AtomicBoolean inTransaction = new AtomicBoolean(false);

    private org.apache.kafka.common.serialization.Serializer<K> keySerializer;

    private static final Map<String, Function<Object, Object>> PRODUCER_PARAMETER_NAMES_MAP;
    static {
        Map<String, Function<Object, Object>> params = new HashMap<>();
        params.put(ProducerConfig.ACKS_CONFIG, MessageUtils::toString);
        params.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, MessageUtils::toString);
        params.put(ProducerConfig.RETRIES_CONFIG, MessageUtils::toInteger);
        params.put(ProducerConfig.BATCH_SIZE_CONFIG, MessageUtils::toInteger);
        params.put(ProducerConfig.LINGER_MS_CONFIG, MessageUtils::toLong);
        params.put(ProducerConfig.BUFFER_MEMORY_CONFIG, MessageUtils::toLong);
        params.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, MessageUtils::toLong);
        params.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, MessageUtils::toInteger);
        params.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, MessageUtils::toInteger);
        params.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, MessageUtils::toInteger);
        params.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, MessageUtils::toBoolean);
        params.put(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, MessageUtils::toInteger);
        params.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, MessageUtils::toString);
        PRODUCER_PARAMETER_NAMES_MAP  = Collections.unmodifiableMap(params);
    }

    KafkaMessageWriter(WriterParameters<V> parameters) {
        super(parameters.getService(), parameters.getConsistency(), parameters.getClientId(), parameters.getConfig(),
                parameters.getValueType(), parameters.isDataEncryption());
        this.topic = parameters.getTopic();
        Properties props = getKafkaProperties();
        setupProducerProperties(parameters, props);
        setupKeySerializer();
        serializer = generateSerializer(parameters);
        setupProducer(props);
    }

    @SuppressWarnings("unchecked")
    private void setupKeySerializer() {
        Class<org.apache.kafka.common.serialization.Serializer> cls =
                org.apache.kafka.common.serialization.Serializer.class;
        final String key = ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
        @SuppressWarnings("UnnecessaryLocalVariable")
        org.apache.kafka.common.serialization.Serializer ser = Optional.ofNullable(config.get(key))
                .map(x -> MessageUtils.toClassObject(cls, x))
                .orElseGet(() -> Optional
                        .ofNullable(config.get(key.replace('.', '_')))
                        .map(x -> MessageUtils.toClassObject(cls, x))
                        .orElseGet(() -> Serdes.String().serializer()));
        keySerializer = ser;
    }

    private void setupProducerProperties(WriterParameters<V> parameters, Properties props) {
        PRODUCER_PARAMETER_NAMES_MAP.forEach((k, v) -> updateProperty(props, k, v));
        setupConsistencyProperties(props);
        setupSSLProperties(parameters.getConfig(), props);
        setupSASLProperties(parameters.getConfig(), props);
    }

    private void setupConsistencyProperties(Properties props) {
        switch (consistency) {
            case EXACTLY_ONCE:
                props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
                setupTransactionId(props);
                break;
            case AT_LEAST_ONCE:
                props.put(ProducerConfig.ACKS_CONFIG, "all");
                break;
            case AT_MOST_ONCE:
                props.put(ProducerConfig.ACKS_CONFIG, "0");
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private KafkaSerializer<V> generateSerializer(WriterParameters<V> parameters) {
        Serializer<V> ser;
        if (Objects.isNull(parameters.getSerializer())) {
            ser = parameters.getValueType().getSerializer();
        } else {
            ser = parameters.getSerializer();
        }
        ser = CryptoSerializerWrapper.getSerializer(config, ser);
        return new KafkaSerdeWrapper(ser).serializer();
    }

    @SuppressWarnings("unchecked")
    private void setupProducer(Properties props) {
        log.fine(() -> "KAFKA producer init: " + getClientId());
        producer = new KafkaProducer(props, keySerializer, serializer);
        if (getConsistency().equals(Consistency.EXACTLY_ONCE)) {
            initTransaction();
        }
    }

    public void close() {
        log.fine(() -> "KAFKA producer close: " + getClientId());
        producer.close();
    }

    public void write(V message) {
        ProducerRecord<K, V> record = new ProducerRecord<>(topic, message);
        sendRecord(record);
    }

    public void write(K key, V value) {
        ProducerRecord<K, V> record = new ProducerRecord<>(topic, key, value);
        sendRecord(record);
    }

    private void sendRecord(ProducerRecord<K, V> record) {
        log.finer(() -> "KAFKA send: " + getClientId() + ": " + record.toString());
        if (getConsistency().equals(Consistency.EXACTLY_ONCE) && inTransaction.compareAndSet(false, true)) {
            transactionalSendRecord(record);
        } else {
            try {
                this.producer.send(record);
            } catch (Throwable e) {
                throw new SinetStreamIOException(e);
            }
        }
    }

    private void transactionalSendRecord(ProducerRecord<K, V> record) {
        log.finer(() -> "KAFKA begin transaction: " + getClientId());
        producer.beginTransaction();
        try {
            producer.send(record);
            commitTransaction();
        } catch (Throwable e) {
            abortTransaction();
            throw new SinetStreamIOException(e);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public void initTransaction() {
        producer.initTransactions();
        log.finer(() -> "KAFKA init transaction: " + getClientId());
    }

    public void beginTransaction() {
        inTransaction.set(true);
        producer.beginTransaction();
        log.finer(() -> "KAFKA begin transaction: " + getClientId());
    }

    @SuppressWarnings("WeakerAccess")
    public void commitTransaction() {
        producer.commitTransaction();
        inTransaction.set(false);
        log.finer(() -> "KAFKA commit transaction: " + getClientId());
    }

    @SuppressWarnings("WeakerAccess")
    public void abortTransaction() {
        producer.abortTransaction();
        inTransaction.set(false);
        log.finer(() -> "KAFKA abort transaction: " + getClientId());
    }
}
