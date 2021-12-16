#!/usr/bin/env python3

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
from sinetstream import MessageReader, MessageWriter, ConnectionError
import pytest
from conftest import (
    SERVICE, TOPIC, SSL_BROKER, SSL_BROKER_BAD_HOSTNAME, CACERT_PATH,
    CLIENT_CERT_PATH, CLIENT_CERT_KEY_PATH, CLIENT_BAD_CERT_PATH,
    CLIENT_BAD_CERT_KEY_PATH,
)

logging.basicConfig(level=logging.CRITICAL)
logger = logging.getLogger(__name__)
pytestmark = pytest.mark.skipif(
    SSL_BROKER is None, reason='KAFKA_SSL_BROKER is not set.')


@pytest.mark.parametrize("io", [MessageReader, MessageWriter])
def test_tls(io, setup_config):
    with io(SERVICE, TOPIC) as _:
        pass


kafka_ssl_params = {'security_protocol': 'SSL'}
if CACERT_PATH is not None:
    kafka_ssl_params['ssl_cafile'] = str(CACERT_PATH)


@pytest.mark.parametrize("io,config_params", [
    pytest.param(MessageReader, kafka_ssl_params),
    pytest.param(MessageWriter, kafka_ssl_params),
])
def test_tls_kafka_parameters(io, setup_config):
    with io(SERVICE, TOPIC) as _:
        pass


@pytest.mark.parametrize("io,config_params", [
    pytest.param(MessageReader, None),
    pytest.param(MessageWriter, None),
])
def test_tls_error(io, setup_config):
    with pytest.raises(ConnectionError):
        with io(SERVICE, TOPIC) as _:
            pass


no_check_hostname_params = {
    'tls': {
        'ca_certs': str(CACERT_PATH),
        'check_hostname': False,
    }
}


@pytest.mark.skipif(
    SSL_BROKER_BAD_HOSTNAME is None,
    reason='KAFKA_SSL_BROKER_BAD_HOSTNAME is not set.')
@pytest.mark.parametrize("io,config_brokers", [
    pytest.param(MessageReader, SSL_BROKER_BAD_HOSTNAME),
    pytest.param(MessageWriter, SSL_BROKER_BAD_HOSTNAME),
])
def test_tls_bad_hostname(io, setup_config):
    with pytest.raises(ConnectionError):
        with io(SERVICE, TOPIC) as _:
            pass


@pytest.mark.skipif(
    SSL_BROKER_BAD_HOSTNAME is None,
    reason='KAFKA_SSL_BROKER_BAD_HOSTNAME is not set.')
@pytest.mark.parametrize("io,config_brokers,config_params", [
    pytest.param(MessageReader, SSL_BROKER_BAD_HOSTNAME,
                 no_check_hostname_params),
    pytest.param(MessageWriter, SSL_BROKER_BAD_HOSTNAME,
                 no_check_hostname_params),
])
def test_tls_no_check_hostname(io, setup_config):
    with io(SERVICE, TOPIC) as _:
        pass


@pytest.mark.skipif(
    CLIENT_CERT_PATH is None or CLIENT_CERT_KEY_PATH is None,
    reason='CLIENT_CERT_PATH or CLIENT_CERT_KEY_PATH is not set.')
@pytest.mark.parametrize("io", [MessageReader, MessageWriter])
def test_tls_client_auth(io, setup_config):
    with io(SERVICE, TOPIC,
            ssl_certfile=str(CLIENT_CERT_PATH),
            ssl_keyfile=str(CLIENT_CERT_KEY_PATH)) as _:
        pass


@pytest.mark.skipif(
    CLIENT_BAD_CERT_PATH is None or CLIENT_BAD_CERT_KEY_PATH is None,
    reason='CLIENT_BAD_CERT_PATH or CLIENT_BAD_CERT_KEY_PATH is not set.')
@pytest.mark.parametrize("io", [MessageReader, MessageWriter])
def test_tls_bad_client_auth(io, setup_config):
    with pytest.raises(ConnectionError):
        with io(SERVICE, TOPIC,
                ssl_certfile=str(CLIENT_BAD_CERT_PATH),
                ssl_keyfile=str(CLIENT_BAD_CERT_KEY_PATH)) as _:
            pass


@pytest.fixture()
def config_brokers():
    logger.debug(f'BROKER: {SSL_BROKER}')
    return [SSL_BROKER]


@pytest.fixture()
def config_params():
    if CACERT_PATH is None:
        return {'tls': True}
    else:
        logger.debug(f'CACERT: {CACERT_PATH}')
        return {
            'tls': {
                'ca_certs': str(CACERT_PATH),
            }
        }
