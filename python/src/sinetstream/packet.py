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


# from sinetstream.api import InvalidMessageError

import io
import logging

logger = logging.getLogger(__name__)

message_marker_v3 = b"\xDF\x03\x00\x00"


class Packet:

    KEYVER_NOENC = 0

    @staticmethod
    def pack(payload, key_version):
        logger.debug(f"Packet.pack:key_version={key_version}")
        bytes_writer = io.BytesIO()
        bytes_writer.write(message_marker_v3)
        bytes_writer.write(key_version.to_bytes(2, byteorder="big", signed=False))
        bytes_writer.write(payload)
        return bytes_writer.getvalue()

    @staticmethod
    def unpack(buf):
        bytes_reader = io.BytesIO(buf)
        if len(buf) <= 4 + 2:
            # assume v2
            logger.debug("Packet.unpack:too short:len={len(buf)}")
            return None, buf
            # emsg = f"unpack: len(buf)={len(buf)}"
            # logger.error(emsg)
            # raise InvalidMessageError(emsg)
        msg_marker = bytes_reader.read(4)
        if msg_marker != message_marker_v3:
            # assume v2
            logger.debug("Packet.unpack:marker mismatch")
            return None, buf
            # emsg = f"unpack: msg_marker={msg_marker}"
            # logger.error(emsg)
            # raise InvalidMessageError(emsg)
        key_version = int.from_bytes(bytes_reader.read(2), byteorder="big", signed=False)
        logger.debug(f"Packet.unpack:key_version={key_version}")
        rest = bytes_reader.read()
        return key_version, rest
