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

**準備中** (2020-06-05 18:05:13 JST)

# テストスクリプトについて

## テストスクリプトを実行するための準備

このディレクトリのテストスクリプトは実際のKafkaブローカーを用いてテストを行い
ます。そのため事前にKafkaブローカーを準備する必要があります。

テストに利用するKafkaブローカーは以下の２つの方法を選択できます。

1. Dockerコンテナを利用する
1. 外部のKafkaブローカーを利用する

## Dockerコンテナを利用する場合

`./docker_kafka` のディレクトリにテストを実行するための`docker-compose.yml`が
用意してあります。`./docker_kafka`にディレクトリを移動してください。

```
$ cd docker_kafka
```

`docker-compose`コマンドを実行して、テスト用ブローカーとテスト実行用コンテナを
起動してください。

```
$ docker-compose up -d
```

> `docker-compose`コマンドは[Install Docker Compose](https://docs.docker.com/compose/install/)などを参考に、事前にインストールしておいてください。

コンテナ起動直後は、以下のような状態になります。


```
$ docker-compose ps
--------------------------------------------------------------------------------
docker_kafka_broker_1   /usr/local/bin/init.sh    Up      1883/tcp, 8080/tcp,
                                                          8883/tcp, 9092/tcp,
                                                          9093/tcp
docker_kafka_test_1     /usr/local/bin/entrypoi   Up
                        nt.sh
```

しばらく待ってテストが正常に終了すると、テスト実行用コンテナ
`docker_kafka_test_1`の`State`が`Exit 0`となります。

```
$ docker-compose ps
        Name                   Command           State            Ports
--------------------------------------------------------------------------------
docker_kafka_broker_1   /usr/local/bin/init.sh   Up       1883/tcp, 8080/tcp,
                                                          8883/tcp, 9092/tcp,
                                                          9093/tcp
docker_kafka_test_1     /usr/local/bin/entrypo   Exit 0
                        int.sh
```

テストの詳細を確認するにはテスト実行用コンテナのログを確認します。

```
$ docker-compose logs test
(途中略)
test_1    | ============================= test session starts ==============================
test_1    | platform linux -- Python 3.7.4, pytest-5.3.2, py-1.8.1, pluggy-0.13.1
test_1    | rootdir: /opt/ss_test, inifile: setup.cfg, testpaths: tests, src
test_1    | plugins: cov-2.8.1, pep8-1.0.6, timeout-1.3.4
test_1    | timeout: 10.0s
test_1    | timeout method: thread
test_1    | timeout func_only: False
test_1    | collected 68 items
test_1    |
test_1    | tests/conftest.py .
test_1    | tests/test_brokers.py .........
test_1    | tests/test_exception.py .....
test_1    | tests/test_pubsub.py ......
test_1    | tests/test_reader.py .................
test_1    | tests/test_tls.py ...............
test_1    | tests/test_writer.py ..............
test_1    | src/sinetstreamplugin/kafka.py .
test_1    |
(途中略)
test_1    | ============================= 68 passed in 11.77s ==============================
(以下略)
```

テストの実行が終わったらコンテナを削除してください。

```
$ docker-compose down
```

SINETStreamライブラリ(プラグインでないほう)に修正があり、対応するパッケージが
まだ[Python Package Index](https://pypi.org/)に登録されていない場合はテスト実
行用コンテナに新しいSINETStreamライブラリのパッケージを組み込む必要があります。
事前に`../../../../dist/`にwheelパッケージを作成しておくとコンテナ起動時に自動的
にパッケージがインストールされます。

## 外部のKafkaブローカーを利用する場合

テストに利用するブローカーのアドレスを環境変数に設定してください。テストに必要
となる環境変数を以下に示します。

* KAFKA_BROKER
    * Kafkaブローカーのアドレス（平文）
* KAFKA_SSL_BROKER
    * Kafkaブローカーのアドレス（SSL）
* CACERT_PATH
    * SSL接続で利用するCA証明書を配置してあるパス
* CLIENT_CERT_PATH
    * クライアント証明書を配置してあるパス
* CLIENT_CERT_KEY_PATH
    * クライアント証明書の鍵ファイルを配置してあるパス
* CLIENT_BAD_CERT_PATH
    * 認証エラーとなるクライアント証明書を配置してあるパス
* CLIENT_BAD_CERT_KEY_PATH
    * 認証エラーとなるクライアント証明書の鍵ファイルを配置してあるパス

`KAFKA_BROKER`以外の環境変数の指定はオプションです。指定されない場合は、対応
するテストの実行がスキップされます。

テストを実行するには、通常の`pytest`の実行と同様に

```
$ pytest
```

または、

```
$ python setup.py test
```

を実行してください。
