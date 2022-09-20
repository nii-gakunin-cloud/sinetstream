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

import logging

import pytest
from conftest import SERVICE

from sinetstream import MessageReader, MessageWriter, TEXT, InvalidArgumentError

logging.basicConfig(level=logging.ERROR)
pytestmark = pytest.mark.usefixtures('setup_config', 'dummy_reader_plugin', 'dummy_writer_plugin')


tmsgs = ['test message 001'*3,
         'test message 002'*3]  # note: *3 means compressable


@pytest.mark.parametrize('sw', [False, True])
def test_comp_onoff(config_topic, sw):
    with MessageWriter(SERVICE, value_type=TEXT, data_compression=sw) as fw:
        for msg in tmsgs:
            fw.publish(msg)
    with MessageReader(SERVICE, value_type=TEXT, data_compression=sw) as fr:
        for expected, msg in zip(tmsgs, fr):
            assert msg.topic == config_topic
            assert msg.value == expected


@pytest.mark.parametrize('io', [MessageReader, MessageWriter])
@pytest.mark.parametrize('alg', ["hoge", None, True])
def test_invalid_alg(io, alg):
    compression = {
        "algorithm": alg,
    }
    with pytest.raises(InvalidArgumentError):
        with io(SERVICE,
                data_compression=True,
                compression=compression) as _:
            pass


@pytest.mark.parametrize('alg', ["gzip", "zstd"])
def test_comp_alg(config_topic, alg):
    compression = {
        "algorithm": alg,
    }
    with MessageWriter(SERVICE, value_type=TEXT,
                       data_compression=True,
                       compression=compression) as fw:
        for msg in tmsgs:
            fw.publish(msg)
    with MessageReader(SERVICE, value_type=TEXT,
                       data_compression=True,
                       compression=compression) as fr:
        for expected, msg in zip(tmsgs, fr):
            assert msg.topic == config_topic
            assert msg.value == expected


@pytest.mark.parametrize('alg,lvl', [("gzip", -1), ("gzip", 0), ("gzip", 1), ("gzip", 5), ("gzip", 9),
                                     ("zstd", -99), ("zstd", 0), ("zstd", 1), ("zstd", 3), ("zstd", 22)])
def test_comp_alg_level(config_topic, alg, lvl):
    compression = {
        "algorithm": alg,
        "level": lvl,
    }
    with MessageWriter(SERVICE, value_type=TEXT,
                       data_compression=True,
                       compression=compression) as fw:
        for msg in tmsgs:
            fw.publish(msg)
    with MessageReader(SERVICE, value_type=TEXT,
                       data_compression=True,
                       compression=compression) as fr:
        for expected, msg in zip(tmsgs, fr):
            assert msg.topic == config_topic
            assert msg.value == expected


@pytest.mark.parametrize("alg,lvl,ex", [("gzip", -2, ValueError), ("gzip", 99, ValueError),
                                        ("zstd", 23, ValueError),
                                        ])
def test_comp_alg_level_outofrange(config_topic, alg, lvl, ex):
    compression = {
        "algorithm": alg,
        "level": lvl,
    }
    with pytest.raises(ex):
        with MessageWriter(SERVICE, value_type=TEXT,
                           data_compression=True,
                           compression=compression) as fw:
            for msg in tmsgs:
                fw.publish(msg)


@pytest.mark.skip
@pytest.mark.parametrize('lvl', ["1", None, True])
def test_comp_invalid_alg_level(config_topic, lvl):
    alg = "gzip"
    compression = {
        "algorithm": alg,
        "level": lvl,
    }
    with pytest.raises(InvalidArgumentError):
        with MessageWriter(SERVICE, value_type=TEXT,
                           data_compression=True,
                           compression=compression) as fw:
            for msg in tmsgs:
                fw.publish(msg)
    with pytest.raises(InvalidArgumentError):
        with MessageReader(SERVICE, value_type=TEXT,
                           data_compression=True,
                           compression=compression) as fr:
            for expected, msg in zip(tmsgs, fr):
                assert msg.topic == config_topic
                assert msg.value == expected


@pytest.mark.parametrize('sw', [False, True])
def test_comp_metrics(config_topic, sw):
    alg = "gzip"
    compression = {
        "algorithm": alg,
    }
    with MessageWriter(SERVICE, value_type=TEXT,
                       data_compression=sw,
                       compression=compression) as fw:
        for msg in tmsgs:
            fw.publish(msg)
        if sw:
            assert fw.metrics.msg_compression_ratio < 1.0
        else:
            assert fw.metrics.msg_compression_ratio == 1.0
    with MessageReader(SERVICE, value_type=TEXT,
                       data_compression=sw,
                       compression=compression) as fr:
        for expected, msg in zip(tmsgs, fr):
            assert msg.topic == config_topic
            assert msg.value == expected
        if sw:
            assert fr.metrics.msg_compression_ratio < 1.0
        else:
            assert fr.metrics.msg_compression_ratio == 1.0


