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

import json
import logging
import os
import requests
import base64

from sinetstream.error import (
    NoConfigError, NoServiceError, InvalidConfigError, AuthorizationError, ConnectionError)

from sinetstream.utils import (
    user_config_dir,
    SecretValue)

import sinetstream.configs


logger = logging.getLogger(__name__)

insecure_log = os.getenv("SINETSTREAM_ENALBE_INSECURE_LOG") is not None


def test_enable_debug0():
    # https://gist.github.com/Daenyth/b57f8522b388e66fcf3b
    from http.client import HTTPConnection  # py3

    log = logging.getLogger('urllib3')
    log.setLevel(logging.DEBUG)

    # logging from urllib3 to console
    ch = logging.StreamHandler()
    ch.setLevel(logging.DEBUG)
    log.addHandler(ch)
    # print statements from `http.client.HTTPConnection` to console/stdout
    HTTPConnection.debuglevel = 1
# test_enable_debug0()


def test_enable_debug():
    # https://docs.python-requests.org/en/master/api/
    from http.client import HTTPConnection
    HTTPConnection.debuglevel = 1
    # logging.basicConfig() # you need to initialize logging,
    # otherwise you will not see anything from requests.
    # logging.getLogger().setLevel(logging.DEBUG)
    logger.setLevel(logging.DEBUG)
    requests_log = logging.getLogger("urllib3")
    requests_log.setLevel(logging.DEBUG)
    requests_log.propagate = True


if insecure_log:
    test_enable_debug()


user_auth_path = user_config_dir + "/auth.json"

api_path = "/api/v1"


def safe_gets(d, ks):
    v = d.get(ks[0])
    if len(ks) == 1:
        return v
    else:
        return safe_gets(v, ks[1:]) if isinstance(v, dict) else None


def get_value(d, k, t, optional=False, default=None):
    if k not in d:
        if optional:
            return default
        raise InvalidConfigError(f"No '{k}' in response")
    v = d.get(k)
    if not isinstance(v, t):
        if type(t) is not tuple:
            t = (t,)
        raise InvalidConfigError(f"'{k}' is not {','.join([x.__class__.__name__ for x in t])}")
    return v


def get_auth_info(auth_path=None):
    auth_path = auth_path if auth_path is not None else user_auth_path
    try:
        path = os.path.expanduser(auth_path)
        logger.debug(f"read {path}")
        with open(path) as fp:
            auth_info = json.load(fp)
            try:
                config_server = get_value(auth_info, "config-server", dict)
                address = get_value(config_server, "address", str)
                user = get_value(config_server, "user", str)
                secret_key = get_value(config_server, "secret-key", str)
            except InvalidConfigError as e:
                e0 = e.args[0].replace("in response", f"in {path}")  # hack...
                raise InvalidConfigError((e0,) + e.args[1:])
            return (address, user, secret_key)
    except FileNotFoundError:
        raise NoConfigError(f"No API key file exist: {auth_path}")


def get_access_token(session, base_uri, user, secret_key):
    resp = session.post(base_uri + "/authentication",
                        json={"user": user,
                              "secret-key": secret_key,
                              "strategy": "api-access"})
    logger.info(f"/authentication => {resp.status_code} {resp.text}")
    if resp.status_code == 201:
        pass
    else:
        logger.error(f"{resp.status_code} {resp.text}")
        if resp.status_code == 401:
            raise AuthorizationError(resp.text)
        else:
            raise ConnectionError(resp.text)
    res = resp.json()
    if insecure_log:
        logger.debug(f"resp.text={resp.text}")

    access_token = get_value(res, "accessToken", str)

    expired = safe_gets(res, ["authentication", "payload", "exp"])
    if insecure_log:
        import datetime
        if expired is not None:
            logger.debug(f"expired={expired}({datetime.datetime.fromtimestamp(expired)})")
        else:
            logger.debug("expired=None")

    return (access_token, expired)


def get_config_info(session, base_uri, common_headers, config_name):
    resp = session.get(base_uri + "/configs/" + config_name, headers=common_headers)
    logger.info(f"/configs/{config_name} => {resp.status_code} {resp.text}")
    if resp.status_code == 200:
        pass
    else:
        logger.error(f"{resp.status_code} {resp.text}")
        if resp.status_code == 401:
            raise AuthorizationError(resp.text)
        elif resp.status_code == 404:
            raise NoServiceError(resp.text)
        else:
            raise ConnectionError(resp.text)
    res = resp.json()
    if insecure_log:
        logger.error(f"{json.dumps(res, indent=2)}")
    # response is:
    # name: str
    # config:
    #     header:
    #         version: num
    #     config:
    #         service_name: dict
    # attachments:
    #     -
    #         target: str
    #         value: str
    # secrets:
    #     -
    #         target: str
    #         id: str

    if type(res) is not dict:
        raise InvalidConfigError("Bad response")
    name = get_value(res, "name", str)
    if name != config_name:
        raise InvalidConfigError(f"name '{name}' is not {config_name}")
    content = get_value(res, "config", dict)

    if "attachments" in res:
        attachments = get_value(res, "attachments", list)
        for attachment in attachments:
            if type(attachment) is not dict:
                raise InvalidConfigError("'attachments[]' is not dict")
    else:
        attachments = []

    if "secrets" in res:
        secrets = get_value(res, "secrets", list)
        for secret in secrets:
            if type(secret) is not dict:
                raise InvalidConfigError("'secrets[]' is not dict")
    else:
        secrets = []

    return (content, attachments, secrets)


