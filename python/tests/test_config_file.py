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

import os
from logging import getLogger
from pathlib import Path

import pytest
from conftest import create_config_file, SERVICE, TOPIC
from sinetstream import (
    MessageReader,
    MessageWriter,
    AT_MOST_ONCE,
    BYTE_ARRAY,
    NoConfigError,
    InvalidConfigError
)

logger = getLogger(__name__)
pytestmark = pytest.mark.usefixtures('dummy_reader_plugin', 'dummy_writer_plugin')


@pytest.mark.parametrize("io", [MessageReader, MessageWriter])
def test_config_dir(setup_config_file, config_file, io):
    ss = io(service=SERVICE, topic=TOPIC, config_file=config_file)
    assert ss.consistency == AT_MOST_ONCE
    assert ss.value_type == BYTE_ARRAY


@pytest.mark.parametrize("io", [MessageReader, MessageWriter])
def test_config_dir_not_exists(setup_config, config_file, io):
    with pytest.raises(NoConfigError):
        io(service=SERVICE, topic=TOPIC, config_file=config_file)


@pytest.fixture()
def setup_config_file(tmp_path, config_brokers, config_topic, config_params, config_file):
    cwd = Path.cwd().absolute()
    try:
        os.chdir(str(tmp_path))
        create_config_file(config_brokers, config_topic, config_params, config=config_file)
        yield
    finally:
        os.chdir(str(cwd))


@pytest.fixture()
def config_file(tmp_path):
    return Path("ss.yml")


def make_secret(x):
    import sinetstream
    priv_key, rsa_cipher, _fingerprint = sinetstream.configs.get_rsa_cipher()
    key_size = priv_key.size_in_bytes()
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


@pytest.fixture()
def config_params():
    # import sinetstream
    return {
        "binbin": b"text in binbin",
        # "enen": sinetstream.utils.SecretValue(make_secret(b'text in enen'), None)
        "enen": "PLACEHOLDER"
    }


def test_config_encrypted(setup_privkey, setup_config_file, config_file):
    with Path(config_file).open(mode="r") as f:
        y = f.read()
    y = y.replace("PLACEHOLDER", f"!sinetstream/encrypted {make_secret(b'text in enen')}")
    with Path(config_file).open(mode="w") as f:
        f.write(y)
    from sinetstream.configs import get_config_params
    confver, svc, params = get_config_params(SERVICE, config_file=config_file)
    assert svc == SERVICE
    assert params["binbin"] == b"text in binbin"
    assert params["enen"] == b"text in enen"


def test_config_service_none2(setup_config_file, config_file):
    import yaml
    with Path(config_file).open(mode="r") as f:
        y = yaml.safe_load(f)
    if "header" in y:
        # V2
        config = y["config"]
    else:
        # V1
        config = y
    k1, v1 = config.items().__iter__().__next__()
    config[k1 + "-second"] = v1
    with Path(config_file).open(mode="w") as f:
        yaml.safe_dump(y, f)

    with pytest.raises(NoConfigError):
        from sinetstream.configs import get_config_params
        get_config_params(None, config_file=config_file)


def test_config_service_none0(setup_config_file, config_file):
    with Path(config_file).open(mode="w") as f:
        print("", file=f)

    with pytest.raises(NoConfigError):
        from sinetstream.configs import get_config_params
        get_config_params(None, config_file=config_file)


def test_config_service_none0_v2(setup_config_file, config_file):
    import yaml
    with Path(config_file).open(mode="r") as f:
        y = yaml.safe_load(f)
    if "header" in y:
        # V2
        y["config"] = {}
    else:
        # V1
        y = {}
    with Path(config_file).open(mode="w") as f:
        yaml.safe_dump(y, f)

    with pytest.raises(InvalidConfigError):
        from sinetstream.configs import get_config_params
        get_config_params(None, config_file=config_file)
