**準備中** (2020-06-05 19:08:22 JST)

# テストについて

## テストを実行するための準備

このディレクトリのテストでは実際のKafkaブローカーを用いてテストを行い
ます。そのため事前にKafkaブローカーを準備する必要があります。

テストに利用するKafkaブローカーは以下の２つの方法を選択できます。

1. Dockerコンテナを利用する
1. 外部のKafkaブローカーを利用する

## Dockerコンテナを利用する場合

`./docker` のディレクトリにテストを実行するための`docker-compose.yml`
などのファイルを用意しています。`./docker`にディレクトリを移動してください。

```
$ cd docker
```

`docker-compose`コマンドを実行して、テスト用ブローカーとテスト実行用コンテナを
起動してください。

```
$ docker-compose up -d
```

> `docker-compose`コマンドは[Install Docker Compose](https://docs.docker.com/compose/install/) などを参考に、事前にインストールしておいてください。

コンテナ起動直後は、以下のような状態になります。


```
$ docker-compose ps
      Name                  Command            State             Ports
--------------------------------------------------------------------------------
docker_broker2_1   /usr/bin/env                Up      1883/tcp,
                   BROKER_HOSTNA ...                   0.0.0.0:28080->8080/tcp,
                                                       8883/tcp, 9092/tcp,
                                                       9093/tcp,
                                                       0.0.0.0:9096->9096/tcp,
                                                       0.0.0.0:9097->9097/tcp
docker_broker_1    /usr/bin/env                Up      1883/tcp,
                   BROKER_HOSTNA ...                   0.0.0.0:18080->8080/tcp,
                                                       8883/tcp,
                                                       0.0.0.0:9092->9092/tcp,
                                                       0.0.0.0:9093->9093/tcp
docker_test_1      ../gradlew check            Up
```

しばらく待って(5～7分程度)テストが正常に終了すると、テスト実行用コンテナ
`docker_test_1`の`State`が`Exit 0`となります。

```
$ docker-compose ps
      Name                  Command            State             Ports
--------------------------------------------------------------------------------
docker_broker2_1   /usr/bin/env                Up       1883/tcp,
                   BROKER_HOSTNA ...                    0.0.0.0:28080->8080/tcp,
                                                        8883/tcp, 9092/tcp,
                                                        9093/tcp,
                                                        0.0.0.0:9096->9096/tcp,
                                                        0.0.0.0:9097->9097/tcp
docker_broker_1    /usr/bin/env                Up       1883/tcp,
                   BROKER_HOSTNA ...                    0.0.0.0:18080->8080/tcp,
                                                        8883/tcp,
                                                        0.0.0.0:9092->9092/tcp,
                                                        0.0.0.0:9093->9093/tcp
docker_test_1      ../gradlew check            Exit 0
```

テストコンテナのログを確認することで、実行状況を確認することができます。

```
$ docker-compose logs test
(中略)
test_1     | > Task :SINETStream-api:generateLombokConfig SKIPPED
test_1     | > Task :SINETStream-api:compileJava
test_1     | > Task :SINETStream-api:processResources UP-TO-DATE
test_1     | > Task :SINETStream-api:classes
test_1     | > Task :SINETStream-api:jar
test_1     | > Task :SINETStream-kafka:generateLombokConfig SKIPPED
test_1     | > Task :SINETStream-kafka:compileJava
test_1     | > Task :SINETStream-kafka:processResources UP-TO-DATE
test_1     | > Task :SINETStream-kafka:classes
test_1     | > Task :SINETStream-kafka:compileTestJava
test_1     | > Task :SINETStream-kafka:processTestResources NO-SOURCE
test_1     | > Task :SINETStream-kafka:testClasses
test_1     | > Task :SINETStream-kafka:test
test_1     | > Task :SINETStream-kafka:jacocoTestReport
test_1     | > Task :SINETStream-kafka:check
test_1     |
test_1     | BUILD SUCCESSFUL in 5m 23s
test_1     | 8 actionable tasks: 6 executed, 2 up-to-date
docker_test_1 exited with code 0
```

テスト結果の詳細については `sinetstream-java/plugin-kafka/build/reports/tests/test/index.html` 
にレポートが作成されています。

テストの実行が終わったらコンテナを削除してください。

```
$ docker-compose down
```

## 外部のKafkaブローカーを利用する場合

テストに利用するブローカーのアドレスを環境変数に設定してください。テストに必要
となる環境変数を以下に示します。

* KAFKA_BROKER
    * Kafkaブローカーのアドレス（平文）
    * (ホスト名):(ポート番号)のフォーマットで指定する
* KAFKA_SSL_BROKER
    * Kafkaブローカーのアドレス（SSL）
    * (ホスト名):(ポート番号)のフォーマットで指定する
* KAFKA_SASL_BROKER
    * Kafkaブローカーのアドレス（SASL_PLAINTEXT）
    * (ホスト名):(ポート番号)のフォーマットで指定する
* KAFKA_SASL_SSL_BROKER
    * Kafkaブローカーのアドレス（SASL_SSL）
    * (ホスト名):(ポート番号)のフォーマットで指定する
* KAFKA_BROKER_IP
    * Kafkaブローカーのアドレス（平文）
    * (IPアドレス):(ポート番号)のフォーマットで指定する
* KAFKA_SSL_BROKER_IP
    * KafkaブローカーのIPアドレス（SSL）
    * (IPアドレス):(ポート番号)のフォーマットで指定する
* KAFKA_SASL_BROKER_IP
    * KafkaブローカーのIPアドレス（SASL_PLAINTEXT）
    * (IPアドレス):(ポート番号)のフォーマットで指定する
* KAFKA_SASL_SSL_BROKER_IP
    * KafkaブローカーのIPアドレス（SASL_SSL）
    * (IPアドレス):(ポート番号)のフォーマットで指定する
* KAFKA_BROKER_HOSTNAME
    * Kafkaブローカーのホスト名（平文）
    
* KAFKA_CERT_URL
    * SSL接続のブローカーに関する証明書を取得できるURL
* KAFKA_SASL_CERT_URL
    * SASL_SSL接続のブローカーに関する証明書を取得できるURL
* TRUSTSTORE_PASSWORD
    * トラストストアのパスワード
* KEYSTORE_PASSWORD
    * キーストアのパスワード
    
テストを実行するには `gradlew` を利用します。
カレントディレクトリがソースツリーのトップディレクトリ
 `sinetstream-java/` である場合は以下のように実行します。

```
$ ./gradlew :SINETStream-kafka:check
```