def get_secret(session, base_uri, common_headers, sec_id, fingerprint):
    headers = common_headers.copy()
    headers["SINETStream-config-publickey"] = fingerprint
    resp = session.get(base_uri + "/secrets/" + sec_id, headers=headers)
    if resp.status_code == 200:
        pass
    else:
        logger.error(f"{resp.status_code} {resp.text}")
        if resp.status_code == 401:
            raise AuthorizationError(resp.text)
        elif resp.status_code == 404:
            raise NoServiceError(resp.text)
        else:
            raise ConnectionError(resp.text)
    res = resp.json()
    if insecure_log:
        logger.error(f"{json.dumps(res, indent=2)}")
    rid = get_value(res, "id", str)
    res_fingerprint = get_value(res, "fingerprint", str, optional=True)
    if res_fingerprint != fingerprint:
        logger.warning(f"fingerprint mismatch: {res_fingerprint}(server sent) != {fingerprint}(my pubkey)")
    target = get_value(res, "target", str)
    value64 = get_value(res, "value", str)
    if rid != sec_id:
        raise InvalidConfigError(f"id={rid} must be {sec_id}")
    value = base64.b64decode(value64)
    evalue = SecretValue(value, fingerprint)
    return (evalue, target)


def dict_update_value(d, path, value):
    for k in path[0:-1]:
        if k not in d:
            d[k] = {}
        if not isinstance(d[k], dict):
            raise InvalidConfigError(f"key {k} in the config file must be dict")
        d = d[k]
    d[path[-1]] = value


def get_config_params_server(service, config_name, mount_args=None, **kwargs):
    auth_info = get_auth_info(auth_path=kwargs.get("auth_path"))
    (address, user, secret_key) = auth_info
    if insecure_log:
        logger.debug(f"auth_info={(address, user, secret_key)}")

    base_uri = address + api_path

    session = requests.Session()
    if mount_args is not None:
        session.mount(*mount_args)

    (access_token, expired) = get_access_token(session, base_uri, user, secret_key)

    common_headers = {
        "Authorization": "Bearer " + access_token
    }

    (content, attachments, secrets) = get_config_info(session, base_uri, common_headers, config_name)

    header = get_value(content, "header", dict)
    config = get_value(content, "config", dict)

    version = get_value(header, "version", int)
    if version != 2:
        raise InvalidConfigError(f"version {version} must be 2")
    # fingerprint_header = get_value(header, "fingerprint", str, optional=True)

    if service is None:
        if len(config) == 0:
            raise NoConfigError("empty config")
        if len(config) > 1:
            logger.error(f"services in {config_name}: {[svc for svc in config.keys()]}")
            raise NoConfigError("The parameter service must be specified "
                                f"since many services are defined in the config {config_name}")
        service = list(config.keys()).pop()

    if service not in config:
        raise NoServiceError()

    import copy
    params = copy.deepcopy(get_value(config, service, dict))

    for attachment in attachments:
        target = get_value(attachment, "target", str)
        path = target.split(".")
        if len(path) < 2:
            raise InvalidConfigError(f"target {target} is too short")
        if path[0] == "*" or path[0] == service:
            value64 = get_value(attachment, "value", str)
            value = base64.b64decode(value64)
            dict_update_value(params, path[1:], value)

    for secret in secrets:
        target = secret.get("target")
        path = target.split(".")
        if path[0] == "*" or path[0] == service:
            ids = secret.get("ids")
            if ids is not None:
                # get the latest id
                sec_id = None
                max_ver = 0
                for e in ids:
                    id1 = e.get("id")
                    ver = e.get("version")
                    if id1 is None or ver is None:
                        raise InvalidConfigError(f"malformed entry '{e}' exists for '{target}'")
                    if ver > max_ver and id1 is not None:
                        max_ver = ver
                        sec_id = id1
                if sec_id is None:
                    raise InvalidConfigError(f"no valid id exists for '{target}'")
            else:
                sec_id = secret.get("id")
            _, _, fingerprint = sinetstream.configs.get_private_key()
            (evalue, sec_target) = get_secret(session, base_uri, common_headers, sec_id, fingerprint)
            if sec_target != target:
                raise InvalidConfigError(f"target={sec_target} must be {target}")
            dict_update_value(params, path[1:], evalue)

    # XXX: attachとsecretでtargetが被った場合はしらん。

    logger.debug(f"params={params}")
    return service, params
