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

import argparse
import io
import logging
import sys

import cv2
from PIL import Image
import numpy
import sinetstream

logging.basicConfig(level=logging.INFO)

parser = argparse.ArgumentParser(description="SINETStream Consumer")
parser.add_argument("-s", "--service",
                    metavar="SERVICE_NAME",
                    required=True)
parser.add_argument("-t", "--topic",
                    metavar="TOPIC",
                    action="append",
                    required=True)

args = parser.parse_args()

print(f"# service={args.service}")
print(f"# topics={','.join(args.topic)}")

with sinetstream.MessageReader(args.service, args.topic) as f:
    for t in args.topic:
        cv2.namedWindow(t, cv2.WINDOW_AUTOSIZE)

    for msg in f:
        img = Image.open(io.BytesIO(msg.value))
        img_numpy = numpy.asarray(img)
        img_numpy_bgr = cv2.cvtColor(img_numpy, cv2.COLOR_RGBA2BGR)
        cv2.imshow(msg.topic, img_numpy_bgr)
        key = cv2.waitKey(1)  # Flush
        if key & 0xFF == ord("q"):
            break

sys.exit(0)
