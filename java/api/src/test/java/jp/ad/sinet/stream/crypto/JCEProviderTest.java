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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JCEProviderTest {

    enum Mode {
        ECB,
        CBC,
        CFB,
        OFB,
        CTR,
        OpenPGPCFB,
        OpenPGP,
    }

    enum AEADMode {
        CCM,
        EAX,
        GCM,
        OCB,
    }

    static Stream<Arguments> jceParameters() {
        List<Arguments> ret = new ArrayList<>();
        for (Mode mode : Mode.values()) {
            for (String padding : Arrays.asList("", "pkcs7", "iso7816", "x923", "PKCS7Padding", "ISO10126Padding")) {
                ret.add(Arguments.arguments(mode, padding));
            }
        }
        return ret.stream();
    }

    @ParameterizedTest
    @MethodSource("jceParameters")
    void aes(Mode mode, String padding) {
        Map<String, Object> params = new HashMap<>();
        params.put("algorithm", "AES");
        params.put("mode", mode.name());
        if (!padding.isEmpty()) {
            params.put("padding", padding);
        }
        JCEProvider provider = new JCEProvider();
        assertTrue(provider.isSupported(params));
    }

    @ParameterizedTest
    @EnumSource(AEADMode.class)
    void aeadAes(AEADMode mode) {
        Map<String, Object> params = new HashMap<>();
        params.put("algorithm", "AES");
        params.put("mode", mode.name());
        JCEProvider provider = new JCEProvider();
        assertTrue(provider.isSupported(params));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void nullMode(String mode) {
        Map<String, Object> params = new HashMap<>();
        params.put("algorithm", "AES");
        params.put("mode", mode);
        JCEProvider provider = new JCEProvider();
        assertFalse(provider.isSupported(params));
    }

    @ParameterizedTest
    @ValueSource(strings = {"XXX", "NONE"})
    void badMode(String mode) {
        Map<String, Object> params = new HashMap<>();
        params.put("algorithm", "AES");
        params.put("mode", mode);
        JCEProvider provider = new JCEProvider();
        assertFalse(provider.isSupported(params));
    }
}
