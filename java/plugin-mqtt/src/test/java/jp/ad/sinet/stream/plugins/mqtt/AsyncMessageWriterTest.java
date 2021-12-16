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

package jp.ad.sinet.stream.plugins.mqtt;

import jp.ad.sinet.stream.api.AsyncMessageWriter;
import jp.ad.sinet.stream.api.Consistency;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import lombok.extern.java.Log;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Timeout(60)
@Log
@EnabledIfEnvironmentVariable(named="RUN_INTERGRATION_TEST", matches = "(?i)true")
class AsyncMessageWriterTest implements ConfigFileAware {

    private List<String> lines;
    @TempDir
    Path workdir;

    @ParameterizedTest
    @EnumSource(Consistency.class)
    void writeMessages(Consistency consistency) throws InterruptedException {
        final AtomicInteger count = new AtomicInteger(0);
        CountDownLatch done = new CountDownLatch(lines.size());
        MessageWriterFactory<String> factory = MessageWriterFactory.<String>builder()
                .configFile(getConfigFile(workdir)).service(getServiceName())
                .consistency(consistency)
                .parameter("max_inflight_messages_set", 20)
                .build();
        try (AsyncMessageWriter<String> writer = factory.getAsyncWriter()) {
            lines.forEach(line ->
                    writer.write(line)
                            .then(v -> count.getAndIncrement())
                            .fail(e -> log.log(Level.WARNING, "write error", e))
                            .always((state, r, e) -> done.countDown()));
            done.await();
        }
        assertEquals(lines.size(), count.get());
    }

    @BeforeEach
    void setupMessages() {
        lines = IntStream.range(0, 50)
                .mapToObj(x -> RandomStringUtils.randomAlphabetic(10))
                .collect(Collectors.toList());
    }
}
