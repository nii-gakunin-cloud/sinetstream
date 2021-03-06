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

[English](kafka-authentication.en.md)

# SINETStreamからKafkaの認証を利用する

## 概要

Kafkaブローカーがクライアント接続に対して提供している認証機構を以下に示す。

* SSL/TLS認証（クライアント認証）
* SASL/GSSAPI(Kerberos)
* SASL/PLAIN
* SASL/SCRAM-SHA-256, SAL/SCRAM-SHA-512
* SASL/OAUTHBEARER
* Delegation token authentication

このうち、SINETStreamでは以下のものをサポートしている。SINETStreamからの利用手順はについては各項目のリンク先に記している。

1. [SSL/TLS認証（クライアント認証）](kafka-authentication-ssl.md)
1. [SASL/PLAIN](kafka-authentication-sasl-plain.md)
1. [SASL/SCRAM-SHA-256, SASL/SCRAM-SHA-512](kafka-authentication-sasl-scram.md)

なお、リンク先の説明ではクライアント側となるSINETStreamの設定方法だけではなく、対応するブローカー側の設定手順についても記した。
