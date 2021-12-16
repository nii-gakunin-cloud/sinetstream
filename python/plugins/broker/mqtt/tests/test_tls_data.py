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
from itertools import product
from sinetstream import MessageReader, MessageWriter, ConnectionError
import pytest
from conftest import (
    SERVICE, TOPIC, SSL_BROKER, SSL_BROKER_BAD_HOSTNAME, CACERT_PATH,
)
import ssl

logging.basicConfig(level=logging.CRITICAL)
logger = logging.getLogger(__name__)
pytestmark = pytest.mark.skipif(
    SSL_BROKER is None, reason='MQTT_SSL_BROKER is not set.')


@pytest.mark.parametrize("io", [MessageReader, MessageWriter])
def test_tls(io, setup_config):
    with io(SERVICE, TOPIC) as _:
        pass


tls_set_params = {
    'tls_set': {
        'ca_certs': str(CACERT_PATH),
    }
}


@pytest.mark.parametrize("io,config_params", [
    pytest.param(MessageReader, tls_set_params),
    pytest.param(MessageWriter, tls_set_params),
])
def test_tls_set(io, setup_config):
    with io(SERVICE, TOPIC) as _:
        pass


@pytest.mark.timeout(15)
@pytest.mark.parametrize("io,config_params", [
    pytest.param(MessageReader, None),
    pytest.param(MessageWriter, None),
])
def test_tls_error(io, setup_config):
    with pytest.raises(ConnectionError):
        io(SERVICE, TOPIC).open()


@pytest.mark.timeout(7)
@pytest.mark.parametrize("io,config_params", [
    pytest.param(MessageReader, None),
    pytest.param(MessageWriter, None),
])
def test_tls_error_timeout(io, setup_config):
    with pytest.raises(ConnectionError):
        io(SERVICE, TOPIC, connection_timeout=3).open()


check_hostname_params = [
    {
        'tls': {
            'ca_certs': str(CACERT_PATH),
        }
    }, {
        'tls': {
            'ca_certs': str(CACERT_PATH),
            'check_hostname': True,
        }
    }, {
        'tls_set': {'ca_certs': str(CACERT_PATH)},
    }, {
        'tls_set': {'ca_certs': str(CACERT_PATH)},
        'tls_insecure_set': {'value': False},
    },
]


@pytest.mark.skipif(
    SSL_BROKER_BAD_HOSTNAME is None,
    reason='MQTT_SSL_BROKER_BAD_HOSTNAME is not set.')
@pytest.mark.parametrize("io,config_brokers,config_params", product(
    [MessageReader, MessageWriter],
    [SSL_BROKER_BAD_HOSTNAME],
    check_hostname_params,
))
def test_tls_bad_hostname(io, setup_config):
    with pytest.raises(ConnectionError):
        with io(SERVICE, TOPIC) as _:
            pass


no_check_hostname_params = [
    {
        'tls': {
            'ca_certs': str(CACERT_PATH),
            'check_hostname': False,
        }
    }, {
        'tls_set': {'ca_certs': str(CACERT_PATH)},
        'tls_insecure_set': {'value': True},
    },
]


@pytest.mark.skipif(
    SSL_BROKER_BAD_HOSTNAME is None,
    reason='MQTT_SSL_BROKER_BAD_HOSTNAME is not set.')
@pytest.mark.parametrize("io,config_brokers,config_params", product(
    [MessageReader, MessageWriter],
    [SSL_BROKER_BAD_HOSTNAME],
    no_check_hostname_params,
))
def test_tls_no_check_hostname(io, setup_config):
    with io(SERVICE, TOPIC) as _:
        pass


cert_reqs_params = [
    {
        'tls_set': {
            'ca_certs': str(CACERT_PATH),
            'cert_reqs': x.name,
        },
    }
    for x in list(ssl.VerifyMode)
]


@pytest.mark.parametrize("io,config_params", product(
    [MessageReader, MessageWriter],
    cert_reqs_params,
))
def test_tls_cert_reqs(io, setup_config):
    with io(SERVICE, TOPIC) as _:
        pass


tls_version_params = [
    {
        'tls_set': {
            'ca_certs': str(CACERT_PATH),
            'tls_version': x.name,
        },
    }
    for x in [ssl.PROTOCOL_TLSv1_2]
]


@pytest.mark.parametrize("io,config_params", product(
    [MessageReader, MessageWriter],
    tls_version_params,
))
def test_tls_version(io, setup_config):
    with io(SERVICE, TOPIC) as _:
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
        with open(str(CACERT_PATH), mode='rb') as f:
            cacert_data = f.read()
        return {
            'tls': {
                'ca_certs_data': cacert_data,
            }
        }
