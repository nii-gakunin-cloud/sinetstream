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

package jp.ad.sinet.stream.api;

import lombok.Getter;
import lombok.extern.java.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Log
public enum Consistency {
    AT_MOST_ONCE(0),
    AT_LEAST_ONCE(1),
    EXACTLY_ONCE(2);

    @Getter
    private final int qos;

    Consistency(int qos) {
        this.qos = qos;
    }

    public static Consistency valueOf(int value) {
        return Optional.ofNullable(valueMap.get(value)).orElseGet(() -> {
            log.warning("does not exist: " + value);
            return null;
        });
    }

    private static final Map<Integer, Consistency> valueMap = new HashMap<>();
    static {
        for (Consistency c : Consistency.values()) {
            valueMap.put(c.getQos(), c);
        }
    }
}
