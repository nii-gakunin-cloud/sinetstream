#!/usr/bin/env python3

# Copyright (C) 2020 National Institute of Informatics
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


import io
import logging

import avro.io
import avro.schema

logger = logging.getLogger(__name__)

# timestamped message with Avro schema
MESSAGE_SCHEMA_JSON = '''{
  "namespace": "jp.ad.sinet.stream",
  "type": "record",
  "name": "message",
  "fields": [
    {
      "name": "tstamp",
      "type": {
        "type": "long",
        "logicalType": "timestamp-micros"
      }
    },
    {
      "name": "msg",
      "type": "bytes"
    }
  ]
}'''


message_schema = avro.schema.Parse(MESSAGE_SCHEMA_JSON)
message_schema_fingerprint = b'\x1f\x9c\x0c\x91\xeb\x33\x66\x4f'  # 64 bits


avro_signle_object_format_marker = b"\xC3\x01"


class Marshaller(object):
    def __init__(self):
        self._writer = avro.io.DatumWriter(message_schema)

    def marshal(self, msg, tstamp):
        assert type(tstamp) == int
        assert type(msg) == bytes
        bytes_writer = io.BytesIO()
        bytes_writer.write(avro_signle_object_format_marker)
        bytes_writer.write(message_schema_fingerprint)
        encoder = avro.io.BinaryEncoder(bytes_writer)
        rec = {"tstamp": tstamp, "msg": msg}
        self._writer.write(rec, encoder)
        return bytes_writer.getvalue()


class Unmarshaller(object):
    def __init__(self):
        self._reader = avro.io.DatumReader(message_schema, message_schema)

    def unmarshal(self, msg):
        from sinetstream.api import InvalidMessageError

        if len(msg) <= (2 + 8):
            emsg = f"Unmarshaller: len(msg)={len(msg)}"
            logger.error(emsg)
            raise InvalidMessageError(emsg)
        bytes_reader = io.BytesIO(msg)
        marker = bytes_reader.read(2)
        if marker != avro_signle_object_format_marker:
            emsg = f"Unmarshaller: marker={marker}"
            logger.error(emsg)
            raise InvalidMessageError(emsg)
        fingerprint = bytes_reader.read(8)
        if fingerprint != message_schema_fingerprint:
            emsg = f"Unmarshaller: fingerprint={fingerprint}"
            logger.error(emsg)
            raise InvalidMessageError(emsg)
        decoder = avro.io.BinaryDecoder(bytes_reader)
        try:
            m = self._reader.read(decoder)
        except AssertionError as ex:
            raise InvalidMessageError(ex)
        # rest = bytes_reader.read()
        # assert len(rest) == 0
        return m["tstamp"], m["msg"]
