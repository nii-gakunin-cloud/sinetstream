**準備中**

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

# SINETStream java

## ファイル構成

* api/
    * `SINETStream` のAPIを提供するライブラリ
* plugin-kafka/
    * `SINETStream` のApache Kafkaプラグイン
* plugin-mqtt/
    * `SINETStream` のMQTT(Eclipse Paho)プラグイン
* build.gradle
    * gradle(ビルドツール)の設定ファイル
* settings.gralde
    * gradle(ビルドツール)の設定ファイル
* gradlew
    * gradleを実行するためのラッパースクリプト
* gradlew.bat
    * gradleを実行するためのラッパースクリプト(windows)
* gradle/
    * gradle wrapperが使用するJARファイルなど
* sample/
    * サンプルプログラム
* README.md

## ビルド手順

次のコマンドを実行するとSINETStreamのJARファイルがビルドされる。

```
$ ./gradlew assemble

BUILD SUCCESSFUL in 2s
9 actionable tasks: 9 executed
```

ビルドが成功すると`BUILD SUCCESSFUL`と表示され、以下のJARファイル
が作成される。

```
./plugin-kafka/build/libs/SINETStream-kafka-1.0.0.jar
./plugin-mqtt/build/libs/SINETStream-mqtt-1.0.0.jar
./api/build/libs/SINETStream-api-1.0.0.jar
```
