#!/usr/bin/env python3

# Copyright (C) 2021 National Institute of Informatics
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
import copy
# import os
# from pathlib import Path
# from shutil import copyfile

import pytest
import requests_mock
import sinetstream
# from conftest import create_config_file, SERVICE, SERVICE_TYPE, BROKER

logging.basicConfig(level=logging.WARNING)
logger = logging.getLogger(__name__)

# note: https://requests-mock.readthedocs.io/en/latest/


# FIXME: 実サーバにつないでテストするのダメ
# FIXME: ~/.config/sinetstream/auth.jsonをつかうからダメ
@pytest.mark.skip
@pytest.mark.parametrize("svc, cfg", [("service-kafka-001", "test-sinetstream-client")])
def test_get_config(svc, cfg):
    service, params = sinetstream.configs.get_config_params(
                svc,
                config=cfg)
    logger.info(f"params={params}")
    print(f"params={params}")
    assert service == svc
    assert params is not None


def test_get_config__auth_401(setup_auth):
    adapter = requests_mock.Adapter()
    adapter.register_uri("POST",
                         "/api/v1/authentication",
                         status_code=401,
                         text="fail by mock")
    try:
        sinetstream.configs.get_config_params("service-kafka-001",
                                              config="stream009",
                                              auth_path=setup_auth,
                                              mount_args=("https://", adapter))
        assert False
    except sinetstream.AuthorizationError:
        pass
    except Exception:
        assert False


authentication_resp = {
    "accessToken": "dummy token",
    "gomi": "gomi must be ignored",
}


def test_get_config__configs_4XX(setup_auth):
    for statuscode, expected in [
            (401, sinetstream.AuthorizationError),
            (404, sinetstream.NoServiceError),
            (500, sinetstream.ConnectionError)]:
        adapter = requests_mock.Adapter()
        adapter.register_uri("POST",
                             "/api/v1/authentication",
                             status_code=201,
                             json=authentication_resp)
        adapter.register_uri("GET",
                             "/api/v1/configs/stream009",
                             status_code=statuscode,
                             text="fail by mock")
        try:
            sinetstream.configs.get_config_params("service-kafka-001",
                                                  config="stream009",
                                                  auth_path=setup_auth,
                                                  mount_args=("https://", adapter))
            assert False
            logger.error(f"statuscode={statuscode} expected={expected}")
        except expected:
            pass
        except Exception as ex:
            logger.error(f"statuscode={statuscode} expected={expected} caught={ex}")
            assert False


configs_resp_simple = {
    "name": "stream009",
    "config": {
        "header": {
            "version": 2,
            "fingerprint": "uso800",
        },
        "config": {
            "service-kafka-001": {
                "uso": 800
            }
        }
    },
    "attachments": [],
    "secrets": [],
}


@pytest.mark.parametrize("svc", ["service-kafka-001", None])
def test_get_config__configs_simple(setup_auth, svc):
    adapter = requests_mock.Adapter()
    adapter.register_uri("POST",
                         "/api/v1/authentication",
                         status_code=201,
                         json=authentication_resp)
    adapter.register_uri("GET",
                         "/api/v1/configs/stream009",
                         status_code=200,
                         json=configs_resp_simple)
    try:
        service, params = sinetstream.configs.get_config_params(
                    svc,
                    config="stream009",
                    auth_path=setup_auth,
                    mount_args=("https://", adapter))
        assert service == "service-kafka-001"
        assert params == configs_resp_simple["config"]["config"]["service-kafka-001"]
    except Exception:
        assert False


def B(x):
    from base64 import b64encode
    return b64encode(x.encode()).decode()


configs_attach_resp = {
    "name": "stream009",
    "config": {
        "header": {
            "version": 2,
            "fingerprint": "uso800",
        },
        "config": {
            "service-kafka-001": {
                "aaa": "AAA",
                "bbb": "BBB",
            }
        }
    },
    "attachments": [
        {"target": "*.aaa",                 "value": B("AAA-overwrite")},
        {"target": "service-kafka-001.bbb", "value": B("BBB-overwrite")},
        {"target": "service-kafka-999.bbb", "value": B("BBB-ignore")},
        {"target": "*.ccc",                 "value": B("CCC-insert")},
        {"target": "service-kafka-001.ddd", "value": B("DDD-insert")},
    ],
}


@pytest.mark.parametrize("svc", ["service-kafka-001", None])
def test_get_config__configs_attach(setup_auth, svc):
    adapter = requests_mock.Adapter()
    adapter.register_uri("POST",
                         "/api/v1/authentication",
                         status_code=201,
                         json=authentication_resp)
    adapter.register_uri("GET",
                         "/api/v1/configs/stream009",
                         status_code=200,
                         json=configs_attach_resp)
    try:
        service, params = sinetstream.configs.get_config_params(
                    svc,
                    config="stream009",
                    auth_path=setup_auth,
                    mount_args=("https://", adapter))
        assert service == "service-kafka-001"
        assert params == {
            "aaa": b"AAA-overwrite",
            "bbb": b"BBB-overwrite",
            "ccc": b"CCC-insert",
            "ddd": b"DDD-insert",
        }
    except Exception:
        assert False


