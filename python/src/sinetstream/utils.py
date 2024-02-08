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

from importlib.metadata import entry_points

logger = logging.getLogger(__name__)

user_config_dir = "~/.config/sinetstream"


# class SecretValue(yaml.YAMLObject):
class SecretValue(object):
    def __init__(self, v, fp):
        self.v = v
        self.fp = fp

    def get(self):
        return (self.v, self.fp)

    def __repr__(self):
        return f"SecretValue(v='{self.v}', fp='{self.fp}')"

    def __str__(self):
        return self.__repr__()


class Registry(object):

    def __init__(self, group, cls=None):
        self.group = group
        self._cls = cls
        self._plugins = {}
        self.register_entry_points()

    def register(self, name, plugin):
        self._plugins[name] = plugin

    def _get_entry_points(self):
        eps = entry_points()
        if isinstance(eps, dict):
            return eps.get(self.group, [])
        return eps.select(group=self.group)

    def register_entry_points(self):
        for ep in self._get_entry_points():
            logger.debug(f"entry_point.name={ep.name}")
            self.register(ep.name, ep)

    def _get_name(self, params):
        if isinstance(params, str):
            return params
        if isinstance(params, dict):
            return params["name"]
        if hasattr(params, "name"):
            return getattr(params, "name")
        return None

    def get(self, params):
        name = self._get_name(params)
        if name in self._plugins:
            cls = self._plugins[name].load()
            if not (self._cls is None or issubclass(cls, self._cls)):
                logger.error(f'{cls} does not implement {self._cls}')
                return None
            return cls
        else:
            logger.error(
                f"the corresponding plugin was not found: {self.group}:{name}")
            return None
