<!--
Copyright (C) 2019 National Institute of Informatics

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

# SINETStream

## Files

* `README.md`           ... this
* `python/`             ... SINETStream for Python
    * `README.md`       ... Python API specification
    * `src/`            ... API of SINETStream
    * `tests/`          ... unit tests
    * `sample/`         ... sample programs
    * `plugins/`
        * `kafka/`      ... SINETStream using Kafka
            * `src/`
            * `tests/`  ... unit tests
* `java/`               ... SINETStream for Java
    * `api/`            ... API of SINETStream for Java
    * `plugin-kafka/`   ... SINETStream using Kafka
    * `plugin-mqtt/`    ... SINETStream using MQTT
    * `sample/`         ... sample programs
    * `USERGUIDE.md`    ... Java API User Guide
* `docs/`               ... for mkdocs


## Install

1. Setup Kafka Server
    1. [Kafka Quickstart](https://kafka.apache.org/quickstart)
1. Install SINETStream
    1. `pip install --user https://github.com/nii-gakunin-cloud/sinetstream/releases/download/v0.9/sinetstream_kafka-0.9.5-py3-none-any.whl https://github.com/nii-gakunin-cloud/sinetstream/releases/download/v0.9/sinetstream-0.9.5-py3-none-any.whl`
1. Create a config file for SINETStream at the home directory like this:
    ```
    service-1:
      type: kafka
        brokers:
            - kafka_server_name:9092
    ```
   Rewrite `kafka_server_name:9092` according to your environment (eg `localhost:9092`)
1. Test
    1. Dowonload source from https://github.com/nii-gakunin-cloud/sinetstream/archive/v0.9.tar.gz
    1. Extract: `tar xzf v0.9.tar.gz`
    1. `cd sinetstream-0.9/python/sample/text`
    1. Run Consumer like this: `python consumer.py  -s service-1 -t hoge`
    1. Run Producer like this: `python producer.py  -s service-1 -t hoge`

## License

SINETStream is licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

For additional information, see the LICENSE.
