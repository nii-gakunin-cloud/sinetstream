#!/usr/bin/env python3

# Copyright (C) 2022 National Institute of Informatics
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

from logging import basicConfig, getLogger, INFO
basicConfig(level=INFO)
logger = getLogger(__name__)


def make_temp_config():
    import pathlib
    import atexit
    service = "tempserv"
    path = pathlib.Path(".sinetstream_config.yml")
    emptymap = "{}"
    config = f"""\
header:
  version: 2
config:
  {service}: {emptymap}
"""
    try:
        with path.open(mode="x") as f:
            atexit.register(path.unlink)
            f.write(config)
        logger.info(f"the temporary config file {path} created")
    except FileExistsError:
        return None
    return service


sep1 = "."
sep2 = "="


def make_parser(argv0, cmd):
    from argparse import ArgumentParser
    parser = ArgumentParser(
        prog=f"{argv0} {cmd}",
        description=f"SINETStream CLI: {cmd}",
        )
    parser.add_argument(
        "--service",
        metavar="SERVICE",
        required=False,
        help="specify the service name")
    parser.add_argument(
        "--config",
        metavar="CONFIG",
        required=False,
        help="specify the config name when using config service")
    parser.add_argument(
        "--text",
        action="store_true",
        required=False,
        help="text mode")
    parser.add_argument(
        "parameters",
        metavar=f"KEY{sep2}VALUE",
        type=str,
        nargs="*",
        help=("parameter for SINETStream "
              f"(ex: brokers{sep2}mqtt.example.net compression{sep1}algorithm{sep2}gzip)"))
    return parser


def setdict(d, ks, v):
    k = ks[0]
    kss = ks[1:]
    if len(kss) == 0:
        d[k] = v
    else:
        d[k] = setdict(d.get(k, {}), kss, v)
    return d


def build_kwargs(args):
    from yaml import load, SafeLoader
    kwargs = {}

    if args.config is None:
        if args.service is None:
            args.service = make_temp_config()
    else:
        kwargs["config"] = args.config

    for p in args.parameters:
        if sep2 not in p:
            logger.error(f"invalid parameter: {p}")
            exit(1)
        key, val = p.split(sep2, 1)
        setdict(kwargs, key.split(sep1), load(val, Loader=SafeLoader))

    return kwargs


def cmd_write(argv0, argv):
    from sinetstream import MessageWriter

    parser = make_parser(argv0, "write")
    parser.add_argument(
        "--file",
        metavar="INPUT",
        type=str,
        required=False,
        help="write the contents of a file as the message")
    parser.add_argument(
        "--message",
        metavar="MESSAGE",
        type=str,
        required=False,
        help="write a single message from the command line")
    parser.add_argument(
        "--line",
        action="store_true",
        required=False,
        help="split separate lines into separate messages")
    args = parser.parse_args(argv)

    kwargs = build_kwargs(args)

    if args.text or args.message or args.line:
        setdict(kwargs, ["value_type"], "text")
    textmode = kwargs.get("value_type", "byte_array") == "text"

    if args.message is not None:
        def get_message():
            yield args.message
    else:
        if args.file is not None:
            fp = open(args.file, mode=("r" if textmode else "rb"))
        else:
            from sys import stdin
            fp = stdin if textmode else stdin.buffer
        if args.line:
            def get_message():
                while True:
                    ln = fp.readline()
                    if len(ln) == 0:
                        return
                    yield ln.rstrip("\r\n")
        else:
            def get_message():
                while True:
                    m = fp.read()
                    if len(m) == 0:
                        return
                    yield m

    try:
        with MessageWriter(args.service, **kwargs) as writer:
            for msg in get_message():
                writer.publish(msg)
    except KeyboardInterrupt:
        pass
    exit()


def cmd_read(argv0, argv):
    # from datetime import datetime
    from sinetstream import MessageReader

    parser = make_parser(argv0, "read")
    parser.add_argument(
        "--verbose",
        dest="ofmt",
        default="verbose",
        action="store_const",
        const="verbose",
        required=False,
        help="print received messages verbosely")
    parser.add_argument(
        "--raw",
        dest="ofmt",
        action="store_const",
        const="raw",
        required=False,
        help="print just received messages")
    parser.add_argument(
        "--file",
        metavar="DIR",
        required=False,
        type=str,
        help="save received messages under the specified directory")
    parser.add_argument(
        "--count",
        metavar="N",
        required=False,
        type=int,
        default=0,
        help="exit after the given count of messages have been received")
    args = parser.parse_args(argv)

    kwargs = build_kwargs(args)

    if args.text:
        setdict(kwargs, ["value_type"], "text")
    textmode = kwargs.get("value_type", "byte_array") == "text"

    if args.file:
        from pathlib import Path
        if not Path(args.file).is_dir():
            logger.error(f"No such directory: {args.file}")
            exit(1)
        import random
        import string
        rand = ''.join(random.choices(string.ascii_letters + string.digits, k=16))
        from urllib.parse import quote

    try:
        from sys import stdout
        with MessageReader(args.service, **kwargs) as reader:
            n = 0
            for msg in reader:
                n += 1
                if args.file:
                    opath = Path(args.file, f"{quote(msg.topic, safe='')}-{rand}-{n}")
                    with open(opath, mode="w" if textmode else "wb") as f:
                        f.write(msg.value)
                else:
                    if args.ofmt == "verbose":
                        # ts = datetime.fromtimestamp(msg.timestamp)
                        # output like "nats sub"
                        stdout.write(f'[#{n}] Received on "{msg.topic}"\n')
                    if textmode:
                        stdout.write(msg.value)
                        if msg.value[-1] != "\n":
                            stdout.write("\n")  # like nats sub
                    else:
                        stdout.buffer.write(msg.value)
                    stdout.flush()
                if args.count and n >= args.count:
                    break
    except KeyboardInterrupt:
        pass
    exit()


def main():
    from sys import argv
    global logger
    logger = getLogger(argv[0])
    cmd = argv[1] if len(argv) >= 2 else None
    if cmd in ["write", "pub"]:
        cmd_write(argv[0], argv[2:])
    elif cmd in ["read", "sub"]:
        cmd_read(argv[0], argv[2:])
    else:
        if cmd is not None:
            logger.error(f"invalid cmd: {cmd}")
        logger.info(f"usage: {argv[0]} write|read ...")
        exit(1)


if __name__ == "__main__":
    main()
