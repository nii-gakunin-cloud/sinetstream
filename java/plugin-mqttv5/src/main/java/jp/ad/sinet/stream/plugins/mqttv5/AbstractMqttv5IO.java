/*
 * Copyright (C) 2023 National Institute of Informatics
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

package jp.ad.sinet.stream.plugins.mqttv5;

import jp.ad.sinet.stream.api.*;
import jp.ad.sinet.stream.api.valuetype.SimpleValueType;
import jp.ad.sinet.stream.utils.KeyStoreUtil;
import jp.ad.sinet.stream.utils.MessageUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttClientPersistence;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttSecurityException;
//import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.client.persist.MqttDefaultFilePersistence;
import org.eclipse.paho.mqttv5.common.MqttMessage;

import org.eclipse.paho.mqttv5.common.packet.UserProperty;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.logging.Level;

@Log
public abstract class AbstractMqttv5IO<T> {

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
    final MqttConnectionOptions connectionOptions;

    private final String websocketPath;

    int reconnectMinDelay = 1;

    /* XXX たぶんいらなくなった
    @Getter
    @Setter
    int reconnectDelay = 1;
    */

    protected final AtomicBoolean closed = new AtomicBoolean(false);

    private static String generateClientId() {
        return "sinetstream-" + System.nanoTime();
    }

    AbstractMqttv5IO(String service, Consistency consistency, String clientId, Map<String, ?> config,
                   ValueType valueType, boolean dataEncryption) {
        this.service = service;
        this.config = Collections.unmodifiableMap(config);
        this.valueType = valueType;
        this.consistency = setupConsistency(consistency);
        this.retain = setupRetain();
        this.connectionOptions = setupConnectionOptions();
        this.dataEncryption = dataEncryption;
        this.websocketPath = getWebSocketPath();

        //XXX this.client = newMqttClient(clientId); 単にこれだけだと cleanStart=true のときにMqttDefaultFilePersistence.checkIsOpen()で落ちる。
        //XXX ちゃんと理解できてない
        //XXX pahoはclientId==nullだと = "" してるのが悪いんかも。
        //XXX MQTTv5でclientIDをブローカー側で生成してCONNACKで返せるようになってるはずだが。
        String realClientId = Optional.ofNullable(clientId).filter(x -> x.trim().length() > 0)
                .orElseGet(AbstractMqttv5IO::generateClientId);
        log.fine(() -> "mqtt clientid = " + realClientId);
        this.client = newMqttClient(realClientId);
    }

    protected abstract T newMqttClient(String realClientId);

    protected abstract IMqttToken mqttConnect(MqttConnectionOptions opts) throws MqttException;

    protected MqttClientPersistence getPersistence() {
        Path dataDir = Paths.get(System.getProperty("user.home"), ".mqtt-persistence");
        //return new MemoryPersistence();
        return new MqttDefaultFilePersistence(dataDir.normalize().toString());
    }

    void connect() {
        if (closed.get()) {
            return;
        }
        log.fine(() -> "Connect to the broker: " + getClientId());
        try {
            IMqttToken ret = mqttConnect(connectionOptions);
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
            int reasonCode = e.getReasonCode();
            if (reasonCode == 135) {
                // Not authorized
                throw new AuthenticationException(e);
            } else {
                throw new ConnectionException(e);
            }
        }
    }

    private MqttConnectionOptions setupConnectionOptions() {
        final MqttConnectionOptions opts = new MqttConnectionOptions();

        Optional.ofNullable(config.get("username_pw_set"))
                .filter(Map.class::isInstance).map(Map.class::cast).ifPresent(x -> {
            Optional.ofNullable(x.get("username"))
                    .filter(String.class::isInstance).map(String.class::cast)
                    .ifPresent(opts::setUserName);
            Optional.ofNullable(x.get("password"))
                    .filter(String.class::isInstance).map(String.class::cast)
                    //.map(String::toCharArray).ifPresent(opts::setPassword);
                    .map(String::getBytes).ifPresent(opts::setPassword);
        });

        assert ((String)config.get("protocol")).equals(Mqttv5Version.MQTTv5.name());

        Optional.ofNullable(config.get("receive_maximum"))
                .map(loggingException(MessageUtils::toInteger))
                .ifPresent(opts::setReceiveMaximum);
        // XXX v3との互換性のために残した方がよいのだろうか？
        // XXX python版はmax_inflight_messages_setのままだしなぁ
        // XXX v5では0x21 Receive Maximumが追加されているしなぁ。
        // XXX とおもったらちがった。python実装を読むと"receive_maximum"とmax_inflight_messages_setの両方が使われていて微妙に違うロジックになっている。
        Optional.ofNullable(config.get("max_inflight_messages_set"))
                .map(loggingException(MessageUtils::toInteger))
                .ifPresent(v -> {
                    log.warning("use receive_maximum: instead of max_inflight_messages_set: in MQTTv5");
                    opts.setReceiveMaximum(v);
                });

        Optional.ofNullable(config.get("maximum_packet_size"))
                .map(loggingException(MessageUtils::toLong))
                .ifPresent(opts::setMaximumPacketSize);

        Optional.ofNullable(config.get("topic_alias_maximum"))
                .map(loggingException(MessageUtils::toInteger))
                .ifPresent(opts::setTopicAliasMaximum);

        Optional.ofNullable(config.get("request_response_info"))
                .map(loggingException(MessageUtils::toBoolean))
                .ifPresent(opts::setRequestResponseInfo);

        Optional.ofNullable(config.get("request_problem_info"))
                .map(loggingException(MessageUtils::toBoolean))
                .ifPresent(opts::setRequestProblemInfo);

        Optional.ofNullable(config.get("user_property"))
                .filter(Map.class::isInstance).map(Map.class::cast).ifPresent(x -> {
                    try {
                        List<UserProperty> userProperties = new ArrayList<UserProperty>(x.size());
                        @SuppressWarnings("unchecked")
                        Map<String, String> xx =  (Map<String,String>)x;
                        xx.forEach((k, v) -> userProperties.add(new UserProperty(k, v)));
                        opts.setUserProperties(userProperties);
                    } catch (Throwable e) {
                        throw new IllegalArgumentException("user property must be Map<String, String>");
                        // log.warning(e.getMessage());
                        // log.log(Level.FINER, e, e::getMessage);
                    }
                });

        Optional.ofNullable(config.get("auth_method"))
                .map(loggingException(MessageUtils::toString))
                .ifPresent(opts::setAuthMethod);

        Optional.ofNullable(config.get("auth_data"))
                .map(loggingException(x -> { return (byte[])x; }))
                .ifPresent(opts::setAuthData);

        Optional.ofNullable(config.get("clean_start")).map(loggingException(MessageUtils::toBoolean))
                .ifPresent(opts::setCleanStart);
        // XXX v3との互換性のために残した方がよいのだろうか？
        Optional.ofNullable(config.get("clean_session")).map(loggingException(MessageUtils::toBoolean))
                .ifPresent(v -> {
                    log.warning("use clean_start: instead of clean_session: in MQTTv5");
                    opts.setCleanStart(v);
                });

        Optional.ofNullable(config.get("session_expiry_interval")).map(loggingException(MessageUtils::toLong))
                .ifPresent(opts::setSessionExpiryInterval);

        Optional.ofNullable(config.get("ws_set_options"))
                .filter(Map.class::isInstance).map(Map.class::cast)
                .flatMap(wsOpt -> Optional.ofNullable(wsOpt.get("headers"))
                .map(headers -> {
                    if (headers instanceof Properties) {
                        @SuppressWarnings("unchecked")
                        Map<String,String> h = (Map<String,String>) headers;
                        return h;
                    } else if (headers instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String,String> h = (Map<String,String>) headers;
                        return h;
                    } else {
                        return null;
                    }
                })).ifPresent(opts::setCustomWebSocketHeaders);

        Optional.ofNullable(config.get("reconnect_delay_set"))
                .filter(Map.class::isInstance).map(Map.class::cast)
                .ifPresent(delayOpt -> {
                    /* XXX gomi
                    Optional.ofNullable(delayOpt.get("max_delay"))
                            .map(loggingException(MessageUtils::toInteger))
                            .ifPresent(opts::setMaxReconnectDelay);
                    Optional.ofNullable(delayOpt.get("min_delay"))
                            .map(loggingException(MessageUtils::toInteger))
                            .ifPresent(v -> reconnectMinDelay = reconnectDelay = v);
                    */
                    /* XXX ちょっとださいのであとでけす
                    Integer minDelay = loggingException(MessageUtils::toInteger).apply(delayOpt.get("min_delay"));
                    Integer maxDelay = loggingException(MessageUtils::toInteger).apply(delayOpt.get("max_delay"));
                    if (maxDelay != null) {
                        opts.setMaxReconnectDelay(maxDelay);
                    }
                    if (minDelay != null || maxDelay != null) {
                        opts.setAutomaticReconnectDelay(MessageUtils.or(minDelay, opts.getAutomaticReconnectMinDelay()),
                                                        MessageUtils.or(maxDelay, opts.getAutomaticReconnectMaxDelay()));
                    }
                    */
                    Optional.ofNullable(delayOpt.get("min_delay"))
                            .map(loggingException(MessageUtils::toInteger))
                            .ifPresent(v -> {
                                opts.setAutomaticReconnectDelay(v,
                                                                opts.getAutomaticReconnectMaxDelay());
                            });
                    Optional.ofNullable(delayOpt.get("max_delay"))
                            .map(loggingException(MessageUtils::toInteger))
                            .ifPresent(v -> {
                                opts.setAutomaticReconnectDelay(opts.getAutomaticReconnectMinDelay(),
                                                                v);
                                opts.setMaxReconnectDelay(v * 1000);
                            });
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
                    // XXX setMaxReconnectDelay が二か所にあるのはどうなのか？
                    Optional.ofNullable(connectOpts.get("max_reconnect_delay"))
                            .map(loggingException(MessageUtils::toInteger))
                            .ifPresent(opts::setMaxReconnectDelay);
                    Optional.ofNullable(connectOpts.get("use_subscription_identifiers"))
                            .map(loggingException(MessageUtils::toBoolean))
                            .ifPresent(opts::setUseSubscriptionIdentifiers);
                    Optional.ofNullable(connectOpts.get("send_reason_messages"))
                            .map(loggingException(MessageUtils::toBoolean))
                            .ifPresent(opts::setSendReasonMessages);
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
    private void setupSSLOptions(MqttConnectionOptions opts) {
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
    private void setupSSLProperties(MqttConnectionOptions opts, Map tls) {
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
    private void setupHttpsHostnameVerification(MqttConnectionOptions opts, Map tls) {
        AtomicReference<Boolean> checkHostname = new AtomicReference<>();
        Optional.ofNullable(tls.get("check_hostname"))
                .map(loggingException(MessageUtils::toBoolean))
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
    private void setupWill(MqttConnectionOptions opts) {
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
            final Long willDelay = Optional.ofNullable(will.get("delay_interval"))
                    .map(MessageUtils::toLong).orElse(null);
            final Function<Long, MqttProperties> fx = wd -> {
                Byte[] validProperties = new Byte[1];
                validProperties[0] = MqttProperties.WILL_DELAY_INTERVAL_IDENTIFIER;
                MqttProperties wdp = new MqttProperties(validProperties);
                wdp.setWillDelayInterval(wd);
                return wdp;
            };
            final MqttProperties willDelayProp = (willDelay != null) ? fx.apply(willDelay) : null;

            Optional.ofNullable(will.get("payload"))
                    .map(loggingException(x -> {
                        if (x instanceof byte[]) {
                            return (byte[]) x;
                        } else if (x instanceof String) {
                            return SimpleValueType.TEXT.getSerializer().serialize(x);
                        } else {
                            return this.valueType.getSerializer().serialize(x);
                        }
                    })).ifPresent(payload -> {
                                opts.setWill(willTopic, new MqttMessage(payload, willQos, willRetain, null));
                                if (willDelayProp != null)
                                    opts.setWillMessageProperties(willDelayProp);
                            });
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
                .map(loggingException(MessageUtils::toBoolean))
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
