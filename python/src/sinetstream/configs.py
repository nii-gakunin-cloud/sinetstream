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
import os
import yaml

from base64 import b64encode, b64decode
from getpass import getpass
from hashlib import sha256
from pathlib import Path
from struct import unpack
from threading import Lock

from Cryptodome.Cipher import PKCS1_OAEP, AES
from Cryptodome.Hash import SHA256
from Cryptodome.PublicKey import RSA

from sinetstream.error import (
    InvalidConfigError, NoConfigError, NoServiceError)
from sinetstream.config_client import get_config_params_server
from sinetstream.utils import user_config_dir
from sinetstream.utils import SecretValue

logger = logging.getLogger(__name__)


def sinetstream_encrypted_ctor(constructor, node):
    x = constructor.construct_yaml_str(node)
    return SecretValue(b64decode(x), None)


def secret_value_repl(representer, native):
    v, fp = native.get()
    return representer.represent_scalar("!sinetstream/encrypted", str(v))


def setup_yaml():
    yaml.add_constructor("!sinetstream/encrypted", sinetstream_encrypted_ctor)
    yaml.add_representer(SecretValue, secret_value_repl)


def yaml_load(s):
    try:
        setup_yaml()
        yml = yaml.load(s, Loader=yaml.Loader)
        return yml
    except yaml.scanner.ScannerError:
        return None


config_files = [
    ".sinetstream_config.yml",
    user_config_dir + "/config.yml",
]


CONFIG_VERSION_2 = 2


def get_config_params_local(service, config_file, **kwargs):
    content, config_file = load_config_file(config_file)
    if "header" in content:
        header = content["header"]
        if "version" in header:
            version = header["version"]
        else:
            version = CONFIG_VERSION_2
        if version != CONFIG_VERSION_2:
            raise InvalidConfigError(f"version={version} is invalid ({CONFIG_VERSION_2} expected)")
        if "config" not in content:
            raise InvalidConfigError(f"No config section in {config_file}")
        config = content["config"]
        # FIXME: fingerprint = header.get("fingerprint")
    else:
        # assume version 1
        config = content

    if not isinstance(config, dict):
        raise InvalidConfigError(f"The config file {config_file} is malformed")
    if len(config) == 0:
        raise InvalidConfigError(f"No service defined in {config_file}")

    if service is None:
        if len(config) > 1:
            logger.error(f"services in {config_file}: {[svc for svc in config.keys()]}")
            raise NoConfigError("The parameter service must be specified "
                                f"since many services are defined in {config_file}")
        service = config.keys().__iter__().__next__()
        # service = [k for k in config.keys()][0]

    return service, config.get(service) if isinstance(config, dict) else None


def load_config_file(config_file):
    if config_file is not None:
        content = load_config_from_file(config_file)
        if content is None:
            logger.error(f"No configuration file exist: {config_file}")
            raise NoConfigError(f"No configuration file exist: {config_file}")
        return content, config_file

    url = os.environ.get("SINETSTREAM_CONFIG_URL")
    if url:
        logger.info(f"SINETSTREAM_CONFIG_URL={url}")
        content = load_config_from_url(url)
        if content:
            return content, url

    for config_file in config_files:
        content = load_config_from_file(config_file)
        if content is not None:
            return content, config_file

    logger.error("No configuration file exist")
    raise NoConfigError()


def load_config_from_url(url):
    import urllib.request
    try:
        with urllib.request.urlopen(url) as res:
            content = res.read().decode("utf-8")
            return yaml_load(content)
    except OSError as ex:
        logger.debug('load config file from URL', stack_info=True)
        logger.warning(f'Could not load from the specified URL: {ex}')
        return None


def load_config_from_file(file):
    try:
        with open(os.path.expanduser(file)) as fp:
            logger.debug(f"load config file from {os.path.abspath(file)}")
            yml = yaml_load(fp.read())
            if yml:
                return yml
    except FileNotFoundError:
        logger.info(f"{file}: not found")


