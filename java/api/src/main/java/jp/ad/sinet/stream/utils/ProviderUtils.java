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

import jp.ad.sinet.stream.api.InvalidConfigurationException;
import jp.ad.sinet.stream.api.SinetStreamException;
import jp.ad.sinet.stream.api.UnsupportedServiceTypeException;
import jp.ad.sinet.stream.spi.CryptoProvider;
import jp.ad.sinet.stream.spi.SinetMessageProvider;

import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

class ProviderUtils<T extends SinetMessageProvider> {

    private final Class<T> providerClass;

    ProviderUtils(Class<T> providerClass) {
        this.providerClass = providerClass;
    }

    @SuppressWarnings("WeakerAccess")
    T getProvider(String type) {
        ServiceLoader<T> loader = ServiceLoader.load(providerClass);
        for (T provider : loader) {
            if (provider.getType().equals(type)) {
                return provider;
            }
        }
        throw new UnsupportedServiceTypeException();
    }

    T getProvider(Map<String, ?> parameters) {
        return getProvider(getProviderType(parameters));
    }

    private String getProviderType(Map<String, ?> parameters) {
        return Optional.ofNullable(parameters.get("type")).map(String.class::cast)
                .orElseThrow(InvalidConfigurationException::new);
    }

    @SuppressWarnings("unchecked")
    CryptoProvider getCryptoProvider(Map<String, Object> parameters) {
        ServiceLoader<CryptoProvider> loader = ServiceLoader.load(CryptoProvider.class);
        for (CryptoProvider provider : loader) {
            if (Optional.ofNullable(parameters.get("crypto"))
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .filter(provider::isSupported)
                    .isPresent()) {
                return provider;
            }
        }
        throw new SinetStreamException("NoCryptoProvider");
    }
}
