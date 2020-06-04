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

package jp.ad.sinet.stream.plugins.mqtt;

import jp.ad.sinet.stream.api.*;
import jp.ad.sinet.stream.api.valuetype.SimpleValueType;
import jp.ad.sinet.stream.utils.KeyStoreUtil;
import jp.ad.sinet.stream.utils.MessageUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.logging.Level;

@Log
public abstract class AbstractMqttIO<T> {

    final T client;

    @Getter
    protected final String service;
    @Getter
    protected final Consistency consistency;
    @Getter
    protected final Map<String, Object> config;
    @Getter
    protected final ValueType valueType;

    @Getter
    protected final boolean retain;

    @Getter
    protected final boolean dataEncryption;

    @Getter
    final MqttConnectOptions connectOptions;

    private final String websocketPath;

    int reconnectMinDelay = 1;

    @Getter
    @Setter
    int reconnectDelay = 1;

    protected final AtomicBoolean closed = new AtomicBoolean(false);

    AbstractMqttIO(String service, Consistency consistency, String clientId, Map<String, ?> config,
                   ValueType valueType, boolean dataEncryption) {
        this.service = service;
        this.config = Collections.unmodifiableMap(config);
        this.valueType = valueType;
        this.consistency = setupConsistency(consistency);
        this.retain = setupRetain();
        this.connectOptions = setupConnectOptions();
        this.dataEncryption = dataEncryption;
        this.websocketPath = getWebSocketPath();

        String realClientId = Optional.ofNullable(clientId).filter(x -> x.trim().length() > 0)
                .orElseGet(MqttClient::generateClientId);
        log.fine(() -> "mqtt clientid = " + realClientId);
        this.client = newMqttClient(realClientId);
    }

    protected abstract T newMqttClient(String realClientId);

    protected abstract IMqttToken mqttConnect(MqttConnectOptions opts) throws MqttException;

    protected MqttClientPersistence getPersistence() {
        Path dataDir = Paths.get(System.getProperty("user.home"), ".mqtt-persistence");
        return new MqttDefaultFilePersistence(dataDir.normalize().toString());
    }

    void connect() {
        if (closed.get()) {
            return;
        }
        log.fine(() -> "Connect to the broker: " + getClientId());
        try {
            IMqttToken ret = mqttConnect(connectOptions);
            log.fine(() -> "connect complete: " + ret.getResponse().toString());
        } catch (MqttSecurityException e) {
			Throwable cause = e.getCause();
			if (cause instanceof NoSuchAlgorithmException) {
				// TLS の設定がエラーの時は接続エラーとする
				throw new ConnectionException(e);
			} else {
				// その他は認証エラーとする
				throw new AuthenticationException(e);
			}
        } catch (MqttException e) {
            throw new ConnectionException(e);
        }
    }

    private MqttConnectOptions setupConnectOptions() {
        final MqttConnectOptions opts = new MqttConnectOptions();

        Optional.ofNullable(config.get("username_pw_set"))
                .filter(Map.class::isInstance).map(Map.class::cast).ifPresent(x -> {
            Optional.ofNullable(x.get("username"))
                    .filter(String.class::isInstance).map(String.class::cast)
                    .ifPresent(opts::setUserName);
            Optional.ofNullable(x.get("password"))
                    .filter(String.class::isInstance).map(String.class::cast)
                    .map(String::toCharArray).ifPresent(opts::setPassword);
        });
        Optional.ofNullable(config.get("protocol"))
                .filter(String.class::isInstance).map(String.class::cast)
                .map(loggingException(MqttVersion::valueOf))
                .map(MqttVersion::getValue)
                .ifPresent(opts::setMqttVersion);

        Optional.ofNullable(config.get("max_inflight_messages_set"))
                .map(loggingException(MessageUtils::toInteger))
                .ifPresent(opts::setMaxInflight);

        Optional.ofNullable(config.get("clean_session")).map(loggingException(MessageUtils::toBoolean))
                .ifPresent(opts::setCleanSession);

        Optional.ofNullable(config.get("ws_set_options"))
                .filter(Map.class::isInstance).map(Map.class::cast)
                .flatMap(wsOpt -> Optional.ofNullable(wsOpt.get("headers"))
                .map(headers -> {
                    if (headers instanceof Properties) {
                        return (Properties) headers;
                    } else if (headers instanceof Map) {
                        Properties ps = new Properties();
                        ps.putAll((Map<?, ?>) headers);
                        return ps;
                    } else {
                        return null;
                    }
                })).ifPresent(opts::setCustomWebSocketHeaders);

        Optional.ofNullable(config.get("reconnect_delay_set"))
                .filter(Map.class::isInstance).map(Map.class::cast)
                .ifPresent(delayOpt -> {
                    Optional.ofNullable(delayOpt.get("max_delay"))
                            .map(loggingException(MessageUtils::toInteger))
                            .ifPresent(opts::setMaxReconnectDelay);
                    Optional.ofNullable(delayOpt.get("min_delay"))
                            .map(loggingException(MessageUtils::toInteger))
                            .ifPresent(v -> reconnectMinDelay = reconnectDelay = v);
                });

        Optional.ofNullable(config.get("connect"))
                .filter(Map.class::isInstance).map(Map.class::cast)
                .ifPresent(connectOpts -> {
                    Optional.ofNullable(connectOpts.get("keepalive"))
                            .map(loggingException(MessageUtils::toInteger))
                            .ifPresent(opts::setKeepAliveInterval);
                    Optional.ofNullable(connectOpts.get("automatic_reconnect"))
                            .map(loggingException(MessageUtils::toBoolean))
                            .ifPresent(opts::setAutomaticReconnect);
                    Optional.ofNullable(connectOpts.get("connection_timeout"))
                            .map(loggingException(MessageUtils::toInteger))
                            .ifPresent(opts::setConnectionTimeout);
                    Optional.ofNullable(connectOpts.get("executor_service_timeout"))
                            .map(loggingException(MessageUtils::toInteger))
                            .ifPresent(opts::setExecutorServiceTimeout);
                });

        setupSSLOptions(opts);
        try {
            setupWill(opts);
        } catch (RuntimeException e) {
            log.warning(e.getMessage());
        }

        return opts;
    }

