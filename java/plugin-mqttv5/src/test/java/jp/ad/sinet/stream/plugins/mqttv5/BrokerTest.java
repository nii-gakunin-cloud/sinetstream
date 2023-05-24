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

package jp.ad.sinet.stream.plugins.mqttv5;

import jp.ad.sinet.stream.api.ConnectionException;
import jp.ad.sinet.stream.api.InvalidConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

@EnabledIfEnvironmentVariable(named="RUN_INTERGRATION_TEST", matches = "(?i)true")
class BrokerTest {

    @Nested
    class StringBroker extends ConfigFileAware.ReaderWriterTest {
    }

    @Nested
    @DisabledIfEnvironmentVariable(named="KAFKA_BROKER_DEFAULT_PORT", matches = "false")
    class DefaultPortBroker extends ConfigFileAware.ReaderWriterTest {
        @Override
        public Optional<Object> getBroker() {
            return Optional.of(System.getenv().getOrDefault("MQTT_BROKER_HOSTNAME", "broker"));
        }
    }

    @Nested
    @DisabledIfEnvironmentVariable(named="KAFKA_BROKER_DEFAULT_PORT", matches = "false")
    class DefaultPortBrokerList extends ConfigFileAware.ReaderWriterTest {
        @Override
        public Optional<Object> getBroker() {
            return Optional.of(Collections.singletonList(
                    System.getenv().getOrDefault("MQTT_BROKER_HOSTNAME", "broker")));
        }
    }

    @Nested
    class ManyBrokers extends ConfigFileAware.ErrorTest {
        @Override
        public Optional<Object> getBroker() {
            return Optional.of(Arrays.asList("broker1:1883", "broker2:1883"));
        }

        @BeforeEach
        @Override
        void setupError() {
            error = InvalidConfigurationException.class;
        }
    }

    @Nested
    class EmptyBrokerList extends ConfigFileAware.ErrorTest {
        @Override
        public Optional<Object> getBroker() {
            return Optional.of(Collections.emptyList());
        }

        @BeforeEach
        @Override
        void setupError() {
            error = InvalidConfigurationException.class;
        }
    }

    @Nested
    class NoBroker extends ConfigFileAware.ErrorTest {
        @Override
        public Optional<Object> getBroker() {
            return Optional.empty();
        }

        @BeforeEach
        @Override
        void setupError() {
            error = InvalidConfigurationException.class;
        }
    }

    @Nested
    class UnknownHost extends ConfigFileAware.ErrorTest {
        @Override
        public Optional<Object> getBroker() {
            return Optional.of("mqtt.example.com:1883");
        }

        @BeforeEach
        @Override
        void setupError() {
            error = ConnectionException.class;
        }
    }

    @Nested
    class Unreachable extends ConfigFileAware.ErrorTest {
        @Override
        public Optional<Object> getBroker() {
            return Optional.of(System.getenv().getOrDefault("MQTT_BROKER_HOSTNAME", "broker") + ":9999");
        }

        @BeforeEach
        @Override
        void setupError() {
            error = ConnectionException.class;
        }
    }
}
