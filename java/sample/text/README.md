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

## ビルド

次のコマンドを実行する。

```
$ ./gradlew build
```

` build/distributions/text-producer-*` にビルドしたコマンドなどを
zip, tar でアーカイブしたファイルが作成されている。


## インストール

ビルドされたアーカイブファイルをインストール先のディレクトリに展開する。
展開したディレクトリの `bin/text-consumer`,
`bin/text-producer` がサンプルプログラムを実行するスクリプトになっている。

## 設定ファイル

サンプルプログラムを実行するディレクトリに設定ファイルを作成する。
`./.sinetstraem_config.yml` に以下のようなファイルを作成する。
ブローカのホスト名は実際に利用する環境に合わせて記述を変更する。

```
service-1:
  type: kafka
  brokers: kafka.example.org:9092
  topic: test-text-topic-1
service-2:
  type: mqtt
  brokers: mqtt.example.org:1883
  topic: test-text-topic-2
```

## 実行手順

まずコンシューマを実行する。

```
$ ./bin/text-consumer -s service-1
```

`-s` には設定ファイルに定義したサービス名を指定する。

次にプロデューサーを実行する。
サービス名はコンシューマと同じ値を指定する。

```
$ ./bin/text-producer -s service-1
```

プロデューサーの標準入力から入力したテキストがブローカに送信される。

## 実行手順 (コンフィグサーバーを使う場合)

1. コンフィグサーバ認証情報を用意する。
    * コンフィグサーバーにログインする。
        * コンフィグサーバのURLはデーター管理者に問い合わせてください。
    * APIアクセスキーを作成する。(まだ作ってなければ or 作ったが有効期限が切れていたら)
    * APIアクセスキーダウンロードする。
    * ダウンロードしたファイルを ~/.config/sinetstream/auth.json に移動する。
    * 他のユーザにアクセスキーが漏れないよう制限する。
        * 具体的には: `chmod 400 ~/.config/sinetstream/auth.json`
2. コンシューマを実行する。

    ```
    $ ./bin/text-consumer -c config-1 -s service-1
    ```

    * `-c` で指定するコンフィグ名はデーター管理者に問い合わせてください。
    * `-s` で指定するサービス名は設定ファイルの記述内容に対応しています。
    * 設定ファイルは実行時にコンフィグサーバーからダウンロードされます。
    * サービス名はコンフィグサーバーの管理画面からコンフィグ情報を参照して調べられますが、データー管理者に問い合わせるのが早いです。
    * サービスが1つしか定義されていなければ `-s` 指定は省略できます。

3. プロデューサーを実行する。

    ```
    $ ./bin/text-producer -s service-1
    ```

    * サービス名とコンシューマと同じ値を指定する。

    プロデューサーの標準入力から入力したテキストがブローカに送信される。
