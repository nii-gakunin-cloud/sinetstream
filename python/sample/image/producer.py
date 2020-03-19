#!/usr/bin/env python3

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
from argparse import ArgumentParser
from sys import exit

from cv2 import VideoCapture, imshow, waitKey
from sinetstream import MessageWriter

logging.basicConfig(level=logging.INFO)


def producer(service, video, preview=False):
    with MessageWriter(service, value_type='image') as writer:
        image = next_frame(video)
        while image is not None:
            writer.publish(image)

            if preview and show_preview(image):
                break
            image = next_frame(video)


def next_frame(video):
    global n_frame
    if not video.isOpened():
        return None
    success, frame = video.read()
    n_frame += 1
    return frame if success else None


def show_preview(image):
    imshow(args.input_video, image)

    # Hit 'q' to stop
    return waitKey(25) & 0xFF == ord("q")


def main(service, video_file, preview=False):
    global n_frame
    video = VideoCapture(video_file)
    if not video.isOpened():
        print(f"ERROR: cannot open the file {video_file}")
        exit(1)
    n_frame = 0
    try:
        producer(service, video, preview)
    finally:
        video.release()
        print(f"Fin video {args.input_video} (#frame={n_frame})")


if __name__ == '__main__':

    parser = ArgumentParser(description="SINETStream Producer")
    parser.add_argument(
        "-s", "--service", metavar="SERVICE_NAME", required=True)
    parser.add_argument(
        "-f", "--input-video", metavar="FILE", required=True)
    parser.add_argument(
        "-l", "--preview", action="store_true", help="show on local too")
    args = parser.parse_args()

    print(f": service={args.service}")
    print(f": input-video={args.input_video}")
    if args.preview:
        print("Hit 'q' to stop")

    main(args.service, args.input_video, args.preview)
