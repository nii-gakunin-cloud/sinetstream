#!/usr/local/bin/python3.6
# vim: expandtab shiftwidth=4

# Copyright (C) 2021 National Institute of Informatics
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
import pathlib
import yaml
import os
import sys

import pytest
from conftest import qclear

from sinetstream import MessageReader, MessageWriter

logging.basicConfig(level=logging.ERROR)
pytestmark = pytest.mark.usefixtures('setup_config', 'dummy_reader_plugin', 'dummy_writer_plugin')

# tests_dir = pathlib.Path.cwd().joinpath("tests").absolute()
data_dir = pathlib.Path.cwd().joinpath("tests", "data").absolute()

msg_list = [
   b"abc",
   b"",
   b"a",
   b"abcdefghijklmnopqrstuvwxyz"*10,
]

service_list = None


def show(s):
    print(s, file=sys.stderr)


def init_config():
    with open(data_dir.joinpath("dot.sinetstream_config.yml"), "r") as f1,\
         open(".sinetstream_config.yml", "w") as f2:
        conf = yaml.safe_load(f1)
        for k, v in list(conf.items()):
            x = v.get("TEST")
            if x is not None:
                del v["TEST"]
                if x == "ONLY":
                    conf = {k: v}
                    break
                if x == "SKIP":
                    del conf[k]
        yaml.safe_dump(conf, f2)

        global service_list
        service_list = conf.keys()


def drain():
    try:
        os.mkdir(data_dir.joinpath("python"))
    except FileExistsError:
        pass

    for service in service_list:
        show(f"drain {service}")
        with MessageWriter(service) as writer:
            writer.debug_last_msg_bytes = True
            for i, msg in enumerate(msg_list, 1):
                result = None
                comment = ""
                try:
                    writer.publish(msg)
                    result = "OK"
                except Exception as e:
                    show(str(e))
                    result = "NG"
                    comment = "#" + str(e)
                show(f"TEST:RESULT: python encode {service} {i} {result} {comment}")
                with open(data_dir.joinpath("python", f"{service}-{i}.in"), "wb") as fin:
                    fin.write(msg)
                with open(data_dir.joinpath("python", f"{service}-{i}.out"), "wb") as fout:
                    fout.write(writer.debug_last_msg_bytes)


def sink():
    def seq():
        i = 1
        while True:
            yield i
            i += 1
    for subdir in os.listdir(data_dir):
        if not os.path.isdir(data_dir.joinpath(subdir)):
            continue
        for service in service_list:
            show(f"TEST: {subdir}/{service}")
            with MessageReader(service) as reader:
                try:
                    for i in seq():
                        with open(data_dir.joinpath(subdir, f"{service}-{i}.in"), "rb") as fin,\
                             open(data_dir.joinpath(subdir, f"{service}-{i}.out"), "rb") as fout:
                            expected = fin.read()
                            ciphertext = fout.read()
                            reader.debug_inject_msg_bytes = (ciphertext, "test-topic", None)
                            result = None
                            comment = ""
                            try:
                                msg = next(reader)
                                if msg.value == expected:
                                    result = "OK"
                                else:
                                    result = "NG"
                                    comment = "#MALFORMED"
                            except Exception as e:
                                show(str(e))
                                result = "NG"
                                comment = "#" + str(e)
                            show(f"TEST:RESULT: {subdir} python {service} {i} {result} {comment}")
                except FileNotFoundError:
                    pass


@pytest.mark.timeout(1800)
def test_writer():
    init_config()
    drain()
    qclear()  # ねんのために
    sink()
