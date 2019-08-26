#!/usr/local/bin/python3.6
# vim: expandtab shiftwidth=4

# Copyright (C) 2019 National Institute of Informatics
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

import logging
import ssl
import sinetstream
import pytest

logging.basicConfig(level=logging.ERROR)
logging.basicConfig(level=logging.DEBUG)


def test_tls_reader_yaml():
    with sinetstream.MessageReader("tls-service-2", "topic") as f:
        pass


def test_tls_reader_yaml_default():
    try:
        with sinetstream.MessageReader("tls-service-2-default", "topic") as f:
            pass
    except ssl.SSLError:
        logging.debug("oreore cert?")
        pass


def test_tls_reader_arg0():
    try:
        with sinetstream.MessageReader("service-2-8883", "topic",
                                       tls=True) as f:
            pass
    except ssl.SSLError:
        logging.debug("oreore cert?")
        pass


def test_tls_reader_arg():
    with sinetstream.MessageReader("service-2-8883", "topic",
                                   security_protocol="SSL",
                                   ca_certs="./tests/cert/ca.pem",
                                   certfile="./tests/cert/client0.crt",
                                   keyfile="./tests/cert/client0.key") as f:
        pass


def test_tls_reader_cauth_ng():
    with pytest.raises(ssl.SSLError):
        with sinetstream.MessageReader("tls-service-2", "topic",
                                       certfile="./tests/cert/bad-client.crt",
                                       keyfile="./tests/cert/bad-client.key") as f:
            pass


def test_tls_writer_yaml():
    with sinetstream.MessageWriter("tls-service-2", "topic") as f:
        pass


def test_tls_writer_arg():
    with sinetstream.MessageWriter("service-2-8883", "topic",
                                   security_protocol="SSL",
                                   ca_certs="./tests/cert/ca.pem",
                                   certfile="./tests/cert/client0.crt",
                                   keyfile="./tests/cert/client0.key") as f:
        pass


def test_tls_writer_cauth_ng():
    with pytest.raises(ssl.SSLError):
        with sinetstream.MessageWriter("tls-service-2", "topic",
                                       certfile="./tests/cert/bad-client.crt",
                                       keyfile="./tests/cert/bad-client.key") as f:
            pass
