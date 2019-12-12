**準備中** (2019-12-12 15:15:40 JST)

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

# SINETStream example

## 準備

SINETStreamのjarファイルを./libs に配置する。

$ mkdir libs
$ cp ../../*/build/libs/SINETStream*.jar libs

## ビルド

次のコマンドを実行する。

```
$ ./gradlew build
```

` build/distributions/image-producer-*` にビルドしたコマンドなどを
zip, tar でアーカイブしたファイルが作成されている。


## インストール

ビルドされたアーカイブファイルをインストール先のディレクトリに展開する。
展開したディレクトリの `bin/image-consumer`,
`bin/image-producer` がサンプルプログラムを実行するスクリプトになっ
ている。

## 設定ファイル

サンプルプログラムを実行するディレクトリに設定ファイルを作成する。
`./.sinetstraem_config.yml` に以下のようなファイルを作成する。
ブローカのホスト名は実際に利用する環境に合わせて記述を変更する。

```
service-1:
  type: kafka
  brokers:
    - kafka1.example.org:9092
    - kafka2.example.org:9092
    - kafka3.example.org:9092
service-2:
  type: mqtt
  brokers: mqtt.example.org:1883
```

## 実行手順

まずコンシューマを実行する。

```
$ ./bin/image-consumer -s service-1 -t test-topic
```

`-s` には設定ファイルに定義したサービス名を指定する。
`-t` はコンシューマがメッセージを取得するトピック名を指定する。

次にプロデューサーを実行する。サービス名とトピック名はコンシューマ
と同じ値を指定する。

```
$ ./bin/image-producer -s service-1 -t test-topic -f movie.mp4
```

`-f` に指定した動画ファイルから画像を切り出してブローカに送信する。

