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
-->

# Sample Program using SINETStream

## How to run

* Kafka is installed.
* Install SINETStream.
* Edit ./.sinetstream_config.yml:
    * Rewrite the hostnames according to your environment.
* Run consumer:
    * `python3 ./consumer.py -s service-1 -t topic-1`
* Run producer on another terminal:
    * `python3 ./producer.py -s service-1 -t topic-1`
    * Enter `xxxx`, the consumer will shows `xxxc=topic-1 value=xxxxx`.
