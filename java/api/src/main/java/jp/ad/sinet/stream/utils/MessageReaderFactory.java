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

package jp.ad.sinet.stream.utils;

import jp.ad.sinet.stream.api.*;
import jp.ad.sinet.stream.api.valuetype.SimpleValueType;
import jp.ad.sinet.stream.spi.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.java.Log;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static jp.ad.sinet.stream.api.Consistency.AT_MOST_ONCE;

@Log
@Builder
public class MessageReaderFactory<T> {

    @Getter
    @Description("Provider name")
    private String type;

    @Getter
    @Description("Service name defined in the configuration file.")
    private String service;

    @Singular
    @Getter
    @Description(value="A list of topics to receive.", singular="The topic to receive.")
    private List<String> topics;

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

    @Getter
    @Parameter("user_data_only")
    @Description("User data is recv from the messaging layer without appending SINETStream header.")
    private Boolean userDataOnly;

    @Getter
    @Parameter(value="receive_timeout_ms", hide=true)
    @Description("The maximum wait time for receiving a message.")
    private Duration receiveTimeout;

    @Singular
    @Getter
    @Description(value = "Overwrites the parameters described in the configuration file with the specified values.",
            singular = "Rewrites the parameters described in the configuration file only for the specified key / value pairs.")
    private Map<String, Object> parameters;

    @Getter
    @Description("If not specified, use default deserializer according to valueType.")
    private Deserializer<T> deserializer;

    @Getter
    @Parameter("data_compression")
    @Description("Message compression.")
    private Boolean dataCompression;

    @Getter
    @Parameter("data_encryption")
    @Description("Message encryption.")
    private Boolean dataEncryption;

    @Getter
    @Parameter("no_config")
    @Description("Don't read a configuration file")
    @Builder.Default
    private Boolean noConfig = false;

    @Getter
    @Parameter(value="config_file", hide=true)
    @Description("configuration file.")
    private Path configFile;

    @Getter
    @Parameter("config_name")
    @Description("configuration name.")
    private String configName;

    @Getter
    @Parameter(hide=true)
    @Description("auth_file")
    private Path authFile;

    @Getter
    @Parameter(hide=true)
    @Description("private_key_file")
    private Path privKeyFile;

    @Getter
    @Parameter(hide=true)
    @Description("debugHttpTransport")
    private Object debugHttpTransport;

    @DefaultParameters
    public static final Map<String, Object> defaultValues;
    static {
        Map<String, Object> values = new HashMap<>();
        values.put("consistency", AT_MOST_ONCE);
        values.put("value_type", SimpleValueType.BYTE_ARRAY);
        values.put("user_data_only", false);
        values.put("data_compression", false);
        values.put("data_encryption", false);
        values.put("receive_timeout_ms", Duration.ofNanos(Long.MAX_VALUE));
        defaultValues = Collections.unmodifiableMap(values);
    }

    @SuppressWarnings("unchecked")
    public MessageReader<T> getReader() {
        setupServiceParameters();
        ProviderUtils<MessageReaderProvider> util = new ProviderUtils<>(MessageReaderProvider.class);
        MessageReaderProvider provider = util.getProvider(parameters);
        if (dataEncryption) {
            CryptoProvider cryptoProvider = util.getCryptoProvider(parameters);
            Optional.ofNullable(parameters.get("crypto"))
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .ifPresent(params -> params.put("provider", cryptoProvider.getCrypto(params)));
        }
        ReaderParameters params = new ReaderParameters(this);
        PluginMessageReader pluginReader = provider.getReader(params);
        return new SinetStreamMessageReader<>(pluginReader, params, deserializer);
    }

    @SuppressWarnings("unchecked")
    public AsyncMessageReader<T> getAsyncReader() {
        setupServiceParameters();
        ProviderUtils<AsyncMessageReaderProvider> util = new ProviderUtils<>(AsyncMessageReaderProvider.class);
        try {
            AsyncMessageReaderProvider provider = util.getProvider(parameters);
            if (dataEncryption) {
                CryptoProvider cryptoProvider = util.getCryptoProvider(parameters);
                Optional.ofNullable(parameters.get("crypto"))
                        .filter(Map.class::isInstance)
                        .map(Map.class::cast)
                        .ifPresent(params -> params.put("provider", cryptoProvider.getCrypto(params)));
            }
            ReaderParameters params = new ReaderParameters(this);
            PluginAsyncMessageReader pluginReader = provider.getAsyncReader(params);
            return new SinetStreamAsyncMessageReader<>(pluginReader, params, deserializer);

        } catch (UnsupportedServiceTypeException e) {
            return new SinetStreamAsyncWrapperMessageReader<>(getReader());
        }
    }