def test_comp_gzip_zdict(config_topic):
    appdata = '{"sensors":[{"timestamp":"2020-06-22T14:59:39+0900","type":"light","value":3.2502134}]}'
    alg = "gzip"
    compression = {
        "algorithm": alg,
    }
    with MessageWriter(SERVICE, value_type=TEXT,
                       data_compression=True,
                       compression=compression) as fw:
        fw.publish(appdata)
        ratio1w = fw.metrics.msg_compression_ratio
    with MessageReader(SERVICE, value_type=TEXT,
                       data_compression=True,
                       compression=compression) as fr:
        msg = next(iter(fr))
        assert msg.topic == config_topic
        assert msg.value == appdata
        ratio1r = fr.metrics.msg_compression_ratio
    assert ratio1w == ratio1r

    zdict = b'"sensors":[{"timestamp":"type":"value":'
    compression = {
        "algorithm": alg,
        alg: {
            "zdict": zdict,
        },
    }
    with MessageWriter(SERVICE, value_type=TEXT,
                       data_compression=True,
                       compression=compression) as fw:
        fw.publish(appdata)
        ratio2w = fw.metrics.msg_compression_ratio
    with MessageReader(SERVICE, value_type=TEXT,
                       data_compression=True,
                       compression=compression) as fr:
        msg = next(iter(fr))
        assert msg.topic == config_topic
        assert msg.value == appdata
        ratio2r = fr.metrics.msg_compression_ratio
    assert ratio2w == ratio2r
    assert ratio2w < ratio1w
    # print(f"ratio1={ratio1w} ratio2={ratio2w}")


