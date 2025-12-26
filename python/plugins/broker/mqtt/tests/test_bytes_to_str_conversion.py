#!/usr/bin/env python3

# Copyright (C) 2025 National Institute of Informatics
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

import pytest
from sinetstreamplugin.mqtt import _conv_bytes_to_str


class TestConvBytesToStr:
    """Test conversion of bytes to str for MQTT parameters."""

    @pytest.mark.parametrize("param_key,field,value,expected", [
        ('username_pw', 'username', b'testuser', 'testuser'),
        ('username_pw', 'password', b'testpass', 'testpass'),
        ('username_pw_set', 'username', b'testuser', 'testuser'),
        ('username_pw_set', 'password', b'testpass', 'testpass'),
    ])
    def test_single_field_bytes_to_str(self, param_key, field, value, expected):
        """Test conversion of individual fields from bytes to str."""
        other_field = 'password' if field == 'username' else 'username'
        params = {
            param_key: {field: value, other_field: 'other_value'},
            'other_param': 'value',
        }
        _conv_bytes_to_str(params)
        assert params[param_key][field] == expected
        assert isinstance(params[param_key][field], str)
        assert params[param_key][other_field] == 'other_value'
        assert params['other_param'] == 'value'

    @pytest.mark.parametrize("param_key", ['username_pw', 'username_pw_set'])
    def test_both_fields_bytes_to_str(self, param_key):
        """Test conversion of both username and password from bytes to str."""
        params = {
            param_key: {
                'username': b'testuser',
                'password': b'testpass',
            },
        }
        _conv_bytes_to_str(params)
        assert params[param_key]['username'] == 'testuser'
        assert params[param_key]['password'] == 'testpass'
        assert isinstance(params[param_key]['username'], str)
        assert isinstance(params[param_key]['password'], str)

    @pytest.mark.parametrize("params", [
        {'username_pw': {'username': 'testuser', 'password': 'testpass'}},
        {'username_pw_set': {'username': 'testuser'}},
        {'other_param': 'value'},
        {},
    ])
    def test_already_str_or_missing_not_modified(self, params):
        """Test that str values and missing params are not modified."""
        original = params.copy()
        if 'username_pw' in params:
            original['username_pw'] = params['username_pw'].copy()
        if 'username_pw_set' in params:
            original['username_pw_set'] = params['username_pw_set'].copy()
        _conv_bytes_to_str(params)
        assert params == original

    def test_non_target_params_not_modified(self):
        """Test that non-target parameters are not modified even if bytes."""
        params = {
            'username_pw': {'username': b'testuser'},
            'some_other_bytes_param': b'value',
        }
        _conv_bytes_to_str(params)
        assert params['username_pw']['username'] == 'testuser'
        assert params['some_other_bytes_param'] == b'value'

    def test_nested_non_target_fields_not_modified(self):
        """Test that nested non-target fields are not modified."""
        params = {
            'username_pw': {
                'username': b'testuser',
                'password': b'testpass',
                'some_other_field': b'value',
            },
        }
        _conv_bytes_to_str(params)
        assert params['username_pw']['username'] == 'testuser'
        assert params['username_pw']['password'] == 'testpass'
        assert params['username_pw']['some_other_field'] == b'value'
