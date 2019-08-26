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

import jp.ad.sinet.stream.api.Message;
import lombok.Data;
import lombok.Getter;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.Optional;

@Data
public class KafkaMessage<T> implements Message<T> {

    @Getter
    private final ConsumerRecord<?, T> raw;

    KafkaMessage(ConsumerRecord<?, T> record) {
        this.raw = record;
    }

    @Override
    public T getValue() {
        return Optional.ofNullable(raw).map(ConsumerRecord::value).orElse(null);
    }

    @Override
    public String getTopic() {
        return Optional.ofNullable(raw).map(ConsumerRecord::topic).orElse(null);
    }
}
