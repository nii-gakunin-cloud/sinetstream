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
--->

[English](https://translate.google.com/translate?hl=en&sl=ja&tl=en&u=https://nii-gakunin-cloud.github.io/sinetstream/server/kafka-connect/index.html "google translate")

# Kafka Connect

[Kafka Connect](https://kafka.apache.org/documentation/#connect)のセットアップ手順を示します。


## 前提条件

ここで示すKafkaコネクタのセットアップ手順は、以下の条件をみたしていることを前提とします。

* Kafkaコネクタの実行環境でDockerコンテナの起動ができること
    - ここで示す手順ではKafkaコネクタをコンテナとして実行するため、コンテナを起動できる環境が必要となります。
* Kafkaコネクタの実行環境で `docker-compose` コマンドが実行できること
    - Kafkaコネクタコンテナの起動には多くのパラメータを指定する必要があるため、`docker-compose`を利用してコンテナを起動します。
* Kafkaコネクタの実行環境からKafkaブローカーにアクセスできること
    - Kafkaブローカーはセットアップ済であり、アクセス可能であることを前提とします。
* Kafkaコネクタの実行環境に対して [Ansible](https://docs.ansible.com/ansible/) で操作できること

## セットアップ手順

* [S3 Sink](001-S3_Sinkコネクタのセットアップ.ipynb)
* [MongDB Sink](002-MongoDB_Sinkコネクタのセットアップ.ipynb)