@pytest.mark.parametrize("svc", ["service-kafka-001", None])
def test_get_config__configs_empty_attach(setup_auth, svc):
    adapter = requests_mock.Adapter()
    adapter.register_uri("POST",
                         "/api/v1/authentication",
                         status_code=201,
                         json=authentication_resp)
    resp = copy.deepcopy(configs_attach_resp)

    def test():
        adapter.register_uri("GET",
                             "/api/v1/configs/stream009",
                             status_code=200,
                             json=resp)
        try:
            service, params = sinetstream.configs.get_config_params(
                        svc,
                        config="stream009",
                        auth_path=setup_auth,
                        mount_args=("https://", adapter))
            assert service == "service-kafka-001"
            assert params == {
                "aaa": "AAA",
                "bbb": "BBB",
            }
        except Exception:
            assert False
    resp["attachments"] = []
    test()
    resp["secrets"] = []
    test()
    del resp["attachments"]
    test()
    del resp["secrets"]
    test()


configs_secret_resp = copy.deepcopy(configs_attach_resp)
configs_secret_resp["secrets"] = [
    {"id": "secret_id_1", "target": "*.himitsu.value"},
    {"ids": [
            {"id": "secret_id_1", "version": 1},
            {"id": "secret_id_2", "version": 2},
     ],
     "target": "*.himitsu2.value"},
]


def test_get_config__secret_4XX(setup_auth, setup_privkey):
    for statuscode, expected in [
            (401, sinetstream.AuthorizationError),
            (404, sinetstream.NoServiceError),
            (500, sinetstream.ConnectionError)]:
        adapter = requests_mock.Adapter()
        adapter.register_uri("POST",
                             "/api/v1/authentication",
                             status_code=201,
                             json=authentication_resp)
        adapter.register_uri("GET",
                             "/api/v1/configs/stream009",
                             status_code=200,
                             json=configs_secret_resp)
        adapter.register_uri("GET",
                             "/api/v1/secrets/secret_id_1",
                             status_code=statuscode,
                             text="fail by mock")
        try:
            sinetstream.configs.get_config_params(
                        "service-kafka-001",
                        config="stream009",
                        auth_path=setup_auth,
                        mount_args=("https://", adapter))
            logger.error(f"statuscode={statuscode} expected={expected}")
            assert False
        except expected:
            pass
        except Exception as ex:
            logger.error(f"statuscode={statuscode} expected={expected} caught={ex}")
            assert False


def make_secret(x):
    priv_key, rsa_cipher, _fingerprint = sinetstream.configs.get_rsa_cipher()
    key_size = priv_key.size_in_bytes()
    # logger.error(f"XXX key_size={key_size}")
    key_size = 16
    import struct
    header = struct.pack("!HBB", 1, 1, 1)
    key = (123).to_bytes(key_size, byteorder="big")
    encrypted_key = rsa_cipher.encrypt(key)
    iv = (456).to_bytes(12, byteorder="big")
    from Cryptodome.Cipher import AES
    cipher = AES.new(key, AES.MODE_GCM, nonce=iv)
    cipher.update(header)
    cipher.update(encrypted_key)
    cipher.update(iv)
    data, tag = cipher.encrypt_and_digest(x)
    from base64 import b64encode
    return b64encode(header + encrypted_key + iv + data + tag).decode()


@pytest.mark.parametrize("svc", ["service-kafka-001", None])
def test_get_config__secret(setup_auth, setup_privkey, svc):
    adapter = requests_mock.Adapter()
    adapter.register_uri("POST",
                         "/api/v1/authentication",
                         status_code=201,
                         json=authentication_resp)
    adapter.register_uri("GET",
                         "/api/v1/configs/stream009",
                         status_code=200,
                         json=configs_secret_resp)
    himitsu_value = b"abcdefghijklmn"
    himitsu_secret = make_secret(himitsu_value)
    secret_resp = {
        "id": "secret_id_1",
        "fingerprint": "1234",
        "target": "*.himitsu.value",
        "value": himitsu_secret,
    }
    adapter.register_uri("GET",
                         "/api/v1/secrets/secret_id_1",
                         status_code=200,
                         json=secret_resp)
    himitsu2_value = b"opqrstuvwxyz"
    himitsu2_secret = make_secret(himitsu2_value)
    secret2_resp = {
        "id": "secret_id_2",
        "fingerprint": "1234",
        "target": "*.himitsu2.value",
        "value": himitsu2_secret,
    }
    adapter.register_uri("GET",
                         "/api/v1/secrets/secret_id_2",
                         status_code=200,
                         json=secret2_resp)
    try:
        service, params = sinetstream.configs.get_config_params(
                    svc,
                    config="stream009",
                    auth_path=setup_auth,
                    mount_args=("https://", adapter))
        assert service == "service-kafka-001"
        assert params["himitsu"]["value"] == himitsu_value
        assert params["himitsu2"]["value"] == himitsu2_value
    except Exception as ex:
        logger.error(f"caught={ex}")
        assert False
