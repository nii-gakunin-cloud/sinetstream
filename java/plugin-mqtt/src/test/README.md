**準備中** (2020-06-05 18:05:13 JST)

# テストについて

## テストを実行するための準備

このディレクトリのテストでは実際のMQTTブローカーを用いてテストを行い
ます。そのため事前にMQTTブローカーを準備する必要があります。

テストに利用するMQTTブローカーは以下の２つの方法を選択できます。

1. Dockerコンテナを利用する
1. 外部のMQTTブローカーを利用する

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
     Name                Command           State               Ports
--------------------------------------------------------------------------------
docker_broker_1   /usr/local/bin/init.sh   Up      0.0.0.0:1883->1883/tcp,
                                                   0.0.0.0:1884->1884/tcp,
                                                   0.0.0.0:1885->1885/tcp,
                                                   0.0.0.0:18080->8080/tcp,
                                                   0.0.0.0:8883->8883/tcp,
                                                   0.0.0.0:8884->8884/tcp,
                                                   0.0.0.0:8885->8885/tcp,
                                                   0.0.0.0:8886->8886/tcp,
                                                   9092/tcp, 9093/tcp
docker_test_1     ../gradlew check         Up
```

テストコンテナのログを確認することで、実行状況を確認することができます。

```
$ docker-compose logs test
(中略)
test_1    | > Task :SINETStream-api:generateLombokConfig SKIPPED
test_1    | > Task :SINETStream-api:compileJava UP-TO-DATE
test_1    | > Task :SINETStream-api:processResources UP-TO-DATE
test_1    | > Task :SINETStream-api:classes UP-TO-DATE
test_1    | > Task :SINETStream-api:jar UP-TO-DATE
test_1    | > Task :SINETStream-mqtt:generateLombokConfig SKIPPED
test_1    | > Task :SINETStream-mqtt:compileJava UP-TO-DATE
test_1    | > Task :SINETStream-mqtt:processResources UP-TO-DATE
test_1    | > Task :SINETStream-mqtt:classes UP-TO-DATE
test_1    | > Task :SINETStream-mqtt:compileTestJava
test_1    | > Task :SINETStream-mqtt:processTestResources NO-SOURCE
test_1    | > Task :SINETStream-mqtt:testClasses
test_1    | > Task :SINETStream-mqtt:test
test_1    | > Task :SINETStream-mqtt:jacocoTestReport
test_1    | > Task :SINETStream-mqtt:check
test_1    |
test_1    | BUILD SUCCESSFUL in 4m 17s
test_1    | 8 actionable tasks: 3 executed, 5 up-to-date
docker_test_1 exited with code 0
```

テスト結果の詳細については `sinetstream-java/plugin-mqtt/build/reports/tests/test/index.html` 
にレポートが作成されています。

テストの実行が終わったらコンテナを削除してください。

```
$ docker-compose down
```

## 外部のMQTTブローカーを利用する場合

テストに利用するブローカーのアドレスを環境変数に設定してください。テストに必要
となる環境変数を以下に示します。

* MQTT_BROKER
    * MQTTブローカーのアドレス（平文）
    * (ホスト名):(ポート番号)のフォーマットで指定する
* MQTT_USER_PASSWD_BROKER
    * パスワード認証を行うMQTTブローカーのアドレス（平文）
    * (ホスト名):(ポート番号)のフォーマットで指定する
* MQTT_WS_BROKER
    * WebSocketで通信を行うMQTTブローカーのアドレス（平文）
    * (ホスト名):(ポート番号)のフォーマットで指定する
* MQTT_SSL_BROKER
    * MQTTブローカーのアドレス（SSL）
    * (ホスト名):(ポート番号)のフォーマットで指定する
* MQTT_SSL_USER_PASSWD_BROKER
    * パスワード認証を行うMQTTブローカーのアドレス（SSL）
    * (ホスト名):(ポート番号)のフォーマットで指定する
* MQTT_SSL_CERT_AUTH_BROKER
    * クライアント認証を行うMQTTブローカーのアドレス（SSL）
    * (ホスト名):(ポート番号)のフォーマットで指定する
* MQTT_SSL_WS_BROKER
    * WebSocketで通信を行うMQTTブローカーのアドレス（SSL）
    * (ホスト名):(ポート番号)のフォーマットで指定する
* MQTT_BROKER_IP
    * MQTTブローカーのアドレス（平文）
    * (IPアドレス):(ポート番号)のフォーマットで指定する
* MQTT_USER_PASSWD_BROKER_IP
    * パスワード認証を行うMQTTブローカーのアドレス（平文）
    * (IPアドレス):(ポート番号)のフォーマットで指定する
* MQTT_WS_BROKER_IP
    * WebSocketで通信を行うMQTTブローカーのアドレス（平文）
    * (IPアドレス):(ポート番号)のフォーマットで指定する
* MQTT_SSL_BROKER_IP
    * MQTTブローカーのアドレス（SSL）
    * (IPアドレス):(ポート番号)のフォーマットで指定する
* MQTT_SSL_USER_PASSWD_BROKER_IP
    * パスワード認証を行うMQTTブローカーのアドレス（SSL）
    * (IPアドレス):(ポート番号)のフォーマットで指定する
* MQTT_SSL_CERT_AUTH_BROKER_IP
    * クライアント認証を行うMQTTブローカーのアドレス（SSL）
    * (IPアドレス):(ポート番号)のフォーマットで指定する
* MQTT_SSL_WS_BROKER_IP
    * WebSocketで通信を行うMQTTブローカーのアドレス（SSL）
    * (IPアドレス):(ポート番号)のフォーマットで指定する
* MQTT_BROKER_HOSTNAME
    * MQTTブローカーのホスト名
* CERT_URL
    * SSL接続のブローカーに関する証明書を取得できるURL
    
テストを実行するには `gradlew` を利用します。
カレントディレクトリがソースツリーのトップディレクトリ
 `sinetstream-java/` である場合は以下のように実行します。

```
$ ./gradlew :SINETStream-mqtt:check
```