def test_comp_gzip_zdict2(config_topic):
    appdata = '''{
  "device": {
    "publisher": "user1@example.com",
    "sysinfo": {
      "product": "flame",
      "tags": "release-keys",
      "brand": "google",
      "hardware": "flame",
      "host": "abfarm818",
      "board": "flame",
      "device": "flame",
      "android": "10",
      "type": "user",
      "model": "Pixel 4",
      "manufacturer": "Google"
    },
    "location": {
      "latitude": "139.767120",
      "longitude": "35.681236"
    }
  },
  "sensors": [
    {
      "values": [
        -4.1984100341796875,
        1.2285531759262085,
        5.224560737609863
      ],
      "timestamp": "20200521T171528.094+0900",
      "type": "accelerometer",
      "name": "LSM6DSR Accelerometer"
    },
    {
      "values": [
        -2.60479736328125,
        -35.95391082763672,
        -29.803524017333984
      ],
      "timestamp": "20200521T171528.108+0900",
      "type": "magnetic_field",
      "name": "LIS2MDL Magnetometer"
    },
    {
      "values": [
        -4.1984100341796875,
        1.2285531759262085,
        5.224560737609863,
        0,
        0,
        0
      ],
      "timestamp": "20200521T171528.094+0900",
      "type": "accelerometer_uncalibrated",
      "name": "LSM6DSR Accelerometer-Uncalibrated"
    },
    {
      "values": [
        145.53375244140625,
        12.763638496398926
      ],
      "timestamp": "20200521T171528.142+0900",
      "type": "orientation",
      "name": "Orientation Sensor"
    },
    {
      "values": [
        -3.492360830307007,
        -1.3770915269851685,
        0.01677818037569523
      ],
      "timestamp": "20200521T171528.137+0900",
      "type": "gyroscope",
      "name": "LSM6DSR Gyroscope"
    },
    {
      "timestamp": "20200521T171528.142+0900",
      "type": "light",
      "name": "TMD3702V Ambient Light Sensor",
      "value": 67.49675750732422
    },
    {
      "timestamp": "20200521T171528.058+0900",
      "type": "pressure",
      "name": "BMP380 Pressure Sensor",
      "value": 1013.4315185546875
    },
    {
      "timestamp": "20200521T171504.599+0900",
      "type": "proximity",
      "name": "TMD3702V Proximity Sensor (wake-up)",
      "value": 0
    },
    {
      "timestamp": "20200521T171528.142+0900",
      "type": "gravity",
      "name": "Gravity Sensor",
      "value": -2.9964520931243896
    },
    {
      "timestamp": "20200521T171528.142+0900",
      "type": "linear_acceleration",
      "name": "Linear Acceleration Sensor",
      "value": 0.3456904888153076
    },
    {
      "timestamp": "20200521T171528.142+0900",
      "type": "front_camera_light",
      "name": "Front Camera Light",
      "value": 67.49675750732422
    },
    {
      "values": [
        0.11430059373378754,
        0.15043552219867706,
        -0.9326679110527039,
        0.30730298161506653,
        0.5235987901687622
      ],
      "timestamp": "20200521T171528.142+0900",
      "type": "rotation_vector",
      "name": "Rotation Vector Sensor"
    },
    {
      "values": [
        48.078224182128906,
        -21.8509464263916,
        -27.835193634033203,
        50.683021545410156,
        14.102962493896484,
        1.9683303833007812
      ],
      "timestamp": "20200521T171528.108+0900",
      "type": "magnetic_field_uncalibrated",
      "name": "LIS2MDL Magnetometer-Uncalibrated"
    },
    {
      "values": [
        0.030247846618294716,
        0.18630923330783844,
        -0.6789514422416687,
        0.709506094455719
      ],
      "timestamp": "20200521T171528.142+0900",
      "type": "game_rotation_vector",
      "name": "Game Rotation Vector Sensor"
    },
    {
      "timestamp": "20200521T171528.142+0900",
      "type": "combo_light",
      "name": "Combo Light",
      "value": 67.49675750732422
    },
    {
      "values": [
        -3.491966724395752,
        -1.3785470724105835,
        0.014227211475372314,
        0.00039416010258719325,
        -0.0014555280795320868,
        -0.0025509686674922705
      ],
      "timestamp": "20200521T171528.137+0900",
      "type": "gyroscope_uncalibrated",
      "name": "LSM6DSR Gyroscope-Uncalibrated"
    },
    {
      "timestamp": "20200521T171528.142+0900",
      "type": "color",
      "name": "TMD3702V Color Sensor",
      "value": 2564.661865234375
    },
    {
      "timestamp": "20200521T171527.526+0900",
      "type": "rear_light",
      "name": "VD6282 Rear Light",
      "value": 0.017337892204523087
    },
    {
      "values": [
        0.33792978525161743,
        0.0094594806432724,
        -0.8538966774940491,
        0.395694762468338,
        0.5235987901687622
      ],
      "timestamp": "20200521T171528.142+0900",
      "type": "geomagnetic_rotation_vector",
      "name": "Geomagnetic Rotation Vector Sensor"
    },
    {
      "timestamp": "20200521T171526.388+0900",
      "type": "tilt_detector",
      "name": "Tilt Sensor",
      "value": 1
    },
    {
      "timestamp": "20200521T171528.155+0900",
      "type": "gyro_temperature",
      "name": "LSM6DSR Temperature",
      "value": 25.546875
    },
    {
      "timestamp": "20200521T171528.118+0900",
 "type": "pressure_temp",
      "name": "BMP380 Temperature",
      "value": 26.900915145874023
    },
    {
      "timestamp": "20200521T171459.521+0900",
      "type": "device_orientation",
      "name": "Device Orientation",
      "value": 0
    },
    {
      "timestamp": "20200521T171527.988+0900",
      "type": "magnetometer_temp",
      "name": "LIS2MDL Temperature",
      "value": 22.875
    }
  ]
}
'''
    alg = "gzip"
    compression = {
        "algorithm": alg,
    }
    with MessageWriter(SERVICE, value_type=TEXT,
                       data_compression=True,
                       compression=compression) as fw:
        fw.publish(appdata)
        ratio1w = fw.metrics.msg_compression_ratio
        # bytes1 = fw.metrics.msg_compressed_bytes_total
    with MessageReader(SERVICE, value_type=TEXT,
                       data_compression=True,
                       compression=compression) as fr:
        msg = next(iter(fr))
        assert msg.topic == config_topic
        assert msg.value == appdata
        ratio1r = fr.metrics.msg_compression_ratio
    assert ratio1w == ratio1r

    zdict = (b'"android":"board":"brand":"device":"hardware":"host":"latitude":'
             b'"location":"longitude":"manufacturer":"model":"name":"product":'
             b'"publisher":"sensors":"sysinfo":"tags":"timestamp":"type":"value":'
             b'"values":"user","accelerometer","magnetic_field","accelerometer_uncalibrated",'
             b'"orientation","gyroscope","light","pressure","proximity","gravity",'
             b'"linear_acceleration","front_camera_light","rotation_vector",'
             b'"magnetic_field_uncalibrated","game_rotation_vector","combo_light",'
             b'"gyroscope_uncalibrated","color","rear_light","geomagnetic_rotation_vector",'
             b'"tilt_detector","gyro_temperature","pressure_temp","device_orientation",'
             b'"magnetometer_temp",')
    compression = {
        "algorithm": alg,
        alg: {
            "zdict": zdict,
        },
    }
    with MessageWriter(SERVICE, value_type=TEXT,
                       data_compression=True,
                       compression=compression) as fw:
        fw.publish(appdata)
        ratio2w = fw.metrics.msg_compression_ratio
        # bytes2 = fw.metrics.msg_compressed_bytes_total
    with MessageReader(SERVICE, value_type=TEXT,
                       data_compression=True,
                       compression=compression) as fr:
        msg = next(iter(fr))
        assert msg.topic == config_topic
        assert msg.value == appdata
        ratio2r = fr.metrics.msg_compression_ratio
    assert ratio2w == ratio2r
    assert ratio2w < ratio1w
    # print(f"ratio1={ratio1w} ratio2={ratio2w}")
    # print(f"len={len(appdata)} bytes1={bytes1} bytes2={bytes2} {bytes1-bytes2}")
    # print(f"{bytes1/len(appdata)} {bytes2/bytes1}")
