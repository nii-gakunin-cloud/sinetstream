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


class SinetError(Exception):
    """Base class for exceptions in this module. """
    pass


class InvalidConfigError(SinetError):
    pass


class NoServiceError(SinetError):
    pass


class UnsupportedServiceTypeError(SinetError):
    pass


class NoConfigError(SinetError):
    pass


class InvalidArgumentError(SinetError):
    pass


class ConnectionError(SinetError):
    pass


class AlreadyConnectedError(SinetError):
    pass


class InvalidMessageError(SinetError):
    pass


class AuthorizationError(SinetError):
    pass
