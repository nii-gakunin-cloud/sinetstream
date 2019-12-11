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

# SINETStream ユーザガイド

## SINETStream とは

SINETStream は、Apache Kafka や MQTT Broker などの多様なメッセージングシステムに対し、以下の操作を統一的に行う機能を提供するラッパーライブラリである。

1. ブローカーへの接続
1. ブローカーからの切断
1. ブローカーへのメッセージ送信
1. ブローカーからのメッセージ受信

SINETStream は現在、バックエンドのメッセージングシステムとして Apache Kafka と MQTT Broker をサポートしている。

> 将来、他のメッセージングシステムをサポートできるよう、バックエンドはプラグインとして拡張可能になっている。

SINETStream は Python API と Java API を提供している。

## 目次

* [Python API](api-python.md)
* [Java API](api-java.md)
* [設定ファイル](config.md)
* [認証と認可](auth.md)

## 用語の定義

| 用語 | 意味 |
| :--- | :--- |
| Reader | メッセージを受診するプログラム |
| Writer | メッセージを送信するプログラム |
| ブローカー | Writerが送信したメッセージをReaderに中継するプログラム |
| トピック | メッセージを送受信するときの論理的なチャンネル |
