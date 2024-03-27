#!/usr/bin/env python3

# Copyright (C) 2023 National Institute of Informatics
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

import pytest

# from sinetstream import (
#     InvalidMessageError,
# )

from sinetstream.packet import Packet


def test_pack():
    buf1 = b"12345"
    kver1 = 123
    buf2 = Packet.pack(buf1, kver1)
    kver2, buf3 = Packet.unpack(buf2)
    assert kver2 == kver1
    assert buf3 == buf1


def test_pack_bigkeyver():
    buf1 = b"12345"
    kver1 = 1 << 16

    with pytest.raises(OverflowError) as _:
        Packet.pack(buf1, kver1)


def test_unpack_badmarker():
    buf1 = b"12345"
    kver1 = 123
    buf2 = Packet.pack(buf1, kver1)
    buf2x = b'\00' + buf2[1:]

    # with pytest.raises(InvalidMessageError) as _:
    #     Packet.unpack(buf2x)
    kver2, buf3 = Packet.unpack(buf2x)
    assert kver2 is None
    assert buf3 == buf2x


def test_unpack_short():
    buf1 = b"12345"
    kver1 = 123
    buf2 = Packet.pack(buf1, kver1)
    buf2x = buf2[0:6]

    # with pytest.raises(InvalidMessageError) as _:
    #     Packet.unpack(buf2x)
    kver2, buf3 = Packet.unpack(buf2x)
    assert kver2 is None
    assert buf3 == buf2x
