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

import boto3

from datetime import datetime
import uuid
import re

import logging
from concurrent.futures.thread import ThreadPoolExecutor
from promise import Promise

from sinetstream import (
    InvalidConfigError,
    InvalidArgumentError)
from sinetstream.spi import PluginMessage
from sinetstream.api import MessageIO

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)

# For debug
# import botocore
# botocore.session.Session().set_debug_logger()


class S3Metrics(object):
    def __int__(self):
        pass

    def __str__(self):
        return ""

    def reset(self):
        pass


class S3Client(object):
    def __init__(self, s3params, s3metrics):
        self._s3params = s3params
        self._s3metrics = s3metrics
        self._s3 = None

    def open(self, timeout=None):
        logger.debug("S3Client:open")
        # https://boto3.amazonaws.com/v1/documentation/api/latest/guide/clients.html
        # https://boto3.amazonaws.com/v1/documentation/api/latest/reference/core/boto3.html
        # https://boto3.amazonaws.com/v1/documentation/api/latest/reference/core/session.html#boto3.session.Session.client
        # https://boto3.amazonaws.com/v1/documentation/api/latest/guide/credentials.html
        param_list = {
            "region_name",
            "api_version"
            "use_ssl",
            "verify",
            "endpoint_url",
            "aws_access_key_id",
            "aws_secret_access_key",
            "aws_session_token",
            "config",
        }
        boto3_client_params = {k: v for k, v in self._s3params.items() if k in param_list}
        self._s3 = boto3.client(service_name="s3", **boto3_client_params)
        # https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/s3.html

    def close(self):
        logger.debug("S3Client:close")
        # no need to close() in S3.
        self._s3 = None

    def _s3_put_obj(self, bucket, path, msg, timestamp):
        logger.debug(f"S3Client:_s3_put_obj:bucket={bucket},path={path}")
        metadata = {"timestamp": str(timestamp)}
        try:
            res = self._s3.put_object(Body=msg, Bucket=bucket, Key=path, Metadata=metadata)
            logger.debug(f"put_object.res={res}")
            assert type(res) is dict
        except Exception:
            raise ConnectionError("put_object failed")

    def _s3_list_obj(self, bucket, prefix, key_filter, continuation_token):
        logger.debug("S3Client:_s3_list_obj:"
                     f"bucket={bucket},prefix={prefix},key_filter={key_filter}")
        max_keys = 100
        # max_keys = 1  # for DEBUG
        if continuation_token is not None:
            res = self._s3.list_objects_v2(Bucket=bucket,
                                           MaxKeys=max_keys,
                                           Prefix=prefix,
                                           ContinuationToken=continuation_token)
        else:
            res = self._s3.list_objects_v2(Bucket=bucket,
                                           MaxKeys=max_keys,
                                           Prefix=prefix)
        logger.debug(f"list_objects_v2.res={res}")
        contents = res["Contents"] if int(res["KeyCount"]) > 0 else []
        key_list = [key for key in [content["Key"] for content in contents]
                    if key_filter(key)]
        next_continucation_token = res["NextContinuationToken"] if res["IsTruncated"] else None
        return (key_list, next_continucation_token)

    def _s3_get_obj(self, bucket, key):
        logger.debug("S3Client:_s3_get_obj")
        res = self._s3.get_object(Bucket=bucket, Key=key)
        logger.debug(f"get_object.res={res}")
        data = res["Body"].read()
        metadata = res["Metadata"]
        return data, metadata, res

    def metrics(self):
        return self._metrics

    def reset_metrics(self):
        pass


def get_mandatory_param(params, k, pfx):
    if k not in params:
        raise(InvalidArgumentError(f"missing mandatory parameter: {pfx}{k}"))
    return params[k]


class S3WriterMetrics(S3Metrics):
    def __init__(self):
        super().__init__()

    def __str__(self):
        return super().__str__()

    def reset(self):
        super().reset()


