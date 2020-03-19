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

# SINETStream java

## Directory structure

* api/
    * Libraries providing for `SINETStream` API
* plugin-kafka/
    * Apache Kafka plugin for `SINETStream`
* plugin-mqtt/
    * MQTT (Eclipse Paho) plugin for `SINETStream`
* plugin-type-image/
    * Plugin for handling image as message
* build.gradle
    * Configuration file of Gradle (build tool)
* settings.gralde
    * Configuration file of Gradle (build tool)
* gradlew
    * Wrapper script for running gradle
* gradlew.bat
    * Wrapper script for running gradle (windows)
* gradle/
    * JAR files used by gradle wrapper
* sample/
    * Sample program
* README.md

## Build procedure

Execute the following command to build the SINETStream JAR file.

```
$ ./gradlew assemble

BUILD SUCCESSFUL in 2s
9 actionable tasks: 9 executed
```

If the build is successful,
`BUILD SUCCESSFUL` message will be output to your terminal and
the following JAR file will be created.

```
./api/build/libs/SINETStream-api-1.1.0.jar
./plugin-kafka/build/libs/SINETStream-kafka-1.1.0.jar
./plugin-mqtt/build/libs/SINETStream-mqtt-1.1.0.jar
./plugin-type-image/build/libs/SINETStream-type-image-1.1.0.jar
```