    private String getWebSocketPath() {
        return Optional.ofNullable(config.get("ws_set_options"))
                .filter(Map.class::isInstance).map(Map.class::cast)
                .flatMap(opts -> Optional.ofNullable(opts.get("path"))
                        .filter(String.class::isInstance).map(String.class::cast))
                .orElse(null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setupSSLOptions(MqttConnectOptions opts) {
        Map tls = new HashMap();
        for (String key : Arrays.asList("tls", "tls_set")) {
            Optional.ofNullable(config.get(key))
                    .filter(Map.class::isInstance).map(Map.class::cast)
                    .ifPresent(tls::putAll);
        }
        tls.putAll(KeyStoreUtil.setupKeyStore(tls));

        setupSSLProperties(opts, tls);
        setupHttpsHostnameVerification(opts, tls);
    }

    @SuppressWarnings("rawtypes")
    private void setupSSLProperties(MqttConnectOptions opts, Map tls) {
        Properties sslProps = new Properties();

        Map<String, String> paramsNameMap = new HashMap<>();
        paramsNameMap.put("tls_version", "protocol");
        paramsNameMap.put("ciphers", "enabledCipherSuites");
        paramsNameMap.forEach((key1, value) ->
                Optional.ofNullable(tls.get(key1))
                        .ifPresent(v -> sslProps.setProperty("com.ibm.ssl." + value, v.toString())));

        List<String> keys = Arrays.asList(
                "keyStore", "keyStorePassword", "keyStoreType", "trustStore", "trustStorePassword",
                "trustStoreType", "enabledCipherSuites",
                "protocol", "contextProvider", "keyStoreProvider", "trustStoreProvider",
                "keyManager", "trustManager"
        );
        for (String key : keys) {
            Optional.ofNullable(tls.get(key))
                    .ifPresent(v -> sslProps.setProperty("com.ibm.ssl." + key, v.toString()));
        }
        if (sslProps.size() > 0) {
            opts.setSSLProperties(sslProps);
        }
    }

    @SuppressWarnings("rawtypes")
    private void setupHttpsHostnameVerification(MqttConnectOptions opts, Map tls) {
        AtomicReference<Boolean> checkHostname = new AtomicReference<>();
        Optional.ofNullable(tls.get("check_hostname"))
                .filter(String.class::isInstance).map(String.class::cast)
                .map(Boolean::parseBoolean)
                .ifPresent(checkHostname::set);
        Optional.ofNullable(tls.get("check_hostname"))
                .filter(Boolean.class::isInstance).map(Boolean.class::cast)
                .ifPresent(checkHostname::set);
        Optional.ofNullable(config.get("tls_insecure_set"))
                .filter(Map.class::isInstance)
				.map(Map.class::cast)
				.ifPresent(insecure -> {
					try {
						Optional.of(insecure.get("value"))
							.map(opt -> ! MessageUtils.toBoolean(opt.toString()))
							.ifPresent(checkHostname::set);
					} catch (Throwable e) {
						throw new InvalidConfigurationException("tls_insecure_set: value cannot cast to boolean.", e);
					}
				});
        if (Objects.nonNull(checkHostname.get())) {
            opts.setHttpsHostnameVerificationEnabled(checkHostname.get());
        }
    }

    @SuppressWarnings("unchecked")
    private void setupWill(MqttConnectOptions opts) {
        Optional.ofNullable(config.get("will_set"))
                .filter(Map.class::isInstance).map(Map.class::cast).ifPresent(will -> {

            final boolean willRetain = Optional.ofNullable(will.get("retain"))
                    .map(MessageUtils::toBoolean).orElse(this.retain);
            final int willQos = Optional.ofNullable(will.get("qos"))
                    .map(MessageUtils::toInteger)
                    .orElseGet(this.consistency::getQos);
            final String willTopic = Optional.ofNullable(will.get("topic"))
                    .filter(String.class::isInstance).map(String.class::cast)
                    .orElseThrow(() -> new InvalidConfigurationException("The topic is not set."));

            Optional.ofNullable(will.get("payload"))
                    .map(loggingException(x -> {
                        if (x instanceof byte[]) {
                            return (byte[]) x;
                        } else if (x instanceof String) {
                            return SimpleValueType.TEXT.getSerializer().serialize(x);
                        } else {
                            return this.valueType.getSerializer().serialize(x);
                        }
                    })).ifPresent(payload -> opts.setWill(willTopic, payload, willQos, willRetain));
        });
    }

    private static <T, R> Function<T, R> loggingException(Function<? super T, ? extends R> mapper) {
        return v -> {
            try {
                return mapper.apply(v);
            } catch (Throwable e) {
                log.warning(e.getMessage());
                log.log(Level.FINER, e, e::getMessage);
                return null;
            }
        };
    }

    private Consistency setupConsistency(final Consistency consistency) {
        return Optional.ofNullable(this.config.get("qos"))
                .filter(String.class::isInstance).map(String.class::cast)
                .map(loggingException(Integer::parseInt))
                .map(Consistency::valueOf)
                .orElseGet(() -> Optional.ofNullable(this.config.get("qos"))
                        .filter(Integer.class::isInstance).map(Integer.class::cast)
                        .map(Consistency::valueOf)
                        .orElse(consistency));
    }

    private boolean setupRetain() {
        AtomicReference<Boolean> retain = new AtomicReference<>();
        Optional.ofNullable(this.config.get("retain"))
                .filter(String.class::isInstance).map(String.class::cast)
                .map(Boolean::parseBoolean)
                .ifPresent(retain::set);
        Optional.ofNullable(this.config.get("retain"))
                .filter(Boolean.class::isInstance).map(Boolean.class::cast)
                .ifPresent(retain::set);
        return Optional.ofNullable(retain.get()).orElse(false);
    }

    @SuppressWarnings("rawtypes")
    protected String getServerURI() {
        Object x = this.config.get("brokers");
        if (Objects.isNull(x)) {
            throw new InvalidConfigurationException();
        }
        if (x instanceof List) {
            List xs = (List) x;
            if (xs.size() != 1) {
                throw new InvalidConfigurationException();
            }
            x = xs.get(0);
        }
        if (x instanceof String) {
            String addr = addressToURI((String) x);
            log.fine(() -> "broker address = " + addr);
            return addr;
        }
        throw new InvalidConfigurationException();
    }

    @SuppressWarnings("rawtypes")
    private boolean isSecure() {
        return Optional.ofNullable(config.get("tls_set")).map(x -> {
            if (x instanceof Map) {
                return ((Map) x).size() > 0;
            } else {
                return null;
            }
        }).orElseGet(() -> Optional.ofNullable(config.get("tls")).map(x -> {
            if (x instanceof Map) {
                return ((Map) x).size() > 0;
            } else if (x instanceof Boolean) {
                return (Boolean) x;
            } else {
                return null;
            }
        }).orElse(false));
    }

    private boolean isWebSocket() {
        return Optional.ofNullable(config.get("transport")).filter(String.class::isInstance).map(String.class::cast)
                .map(String::toLowerCase).filter("websockets"::equals).isPresent();
    }

    private String getUriPrefix() {
        if (isWebSocket()) {
            if (isSecure()) {
                return "wss://";
            } else {
                return "ws://";
            }
        } else {
            if (isSecure()) {
                return "ssl://";
            } else {
                return "tcp://";
            }
        }
    }

    private String addressToURI(String x){
        if (!isWebSocket() || Objects.isNull(websocketPath)) {
            return getUriPrefix() + x;
        } else {
            return getUriPrefix() + x + websocketPath;
        }
    }

    public abstract String getClientId();

    public final void close() {
        if (closed.getAndSet(true)) {
            return;
        }
        doClose();
    }

    protected abstract void doClose();
}
