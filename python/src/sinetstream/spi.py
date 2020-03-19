#!/usr/local/bin/python3.6
# vim: expandtab shiftwidth=4

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
from abc import ABCMeta, abstractmethod

logger = logging.getLogger(__name__)


def _subclasscheck(subclass, methods):
    if all(any(method in cls.__dict__ for cls in subclass.__mro__)
           for method in methods):
        return True
    return NotImplemented


class PluginMessageWriter(metaclass=ABCMeta):
    @abstractmethod
    def open(self):
        pass

    @abstractmethod
    def close(self):
        pass

    @abstractmethod
    def publish(self, message):
        pass

    @classmethod
    def __subclasshook__(cls, subclass):
        return _subclasscheck(subclass, ['open', 'close', 'publish'])


class PluginMessageReader(metaclass=ABCMeta):
    @abstractmethod
    def open(self):
        pass

    @abstractmethod
    def close(self):
        pass

    @abstractmethod
    def __iter__(self):
        pass

    @classmethod
    def __subclasshook__(cls, subclass):
        return _subclasscheck(subclass, ['open', 'close', '__iter__'])


class PluginValueType(metaclass=ABCMeta):
    @property
    @abstractmethod
    def serializer(self):
        pass

    @property
    @abstractmethod
    def deserializer(self):
        pass

    @classmethod
    def __subclasshook__(cls, subclass):
        return _subclasscheck(subclass, ['serializer', 'deserializer'])
