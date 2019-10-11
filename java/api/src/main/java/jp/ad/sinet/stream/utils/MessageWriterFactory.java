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

package jp.ad.sinet.stream.utils;

import jp.ad.sinet.stream.api.*;
import jp.ad.sinet.stream.spi.CryptoProvider;
import jp.ad.sinet.stream.spi.MessageWriterProvider;
import jp.ad.sinet.stream.spi.WriterParameters;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.java.Log;

import java.util.*;
import java.util.function.Function;

import static jp.ad.sinet.stream.api.Consistency.AT_MOST_ONCE;

@Log
@Builder
public class MessageWriterFactory<T> {

    @Getter
    @Parameter(required=true)
    @Description("Service name defined in the configuration file.")
    private String service;

    @Getter
    @Description(value="The topic to send.")
    private String topic;

    @Getter
    @Description("consistency: AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE")
    private Consistency consistency;

    @Getter
    @Parameter("client_id")
    @Description("If not specified, the value is automatically generated.")
    private String clientId;

    @Getter
    @Parameter("value_type")
    @Description("The type of message.")
    private ValueType valueType;

    @Singular
    @Getter
    @Description(value = "Overwrites the parameters described in the configuration file with the specified values.",
            singular = "Rewrites the parameters described in the configuration file only for the specified key / value pairs.")
    private Map<String, Object> parameters;

    @Getter
    @Description("If not specified, use default serializer according to valueType.")
    private Serializer<T> serializer;

    @Getter
    @Parameter("data_encryption")
    @Description("Message encryption.")
    private Boolean dataEncryption;

    @DefaultParameters
    public static final Map<String, Object> defaultValues;
    static {
        Map<String, Object> values = new HashMap<>();
        values.put("consistency", AT_MOST_ONCE);
        values.put("value_type", ValueType.TEXT);
        values.put("data_encryption", false);
        defaultValues = Collections.unmodifiableMap(values);
    }

    @SuppressWarnings("unchecked")
    public MessageWriter<T> getWriter() {
        setupServiceParameters();
        ProviderUtils<MessageWriterProvider<T>> util = new ProviderUtils(MessageWriterProvider.class);
        MessageWriterProvider<T> provider = util.getProvider(parameters);
        if (dataEncryption) {
            CryptoProvider cryptoProvider = util.getCryptoProvider(parameters);
            Optional.ofNullable(parameters.get("crypto"))
                    .filter(Map.class::isInstance).map(Map.class::cast)
                    .ifPresent(params -> params.put("provider", cryptoProvider.getCrypto(params)));
        }
        return provider.getWriter(new WriterParameters<>(this));
    }

    private void setupServiceParameters() {
        MessageUtils utils = new MessageUtils();
        Map<String, Object> serviceParameters = new HashMap<>(defaultValues);
        serviceParameters.putAll(utils.loadServiceParameters(service));
        utils.mergeParameters(serviceParameters, parameters);
        updateFactoryParameters(serviceParameters);
        if (Objects.isNull(topic)) {
            throw new IllegalStateException("Topic has not been set.");
        }
    }

    private void updateFactoryParameters(Map<String, Object> params) {
        if (Objects.isNull(consistency)) {
            Optional.ofNullable(params.get("consistency")).map(MessageUtils::toConsistency)
                    .ifPresent(x -> consistency = x);
        }
        if (Objects.isNull(valueType)) {
            Optional.ofNullable(params.get("value_type")).map(MessageUtils::toMessageType)
                    .ifPresent(x -> valueType = x);
        }
        if (Objects.isNull(dataEncryption)) {
            Optional.ofNullable(params.get("data_encryption")).map(MessageUtils::toBoolean)
                    .ifPresent(x -> dataEncryption = x);
        }
        if (Objects.isNull(clientId)) {
            Optional.ofNullable(params.get("client_id")).filter(String.class::isInstance).map(String.class::cast)
                    .ifPresent(x -> clientId = x);
        }
        if (Objects.isNull(topic)) {
            Optional.ofNullable(params.get("topic")).map(toTopic).ifPresent(x -> topic = x);
        }
        if (Objects.isNull(serializer)) {
            updateSerializer(params);
        }
        Optional.ofNullable(params.get("brokers")).map(MessageUtils::toBrokers)
                .ifPresent(x -> params.put("brokers", x));
        parameters = params;
    }

    @SuppressWarnings("unchecked")
    private void updateSerializer(Map<String, Object> params) {
        Optional.ofNullable(params.get("value_serializer"))
                .map(MessageUtils::toSerializer)
                .ifPresent(x -> serializer = x);
    }

    private static Function<Object, String> toTopic = value -> {
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof List) {
            List items = (List) value;
            switch (items.size()) {
                case 0:
                    break;
                case 1:
                    Object item = items.get(0);
                    if (item instanceof String) {
                        return (String) item;
                    }
                    break;
                default:
                    log.warning("You cannot set multiple topics in MessageWriter.");
                    throw new InvalidConfigurationException();
            }
        }
        return null;
    };
}
