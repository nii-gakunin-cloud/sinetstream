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

package jp.ad.sinet.stream.api.valuetype;

import jp.ad.sinet.stream.api.ValueType;
import jp.ad.sinet.stream.spi.ValueTypeProvider;

import java.util.*;
import java.util.stream.Collectors;

public class ValueTypeFactory {

    private static final Map<String, ValueType> valueTypes;
    static {
        HashMap<String, ValueType> map = new HashMap<>();
        map.putAll(Arrays.stream(SimpleValueType.values())
                .collect(Collectors.toMap(SimpleValueType::getName, v -> v)));

        ServiceLoader<ValueTypeProvider> loader = ServiceLoader.load(ValueTypeProvider.class);
        for (ValueTypeProvider provider : loader) {
            map.put(provider.getName(), provider.getValueType());
        }
        valueTypes = Collections.unmodifiableMap(map);
    }

    public ValueType get(String name) {
        return valueTypes.get(name);
    }
}
