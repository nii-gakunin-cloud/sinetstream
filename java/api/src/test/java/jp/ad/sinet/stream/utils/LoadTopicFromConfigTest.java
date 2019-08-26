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

import jp.ad.sinet.stream.ConfigFileAware;
import jp.ad.sinet.stream.api.InvalidConfigurationException;
import jp.ad.sinet.stream.api.MessageIO;
import jp.ad.sinet.stream.api.MessageReader;
import jp.ad.sinet.stream.api.MessageWriter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class LoadTopicFromConfigTest implements ConfigFileAware {

    interface LoadTopicFromConfigTests<T extends MessageIO, B> {
        @Test
        default void loadTopic() {
            B factory = getFactory("service-with-topic");
            try (T target = getTarget(factory)) {
                assertNotNull(target);
                assertEquals("test-topic-java-001", target.getTopic());
            }
        }

        @Test
        default void loadTopicAsList() {
            B factory = getFactory("service-with-topic-as-list");
            try (T target = getTarget(factory)) {
                assertNotNull(target);
                assertEquals("test-topic-java-001", target.getTopic());
            }
        }

        @Test
        default void noTopic() {
            B factory = getFactory("service-0");
            assertThrows(IllegalStateException.class, () -> getTarget(factory));
        }

        @Test
        default void emptyListTopic() {
            B factory = getFactory("service-with-empty-topic-list");
            assertThrows(IllegalStateException.class, () -> getTarget(factory));
        }

        B getFactory(String service);
        T getTarget(B factory);
    }

    @Nested
    class MessageWriterTest implements LoadTopicFromConfigTests<MessageWriter<String>, MessageWriterFactory<String>> {

        @Test
        void loadTopics() {
            MessageWriterFactory<String> factory =
                    MessageWriterFactory.<String>builder().service("service-with-multiple-topics").build();
            assertThrows(InvalidConfigurationException.class, factory::getWriter);
        }

        @Override
        public MessageWriterFactory<String> getFactory(String service) {
            return MessageWriterFactory.<String>builder().service(service).build();
        }

        @Override
        public MessageWriter<String> getTarget(MessageWriterFactory<String> factory) {
            return factory.getWriter();
        }
    }

    @Nested
    class MessageReaderTest implements LoadTopicFromConfigTests<MessageReader<String>, MessageReaderFactory<String>> {
        @Test
        void loadTopics() {
            MessageReaderFactory<String> factory = getFactory("service-with-multiple-topics");
            try (MessageReader<String> reader = factory.getReader()) {
                assertNotNull(reader);
                assertIterableEquals(Arrays.asList("test-topic-java-001", "test-topic-java-002"), reader.getTopics());
            }
        }

        @Override
        public MessageReaderFactory<String> getFactory(String service) {
            return MessageReaderFactory.<String>builder().service(service).build();
        }

        @Override
        public MessageReader<String> getTarget(MessageReaderFactory<String> factory) {
            return factory.getReader();
        }
    }
}