class BaseS3Writer(S3Client):
    def day_format(dt):
        return f"{dt.year}/{dt.month:02}/{dt.day:02}"

    def hour_format(dt):
        return f"{dt.year}/{dt.month:02}/{dt.day:02}/{dt.hour:02}"

    def minute_format(dt):
        return f"{dt.year}/{dt.month:02}/{dt.day:02}/{dt.hour:02}/{dt.minute:02}"

    _formats = {
        "day": day_format,
        "hour": hour_format,
        "minute": minute_format,
    }

    def __init__(self, params):
        logger.debug("BaseS3Writer:init")
        if "brokers" in params:
            raise InvalidConfigError("brokers: cannot be specfied; use s3.endpoint_url")
        self._topic = params["topic"]
        self._consistency = params["consistency"]
        self._s3params = get_mandatory_param(params, "s3", "")
        self._bucket = get_mandatory_param(self._s3params, "bucket", "s3.")
        self._prefix = get_mandatory_param(self._s3params, "prefix", "s3.")
        self._suffix = get_mandatory_param(self._s3params, "suffix", "s3.")
        name = get_mandatory_param(self._s3params, "name", "s3.")
        self._format = self._formats.get(name)
        if self._format is None:
            raise InvalidArgumentError(f"s3.name={name} must be {','.join(self._formats.keys())}")
        utc_offset = self._s3params.get("utc_offset")
        try:
            self._timezone = (datetime.strptime(utc_offset, "%z").tzinfo if utc_offset is not None
                              else None)
        except Exception:
            raise InvalidArgumentError(f"s3.utc_offset={utc_offset} is invalid")
        self._uuid = str(uuid.uuid4())
        self._seqno = 0
        self._metrics = S3WriterMetrics()

        super().__init__(self._s3params, self._metrics)

    def _publish(self, msg):
        assert isinstance(msg, bytes)
        assert hasattr(msg, "timestamp")
        logger.debug(f"type(msg)={type(msg)}")
        tstamp = int(msg.timestamp / 1000_000)
        dt = datetime.fromtimestamp(tstamp, self._timezone)
        self._seqno += 1
        path = (f"{self._prefix}/"
                f"{self._topic}/"
                f"{self._format(dt)}/"
                f"{self._topic}-{self._uuid}-{self._seqno}{self._suffix}")
        self._s3_put_obj(self._bucket, path, msg, int(msg.timestamp))

    def metrics(self):
        return self._metrics

    def reset_metrics(self):
        return self._metrics.reset()

    def info(self, ipath, kwargs):
        writer_lst = {
            "uuid": lambda *_: self._uuid,
            "seqno": lambda *_: self._seqno,
        }
        if len(ipath) == 0:
            return {"writer": MessageIO._fill_info(ipath, kwargs, writer_lst)}
        else:
            if ipath[0] == "writer":
                return MessageIO._fill_info(ipath[1:], kwargs, writer_lst)
            else:
                return None


class S3Writer(BaseS3Writer):
    def __init__(self, params):
        logger.debug("S3Writer:init")
        super().__init__(params)

    def publish(self, msg):
        return self._publish(msg)


class S3AsyncWriter(BaseS3Writer):
    def __init__(self, params):
        logger.debug("S3AsyncWriter:init")
        super().__init__(params)

    def publish(self, msg):
        return Promise(lambda resolve, reject: [self._publish(msg), resolve(None)])


class S3ReaderMetrics(S3Metrics):
    pass


class BaseS3Reader(S3Client):
    def make_filter(prefix, topics, name, suffix):
        if type(topics) is str:
            topics = [topics]

        def escape(topic):
            # XXX この動作は仕様どおりか？
            if topic == "*":
                return "[^/]+"
            else:
                return re.escape(topic)

        year = name in ["day", "hour", "minute"]
        month = name in ["day", "hour", "minute"]
        day = name in ["day", "hour", "minute"]
        hour = name in ["hour", "minute"]
        minute = name in ["minute"]

        pat = ("^" +
               prefix + "/" +
               "(?:" + "|".join(
                   escape(topic) + "/" +
                   ("[1-9][0-9][0-9][0-9]/" if year else "") +
                   ("(?:0[1-9]|1[012])/" if month else "") +
                   ("(?:0[1-9]|[12][0-9]|3[01])/" if day else "") +
                   ("(?:[01][0-9]|2[0-3])/" if hour else "") +
                   ("(?:[0-5][0-9])/" if minute else "") +
                   escape(topic) for topic in topics) + ")" +
               "-[^/]+" + suffix +
               "$")
        prog = re.compile(pat)
        return lambda path: re.match(prog, path) is not None

    def __init__(self, params):
        logger.debug("BaseS3Reader:init")
        if "brokers" in params:
            raise InvalidConfigError("brokers: cannot be specfied; use s3.endpoint_url")
        self._topics = params["topics"]
        self._consistency = params["consistency"]
        self._s3params = get_mandatory_param(params, "s3", "")
        self._bucket = get_mandatory_param(self._s3params, "bucket", "s3.")
        self._prefix = get_mandatory_param(self._s3params, "prefix", "s3.")
        self._suffix = get_mandatory_param(self._s3params, "suffix", "s3.")
        name = get_mandatory_param(self._s3params, "name", "s3.")
        names = ["day", "hour", "minute"]
        if name not in names:
            raise InvalidArgumentError(f"s3.name={name} must be {','.join(names)}")
        self._key_filter = BaseS3Reader.make_filter(self._prefix, self._topics, name, self._suffix)

        super().__init__(self._s3params, S3ReaderMetrics())

    def _list_obj(self, token):
        res = self._s3_list_obj(self._bucket,
                                self._prefix,
                                self._key_filter,
                                token)
        key_list, token = res
        return key_list, token

    def _get_obj(self, key):
        logger.debug(f"BaseS3Reader:_get_obj:key={key}")
        data, metadata, raw = self._s3_get_obj(self._bucket, key)
        try:
            tstamp = int(metadata["timestamp"])
        except (KeyError, ValueError):
            tstamp = 0
        payload = PluginMessage(data)
        payload.set_timestamp(tstamp)
        topic = key[len(self._prefix)+1:].split(sep="/", maxsplit=1)[0]
        # note: key = prefix/topic/name.../topic-uuid-seqno.suffix
        return payload, topic, raw

    def info(self, name, kwargs):
        return None


