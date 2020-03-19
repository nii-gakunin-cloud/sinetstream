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

import numpy as np
import pytest
from sinetstream import InvalidArgumentError
from sinetstreamplugin.valuetype.image import ImageValueType

logging.basicConfig(level=logging.ERROR)


def test_serialize_result_type(image_object):
    value_type = ImageValueType()
    result = value_type.serializer(image_object)
    assert type(result) is bytes


def test_serialize_bad_object(image_bytes):
    value_type = ImageValueType()
    with pytest.raises(InvalidArgumentError):
        value_type.serializer(image_bytes)


def test_deserialize_result_type(image_bytes):
    value_type = ImageValueType()
    result = value_type.deserializer(image_bytes)
    assert type(result) is np.ndarray


def test_deserialize_bad_data(image_bytes):
    data = bytearray(image_bytes)
    data[2] = ord('X')
    value_type = ImageValueType()
    with pytest.raises(InvalidArgumentError):
        value_type.deserializer(data)


def test_identity(image_object):
    value_type = ImageValueType()
    bytes_data = value_type.serializer(image_object)
    out_img = value_type.deserializer(bytes_data)
    assert np.array_equal(image_object, out_img)
