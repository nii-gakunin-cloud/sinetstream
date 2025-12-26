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
    CONFVER1, CONFVER2, CONFVER3, CONFVER,
    SERVICE, TOPIC, SSL_BROKER, SSL_BROKER_BAD_HOSTNAME, CACERT_PATH,
)
import ssl

logging.basicConfig(level=logging.CRITICAL)
logger = logging.getLogger(__name__)
pytestmark = pytest.mark.skipif(
    SSL_BROKER is None, reason='MQTT_SSL_BROKER is not set.')


ca_certs_data = None

if CACERT_PATH is not None:
    logger.debug(f'CACERT: {CACERT_PATH}')
    with open(str(CACERT_PATH), mode='rb') as f:
        ca_certs_data = f.read()

def config_minimal_params():
    if CACERT_PATH is None:
        return {'tls': True}
    else:
        logger.debug(f'CACERT: {CACERT_PATH}')
        return {
            'tls': {
                'ca_certs_data': ca_certs_data,
            }
        }


minimal_params = config_minimal_params()


@pytest.mark.parametrize("io,config_comm_params,config_version", [
    pytest.param(MessageReader, minimal_params, CONFVER1),
    pytest.param(MessageWriter, minimal_params, CONFVER1),
    pytest.param(MessageReader, minimal_params, CONFVER3),
    pytest.param(MessageWriter, minimal_params, CONFVER3),
])
def test_tls(io, setup_config):
    with io(SERVICE, TOPIC) as _:
        pass


tls_set_params_v1 = {
    'tls_set': {
        'ca_certs_data': ca_certs_data,
    }
}


@pytest.mark.parametrize("io,config_comm_params,config_version", [
    pytest.param(MessageReader, tls_set_params_v1, CONFVER1),
    pytest.param(MessageWriter, tls_set_params_v1, CONFVER1),
])
def test_tls_set_v1(io, setup_config):
    with io(SERVICE, TOPIC) as _:
        pass


tls_set_params_v3 = {
    'tls': {
        'ca_certs_data': ca_certs_data,
    }
}


@pytest.mark.parametrize("io,config_mqtt_params,config_version", [
    pytest.param(MessageReader, tls_set_params_v3, CONFVER3),
    pytest.param(MessageWriter, tls_set_params_v3, CONFVER3),
])
def test_tls_set_v3(io, setup_config):
    with io(SERVICE, TOPIC) as _:
        pass


@pytest.mark.timeout(15)
@pytest.mark.parametrize("io,config_comm_params,config_version", [
    pytest.param(MessageReader, None, CONFVER1),
    pytest.param(MessageWriter, None, CONFVER1),
    pytest.param(MessageReader, None, CONFVER3),
    pytest.param(MessageWriter, None, CONFVER3),
])
def test_tls_error(io, setup_config):
    with pytest.raises(ConnectionError):
        io(SERVICE, TOPIC).open()


@pytest.mark.timeout(7)
@pytest.mark.parametrize("io,config_comm_params,config_version", [
    pytest.param(MessageReader, None, CONFVER1),
    pytest.param(MessageWriter, None, CONFVER1),
])
def test_tls_error_timeout_v1(io, setup_config):
    with pytest.raises(ConnectionError):
        io(SERVICE, TOPIC, connection_timeout=3).open()


@pytest.mark.timeout(7)
@pytest.mark.parametrize("io,config_comm_params,config_version", [
    pytest.param(MessageReader, None, CONFVER3),
    pytest.param(MessageWriter, None, CONFVER3),
])
def test_tls_error_timeout_v3(io, setup_config):
    with pytest.raises(ConnectionError):
        io(SERVICE, TOPIC, type_spec={"connection_timeout":3}).open()


check_hostname_params_v1 = [
    {
        'tls': {
            'ca_certs_data': ca_certs_data,
        }
    }, {
        'tls': {
            'ca_certs_data': ca_certs_data,
            'check_hostname': True,
        }
    }, {
        'tls_set': {'ca_certs_data': ca_certs_data},
    }, {
        'tls_set': {'ca_certs_data': ca_certs_data},
        'tls_insecure_set': {'value': False},
    },
]


@pytest.mark.skipif(
    SSL_BROKER_BAD_HOSTNAME is None,
    reason='MQTT_SSL_BROKER_BAD_HOSTNAME is not set.')
@pytest.mark.parametrize("io,config_brokers,config_comm_params,config_version", product(
    [MessageReader, MessageWriter],
    [SSL_BROKER_BAD_HOSTNAME],
    check_hostname_params_v1,
    [CONFVER1],
))
def test_tls_bad_hostname_v1(io, setup_config):
    with pytest.raises(ConnectionError):
        with io(SERVICE, TOPIC) as _:
            pass


