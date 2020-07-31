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

# テストスクリプトについて

## テストスクリプトを実行するための準備

このディレクトリのテストスクリプトは実際のMQTTブローカーを用いてテストを行い
ます。そのため事前にMQTTブローカーを準備する必要があります。

テストに利用するMQTTブローカーは以下の２つの方法を選択できます。

1. Dockerコンテナを利用する
1. 外部のMQTTブローカーを利用する

## Dockerコンテナを利用する場合

`./docker_mqtt` のディレクトリにテストを実行するための`docker-compose.yml`が
用意してあります。`./docker_mqtt`にディレクトリを移動してください。

```
$ cd docker_mqtt
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
        Name                   Command           State            Ports
--------------------------------------------------------------------------------
docker_mqtt_broker_1   /usr/local/bin/init.sh    Up      1883/tcp, 8080/tcp,
                                                         8883/tcp, 9092/tcp,
                                                         9093/tcp
docker_mqtt_test_1     /usr/local/bin/entrypoi   Up
                       nt.sh

```

しばらく待ってテストが正常に終了すると、テスト実行用コンテナ
`docker_mqtt_test_1`の`State`が`Exit 0`となります。

```
$ docker-compose ps
        Name                   Command           State            Ports
--------------------------------------------------------------------------------
docker_mqtt_broker_1   /usr/local/bin/init.sh    Up       1883/tcp, 8080/tcp,
                                                          8883/tcp, 9092/tcp,
                                                          9093/tcp
docker_mqtt_test_1     /usr/local/bin/entrypoi   Exit 0
                       nt.sh
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
test_1    | collected 64 items
test_1    |
test_1    | tests/conftest.py .
test_1    | tests/test_brokers.py ...........
test_1    | tests/test_exception.py .....
test_1    | tests/test_pubsub.py ......
test_1    | tests/test_reader.py ..............
test_1    | tests/test_tls.py ...ss....ssss
test_1    | tests/test_writer.py .............
test_1    | src/sinetstreamplugin/mqtt.py .
(途中略)
test_1    | ======================== 58 passed, 6 skipped in 4.10s =========================
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

### テストを追加するとき

テストコードを追加するときにテストするたびにコンテナを作成すると時間がかかるので、コンテナの中で作業すると効率がよいです。

`docker-compose.yml` を編集して次の2行を追加します。

```
services:
  test:
    command: bash  # この行を追加
    tty: true      # この行を追加
```

`docker-compose up -d` でコンテナを立ち上げたあと、コンテナの中に入ってテストを実行します。

```
$ docker exec -it docker_mqtt_test_1 bash
root@1234567890ab:/opt/ss_test# cd /opt/ss_test
root@1234567890ab:/opt/ss_test# python setup.py test
running pytest
(途中略)
root@1234567890ab:/opt/ss_test# exit
$ 
```

## 外部のMQTTブローカーを利用する場合

テストに利用するブローカーのアドレスを環境変数に設定してください。テストに必要
となる環境変数を以下に示します。

* MQTT_BROKER
    * MQTTブローカーのアドレス（平文）
* MQTT_SSL_BROKER
    * MQTTブローカーのアドレス（SSL）
* CACERT_PATH
    * SSL接続で利用するCA証明書を配置してあるパス

`MQTT_BROKER`以外の環境変数の指定はオプションです。指定されない場合は、対応
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
