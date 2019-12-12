**準備中** (2019-12-12 12:59:06 JST)

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

# SINETStream python

## ファイル構成

* src/
    * Python版SINETStreamの実装
* tests/
    * ユニットテスト
* sample/
    * サンプルプログラム
* plugins/
    * kafka/
        * Python版SINETStreamのKafka固有部分
    * mqtt
        * Python版SINETStreamのMQTT固有部分
* README.md

## ビルド手順

次のコマンドを実行するとSINETStreamのTARファイルがビルドされる。

```
$ python3 setup.py bdist_wheel
$ cd plugins/kafka
$ python3 setup.py bdist_wheel
$ cd ../mqtt
$ python3 setup.py bdist_wheel
$ cd ../..
```

ビルドが成功すると以下のWHLファイルが作成される。

```
./dist/sinetstream-1.0.0-py3-none-any.whl
./plugins/kafka/dist/sinetstream_kafka-1.0.0-py3-none-any.whl
./plugins/mqtt/dist/sinetstream_mqtt-1.0.0-py3-none-any.whl
```

## インストール

pypiに登録してあるパッケージを利用することもできる。

```
pip3 install --user sinetstream-kafka sinetstream-mqtt
```

## 依存関係にあるライブラリ

* [kafka-python](https://kafka-python.readthedocs.io/en/master/)
* [mqtt client](https://www.eclipse.org/paho/clients/python/docs/)

