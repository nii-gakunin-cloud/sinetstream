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
import jp.ad.sinet.stream.spi.PluginMessageWriter;
import jp.ad.sinet.stream.spi.WriterParameters;
import jp.ad.sinet.stream.utils.MessageUtils;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

@Log
public class KafkaMessageWriter extends KafkaBaseIO implements PluginMessageWriter {

    private KafkaProducer<String, byte[]> producer;

    @Getter
    private final String topic;

    private final AtomicBoolean inTransaction = new AtomicBoolean(false);

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

    KafkaMessageWriter(WriterParameters parameters) {
        super(parameters.getService(), parameters.getConsistency(), parameters.getClientId(), parameters.getConfig(),
                parameters.getValueType(), parameters.isDataEncryption());
        this.topic = parameters.getTopic();
        Properties props = getKafkaProperties();
        setupProducerProperties(parameters, props);
        try {
            setupProducer(props);
        } catch (Throwable ex) {
            throw wrapSinetStreamException(ex);
        }
    }

    private void setupProducerProperties(WriterParameters parameters, Properties props) {
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

    private void setupProducer(Properties props) {
        log.fine(() -> "KAFKA producer init: " + getClientId());
        producer = new KafkaProducer<>(props, Serdes.String().serializer(), Serdes.ByteArray().serializer());
        if (getConsistency().equals(Consistency.EXACTLY_ONCE)) {
            initTransaction();
        }
    }

    public void close() {
        log.fine(() -> "KAFKA producer close: " + getClientId());
        producer.close();
    }

    public void write(byte[] message) {
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, message);
        sendRecord(record);
    }

    private void sendRecord(ProducerRecord<String, byte[]> record) {
        log.finer(() -> "KAFKA send: " + getClientId() + ": " + record.toString());
        if (getConsistency().equals(Consistency.EXACTLY_ONCE) && inTransaction.compareAndSet(false, true)) {
            transactionalSendRecord(record);
        } else {
            try {
                this.producer.send(record).get();
            } catch (Throwable e) {
                throw wrapSinetStreamException(e);
            }
        }
    }

    private void transactionalSendRecord(ProducerRecord<String, byte[]> record) {
        log.finer(() -> "KAFKA begin transaction: " + getClientId());
        producer.beginTransaction();
        try {
            producer.send(record);
            commitTransaction();
        } catch (Throwable e) {
            abortTransaction();
            throw wrapSinetStreamException(e);
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
