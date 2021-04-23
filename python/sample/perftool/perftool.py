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
from os import urandom
from sys import stdout
import csv
import json
import threading

from sinetstream import (
    MessageReader, MessageWriter,
    AsyncMessageReader, AsyncMessageWriter,
    BYTE_ARRAY)


def measure(service,
            format,
            output_count,
            num_samples,
            payload_size,
            async_api):
    if async_api:
        results = do_async(service, output_count, num_samples, payload_size)
    else:
        results = do_sync(service, output_count, num_samples, payload_size)

    output(format, results)


def do_async(service, output_count, num_samples, payload_size):
    def on_message(message):
        pass

    results = []
    for i in range(output_count):
        with AsyncMessageReader(service, value_type=BYTE_ARRAY) as reader:
            reader.on_message = on_message

            with AsyncMessageWriter(service,  value_type=BYTE_ARRAY) as writer:
                for i in range(num_samples):
                    message = get_message(payload_size)
                    writer.publish(message)

                results.append({
                        'writer': writer.metrics,
                        'reader': reader.metrics,
                        })

    return results


def do_sync(service, output_count, num_samples, payload_size):
    reader = None

    def consumer(service, event):
        nonlocal reader
        with MessageReader(service, value_type=BYTE_ARRAY) as reader_thread:
            reader = reader_thread
            event.set()

            for message in reader_thread:
                pass

    event = threading.Event()  # for waiting to create consumer
    # run consumer on thread
    thread = threading.Thread(target=consumer, args=(service, event))
    thread.daemon = True
    thread.start()

    results = []
    with MessageWriter(service, value_type=BYTE_ARRAY) as writer:
        event.wait()  # wait to create reader
        for i_output in range(output_count):
            writer.reset_metrics()
            reader.reset_metrics()
            for i_samples in range(num_samples):
                message = get_message(payload_size)
                writer.publish(message)

            results.append({
                    'writer': writer.metrics,
                    'reader': reader.metrics,
                    })

    return results


def get_message(payload_size):
    return urandom(payload_size)


def output(format, results):
    if 'tsv' == format:
        output_tsv(results)
    else:
        output_json(results)


def output_tsv(results):
    csv_writer = csv.writer(stdout, delimiter='\t')
    header = get_tsv_header()
    csv_writer.writerow(header)

    for metrics in results:
        producer_metrics = SafeMetrics(metrics['writer'])
        consumer_metrics = SafeMetrics(metrics['reader'])
        output = [
            producer_metrics.start_time,
            producer_metrics.start_time_ms,
            producer_metrics.end_time,
            producer_metrics.end_time_ms,
            producer_metrics.time,
            producer_metrics.time_ms,
            producer_metrics.msg_count_total,
            producer_metrics.msg_count_rate,
            producer_metrics.msg_bytes_total,
            producer_metrics.msg_bytes_rate,
            producer_metrics.msg_size_min,
            producer_metrics.msg_size_max,
            producer_metrics.msg_size_avg,
            producer_metrics.error_count_total,
            producer_metrics.error_count_rate,
            consumer_metrics.start_time,
            consumer_metrics.start_time_ms,
            consumer_metrics.end_time,
            consumer_metrics.end_time_ms,
            consumer_metrics.time,
            consumer_metrics.time_ms,
            consumer_metrics.msg_count_total,
            consumer_metrics.msg_count_rate,
            consumer_metrics.msg_bytes_total,
            consumer_metrics.msg_bytes_rate,
            consumer_metrics.msg_size_min,
            consumer_metrics.msg_size_max,
            consumer_metrics.msg_size_avg,
            consumer_metrics.error_count_total,
            consumer_metrics.error_count_rate,
            ]
        csv_writer.writerow(output)


def get_tsv_header():
    header = [
        'writer_start_time',
        'writer_start_time_ms',
        'writer_end_time',
        'writer_end_time_ms',
        'writer_time',
        'writer_time_ms',
        'writer_msg_count_total',
        'writer_msg_count_rate',
        'writer_msg_bytes_total',
        'writer_msg_bytes_rate',
        'writer_msg_size_min',
        'writer_msg_size_max',
        'writer_msg_size_avg',
        'writer_error_count_total',
        'writer_error_count_rate',
        'reader_start_time',
        'reader_start_time_ms',
        'reader_end_time',
        'reader_end_time_ms',
        'reader_time',
        'reader_time_ms',
        'reader_msg_count_total',
        'reader_msg_count_rate',
        'reader_msg_bytes_total',
        'reader_msg_bytes_rate',
        'reader_msg_size_min',
        'reader_msg_size_max',
        'reader_msg_size_avg',
        'reader_error_count_total',
        'reader_error_count_rate',
        ]
    return header