class S3Reader(BaseS3Reader):
    def __init__(self, params):
        logger.debug("S3Reader:init")
        super().__init__(params)

    class Iter(object):
        def __init__(self, s3reader, key_list, token):
            logger.debug("S3Reader.Iter:__init__")
            self._s3reader = s3reader
            self._key_list = key_list
            self._token = token

        def __next__(self):
            logger.debug("S3Reader.Iter:__next__")
            s3reader = self._s3reader
            if len(self._key_list) == 0:
                if self._token is None:
                    raise StopIteration()
                else:
                    self._key_list, self._token = s3reader._list_obj(self._token)
                    return self.__next__()
            key = self._key_list.pop()
            payload, topic, raw = s3reader._get_obj(key)
            return payload, topic, raw

    def __iter__(self):
        logger.debug("S3Reader:__iter__")
        key_list, token = self._list_obj(None)
        return S3Reader.Iter(self, key_list, token)


class S3AsyncReader(BaseS3Reader):
    def __init__(self, params):
        logger.debug("S3AsyncReader:init")
        super().__init__(params)
        self._reader_executor = None
        self._on_message = None
        self._on_failure = None
        self._closed = True
        self._future = None

    def open(self):
        logger.debug("S3AsyncReader:open")
        super().open()
        self._closed = False
        self._reader_executor = ThreadPoolExecutor(max_workers=1)
        # becasue boto3 is sequenctial, one worker is sufficient.
        self._start_reader()

    def _start_reader(self):
        logger.debug("S3AsyncReader:_start_reader")
        if ((self._future is None and
             self._is_set_callback() and
             self._reader_executor is not None
             )):
            logger.debug("S3AsyncReader:_start_reader:submit")
            self._future = self._reader_executor.submit(self._reader_loop)

    def _reader_loop(self):
        logger.debug("S3AsyncReader:_reader_loop:start")
        token = None
        while True:
            logger.debug("_reader_loop:loop")
            if self._closed:
                logger.debug("S3AsyncReader:_reader_loop:closed")
                break
            key_list, token = self._list_obj(token)
            logger.debug(f"_reader_loop:key_list={key_list}")
            while len(key_list) > 0:
                key = key_list.pop()
                payload, topic, raw = self._get_obj(key)
                self._on_message(payload, topic, raw)
            if token is None:
                logger.debug("S3AsyncReader:reader_loop:eof")
                break
        logger.debug("S3AsyncReader:_reader_loop:end")

    def close(self):
        super().close()
        self._closed = True
        if self._reader_executor is not None:
            self._reader_executor.shutdown()
            self._reader_executor = None

    def _is_set_callback(self):
        return not (self._on_message is None and self._on_failure is None)

    @property
    def on_message(self):
        return self._on_message

    @on_message.setter
    def on_message(self, on_message):
        self._on_message = on_message
        self._start_reader()

    @property
    def on_failure(self):
        return self._on_failure

    @on_failure.setter
    def on_failure(self, on_failure):
        self._on_failure = on_failure
        self._start_reader()
