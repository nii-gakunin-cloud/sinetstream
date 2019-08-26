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

import org.junit.jupiter.api.Test;

import static jp.ad.sinet.stream.api.Consistency.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConsistencyTest {

    @Test
    void valueOf0() {
        assertEquals(AT_MOST_ONCE, Consistency.valueOf(0));
    }

    @Test
    void valueOf1() {
        assertEquals(AT_LEAST_ONCE, Consistency.valueOf(1));
    }

    @Test
    void valueOf2() {
        assertEquals(EXACTLY_ONCE, Consistency.valueOf(2));
    }

    @Test
    void valueOfBadValue() {
        assertNull(Consistency.valueOf(-1));
    }
}
