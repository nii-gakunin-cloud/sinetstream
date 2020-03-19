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

package jp.ad.sinet.stream.api;

import lombok.Value;

import java.util.Objects;

@Value
public class Message<T> {
    private T value;
    private String topic;
    private Long timestampMicroseconds;
    private Object raw;

    public Long getTimestamp() {
        if (Objects.isNull(timestampMicroseconds)) {
            return null;
        }
        return timestampMicroseconds / 1000_000L;
    }
}
