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
from abc import ABCMeta, abstractmethod

logger = logging.getLogger(__name__)


def _subclasscheck(subclass, methods):
    if all(any(method in cls.__dict__ for cls in subclass.__mro__)
           for method in methods):
        return True
    return NotImplemented


class PluginMessage(bytes):
    def set_timestamp(self, timestamp):
        self.timestamp = timestamp

    def __str__(self):
        s = super().__str__()
        if hasattr(self, "timestamp"):
            return f"{__class__}({s}, timestamp={self.timestamp})"
        else:
            return s


class PluginMessageWriter(metaclass=ABCMeta):
    @abstractmethod
    def open(self):
        pass

    @abstractmethod
    def close(self):
        pass

    def metrics(self):
        return None

    def reset_metrics(self):
        pass

    def info(self, name, kwargs):
        return None

    @abstractmethod
    def publish(self, message):
        pass

    @classmethod
    def __subclasshook__(cls, subclass):
        return _subclasscheck(subclass, ['open', 'close', 'metrics', 'reset_metrics', 'publish'])


class PluginAsyncMessageWriter(metaclass=ABCMeta):
    @abstractmethod
    def open(self):
        pass

    @abstractmethod
    def close(self):
        pass

    def metrics(self):
        return None

    def reset_metrics(self):
        pass

    def info(self, name, kwargs):
        return None

    @abstractmethod
    def publish(self, message):
        pass

    @classmethod
    def __subclasshook__(cls, subclass):
        return _subclasscheck(subclass, ['open', 'close', 'metrics', 'reset_metrics', 'publish'])


class PluginMessageReader(metaclass=ABCMeta):
    @abstractmethod
    def open(self):
        pass

    @abstractmethod
    def close(self):
        pass

    def metrics(self):
        return None

    def reset_metrics(self):
        pass

    def info(self, name, kwargs):
        return None

    @abstractmethod
    def __iter__(self):
        pass

    @classmethod
    def __subclasshook__(cls, subclass):
        return _subclasscheck(subclass, ['open', 'close', 'metrics', 'reset_metrics', '__iter__'])


class PluginAsyncMessageReader(metaclass=ABCMeta):
    @abstractmethod
    def open(self):
        pass

    @abstractmethod
    def close(self):
        pass

    def metrics(self):
        return None

    def reset_metrics(self):
        pass

    def info(self, name, kwargs):
        return None

    @property
    @abstractmethod
    def on_message(self):
        pass

    @on_message.setter
    @abstractmethod
    def on_message(self, on_message):
        pass

    @property
    @abstractmethod
    def on_failure(self):
        pass

    @on_failure.setter
    @abstractmethod
    def on_failure(self, on_failure):
        pass

    @classmethod
    def __subclasshook__(cls, subclass):
        return _subclasscheck(
            subclass, ['open', 'close', 'metrics', 'reset_metrics', 'on_message', 'on_failure'])


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


class PluginCompression(metaclass=ABCMeta):
    @property
    @abstractmethod
    def compressor(self):
        pass

    @property
    @abstractmethod
    def decompressor(self):
        pass

    @classmethod
    def __subclasshook__(cls, subclass):
        return _subclasscheck(subclass, ['compressor', 'decompressor'])