    @Parameter(hide=true)
    private List<File> tmpLst;
    public List<File> getTmpLst() { return tmpLst; }

    private void setupServiceParameters() {
        MessageUtils utils = new MessageUtils();
        Map<String, Object> serviceParameters = new HashMap<>(defaultValues);
        if (!noConfig && !MessageUtils.toBoolean(parameters.getOrDefault("no_config", false)))
            serviceParameters.putAll(utils.loadServiceParameters(service, configFile, configName, authFile, privKeyFile, debugHttpTransport));
        utils.mergeParameters(serviceParameters, parameters);
        tmpLst = new LinkedList<File>();
        ConfigLoader.replaceInlineData(serviceParameters, tmpLst);
        updateFactoryParameters(serviceParameters);
        /* XXX broker plugin should whether check topics is set.
        if (topics.isEmpty()) {
            throw new InvalidConfigurationException("Topic has not been set.");
        }
        */
        if (!parameters.containsKey("type") && type != null) {
            parameters.put("type", type);
        }
    }

    private void updateFactoryParameters(Map<String, Object> params) {
        if (Objects.isNull(type)) {
            Optional.ofNullable(params.get("type")).map(MessageUtils::toString)
                    .ifPresent(x -> type = x);
        }
        if (Objects.isNull(consistency)) {
            Optional.ofNullable(params.get("consistency")).map(MessageUtils::toConsistency)
                    .ifPresent(x -> consistency = x);
        }
        if (Objects.isNull(valueType)) {
            Optional.ofNullable(params.get("value_type")).map(MessageUtils::toMessageType)
                    .ifPresent(x -> valueType = x);
        }
        if (Objects.isNull(userDataOnly)) {
            Optional.ofNullable(params.get("user_data_only")).map(MessageUtils::toBoolean)
                    .ifPresent(x -> userDataOnly = x);
        }
        if (Objects.isNull(dataCompression)) {
            Optional.ofNullable(params.get("data_compression")).map(MessageUtils::toBoolean)
                    .ifPresent(x -> dataCompression = x);
        }
        if (Objects.isNull(dataEncryption)) {
            Optional.ofNullable(params.get("data_encryption")).map(MessageUtils::toBoolean)
                    .ifPresent(x -> dataEncryption = x);
        }
        if (Objects.isNull(clientId)) {
            Optional.ofNullable(params.get("client_id")).filter(String.class::isInstance).map(String.class::cast)
                    .ifPresent(x -> clientId = x);
        }
        if (topics.isEmpty()) {
            Optional.ofNullable(params.get("topic")).map(toTopic)
                    .ifPresent(x -> topics = Collections.unmodifiableList(x));
        }
        if (Objects.isNull(receiveTimeout)) {
            Optional.ofNullable(params.get("receive_timeout_ms")).map(MessageUtils::toReceiveTimeout)
                    .ifPresent(x -> receiveTimeout = x);
        }
        if (Objects.isNull(deserializer)) {
            updateDeserializer(params);
        }
        Optional.ofNullable(params.get("brokers")).map(MessageUtils::toBrokers)
                .ifPresent(x -> params.put("brokers", x));
        parameters = params;
    }

    @SuppressWarnings("unchecked")
    private void updateDeserializer(Map<String, Object> params) {
        Optional.ofNullable(params.get("value_deserializer"))
                .map(MessageUtils::toDeserializer)
                .ifPresent(x -> deserializer = x);
    }

    @SuppressWarnings("unchecked")
    private static Function<Object, List<String>> toTopic = value -> {
        if (value instanceof String) {
            return Collections.singletonList((String) value);
        } else if (value instanceof List) {
            @SuppressWarnings("rawtypes") List items = (List) value;
            return (List<String>) items.stream().map(Object::toString).collect(Collectors.toList());
        }
        return null;
    };
}