def output_json(results):
    for metrics in results:
        output = {
            'writer': get_properties(SafeMetrics(metrics['writer'])),
            'reader': get_properties(SafeMetrics(metrics['reader'])),
            }
        print(json.dumps(output))


def get_properties(metrics):
    properties = {
        'start_time': metrics.start_time,
        'start_time_ms': metrics.start_time_ms,
        'end_time': metrics.end_time,
        'end_time_ms': metrics.end_time_ms,
        'time': metrics.time,
        'time_ms': metrics.time_ms,
        'msg_count_total': metrics.msg_count_total,
        'msg_count_rate': metrics.msg_count_rate,
        'msg_bytes_total': metrics.msg_bytes_total,
        'msg_bytes_rate': metrics.msg_bytes_rate,
        'msg_size_min': metrics.msg_size_min,
        'msg_size_max': metrics.msg_size_max,
        'msg_size_avg': metrics.msg_size_avg,
        'error_count_total': metrics.error_count_total,
        'error_count_rate': metrics.error_count_rate,
    }
    return properties


# workaround for "division by zero" when msg_count_total = 0
class SafeMetrics(object):
    metrics_org = None
    hasCount = True

    def __init__(self, metrics):
        self.metrics_org = metrics
        self.hasCount = (0 < metrics.msg_count_total)

    @property
    def start_time(self):
        return self.metrics_org.start_time

    @property
    def end_time(self):
        return self.metrics_org.end_time

    @property
    def msg_count_total(self):
        return self.metrics_org.msg_count_total

    @property
    def msg_bytes_total(self):
        return self.metrics_org.msg_bytes_total

    @property
    def msg_size_min(self):
        return self.metrics_org.msg_size_min

    @property
    def msg_size_max(self):
        return self.metrics_org.msg_size_max

    @property
    def error_count_total(self):
        return self.metrics_org.error_count_total

    @property
    def start_time_ms(self):
        return self.metrics_org.start_time_ms

    @property
    def end_time_ms(self):
        return self.metrics_org.end_time_ms

    @property
    def time(self):
        return self.metrics_org.time

    @property
    def time_ms(self):
        return self.metrics_org.time_ms

    @property
    def msg_count_rate(self):
        return self.metrics_org.msg_count_rate if self.hasCount else 0

    @property
    def msg_bytes_rate(self):
        return self.metrics_org.msg_bytes_rate if self.hasCount else 0

    @property
    def msg_size_avg(self):
        return self.metrics_org.msg_size_avg if self.hasCount else 0

    @property
    def error_count_rate(self):
        return self.metrics_org.error_count_rate if self.hasCount else 0


if __name__ == '__main__':
    parser = ArgumentParser(description="SINETStream metrics tool")
    parser.add_argument(
        "-s",
        "--service",
        metavar="SERVICE_NAME",
        required=True)
    parser.add_argument(
        "-f",
        "--format",
        metavar="FORMAT",
        choices=['json', 'tsv'],
        default='json',
        required=False)
    parser.add_argument(
        "-c",
        "--output-count",
        metavar="OUTPUT_COUNT",
        type=int,
        default=1,
        required=False)
    parser.add_argument(
        "-n",
        "--num-samples",
        metavar="NUM_SAMPLES",
        type=int,
        default=300,
        required=False)
    parser.add_argument(
        "-p",
        "--payload-size",
        metavar="PAYLOAD_SIZE",
        type=int,
        default=1024,
        required=False)
    parser.add_argument(
        "-a",
        "--async-api",
        action="store_true",
        required=False)
    args = parser.parse_args()

    try:
        measure(args.service,
                args.format,
                args.output_count,
                args.num_samples,
                args.payload_size,
                args.async_api)
    except KeyboardInterrupt:
        pass
