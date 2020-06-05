**準備中** (2020-06-05 14:26:44 JST)

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
--->

[日本語](README.md)

## Concept of messaging system

SINETStream is a messaging system that adopts a [topic-based publish/subscribe model](https://en.wikipedia.org/wiki/Publish%E2%80%93subscribe_pattern).
It provides an abstraction layer of Broker, which in turn employs Kafka or MQTT as its backend.

In SINETStream, the publisher is called Writer, and the subscriber is called Reader.

![Conceptual diagram of the messaging system](docs/images/overview.png)

Broker's configuration and communication parameters are abstracted as "service" in SINETStream.
Writers and Readers can communicate with any Broker just by specifying a "service".

A logical channel in Broker is called a topic.
Writers and Readers send/receive a message specifying a topic, allowing different types of messages to be transferred through a Broker.

## Directory structure

* README.en.md
    * [README.en.md](README.en.md)
* python/
    * [README.en.md](python/README.en.md)
        * Build procedure of the Python version of SINETStream
    * src/
        * Common files of the Python version of SINETStream
    * plugins/
        * broker/
            * kafka/
                * Kafka-specific files of the Python version of SINETStream
            * mqtt/
                * MQTT-specific files of the Python version of SINETStream
        * value_type/
            * image/
                * Support for messages with image payload
    * sample/
        * Sample programs
* java/
    * [README.en.md](java/README.en.md)
        * Build procedure of the Java version of SINETStream
    * api/
        * Common files of the Java version of SINETStream
    * plugin-kafka/
        * Kafka-specific files of the Java version of SINETStream
    * plugin-mqtt/
        * MQTT-specific files of the Java version of SINETStream
    * plugin-type-image/
        * Support for messages with image payload
    * sample/
        * Sample programs
* docs/
    * userguide/
        * [User guide](docs/userguide/index.en.md)
    * tutorial/
        * [Tutorial](docs/tutorial/index.en.md)
    * developer_guide/
        * [Developer guide](https://translate.google.com/translate?hl=en&sl=ja&tl=en&u=https://nii-gakunin-cloud.github.io/sinetstream/docs/developer_guide/index.html)

## Operating environment

The SINETStream API supports the following languages.

* Python 3.6
* Java 8

The SINETStream API supports the following messaging systems.

* [Apache Kafka](https://kafka.apache.org/) 2.2.1
* MQTT v3.1, v3.1.1
    * [Eclipse Mosquitto](https://mosquitto.org/) v1.6.2

The operating environment of SINETStream is as follows:

* CentOS 7.6
* Windows 10

## Preparation

SINETStream uses Kafka or MQTT as a backend messaging system of Broker.
Therefore, you need to install one of these messaging systems along with SINETStream.
The tutorial package utilizes a Docker container to install the required software components,
i.e., SINETStream, Kafka, and MQTT.

1. Kafka broker settings
    * [Kafka Quickstart](https://kafka.apache.org/quickstart)
1. MQTT broker settings
    * [Eclipse Mosquitto: Installing](https://github.com/eclipse/mosquitto#installing)
    * [Eclipse Mosquitto: Quick start](https://github.com/eclipse/mosquitto#quick-start)
1. Installing SINETStream
    * Python: `pip3 install --user sinetstream-kafka sinetstream-mqtt`
    * Java: Please refer to the Java version of README.

Please Refer to the [tutorial](docs/tutorial/index.en.md) using docker container.

## Links

* [Tutorial](docs/tutorial/index.en.md)
* [User Guide](docs/userguide/index.en.md)
* [Performance measurement of SINETStream](docs/performance/index.en.md)
* [Update log](CHANGELOG.md)

## License

[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
