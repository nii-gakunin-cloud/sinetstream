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

import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface MessageReader<T> extends MessageIO {

    Message<T> read();

    default Stream<Message<T>> stream() {
        Iterator<Message<T>> iterator = new Iterator<Message<T>>() {
            private Message<T> cache = null;

            @Override
            public boolean hasNext() {
                this.cache = read();
                return Objects.nonNull(this.cache);
            }

            @Override
            public Message<T> next() {
                return Optional.ofNullable(this.cache).orElseGet(() -> read());
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED | Spliterator.NONNULL), false);
    }

    List<String> getTopics();

    Duration getReceiveTimeout();

    Deserializer<T> getDeserializer();
}
