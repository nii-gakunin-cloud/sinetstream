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
from sinetstreamplugin.kafka import conv_bytes_to_str


class TestConvBytesToStr:
    """Test conversion of bytes to str for SASL parameters."""

    @pytest.mark.parametrize("param_name,value,expected", [
        ('sasl_plain_username', b'testuser', 'testuser'),
        ('sasl_plain_password', b'testpass', 'testpass'),
    ])
    def test_single_param_bytes_to_str(self, param_name, value, expected):
        """Test conversion of individual SASL parameters from bytes to str."""
        params = {param_name: value, 'other_param': 'value'}
        conv_bytes_to_str(params)
        assert params[param_name] == expected
        assert isinstance(params[param_name], str)
        assert params['other_param'] == 'value'

    def test_multiple_sasl_params_bytes(self):
        """Test conversion of multiple SASL parameters from bytes to str."""
        params = {
            'sasl_plain_username': b'testuser',
            'sasl_plain_password': b'testpass',
        }
        conv_bytes_to_str(params)
        assert params['sasl_plain_username'] == 'testuser'
        assert params['sasl_plain_password'] == 'testpass'
        assert isinstance(params['sasl_plain_username'], str)
        assert isinstance(params['sasl_plain_password'], str)

    @pytest.mark.parametrize("params", [
        {'sasl_plain_username': 'testuser', 'sasl_plain_password': 'testpass'},
        {'other_param': 'value'},
        {},
    ])
    def test_already_str_or_missing_not_modified(self, params):
        """Test that str values and missing params are not modified."""
        original = params.copy()
        conv_bytes_to_str(params)
        assert params == original

    def test_non_sasl_bytes_params_not_modified(self):
        """Test that non-SASL parameters are not modified even if bytes."""
        params = {
            'sasl_plain_username': b'testuser',
            'some_other_bytes_param': b'value',
        }
        conv_bytes_to_str(params)
        assert params['sasl_plain_username'] == 'testuser'
        assert params['some_other_bytes_param'] == b'value'
