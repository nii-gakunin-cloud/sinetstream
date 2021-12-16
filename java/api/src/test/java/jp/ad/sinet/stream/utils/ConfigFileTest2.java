/*
 * Copyright (C) 2021 National Institute of Informatics
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
import jp.ad.sinet.stream.api.MessageReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Map;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;
import java.io.InputStream;


public class ConfigFileTest2 implements ConfigFileAware {

    private String readfile(String path) throws Exception {
        return new String(readbinfile(path));
    }
    private byte[] readbinfile(String path) throws Exception {
        return Files.readAllBytes(Paths.get(path));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testBinary() throws Exception {
        final String SERVICE = "service-with-binary";
        byte[] expected = "This is a test".getBytes();
        MessageReaderFactory<String> readerBuilder =
                MessageReaderFactory.<String>builder().service(SERVICE).build();
        try (MessageReader<String> reader = readerBuilder.getReader()) {
            byte[] actual = (byte[])reader.getConfig().get("binparam");
            assertArrayEquals(expected, actual);

            assertEquals("hoge contents", (String)reader.getConfig().get("hoge_data"));
            assertEquals("hoge contents", readfile((String)reader.getConfig().get("hoge")));
            byte[] boke_data = {0,1,2,3,4,5,6,7,8,9,10,11};
            assertArrayEquals(boke_data, (byte[])reader.getConfig().get("boke_data"));
            assertArrayEquals(boke_data, readbinfile((String)reader.getConfig().get("boke")));

            Map<String, Object> boke = (Map<String, Object>) reader.getConfig().get("fuga");
            assertEquals("hoge contents 2", (String)boke.get("hoge_data"));
            assertEquals("hoge contents 2", readfile((String)boke.get("hoge")));
        }
    }

    @Test
    void testEncrypted() throws Exception {
        Path privateKeyFile = Paths.get("private_key.pem");
        try (InputStream in = ConfigClientTest.class.getResourceAsStream("/private_key.pem")) {
            Files.copy(in, privateKeyFile);
            final String SERVICE = "service-with-encrypted";
            MessageReaderFactory<String> readerBuilder =
                    MessageReaderFactory.<String>builder().service(SERVICE).privKeyFile(privateKeyFile).build();
            try (MessageReader<String> reader = readerBuilder.getReader()) {
                byte[] actual = (byte[])reader.getConfig().get("test-secret");
                assertArrayEquals("This is a test.\r\n".getBytes(), actual);
            }
        }
        finally {
            Files.deleteIfExists(privateKeyFile);
        }
    }
}