check_hostname_params_v3 = [
    {
        'tls': {
            'ca_certs_data': ca_certs_data,
        }
    }, {
        'tls': {
            'ca_certs_data': ca_certs_data,
            'check_hostname': True,
        }
    }, {
        'type_spec': {
            'tls_set': {'ca_certs_data': ca_certs_data},
        }
    }, {
        'type_spec': {
            'tls': {'ca_certs_data': ca_certs_data},
            'tls_insecure': {'value': False},
        }
    },
]


@pytest.mark.skipif(
    SSL_BROKER_BAD_HOSTNAME is None,
    reason='MQTT_SSL_BROKER_BAD_HOSTNAME is not set.')
@pytest.mark.parametrize("io,config_brokers,config_comm_params,config_version", product(
    [MessageReader, MessageWriter],
    [SSL_BROKER_BAD_HOSTNAME],
    check_hostname_params_v3,
    [CONFVER3],
))
def test_tls_bad_hostname_v3(io, setup_config):
    with pytest.raises(ConnectionError):
        with io(SERVICE, TOPIC) as _:
            pass


no_check_hostname_params_v1 = [
    {
        'tls': {
            'ca_certs_data': ca_certs_data,
            'check_hostname': False,
        }
    }, {
        'tls_set': {'ca_certs_data': ca_certs_data},
        'tls_insecure_set': {'value': True},
    },
]


@pytest.mark.skipif(
    SSL_BROKER_BAD_HOSTNAME is None,
    reason='MQTT_SSL_BROKER_BAD_HOSTNAME is not set.')
@pytest.mark.parametrize("io,config_brokers,config_comm_params,config_version", product(
    [MessageReader, MessageWriter],
    [SSL_BROKER_BAD_HOSTNAME],
    no_check_hostname_params_v1,
    [CONFVER1],
))
def test_tls_no_check_hostname_v1(io, setup_config):
    with io(SERVICE, TOPIC) as _:
        pass


no_check_hostname_params_v3 = [
    {
        'tls': {
            'ca_certs_data': ca_certs_data,
            'check_hostname': False,
        }
    }, {
        'type_spec': {
            'tls': {'ca_certs_data': ca_certs_data},
            'tls_insecure': {'value': True},
        }
    },
]


@pytest.mark.skipif(
    SSL_BROKER_BAD_HOSTNAME is None,
    reason='MQTT_SSL_BROKER_BAD_HOSTNAME is not set.')
@pytest.mark.parametrize("io,config_brokers,config_comm_params,config_version", product(
    [MessageReader, MessageWriter],
    [SSL_BROKER_BAD_HOSTNAME],
    no_check_hostname_params_v3,
    [CONFVER3],
))
def test_tls_no_check_hostname_v3(io, setup_config):
    with io(SERVICE, TOPIC) as _:
        pass


cert_reqs_params_v1 = [
    {
        'tls_set': {
            'ca_certs_data': ca_certs_data,
            'cert_reqs': x.name,
        },
    }
    for x in list(ssl.VerifyMode)
]


@pytest.mark.parametrize("io,config_mqtt_params,config_version", product(
    [MessageReader, MessageWriter],
    cert_reqs_params_v1,
    [CONFVER1],
))
def test_tls_cert_reqs_v1(io, setup_config):
    with io(SERVICE, TOPIC) as _:
        pass


cert_reqs_params_v3 = [
    {
        'tls': {
            'ca_certs_data': ca_certs_data,
            'cert_reqs': x.name,
        },
    }
    for x in list(ssl.VerifyMode)
]


@pytest.mark.parametrize("io,config_mqtt_params,config_version", product(
    [MessageReader, MessageWriter],
    cert_reqs_params_v3,
    [CONFVER3],
))
def test_tls_cert_reqs_v3(io, setup_config):
    with io(SERVICE, TOPIC) as _:
        pass


tls_version_params_v1 = [
    {
        'tls_set': {
            'ca_certs_data': ca_certs_data,
            'tls_version': x.name,
        },
    }
    for x in [ssl.PROTOCOL_TLSv1_2]
]


@pytest.mark.parametrize("io,config_mqtt_params,config_version", product(
    [MessageReader, MessageWriter],
    tls_version_params_v1,
    [CONFVER1],
))
def test_tls_version_v1(io, setup_config):
    with io(SERVICE, TOPIC) as _:
        pass


tls_version_params_v3 = [
    {
        'tls': {
            'ca_certs_data': ca_certs_data,
            'tls_version': x.name,
        },
    }
    for x in [ssl.PROTOCOL_TLSv1_2]
]


@pytest.mark.parametrize("io,config_mqtt_params,config_version", product(
    [MessageReader, MessageWriter],
    tls_version_params_v3,
    [CONFVER3],
))
def test_tls_version_v3(io, setup_config):
    with io(SERVICE, TOPIC) as _:
        pass


@pytest.fixture()
def config_brokers():
    logger.debug(f'BROKER: {SSL_BROKER}')
    return [SSL_BROKER]
