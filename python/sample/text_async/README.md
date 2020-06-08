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


端末から入力した文字列をKafkaブローカーに送信するプロデューサーと、Kafka
ブローカーから受信した文字列を表示するコンシューマーのサンプルプログラムを
示します。このディレクトリにあるサンプルプログラムではSINETStreamの非同期
APIを用いたサンプルプログラムになっています。

## 前提条件

サンプルプログラムの実行環境からアクセスできるKakfaブローカーが用意されている
ことを前提とします。

## サンプルプログラムの実行方法

1. SINETStreamをインストールする
```
$ pip install --user sinetstream sinetstream-kafka
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
    * 後で実行するプロデューサーで入力した文字列が表示されます。
    * 表示内容はプロデューサーが文字列を送信した時刻、トピック名、送信文字列となります。
    * `-s` で指定するサービス名は設定ファイルの記述内容に対応しています。
```
$ python3 ./consumer.py -s text-1
```
4. プロデューサを実行する
    * コンシューマーとは別の端末で実行してください。
    * プログラムが起動したら端末から文字列を入力してください。
```
$ python3 ./producer.py -s text-1
```
