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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import javax.xml.bind.DatatypeConverter;

class PacketTest {
    private byte[] getRandom() {
        byte[] bs = new byte[123];
        new Random().nextBytes(bs);
        return bs;
    }

    @ParameterizedTest
    @ValueSource(shorts = {0, 1, 2, (1<<15)-1})
    void valid_v3_kx(short keyVersion) {
        byte formatVersion = 3;
        byte[] msg = getRandom();
        byte[] pkt1 = Packet.encode(formatVersion, keyVersion, msg);
        //System.err.println("msg=" + DatatypeConverter.printHexBinary(msg));
        //System.err.println("pkt1=" + DatatypeConverter.printHexBinary(pkt1));
        Packet pkt2 = Packet.decode(pkt1);
        //System.err.println("pkt2=" + pkt2);
        assertEquals(formatVersion, pkt2.getHeader().getFormatVersion());
        assertEquals(keyVersion, pkt2.getHeader().getKeyVersion());
        //System.err.println("msg2=" + DatatypeConverter.printHexBinary(pkt2.getMessage()));
        assertArrayEquals(msg, pkt2.getMessage());
    }

    @Test
    void zero_body() {
        byte formatVersion = 3;
        short keyVersion = 1;
        byte[] msg = new byte[0];
        byte[] pkt1 = Packet.encode(formatVersion, keyVersion, msg);
        Packet pkt2 = Packet.decode(pkt1);
        assertEquals(formatVersion, pkt2.getHeader().getFormatVersion());
        assertEquals(keyVersion, pkt2.getHeader().getKeyVersion());
        assertArrayEquals(msg, pkt2.getMessage());
    }

    @Test
    void userdataonly() {
        byte[] msg = getRandom();
        Packet pkt2 = Packet.decode(msg);
        assertNull(pkt2);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, // marker
                         1, // format_version
                         2, // filler1
                         3  // filler2
                         })
    void bad_header(int i) {
        byte formatVersion = 3;
        short keyVersion = 1;
        byte[] msg = getRandom();
        byte[] pkt1 = Packet.encode(formatVersion, keyVersion, msg);
        pkt1[i]++; // breaker
        Packet pkt2 = Packet.decode(pkt1);
        assertNull(pkt2);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, // marker
                         1, // format_version
                         2, // filler1
                         3  // filler2
                         })
    void short_header(int n) {
        byte formatVersion = 3;
        short keyVersion = 1;
        byte[] msg = new byte[0];
        byte[] pkt1 = Packet.encode(formatVersion, keyVersion, msg);
        byte[] pkt1s = new byte[n];
        System.arraycopy(pkt1, 0, pkt1s, 0, n);
        Packet pkt2 = Packet.decode(pkt1s);
        assertNull(pkt2);
    }
}
