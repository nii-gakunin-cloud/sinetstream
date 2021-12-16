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

package jp.ad.sinet.stream.crypto;

import jp.ad.sinet.stream.api.Crypto;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JCEProcessorTest {

    static Stream<Arguments> jceParameters() {
        List<Arguments> ret = new ArrayList<>();
        for (String mode : Arrays.asList("CCB", "CFB", "OFB", "CTR", "OpenPGP")) {
            for (String padding : Arrays.asList("", "pkcs7", "iso7816", "x923", "PKCS7Padding", "ISO10126Padding")) {
                ret.add(Arguments.arguments(mode, padding));
            }
        }
        return ret.stream();
    }

    @ParameterizedTest
    @MethodSource("jceParameters")
    void aes() {
        Map<String, Object> params = new HashMap<>();
        params.put("algorithm", "AES");

        params.put("mode", "CBC");
        params.put("padding", "pkcs7");

        params.put("password", "secret-001");

        JCEProvider provider = new JCEProvider();
        assertTrue(provider.isSupported(params));
        Crypto crypto = provider.getCrypto(params);
        Function<byte[], byte[]> enc = crypto.getEncoder(params);
        Function<byte[], byte[]> dec = crypto.getDecoder(params);

        byte[] data = "abcdefg".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = enc.apply(data);
        byte[] data1 = dec.apply(encrypted);
        assertArrayEquals(data, data1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"EAX", "GCM"})
    void aesAAD(String mode) {
        Map<String, Object> params = new HashMap<>();
        params.put("algorithm", "AES");
        params.put("mode", mode);

        params.put("password", "secret-001");

        JCEProvider provider = new JCEProvider();
        assertTrue(provider.isSupported(params));
        Crypto crypto = provider.getCrypto(params);
        Function<byte[], byte[]> enc = crypto.getEncoder(params);
        Function<byte[], byte[]> dec = crypto.getDecoder(params);

        byte[] data = "abcdefg".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = enc.apply(data);
        byte[] data1 = dec.apply(encrypted);
        assertArrayEquals(data, data1);
    }

    @ParameterizedTest
    @ValueSource(ints = {128, 192, 256})
    void keySpecfiedNG(int keylen) {
        Map<String, Object> params = new HashMap<>();
        params.put("algorithm", "AES");
        params.put("mode", "GCM");

        params.put("key_length", keylen);
        params.put("key", Arrays.copyOfRange("12345678901234567890123456789012".getBytes(), 0, keylen/8));

        JCEProvider provider = new JCEProvider();
        assertTrue(provider.isSupported(params));
        Crypto crypto = provider.getCrypto(params);
        Function<byte[], byte[]> enc = crypto.getEncoder(params);


        Map<String, Object> params2 = new HashMap<>();
        params2.put("algorithm", "AES");
        params2.put("mode", "GCM");

        params2.put("key_length", keylen);
        params2.put("key", Arrays.copyOfRange("12345678901234567890123456789012".getBytes(), 0, keylen/8));

        JCEProvider provider2 = new JCEProvider();
        assertTrue(provider2.isSupported(params2));
        params2.put("key", Arrays.copyOfRange("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX".getBytes(), 0, keylen/8));
        Crypto crypto2 = provider.getCrypto(params2);
        Function<byte[], byte[]> dec = crypto2.getDecoder(params2);

        byte[] data = "abcdefg".getBytes(StandardCharsets.UTF_8);
        boolean caught = false;
        try {
            byte[] encrypted = enc.apply(data);
            byte[] data1 = dec.apply(encrypted);
            assertArrayEquals(data, data1);
        }
        catch (Exception e) {
            System.err.println("caught: " + e);
            caught = true;
        }
        assert(caught);
    }

    @ParameterizedTest
    @ValueSource(ints = {128, 192, 256})
    void keySpecfied(int keylen) {
        Map<String, Object> params = new HashMap<>();
        params.put("algorithm", "AES");
        params.put("mode", "GCM");

        params.put("key_length", keylen);
        params.put("key", Arrays.copyOfRange("12345678901234567890123456789012".getBytes(), 0, keylen/8));

        JCEProvider provider = new JCEProvider();
        assertTrue(provider.isSupported(params));
        Crypto crypto = provider.getCrypto(params);
        Function<byte[], byte[]> enc = crypto.getEncoder(params);
        Function<byte[], byte[]> dec = crypto.getDecoder(params);

        byte[] data = "abcdefg".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = enc.apply(data);
        byte[] data1 = dec.apply(encrypted);
        assertArrayEquals(data, data1);
    }

    @Disabled
    @Test
    void aesECB() {
        Map<String, Object> params = new HashMap<>();
        params.put("algorithm", "AES");
        params.put("mode", "ECB");
        params.put("padding", "pkcs7");

        params.put("password", "secret-001");

        JCEProvider provider = new JCEProvider();
        assertTrue(provider.isSupported(params));
        Crypto crypto = provider.getCrypto(params);
        Function<byte[], byte[]> enc = crypto.getEncoder(params);
        Function<byte[], byte[]> dec = crypto.getDecoder(params);

        byte[] data = "abcdefg".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = enc.apply(data);
        byte[] data1 = dec.apply(encrypted);
        System.out.println(new String(data1, StandardCharsets.UTF_8));
    }
}
