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

from sinetstream.spi import PluginValueType
from sinetstream.utils import Registry

TEXT = "text"
BYTE_ARRAY = "byte_array"
value_type_registry = Registry('sinetstream.value_type', PluginValueType)


class ByteArrayValueType(object):
    @property
    def serializer(self):
        return None

    @property
    def deserializer(self):
        return None


class TextValueType(object):
    @property
    def serializer(self):
        return lambda x: x.encode()

    @property
    def deserializer(self):
        return lambda x: x.decode()


class ByteArrayValueTypeEntryPoint(object):
    @classmethod
    def load(cls):
        return ByteArrayValueType


class TextValueTypeEntryPoint(object):
    @classmethod
    def load(cls):
        return TextValueType


value_type_registry.register(BYTE_ARRAY, ByteArrayValueTypeEntryPoint)
value_type_registry.register(TEXT, TextValueTypeEntryPoint)
