**準備中** (2020-06-05 14:26:44 JST)

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

# SINETStreamのサンプルプログラム

動画ファイルからフレームを読み取りKafkaブローカーに画像として送信するプロ
デューサーと、Kafkaブローカーから受信した画像を表示するコンシューマーの
サンプルプログラムを示します。

## 前提条件

サンプルプログラムの実行環境からアクセスできるKakfaブローカーが用意されている
ことを前提とします。

## サンプルプログラムの実行方法

1. SINETStreamをインストールする
```
$ pip install --user sinetstream sinetstream-kafka sinetstream-type-image
```
2. SINETStreamの設定ファイルを用意する
   * サンプルの設定ファイルを`sample_sinetstream_config.yml`に用意してあります。
   * SINETStreamが読み込めるように`.sinetstream_config.yml`にコピーしてください。
   * 設定ファイルの`brokers`の指定を実際のブローカーのアドレスに書き換えてください。

```
$ cp sample_sinetstream_config.yml .sinetstream_config.yml
$ sed -i -e '/brokers/s/kafka.example.org:9092/{ブローカのアドレス}/' .sinetstream_config.yml
```
3. コンシューマを実行する
```
$ python3 ./consumer.py -s image-1
```
4. プロデューサを実行する
    * コンシューマーとは別の端末で実行してください。
    * `-f` の後に動画ファイルを指定してください。
    * 指定した動画ファイルからフレームを読み取り、画像としてKafkaブローカーに送信します。
    * `-s` で指定するサービス名は設定ファイルの記述内容に対応しています。
```
$ python3 ./producer.py -s image-1 -f video.mp4
```
