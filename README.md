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
--->

# SINETStream

## メッセージングシステムの概念

SINETStreamは
[トピックベースのPublish/Subscribeモデル](https://ja.wikipedia.org/wiki/%E5%87%BA%E7%89%88-%E8%B3%BC%E8%AA%AD%E5%9E%8B%E3%83%A2%E3%83%87%E3%83%AB)
のメッセージングシステムである。
Brokerを実現するバックエンドのメッセージングシステムとしてKafkaまたはMQTTを利用している。

SINETStreamではPublisherをWriterと呼び、SubscriberをReaderと呼ぶ。

![メッセージングシステムの概念図](docs/images/overview.png)

Brokerの構成情報やBrokerとの通信パラメータをまとめたものをSINETStreamではサービスと呼ぶ。
WriterやReaderはサービスを指定するだけでブローカーに接続してメッセージの送受信ができる。

トピックとはブローカーにおける論理的なチャンネルであり、
Writer/Readerはトピックを指定してメッセージの送受信を行うことで
異なる種類のメッセージ配信を1つのブローカー上で行える。

## ファイル構成

* [README.md](README.md)
    * このファイル
* python/
    * [README.md](python/README.md)
        * Python版SINETStreamのビルド手順
    * src/
        * Python版SINETStreamの共通部分
    * plugins/
        * kafka/
            * Python版SINETStreamのKafka固有部分
        * mqtt/
            * Python版SINETStreamのMQTT固有部分
    * sample/
        * サンプルプログラム
* java/
    * [README.md](java/README.md)
        * Java版SINETStreamのビルド手順
    * api/
        * Java版SINETStreamの共通部分
    * plugin-kafka/
        * Java版SINETStreamのKafka固有部分
    * plugin-mqtt/
        * Java版SINETStreamのMQTT固有部分
    * sample/
        * サンプルプログラム
* docs/
    * userguide/
        * [ユーザガイド](docs/userguide/index.md)
    * tutorial/
        * [チュートリアル](docs/tutorial/index.md)

## 動作環境

SINETStream API では以下の言語をサポートする。

* Python 3.6
* Java 8

SINETStream では以下のメッセージングシステムをサポートする。

* [Apache Kafka](https://kafka.apache.org/) 2.2.1
* MQTT v3.1, v3.1.1
    * [Eclipse Mosquitto](https://mosquitto.org/) v1.6.2

SINETStreamの動作環境は以下の通り。

* CentOS 7.6
* Windows 10

## 準備

SINETStreamでは、Brokerを実現するバックエンドのメッセージングシステムとしてKafkaまたはMQTTを利用している。
そのため、SINETStreamとともに、これらのメッセージングシステムのどちらかをインストールする必要がある。
チュートリアルパッケージでは、dockerコンテナを利用して必要なソフトウェア一式（SINETStream, Kafka, MQTT）をインストールする方法を用意している。

1. Kafkaブローカーの設定
    * [Kafka Quickstart](https://kafka.apache.org/quickstart)
1. MQTTブローカーの設定
    * [Eclipse Mosquitto: Installing](https://github.com/eclipse/mosquitto#installing)
    * [Eclipse Mosquitto: Quick start](https://github.com/eclipse/mosquitto#quick-start)
1. SINETStreamのインストール
    * Python: `pip3 install --user sinetstream-kafka sinetstream-mqtt`
    * Java: Java版READMEを参照

dockerコンテナをつかった[チュートリアル](docs/tutorial/index.md)も参考のこと。

## リンク

* [チュートリアル](docs/tutorial/index.md)
* [ユーザガイド](docs/userguide/index.md)
* [SINETStream性能測定結果](docs/performance/index.md)
* [更新履歴](CHANGELOG.md)

## ライセンス

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).
