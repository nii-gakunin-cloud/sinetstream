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
import jp.ad.sinet.stream.api.ValueType;
import jp.ad.sinet.stream.utils.KeyStoreUtil;
import jp.ad.sinet.stream.utils.MessageUtils;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log
public class KafkaBaseIO {

    private static final AtomicInteger KAFKA_TRANSACTION_ID_SEQUENCE = new AtomicInteger(1);
    @Getter
    protected final String service;

    @Getter
    protected final Consistency consistency;

    @Getter
    protected final Map<String, Object> config;

    @Getter
    private final String clientId;

    @Getter
    private final ValueType valueType;

    @Getter
    private final boolean dataEncryption;

    private static final AtomicInteger KAFKA_CLIENT_ID_SEQUENCE = new AtomicInteger(1);

    private static final Map<String, Function<Object, Object>> PARAMETER_NAMES_MAP;
    static {
        Map<String, Function<Object, Object>> params = new HashMap<>();
        params.put(CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG, MessageUtils::toInteger);
        params.put(CommonClientConfigs.RETRY_BACKOFF_MS_CONFIG, MessageUtils::toLong);
        params.put(CommonClientConfigs.RECONNECT_BACKOFF_MS_CONFIG, MessageUtils::toLong);
        params.put(CommonClientConfigs.RECONNECT_BACKOFF_MAX_MS_CONFIG, MessageUtils::toLong);
        params.put(CommonClientConfigs.RECEIVE_BUFFER_CONFIG, MessageUtils::toInteger);
        params.put(CommonClientConfigs.SEND_BUFFER_CONFIG, MessageUtils::toInteger);
        params.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, MessageUtils::toString);
        params.put(CommonClientConfigs.CONNECTIONS_MAX_IDLE_MS_CONFIG, MessageUtils::toLong);
        params.put(CommonClientConfigs.METADATA_MAX_AGE_CONFIG, MessageUtils::toLong);
        params.put(CommonClientConfigs.CLIENT_DNS_LOOKUP_CONFIG, MessageUtils::toString);
        PARAMETER_NAMES_MAP  = Collections.unmodifiableMap(params);
    }

    private static final Map<String, Function<Object, Object>> SSL_PARAMETER_NAMES_MAP;
    static {
        Map<String, Function<Object, Object>> params = new HashMap<>();
        params.put(SslConfigs.SSL_TRUSTMANAGER_ALGORITHM_CONFIG, MessageUtils::toString);
        params.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, MessageUtils::toString);
        params.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, MessageUtils::toString);
        params.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, MessageUtils::toString);
        params.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, MessageUtils::toString);
        params.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, MessageUtils::toString);
        params.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, MessageUtils::toString);
        params.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, MessageUtils::toString);
        params.put(SslConfigs.SSL_ENABLED_PROTOCOLS_CONFIG, MessageUtils::toStringList);
        params.put(SslConfigs.SSL_PROTOCOL_CONFIG, MessageUtils::toString);
        params.put(SslConfigs.SSL_CIPHER_SUITES_CONFIG, MessageUtils::toStringList);
        params.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, MessageUtils::toString);
        SSL_PARAMETER_NAMES_MAP  = Collections.unmodifiableMap(params);
    }

    @Getter
    private String transactionId;

    KafkaBaseIO(String service, Consistency consistency, String clientId, Map<String, ?> config, ValueType valueType, boolean dataEncryption) {
        this.service = service;
        this.consistency = consistency;
        this.config = Collections.unmodifiableMap(config);
        this.clientId = Objects.nonNull(clientId) && !clientId.trim().isEmpty()  ? clientId : generateClientId(service);
        this.valueType = valueType;
        this.dataEncryption = dataEncryption;
    }

    private String generateClientId(String service) {
        return service + '-' + KAFKA_CLIENT_ID_SEQUENCE.getAndIncrement() + '-' + RandomStringUtils.randomAlphabetic(8);
    }

    void updateProperty(Properties props, String key, Function<Object, Object> mapper) {
        Optional.ofNullable(config.get(key.replace('.', '_'))).map(loggingException(mapper))
                .ifPresent(x -> props.put(key, x));
        Optional.ofNullable(config.get(key)).map(loggingException(mapper))
                .ifPresent(x -> props.put(key, x));
    }

    Properties getKafkaProperties() {
        Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers());
        Optional.ofNullable(clientId).ifPresent(v -> props.put(CommonClientConfigs.CLIENT_ID_CONFIG, v));
        PARAMETER_NAMES_MAP.forEach((k, v) -> updateProperty(props, k, v));
        return props;
    }

    private String getBootstrapServers() {
        List<String> servers = new ArrayList<>();
        Optional.ofNullable(config.get("brokers"))
                .filter(String.class::isInstance).map(String.class::cast)
                .ifPresent(servers::add);
        servers.addAll(Optional.ofNullable(config.get("brokers"))
                .filter(List.class::isInstance).map(List.class::cast)
                .map(v ->
                        ((List<?>) v).stream()
                                .filter(String.class::isInstance).map(String.class::cast)
                                .collect(Collectors.toList())
                ).orElseGet(Collections::emptyList));

        Function<String, String> appendDefaultPort = (srv) -> srv.indexOf(':') >= 0 ? srv : srv + ":9092";
        return servers.stream().map(appendDefaultPort).collect(Collectors.joining(","));
    }

    void setupSSLProperties(Map config, Properties props) {
        Map<String, String> tls = getInitSSLProperties(config);

        Map<String, String> paramsNameMap = new HashMap<>();
        paramsNameMap.put("trustStore", SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG);
        paramsNameMap.put("trustStoreType", SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG);
        paramsNameMap.put("trustStorePassword", SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG);
        paramsNameMap.put("keyStore", SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG);
        paramsNameMap.put("keyStoreType", SslConfigs.SSL_KEYSTORE_TYPE_CONFIG);
        paramsNameMap.put("keyStorePassword", SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG);

        paramsNameMap.forEach((key0, key1) ->
                Optional.ofNullable(tls.get(key0)).ifPresent(value -> props.put(key1, value)));

        Consumer<Boolean> setupCheckHostname = (x) -> {
            if (!x) {
                props.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
            }
        };
        Optional.ofNullable(tls.get("check_hostname")).map(Boolean::parseBoolean).ifPresent(setupCheckHostname);

        if (isTls(config, tls)) {
            props.putIfAbsent(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        }
        SSL_PARAMETER_NAMES_MAP.forEach((k, v) -> updateProperty(props, k, v));
    }

    private boolean isTls(Map config, Map<String, String> tls) {
        return tls.size() > 0 ||
                Optional.ofNullable(config.get("tls")).map(x -> {
                    if (x instanceof Map) {
                        return ((Map) x).size() > 0;
                    } else if (x instanceof Boolean) {
                        return (Boolean) x;
                    } else {
                        return null;
                    }
                }).orElse(false);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getInitSSLProperties(Map config) {
        Map<String, String> tls = new HashMap<>();
        Optional.ofNullable(config.get("tls"))
                .filter(Map.class::isInstance).map(Map.class::cast)
                .ifPresent(x -> x.forEach((k, v) -> tls.put(k.toString(), v.toString())));

        tls.putAll(kafkaToTls(config));
        tls.putAll(KeyStoreUtil.setupKeyStore(tls));
        return tls;
    }

    private Map<String, String> kafkaToTls(Map<String, Object> config) {
        Map<String, String> paramsNameMap = new HashMap<>();
        paramsNameMap.put("ssl_cafile", "ca_certs");
        paramsNameMap.put("ssl_certfile", "certfile");
        paramsNameMap.put("ssl_keyfile", "keyfile");
        paramsNameMap.put("ssl_password", "keyfilePassword");
        paramsNameMap.put("ssl_ciphers", "ciphers");
        paramsNameMap.put("ssl_check_hostname", "check_hostname");

        Map<String, String> ret = new HashMap<>();
        paramsNameMap.forEach((key0, key1) ->
                Optional.ofNullable(config.get(key0)).ifPresent(v -> ret.put(key1, v.toString())));
        return ret;
    }

    private static <T, R> Function<T, R> loggingException(Function<? super T, ? extends R> mapper) {
        return v -> {
            try {
                return mapper.apply(v);
            } catch (Throwable e) {
                log.warning(e.getMessage());
                return null;
            }
        };
    }

    void setupTransactionId(Properties props) {
        Optional.ofNullable(config.get(ProducerConfig.TRANSACTIONAL_ID_CONFIG))
                .map(MessageUtils::toString).ifPresent(x -> transactionId = x);
        if (Objects.isNull(transactionId) || transactionId.isEmpty()) {
            transactionId = generateTransactionId();
        }
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, transactionId);
    }

    private String generateTransactionId() {
        return service + "-transaction-" + KAFKA_TRANSACTION_ID_SEQUENCE.getAndIncrement() + '-'
                + RandomStringUtils.randomAlphabetic(8);
    }
}
