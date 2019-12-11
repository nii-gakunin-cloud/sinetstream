**準備中** (2019-12-11 15:59:00 JST)

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

SINETStream ユーザガイド

# 認証と認可

## 概要

認証とは事前に登録されたユーザだけが接続を許可される機能で、
認可とはユーザが行える操作 (メッセージを読んだり書いたり) を制限する機能である。

認証はブローカーとクライアントの両方に設定する。認可はブローカーの設定のみである。
ブローカーの設定はバックエンドのメッセージングシステムによって異なる。

関連する設定ファイルを以下に示す。

* 認証
    * ブローカー
        * Kafka: `server.properties`
        * MQTT (mosquitto): `mosquitto.conf`
    * クライアント
        * `.sinetstream_config.yml`
* 認可
    * ブローカー
        * Kafka: `server.properties`
        * MQTT (mosquitto): `mosquitto.conf`

## 目次

* [Kafkaの認証の設定](auth-kafka.md#kafkaの認証の設定) を参照
* [MQTTの認証の設定](auth-mqtt.md#mqttの認証の設定) を参照
* [Kafkaの認可の設定](auth-kafka.md#kafkaの認可の設定) を参照
* [MQTTの認可の設定](auth-kafka.md#mqttの認可の設定) を参照
* 設定例
    * [Kafkaの設定例](auth-kafka.md#kafkaの設定例)
    * [MQTTの設定例](auth-kafka.md#mqttの設定例)

## 制限事項

認証・認可でエラーが起きたときの処理はバックエンドのメッセージングシステムに依存しており、ユーザへの通知方法が統一されていない。
現状は以下のとおり:

| バックエンド | 機能 | 言語 | エラー時の挙動 |
| --- | --- | --- | --- |
| Kafka | 認証 | Python | 例外発生 (kafka.errors.NoBrokersAvailable -> sinetstream.api.ConnectionError) |
| Kafka | 認証 | Java | 例外は発生せず。送信・受信は失敗するがエラーにならない |
| Kafka | 認可 | Python | producerでは例外発生 on metadata updating failrue (kafka.errors.KafkaTimeoutError)、consumerではメッセージが届かず受信できない |
| Kafka | 認可 | Java | 送信・受信は失敗するがエラーにならない |
| MQTT | 認証 | Python | エラーメッセージが表示されるがエラーにはならない |
| MQTT | 認証 | Java | 例外発生 (jp.ad.sinet.stream.api.ConnectionException) |
| MQTT | 認可 | Python | 送信・受信は失敗するがエラーにならない |
| MQTT | 認可 | Java | 送信・受信は失敗するがエラーにならない |

