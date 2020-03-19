<!--
Copyright (C) 2020 National Institute of Informatics

Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

[日本語](README.md)

# SINETStream python

## Directory structure

* src/
    * Implementation of SINETStream for Python
* tests/
    * Unit test
* sample/
    * Sample program
* plugins/
    * broker/
        * kafka/
            * Kafka Plugin for SINETStream
        * mqtt
            * MQTT Plugin for SINETStream
    * value_type/
        * image/
            * Plugin for handling image as message
* README.md

## Build procedure

Execute the following command to build the SINETStream TAR file.

```
$ python3 setup.py bdist_wheel
$ cd plugins/broker/kafka
$ python3 setup.py bdist_wheel
$ cd ../mqtt
$ python3 setup.py bdist_wheel
$ cd ../../value_type/image
$ python3 setup.py bdist_wheel
```

If the build is successful, the following WHL file will be created.

```
./dist/sinetstream-1.1.0-py3-none-any.whl
./plugins/broker/kafka/dist/sinetstream_kafka-1.1.0-py3-none-any.whl
./plugins/broker/mqtt/dist/sinetstream_mqtt-1.1.0-py3-none-any.whl
./plugins/value_type/image/dist/sinetstream_type_image-1.1.0-py3-none-any.whl
```

## Install

You can also use the packages registered in pypi.

```
pip3 install --user sinetstream-kafka sinetstream-mqtt
```

If you want to handle image as message, please install the image plugin as follows.

```
pip3 install --user sinetstream-type-image
```

## Dependent libraries

* [kafka-python](https://kafka-python.readthedocs.io/en/master/)
* [mqtt client](https://www.eclipse.org/paho/clients/python/docs/)
