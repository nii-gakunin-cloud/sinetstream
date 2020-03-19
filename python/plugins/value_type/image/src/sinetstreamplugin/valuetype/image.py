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
import cv2
import numpy as np
from sinetstream import InvalidArgumentError


class ImageValueType(object):
    def __init__(self, img_format='.png', img_params=()):
        self._format = img_format
        self._encode_params = img_params
        self._decode_flags = cv2.IMREAD_COLOR

    def _image_to_bytes(self, image):
        if type(image) is not np.ndarray:
            raise InvalidArgumentError(f'invalid image object type: {type(image)}')
        ret, enc_img = cv2.imencode(self._format, image, self._encode_params)
        if not ret:
            raise InvalidArgumentError('image encoding failed')
        return enc_img.tobytes()

    def _image_from_bytes(self, byte_data):
        nd_array = np.frombuffer(byte_data, dtype='uint8')
        ret = cv2.imdecode(nd_array, self._decode_flags)
        if ret is None:
            raise InvalidArgumentError('not a byte sequence representing an image')
        return ret

    @property
    def serializer(self):
        return self._image_to_bytes

    @property
    def deserializer(self):
        return self._image_from_bytes
