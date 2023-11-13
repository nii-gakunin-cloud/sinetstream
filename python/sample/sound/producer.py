#!/usr/bin/env python

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

from argparse import ArgumentParser
from queue import Queue

import sounddevice as sd
from sinetstream import MessageWriter

q = Queue()


def callback(indata, frames, time, status):
    if status:
        print(status)
    q.put(indata.copy())


def producer(params):
    with (
        sd.InputStream(
            samplerate=params.samplerate,
            channels=params.channels,
            blocksize=params.blocksize if params.blocksize > 0 else None,
            device=params.device,
            callback=callback,
        ),
        MessageWriter(params.service) as writer,
    ):
        while True:
            message = q.get()
            writer.publish(message)


def int_or_str(text):
    try:
        return int(text)
    except ValueError:
        return text


def get_parser():
    parser = ArgumentParser(description="sound consumer")
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("-s", "--service", type=str, help="service name")
    group.add_argument("-c", "--config", type=str, help="config name")
    parser.add_argument(
        "-d", "--device", type=int_or_str, help="input device (numeric ID or substring)"
    )
    parser.add_argument(
        "-r",
        "--samplerate",
        type=int,
        default=44100,
        help="sample rate (default: 44100)",
    )
    parser.add_argument(
        "-b",
        "--blocksize",
        type=int,
        default=0,
        help="block size (default: 0)",
    )
    parser.add_argument(
        "-C", "--channels", type=int, default=1, help="number of channels (default: 1)"
    )
    parser.add_argument("--list-device", action="store_true", help="show device list")

    return parser


def show_device_list(args, parser):
    if args.list_device:
        print(sd.query_devices())
        parser.exit()


if __name__ == "__main__":
    parser = get_parser()
    args = parser.parse_args()
    show_device_list(args, parser)
    try:
        producer(args)
    except KeyboardInterrupt:
        pass
