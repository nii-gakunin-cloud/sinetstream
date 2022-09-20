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

import jp.ad.sinet.stream.api.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/*
> Task :SINETStream-api:test
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by javassist.util.proxy.SecurityActions (file:/home/koie/.gradle/caches/modules-2/files-2.1/org.javassist/javassist/3.24.0-GA/d7466fc2e3af7c023e95c510f06448ad29b225b3/javassist-3.24.0-GA.jar) to method java.lang.ClassLoader.defineClass(java.lang.String,byte[],int,int,java.security.ProtectionDomain)
WARNING: Please consider reporting this to the maintainers of javassist.util.proxy.SecurityActions
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release

EnvConfigTest > noServiceByReader FAILED
    org.mockito.exceptions.base.MockitoException at EnvConfigTest.java:52
        Caused by: org.mockito.exceptions.base.MockitoException at EnvConfigTest.java:52
            Caused by: java.lang.IllegalStateException at EnvConfigTest.java:52
                Caused by: java.lang.UnsupportedOperationException at EnvConfigTest.java:52

import org.junit.jupiter.api.Disabled;
@Disabled("!!!org.mockito.exceptions.base.MockitoException!!!")
*/
/*
@RunWith(PowerMockRunner.class)
@PrepareForTest(ConfigLoader.class)
*/
public class EnvConfigTest {
/*

    @Rule
    public ExpectedException ex = ExpectedException.none();

    @Before
    public void setupEnv() {
        URL url = EnvConfigTest.class.getResource("/sinetstream_config.yml");
        PowerMockito.spy(System.class);
        PowerMockito.when(System.getenv("SINETSTREAM_CONFIG_URL")).thenReturn(url.toString());
    }

    @Test
    public void getWriter() {
        String service = "service-0";
        String topic = "test-topic-java-001";
        MessageWriterFactory<String> builder = MessageWriterFactory.<String>builder().service(service).topic(topic).build();
        try (MessageWriter<String> writer = builder.getWriter()) {
            assertThat(writer, is(notNullValue()));
            assertThat(writer.getService(), is(service));
            assertThat(writer.getTopic(), is(topic));
            assertThat(writer.getConfig().get("type"), is("dummy"));
        }
    }

    @Test
    public void unsupportedServiceTypeByWriter() {
        ex.expect(UnsupportedServiceTypeException.class);
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder().service("service-1").topic("test-topic-java-001").build();
        builder.getWriter();
    }

    @Test
    public void noServiceByWriter() {
        ex.expect(NoServiceException.class);
        MessageWriterFactory<String> factory = MessageWriterFactory.<String>builder().service("service-X").topic("test-topic-java-001").build();
        factory.getWriter();
    }

    @Test
    public void noServiceTypeByWriter() {
        ex.expect(InvalidConfigurationException.class);
        MessageWriterFactory<String> builder =
                MessageWriterFactory.<String>builder().service("service-Z").topic("test-topic-java-001").build();
        builder.getWriter();
    }

    @Test
    public void getReader() {
        String service = "service-0";
        List<String> topics = Arrays.asList("test-topic-java-001", "test-topic-java-002");
        MessageReaderFactory<String> builder = MessageReaderFactory.<String>builder().service(service).topics(topics).build();
        try (MessageReader<String> reader = builder.getReader()) {
            assertThat(reader, is(notNullValue()));
            assertThat(reader.getService(), is(service));
            assertThat(reader.getTopics(), is(topics));
        }
    }

    @Test
    public void unsupportedServiceTypeByReader() {
        ex.expect(UnsupportedServiceTypeException.class);
        MessageReaderFactory<String> builder =
                MessageReaderFactory.<String>builder().service("service-1").topic("test-topic-java-001").build();
        builder.getReader();
    }

    @Test
    public void noServiceByReader() {
        ex.expect(NoServiceException.class);
        MessageReaderFactory<String> factory = MessageReaderFactory.<String>builder().service("service-X").topic("test-topic-java-001").build();
        factory.getReader();
    }

    @Test
    public void noServiceTypeByReader() {
        ex.expect(InvalidConfigurationException.class);
        MessageReaderFactory<String> builder =
                MessageReaderFactory.<String>builder().service("service-Z").topic("test-topic-java-001").build();
        builder.getReader();
    }
*/
}
