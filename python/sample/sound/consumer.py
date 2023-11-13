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

from sinetstream import MessageReader
from soundfile import SoundFile, available_formats, available_subtypes


def save(params):
    with (
        SoundFile(
            file=params.file,
            mode="x" if not params.force else "w",
            samplerate=params.samplerate,
            channels=params.channels,
            format=params.format,
            subtype=params.subtype,
        ) as f,
        MessageReader(service=params.service, config=params.config) as reader,
    ):
        try:
            for message in reader:
                f.write(message.value)
        except KeyboardInterrupt:
            print(f"\nrecording finished: {args.file}")


def get_parser():
    parser = ArgumentParser(description="sound consumer")
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("-s", "--service", type=str, help="service name")
    group.add_argument("-c", "--config", type=str, help="config name")
    parser.add_argument("-f", "--file", type=str, help="file name", required=True)
    parser.add_argument("--force", action="store_true", help="overwrite existing file")
    parser.add_argument(
        "-r",
        "--samplerate",
        type=int,
        default=44100,
        help="sample rate (default: 44100)",
    )
    parser.add_argument(
        "-C", "--channels", type=int, default=1, help="number of channels (default: 1)"
    )
    parser.add_argument(
        "--format",
        type=str,
        default="FLAC",
        help="file format (default: FLAC)",
    )
    parser.add_argument(
        "--subtype",
        type=str,
        default="PCM_16",
        help="file subtype (default: PCM_16)",
    )
    parser.add_argument("--list-format", action="store_true", help="show file formats")
    parser.add_argument(
        "--list-subtype", action="store_true", help="show file subtypes"
    )
    return parser


def show_list(args, parser):
    if args.list_format:
        for k, v in available_formats().items():
            print(f"{k}: {v}")
        parser.exit(0)
    if args.list_subtype:
        print(f"format: {args.format}")
        print("-" * 20)
        for k, v in available_subtypes(args.format).items():
            print(f"{k}: {v}")
        parser.exit(0)


if __name__ == "__main__":
    parser = get_parser()
    args = parser.parse_args()
    show_list(args, parser)
    save(args)
