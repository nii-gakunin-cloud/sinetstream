#!/usr/bin/env python3

# Copyright (C) 2023 National Institute of Informatics
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

from io import BytesIO

import numpy as np
from sinetstream import InvalidArgumentError


class NdarrayValueType:
    def _ndarray_to_npy(self, ary):
        f = BytesIO()
        try:
            np.save(f, ary, allow_pickle=False)
            return f.getvalue()
        except ValueError as e:
            raise InvalidArgumentError(e) from e

    def _npy_to_ndarray(self, npy):
        try:
            f = BytesIO(npy)
            return np.load(f, allow_pickle=False)
        except ValueError as e:
            raise InvalidArgumentError(e) from e

    @property
    def serializer(self):
        return self._ndarray_to_npy

    @property
    def deserializer(self):
        return self._npy_to_ndarray
