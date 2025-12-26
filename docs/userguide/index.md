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

[English](index.en.md)

# SINETStream ユーザガイド

## SINETStream とは

SINETStream は、Apache Kafka や MQTT Broker などの多様なメッセージングシステムに対し、以下の操作を統一的に行う機能を提供するラッパーライブラリである。

1. ブローカーへの接続
1. ブローカーからの切断
1. ブローカーへのメッセージ送信
1. ブローカーからのメッセージ受信

<!---
SINETStream は現在、バックエンドのメッセージングシステムとして Apache Kafka と MQTT Broker などをサポートしている。
--->
多様なメッセージングシステムをサポートできるよう、バックエンドはプラグインとして拡張可能になっている。

SINETStream は Python API (CPython と MicroPython) と Java API 、および Android API (MQTTのみ) を提供している。
Android版に関しては、IoT (Internet of Things) 端末としてセンサー情報を配信する用途に資することも想定する。
このため、Android端末の具備するセンサーの読取値を周期的にJSON形式で出力する `SINETStreamHelper` ライブラリも併せて提供する。

## 目次

* [Python版ユーザガイド](api-python.md)
* [Java版ユーザガイド](api-java.md)
* [Android版ユーザガイド](android.md)
    * [Android API](api-android.md)
    * [Android API (Javadoc)](http://javadoc.android.sinetstream.net/sinetstream-android/)
    * [Android版設定ファイル](config-android.md)
    * [Android版設定クライアント](config-client-android.md)
* [SINETStreamHelperユーザガイド](libhelper.md)
    * [SINETStreamHelper API](api-libhelper.md)
    * [SINETStreamHelper API (Javadoc)](http://javadoc.android.sinetstream.net/sinetstream-android-helper/)
* [設定ファイル](config.md)
* [認証と認可](auth.md)
* [CLI(Command Line Interface)](https://github.com/nii-gakunin-cloud/sinetstream/blob/main/java/sample/cli/README.md)
* [互換性](compat.md)

## 用語の定義

| 用語 | 意味 |
| :--- | :--- |
| Reader | メッセージを受信するプログラム |
| Writer | メッセージを送信するプログラム |
| ブローカー | Writerが送信したメッセージをReaderに中継するプログラム |
| トピック | メッセージを送受信するときの論理的なチャンネル |