private_key_cache_lock = Lock()
private_key_cache = None  # (path, privkey, fp)


def get_private_key():
    global private_key_cache_lock
    global private_key_cache
    with private_key_cache_lock:
        if private_key_cache is not None:
            return private_key_cache
    set_key_cache(load_private_key())
    return get_private_key()


def set_key_cache(x):
    global private_key_cache_lock
    global private_key_cache
    with private_key_cache_lock:
        private_key_cache = x


def clear_key_cache():
    global private_key_cache_lock
    global private_key_cache
    with private_key_cache_lock:
        private_key_cache = None


def load_private_key(private_key_path=None):
    def make_fingerprint(publickey):
        pubkey = b64decode(publickey.export_key(format="OpenSSH").split()[1])
        fp = b64encode(sha256(pubkey).digest()).decode("utf-8").rstrip("=")
        logger.debug(f"fingerprint={fp}")
        return fp

    if private_key_path is None:
        private_key_path = Path(user_config_dir + "/private_key.pem").expanduser()
    pw = os.environ.get("SINETSTREAM_PRIVATE_KEY_PASSPHRASE", None)
    last_ex = None
    n_try = 3
    for i in range(n_try):
        with private_key_path.open() as f:
            try:
                private_key = RSA.importKey(f.read(), passphrase=pw)
                fingerprint = make_fingerprint(private_key.publickey())
                return (private_key_path, private_key, fingerprint)
            except Exception as ex:
                logger.error(f"{ex}")
                last_ex = ex
                pw = getpass(f"Enter passphrase for {private_key_path}: ")
    raise last_ex


def get_rsa_cipher():
    _, priv_key, fingerprint = get_private_key()
    cipher = PKCS1_OAEP.new(priv_key, hashAlgo=SHA256)
    return priv_key, cipher, fingerprint


def check_sec_header(header):
    ver, public_key_type, common_key_type = unpack("!HBB", header)
    if ver != 1:
        raise RuntimeError('Unsupported version')
    if public_key_type != 1:
        raise RuntimeError('Unsupported types of public key cryptography')
    if common_key_type != 1:
        raise RuntimeError('Unsupported type of symmetric key cipher')


def parse_sec_data(sec_data, key_size):
    header = sec_data[0:4]
    check_sec_header(header)
    encrypted_key = sec_data[4: (4 + key_size)]
    iv = sec_data[(4 + key_size): (4 + key_size + 12)]
    data = sec_data[(4 + key_size + 12): -16]
    tag = sec_data[-16:]
    return header, encrypted_key, iv, data, tag


def decrypt_sec_data(sec_data, fingerprint):
    priv_key, rsa_cipher, fp = get_rsa_cipher()
    if fingerprint is not None and fingerprint != fp:
        raise InvalidConfigError(f"pubkey mismatch: encrypted with {fingerprint}, but my pubkey is {fp}")
    header, encrypted_key, iv, data, tag = parse_sec_data(sec_data, priv_key.size_in_bytes())
    key = rsa_cipher.decrypt(encrypted_key)
    cipher = AES.new(key, AES.MODE_GCM, nonce=iv)
    cipher.update(header)
    cipher.update(encrypted_key)
    cipher.update(iv)
    result = cipher.decrypt_and_verify(data, tag)
    return result


def decrypt_params(x):
    if isinstance(x, dict):
        return {k: decrypt_params(v) for k, v in x.items()}
    elif isinstance(x, list):
        return [decrypt_params(v) for v in x]
    elif isinstance(x, SecretValue):
        ev, fp = x.get()
        return decrypt_sec_data(ev, fp)
    else:
        return x


def get_config_params(service=None, config=None, config_file=None, **kwargs):
    if config is None:
        service, params = get_config_params_local(service, config_file, **kwargs)
    else:
        service, params = get_config_params_server(service, config, **kwargs)
    if params is None:
        logger.error(f"invalid service: {service}")
        raise NoServiceError()

    params = decrypt_params(params)
    clear_key_cache()

    return service, params
