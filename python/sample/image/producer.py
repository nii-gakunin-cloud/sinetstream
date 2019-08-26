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

parser = argparse.ArgumentParser(description="SINETStream Producer")
parser.add_argument("-s", "--service",
                    metavar="SERVICE_NAME",
                    required=True)
parser.add_argument("-t", "--topic",
                    metavar="TOPIC",
                    required=True)
parser.add_argument("-f", "--input-video",
                    metavar="FILE",
                    required=True)
parser.add_argument("-l", "--preview",
                    action="store_true",
                    help="show on local too")

args = parser.parse_args()

print(f"# service={args.service}")
print(f"# topic={args.topic}")
print(f"# input-video={args.input_video}")

jpeg_quality = 95       # 0..100
enc_param = [int(cv2.IMWRITE_JPEG_QUALITY), jpeg_quality]

video = cv2.VideoCapture(args.input_video)
if not video.isOpened():
    print(f"ERROR: cannot open the file {args.input_video}")
    sys.exit(1)

n_frame = 0
with sinetstream.MessageWriter(args.service, args.topic) as f:
    if args.preview:
        print("Hit 'q' to stop")
    while video.isOpened():
        success, frame = video.read()
        if not success:
            break
        n_frame += 1
        ret, buffer = cv2.imencode('.jpg', frame, enc_param)
        value = buffer.tobytes()
        f.publish(value)
        if args.preview:
            img = Image.open(io.BytesIO(value))
            img_numpy = numpy.asarray(img)
            img_numpy_bgr = cv2.cvtColor(img_numpy, cv2.COLOR_RGBA2BGR)
            cv2.imshow(args.input_video, img_numpy_bgr)
            key = cv2.waitKey(1)  # Flush
            if key & 0xFF == ord("q"):
                break

video.release()

print(f"Fin video {args.input_video} (#frame={n_frame})")
sys.exit(0)
