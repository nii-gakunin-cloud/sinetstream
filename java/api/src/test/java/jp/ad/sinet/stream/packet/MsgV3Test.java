/*
 * Copyright (C) 2024 National Institute of Informatics
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

package jp.ad.sinet.stream.packet;


import jp.ad.sinet.stream.api.*;
import jp.ad.sinet.stream.api.valuetype.SimpleValueType;
import jp.ad.sinet.stream.utils.MessageReaderFactory;
import jp.ad.sinet.stream.utils.MessageWriterFactory;
import jp.ad.sinet.stream.utils.Pair;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.Arguments;
//import org.junit.jupiter.params.provider.MethodSource;
//import org.junit.jupiter.params.provider.ValueSource;

//import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
//import java.util.stream.Stream;
import java.util.stream.IntStream;
import java.util.stream.Collectors;
import java.time.Duration;

//import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
//import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.Builder;

class MsgV3Test {

    static int keyLen = 256;
    static byte[] key1 = makeRandomBytes(keyLen / 8);
    static byte[] key2 = makeRandomBytes(keyLen / 8);
    static byte[] key3 = makeRandomBytes(keyLen / 8);

    private List<String> lines;

    @BeforeEach
    void setupMessages() {
        lines = IntStream.range(0, 10)
                .mapToObj(x -> StringUtils.repeat("abc", x * x))
                .collect(Collectors.toList());
    }

    static byte[] makeRandomBytes(int len) {
        byte[] bytes = new byte[len];
        (new Random()).nextBytes(bytes);
        return bytes;
    }

    @Builder
    static class Arg {
        @Builder.Default
        int message_format = 3;
        boolean data_encryption;
        @Builder.Default
        String crypto_algorithm = "AES";
        @Builder.Default
        int crypto_key_length = keyLen;
        @Builder.Default
        String crypto_mode = "GCM";
        byte[] crypto_key;
        byte[] crypto_key1;
        byte[] crypto_key2;
        byte[] crypto_key3;
    };

    Map<String, Object> makeParams(Arg arg) {
        return new HashMap<String, Object>() {
            {
                put("message_format", arg.message_format);
                put("data_encryption", arg.data_encryption);
                put("crypto",
                    new HashMap<String, Object>() {
                        {
                            put("algorithm", arg.crypto_algorithm);
                            put("key_length", arg.crypto_key_length);
                            put("mode", arg.crypto_mode);
                            if (arg.crypto_key != null)
                                put("key", arg.crypto_key);
                            Map<Integer, Object> keys = new HashMap<Integer, Object>() {
                                {
                                    if (arg.crypto_key1 != null)
                                        put(1, arg.crypto_key1);
                                    if (arg.crypto_key2 != null)
                                        put(2, arg.crypto_key2);
                                    if (arg.crypto_key3 != null)
                                        put(3, arg.crypto_key3);
                                }
                            };
                            if (!keys.isEmpty())
                                put("_keys", keys);
                        }
                    });
            }
        };
    }

    @Test
    void test_off_33() {
        dotest(
            Arg.builder()
                .message_format(3)
                .data_encryption(false)
                .build(),
            Arg.builder()
                .message_format(3)
                .data_encryption(false)
                .build(),
            null);
    }

    @Test
    void test_off_32() {
        dotest(
            Arg.builder()
                .message_format(3)
                .data_encryption(false)
                .build(),
            Arg.builder()
                .message_format(2)
                .data_encryption(false)
                .build(),
            InvalidMessageException.class);
    }

    @Test
    void test_off_22() {
        dotest(
            Arg.builder()
                .message_format(2)
                .data_encryption(false)
                .build(),
            Arg.builder()
                .message_format(2)
                .data_encryption(false)
                .build(),
            null);
    }

    @Test
    void test_off_23() {
        dotest(
            Arg.builder()
                .message_format(2)
                .data_encryption(false)
                .build(),
            Arg.builder()
                .message_format(3)
                .data_encryption(false)
                .build(),
            null);
    }

    @Test
    void test_on_33_k1_k1() {
        dotest(
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key1(key1)
                .build(),
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key1(key1)
                .build(),
            null);
    }

    @Test
    void test_on_33_k1_k12() {
        dotest(
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key1(key1)
                .build(),
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key1(key1)
                .crypto_key2(key2)
                .build(),
            null);
    }

    @Test
    void test_on_33_k1_k123() {
        dotest(
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key1(key1)
                .build(),
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key1(key1)
                .crypto_key2(key2)
                .crypto_key3(key3)
                .build(),
            null);
    }

    @Test
    void test_on_33_k1_k23() {
        dotest(
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key1(key1)
                .build(),
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key2(key2)
                .crypto_key3(key3)
                .build(),
            InvalidMessageException.class);
    }

    @Test
    void test_on_33_k12_k1() {
        dotest(
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key1(key1)
                .crypto_key2(key2)
                .build(),
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key1(key1)
                .build(),
            InvalidMessageException.class);
    }

    @Test
    void test_on_33_k12_k12() {
        dotest(
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key1(key1)
                .crypto_key2(key2)
                .build(),
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key1(key1)
                .crypto_key2(key2)
                .build(),
            null);
    }

    @Test
    void test_on_33_k12_k123() {
        dotest(
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key1(key1)
                .crypto_key2(key2)
                .build(),
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key1(key1)
                .crypto_key2(key2)
                .crypto_key3(key3)
                .build(),
            null);
    }

    @Test
    void test_on_33_k12_k23() {
        dotest(
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key1(key1)
                .crypto_key2(key2)
                .build(),
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key2(key2)
                .crypto_key3(key3)
                .build(),
            null);
    }

    @Test
    void test_on_33_k2_k1() {
        dotest(
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key2(key2)
                .build(),
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key1(key1)
                .build(),
            InvalidMessageException.class);
    }

    @Test
    void test_on_33_k2_k12() {
        dotest(
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key2(key2)
                .build(),
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key1(key1)
                .crypto_key2(key2)
                .build(),
            null);
    }

    @Test
    void test_on_33_k2_k123() {
        dotest(
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key2(key2)
                .build(),
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key1(key1)
                .crypto_key2(key2)
                .crypto_key3(key3)
                .build(),
            null);
    }

    @Test
    void test_on_33_k2_k23() {
        dotest(
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key2(key2)
                .build(),
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key2(key2)
                .crypto_key3(key3)
                .build(),
            null);
    }

    @Test
    void test_on_33_k2_k2() {
        dotest(
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key2(key2)
                .build(),
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key2(key2)
                .build(),
            null);
    }

    @Test
    void test_on_22_k2_k2() {
        dotest(
            Arg.builder()
                .message_format(2)
                .data_encryption(true)
                .crypto_key(key2)
                .build(),
            Arg.builder()
                .message_format(2)
                .data_encryption(true)
                .crypto_key(key2)
                .build(),
            null);
    }

    @Test
    void test_on_23_k2_k1() {
        dotest(
            Arg.builder()
                .message_format(2)
                .data_encryption(true)
                .crypto_key(key2)
                .build(),
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key1(key1)
                .build(),
            SinetStreamException.class);
    }

    @Test
    void test_on_23_k2_k2() {
        dotest(
            Arg.builder()
                .message_format(2)
                .data_encryption(true)
                .crypto_key(key2)
                .build(),
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key2(key2)
                .build(),
            null);
    }

    @Test
    void test_on_23_k2_k12() {
        dotest(
            Arg.builder()
                .message_format(2)
                .data_encryption(true)
                .crypto_key(key2)
                .build(),
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key1(key1)
                .crypto_key2(key2)
                .build(),
            null);
    }

    @Test
    void test_on_23_k2_k123() {
        dotest(
            Arg.builder()
                .message_format(2)
                .data_encryption(true)
                .crypto_key(key2)
                .build(),
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key1(key1)
                .crypto_key2(key2)
                .crypto_key3(key3)
                .build(),
            SinetStreamException.class);
    }

    @Test
    void test_on_23_k2_k23() {
        dotest(
            Arg.builder()
                .message_format(2)
                .data_encryption(true)
                .crypto_key(key2)
                .build(),
            Arg.builder()
                .message_format(3)
                .data_encryption(true)
                .crypto_key2(key2)
                .crypto_key3(key3)
                .build(),
            SinetStreamException.class);
    }

    void dotest(Arg writerArg, Arg readerArg, Class expected) {
        //System.out.println("writerArg=" + writerArg.toString());
        //System.out.println("readerArg=" + readerArg.toString());

        String topic = "topic-" + RandomStringUtils.randomAlphabetic(5);

        MessageWriterFactory<String> writerFactory = MessageWriterFactory.<String>builder()
                .noConfig(true)
                .valueType(SimpleValueType.TEXT)
                .type("dummy")
                .topic(topic)
                .parameters(makeParams(writerArg))
                .build();

        MessageReaderFactory<String> readerBuilder = MessageReaderFactory.<String>builder()
                .noConfig(true)
                .valueType(SimpleValueType.TEXT)
                .type("dummy")
                .topic(topic)
                .parameters(makeParams(readerArg))
                .receiveTimeout(Duration.ofMillis(100))
                .build();

        Exception caught = null;
        try (MessageReader<String> reader = readerBuilder.getReader();
             MessageWriter<String> writer = writerFactory.getWriter()) {
            //System.out.println("XXX:write");
            lines.forEach(writer::write);
            //System.out.println("XXX:read");
            assertIterableEquals(lines, reader.stream().map(Message::getValue).collect(Collectors.toList()));
            //System.out.println("XXX:no exception");
        }
        catch (Exception ex) {
            //System.out.println("XXX:caught:" + ex);
            caught = ex;
        }
        if (expected != null) {
            //assertEquals(expected.getMessage(), caught.getMessage());
            assertEquals(expected, caught.getClass());
        } else {
            assertNull(caught);
        }
    }
}

/*
# !!!TESTS!!!
# msgver={2,3}
# role={write,reader}
# enc={on,off}
# key={{1},{1,2},{1,2,3},{2,3}}
# keyver={1,2,3}
#
# udo={off,on} #user_data_only
#
#
# enc=off
#     writer=v3
#         reader=v3 -> OK
#         reader=v2 -> NG
#     writer=v2
#         reader=v2 -> OK
#         reader=v3 -> OK
#
# enc=on
#     writer=v3
#         key={1}
#             keyver=1
#                 reader=v3
#                     key={1} -> OK test_33_enc_k1_kn1
#                     key={1,2} -> OK test_33_enc_k1_k12
#                     key={1,2,3} -> OK test_33_enc_k1_k123
#                     key={2,3} -> NG test_33_enc_k1_k23
#         key={1,2}
#             keyver=2
#                 reader=v3
#                     key={1} -> NG test_33_enc_k12_k1
#                     key={1,2} -> OK test_33_enc_k12_k12
#                     key={1,2,3} -> OK test_33_enc_k12_k123
#                     key={2,3} -> OK test_33_enc_k12_k23

#         key={2}
#             keyver=2
#                 reader=v3
#                     key={2} -> OK test_33_enc_k2_k2

#     writer=v2
#         key=2
#             reader=v2
#                 key=2 -> OK test_22_enc_k2_k2
#             reader=v3
#                 key={1} -> NG test_23_enc_k2_k1
#                 key={2} -> OK test_23_enc_k2_k2
#                 key={1,2} -> OK test_23_enc_k2_k12
#                 key={1,2,3} -> NG test_23_enc_k2_k123
#                 key={2,3} -> NG test_23_enc_k2_k23
#
*/
