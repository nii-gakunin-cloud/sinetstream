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

from argparse import ArgumentParser
from datetime import datetime
from sys import stderr

from sinetstream import MessageReader


def consumer(service):
    with MessageReader(service) as reader:
        for message in reader:
            show_message(message)


def show_message(message):
    ts = datetime.fromtimestamp(message.timestamp)
    print(f"[{ts}] topic={message.topic} value='{message.value}'")


if __name__ == '__main__':
    parser = ArgumentParser(description="SINETStream Consumer")
    parser.add_argument(
        "-s", "--service", metavar="SERVICE_NAME", required=True)
    args = parser.parse_args()

    print("Press ctrl-c to exit the program.", file=stderr)
    print(f": service={args.service}", file=stderr)

    try:
        consumer(args.service)
    except KeyboardInterrupt:
        pass
