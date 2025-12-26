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

import logging
from itertools import product
from sinetstream import MessageReader, MessageWriter, ConnectionError
import pytest
from conftest import (
    CONFVER1, CONFVER2, CONFVER3, CONFVER,
    SERVICE, TOPIC, SSL_CERT_AUTH_BROKER,
    CACERT_PATH, CLIENT_CERT_PATH, CLIENT_CERT_KEY_PATH,
    CLIENT_BAD_CERT_PATH, CLIENT_BAD_CERT_KEY_PATH,
)

logging.basicConfig(level=logging.CRITICAL)
logger = logging.getLogger(__name__)
pytestmark = pytest.mark.skipif(
    SSL_CERT_AUTH_BROKER is None,
    reason='MQTT_SSL_CERT_AUTH_BROKER is not set.')


no_client_auth_params_v1 = [
    {
        'tls': {'ca_certs': str(CACERT_PATH)}
    }, {
        'tls_set': {'ca_certs': str(CACERT_PATH)}
    },
]


@pytest.mark.parametrize("io,config_comm_params,config_version", product(
    [MessageReader, MessageWriter],
    no_client_auth_params_v1,
    [CONFVER1],
))
def test_no_client_cert_tls(io, setup_config):
    with pytest.raises(ConnectionError):
        with io(SERVICE, TOPIC) as _:
            pass


no_client_auth_params_v3 = [
    {
        'tls': {'ca_certs': str(CACERT_PATH)}
    },
]


@pytest.mark.parametrize("io,config_comm_params,config_version", product(
    [MessageReader, MessageWriter],
    no_client_auth_params_v3,
    [CONFVER3],
))
def test_no_client_cert_tls_cfv3(io, setup_config):
    with pytest.raises(ConnectionError):
        with io(SERVICE, TOPIC) as _:
            pass


client_auth_params_v1 = [
    {
        'tls': {
            'ca_certs': str(CACERT_PATH),
            'certfile': str(CLIENT_CERT_PATH),
            'keyfile': str(CLIENT_CERT_KEY_PATH),
        }
    }, {
        'tls_set': {
            'ca_certs': str(CACERT_PATH),
            'certfile': str(CLIENT_CERT_PATH),
            'keyfile': str(CLIENT_CERT_KEY_PATH),
        }
    },
]


@pytest.mark.skipif(
    CLIENT_CERT_PATH is None or CLIENT_CERT_KEY_PATH is None,
    reason='CLIENT_CERT_PATH or CLIENT_CERT_KEY_PATH is not set.')
@pytest.mark.parametrize("io,config_comm_params,config_version", product(
    [MessageReader, MessageWriter],
    client_auth_params_v1,
    [CONFVER1],
))
def test_tls_client_auth(io, setup_config):
    with io(SERVICE, TOPIC) as _:
        pass


client_auth_params_v3 = [
    {
        'tls': {
            'ca_certs': str(CACERT_PATH),
            'certfile': str(CLIENT_CERT_PATH),
            'keyfile': str(CLIENT_CERT_KEY_PATH),
        }
    }, {
        'tls_set': {
            'ca_certs': str(CACERT_PATH),
            'certfile': str(CLIENT_CERT_PATH),
            'keyfile': str(CLIENT_CERT_KEY_PATH),
        }
    },
]


@pytest.mark.skipif(
    CLIENT_CERT_PATH is None or CLIENT_CERT_KEY_PATH is None,
    reason='CLIENT_CERT_PATH or CLIENT_CERT_KEY_PATH is not set.')
@pytest.mark.parametrize("io,config_comm_params,config_version", product(
    [MessageReader, MessageWriter],
    client_auth_params_v1,
    [CONFVER1],
))
def test_tls_client_auth(io, setup_config):
    with io(SERVICE, TOPIC) as _:
        pass


bad_client_auth_params_v1 = [
    {
        'tls': {
            'ca_certs': str(CACERT_PATH),
            'certfile': str(CLIENT_BAD_CERT_PATH),
            'keyfile': str(CLIENT_BAD_CERT_KEY_PATH),
        }
    },
    {
        'tls_set': {
            'ca_certs': str(CACERT_PATH),
            'certfile': str(CLIENT_BAD_CERT_PATH),
            'keyfile': str(CLIENT_BAD_CERT_KEY_PATH),
        }
    },
]


@pytest.mark.skipif(
    CLIENT_BAD_CERT_PATH is None or CLIENT_BAD_CERT_KEY_PATH is None,
    reason='CLIENT_BAD_CERT_PATH or CLIENT_BAD_CERT_KEY_PATH is not set.')
@pytest.mark.parametrize("io,config_comm_params", product(
    [MessageReader, MessageWriter],
    bad_client_auth_params_v1,
))
def test_tls_bad_client_auth(io, setup_config):
    with pytest.raises(ConnectionError):
        with io(SERVICE, TOPIC) as _:
            pass


bad_client_auth_params_v3 = [
    {
        'tls': {
            'ca_certs': str(CACERT_PATH),
            'certfile': str(CLIENT_BAD_CERT_PATH),
            'keyfile': str(CLIENT_BAD_CERT_KEY_PATH),
        }
    },
]


@pytest.mark.skipif(
    CLIENT_BAD_CERT_PATH is None or CLIENT_BAD_CERT_KEY_PATH is None,
    reason='CLIENT_BAD_CERT_PATH or CLIENT_BAD_CERT_KEY_PATH is not set.')
@pytest.mark.parametrize("io,config_comm_params", product(
    [MessageReader, MessageWriter],
    bad_client_auth_params_v1,
))
def test_tls_bad_client_auth(io, setup_config):
    with pytest.raises(ConnectionError):
        with io(SERVICE, TOPIC) as _:
            pass


@pytest.fixture()
def config_brokers():
    logger.debug(f'BROKER: {SSL_CERT_AUTH_BROKER}')
    return [SSL_CERT_AUTH_BROKER]
