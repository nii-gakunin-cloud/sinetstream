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

from io import SEEK_SET, BytesIO
from time import time, time_ns

import pytest

from sinetstream import (
    MessageReader,
    MessageWriter,
    SINETStreamMessageDecoder,
    SINETStreamMessageEncoder,
)
from sinetstream.api import Message

smsgs = [
    "",
    "1",
    "test2",
    "test3" * 1000,
    "test44",
]

msgs = list(map(lambda x: x.encode(), smsgs))


@pytest.mark.parametrize(
    "comp,udato,tstamp",
    [
        # data_compression, user_data_only, iostream.timestamp
        (False, False, None),
        (True, False, None),
        (True, True, None),
        (False, True, None),
        (False, False, 0),
        (False, False, 1.1),
    ],
)
def test_pubsub(comp, udato, tstamp):
    t_start = time()
    xs = []
    with BytesIO() as bio, MessageWriter(
        type="iostream",
        no_config=True,
        data_compression=comp,
        user_data_only=udato,
        iostream={"iobase": bio},   # v1,v2
        type_spec={"iobase": bio},  # v3
    ) as w:
        for m in msgs:
            if udato and len(m) == 0:
                # when user_data_only=True,
                # cannot handle empty message.
                # because message boundary doesn't exist.
                # for example: "" means "" or ""+"" or ""+""+""...
                continue
            w.publish(m, timestamp=tstamp)
            b = bio.getvalue()
            xs.append((b, m))
            # reset buffer
            bio.seek(0, SEEK_SET)
            bio.truncate(0)
    t_end = time()

    ms = []
    with BytesIO() as bio, MessageReader(
        type="iostream",
        no_config=True,
        data_compression=comp,
        user_data_only=udato,
        iostream={"iobase": bio},   # v1,v2
        type_spec={"iobase": bio},  # v3
    ) as r:
        i = iter(r)
        for x, m in xs:
            bio.write(x)
            bio.seek(0, SEEK_SET)
            mm = next(i)
            ms.append((mm, m))
            # reset buffer
            bio.seek(0, SEEK_SET)
            bio.truncate(0)

    for b, a in ms:
        assert a == b.value
        assert b.topic is None
        if udato:
            pass
        elif tstamp is not None:
            assert b.timestamp == tstamp
        else:
            assert b.timestamp > t_start and b.timestamp < t_end


@pytest.mark.parametrize(
    "comp,tstamp",
    [
        # data_compression, iostream.timestamp
        (False, None),
        (True, None),
        (False, 0),
        (False, 1.1),
    ],
)
def test_pubsub_compound(comp, tstamp):
    with BytesIO() as bio:
        t_start = time()
        with MessageWriter(
            type="iostream",
            no_config=True,
            data_compression=comp,
            iostream={"iobase": bio},   # v1,v2
            type_spec={"iobase": bio},  # v3
        ) as w:
            for m in msgs:
                w.publish(m, timestamp=tstamp)
        t_end = time()

        bio.seek(0, SEEK_SET)

        with MessageReader(
            type="iostream",
            no_config=True,
            data_compression=comp,
            iostream={"iobase": bio},   # v1,v2
            type_spec={"iobase": bio},  # v3
        ) as r:
            i = iter(r)
            for m2, m1 in zip(i, msgs):
                assert m2.value == m1
                assert m2.topic is None
                if tstamp is not None:
                    assert m2.timestamp == tstamp
                else:
                    assert m2.timestamp > t_start and m2.timestamp < t_end


def test_pubsub_concat():
    comp = False
    # (printf 'a'|gzip -c; printf 'b'|gzip -c) | gzip -cd => 'ab'
    # SINETStream uses zlib not gzip, concatanation does not work.
    # comp = True
    # smsgs = ["AAA", "BBB"]

    with BytesIO() as bio:
        with MessageWriter(
            type="iostream",
            no_config=True,
            value_type="text",
            data_compression=comp,
            # compression={"algorithm": "zstd"},
            user_data_only=True,
            iostream={"iobase": bio},   # v1,v2
            type_spec={"iobase": bio},  # v3
        ) as w:
            for m in smsgs:
                w.publish(m)

        bio.seek(0, SEEK_SET)

        with MessageReader(
            type="iostream",
            no_config=True,
            value_type="text",
            data_compression=comp,
            # compression={"algorithm": "zstd"},
            user_data_only=True,
            iostream={"iobase": bio},   # v1,v2
            type_spec={"iobase": bio},   # v3
        ) as r:
            i = iter(r)
            m2 = next(i)
            assert m2.value == "".join(smsgs)


@pytest.mark.parametrize("value_type,datalist", [("byte_array", msgs), ("text", smsgs)])
def test_codec_with(value_type, datalist):
    writer_params = {
        "value_type": value_type,
        "no_config": True,
    }
    reader_params = writer_params

    with SINETStreamMessageEncoder(**writer_params) as enc, \
         SINETStreamMessageDecoder(**reader_params) as dec:
        for i, m in enumerate(datalist):
            b = enc.encode(m, timestamp=i)
            m2 = dec.decode(b)
            assert m2.value == m
            assert m2.timestamp == i

        for m in datalist:
            t_start = time_ns() // 1000
            b = enc.encode(m)
            t_end = time_ns() // 1000
            m2 = dec.decode(b)
            assert isinstance(m2, Message)
            assert m2.value == m
            assert m2.timestamp_us >= t_start and m2.timestamp_us <= t_end


@pytest.mark.parametrize("value_type,datalist", [("byte_array", msgs), ("text", smsgs)])
def test_codec_without(value_type, datalist):
    writer_params = {
        "value_type": value_type,
        "no_config": True,
    }
    reader_params = writer_params

    enc = SINETStreamMessageEncoder(**writer_params)
    dec = SINETStreamMessageDecoder(**reader_params)

    enc.open()  # XXX Currently it works without it.
    dec.open()  # XXX Currently it works without it.

    for m, i in zip(datalist, range(len(datalist))):
        b = enc.encode(m, timestamp=i)
        m2 = dec.decode(b)
        assert m2.value == m
        assert m2.timestamp == i

    enc.close()  # XXX Currently it works without it.
    dec.close()  # XXX Currently it works without it.


@pytest.mark.parametrize(
    "service,comp,udato",
    [
        # service, data_compression, user_data_only
        (None, False, False),
        (None, True, False),
        (None, True, True),
        (None, False, True),
        ("s1", False, False),
    ],
)
def test_codec_any(service, comp, udato):
    writer_params = {
        "service": service,
        "value_type": "text",
        "data_compression": comp,
        "user_data_only": udato,
    }
    reader_params = writer_params

    with SINETStreamMessageEncoder(**writer_params) as enc, \
         SINETStreamMessageDecoder(**reader_params) as dec:
        for m in smsgs:
            b = enc.encode(m) if not udato or len(m) > 0 else b""
            m2 = (
                dec.decode(b)
                if not udato or len(b) > 0
                else Message("", None, None, None)
            )
            assert m2.value == m
