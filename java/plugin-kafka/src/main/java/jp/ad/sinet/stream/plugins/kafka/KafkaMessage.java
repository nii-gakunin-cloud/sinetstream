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

import jp.ad.sinet.stream.spi.PluginMessageWrapper;
import jp.ad.sinet.stream.utils.Timestamped;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.Optional;

@EqualsAndHashCode
@ToString
public class KafkaMessage implements PluginMessageWrapper {

    @Getter
    private final ConsumerRecord<String, byte[]> raw;

    KafkaMessage(ConsumerRecord<String, byte[]> record) {
        this.raw = record;
    }

    @Override
    public Timestamped<byte[]> getValue() {
        return Optional.ofNullable(raw).map(ConsumerRecord::value).map(x -> new Timestamped<byte[]>(x, 0)).orElse(null);
    }

    @Override
    public String getTopic() {
        return Optional.ofNullable(raw).map(ConsumerRecord::topic).orElse(null);
    }
}
