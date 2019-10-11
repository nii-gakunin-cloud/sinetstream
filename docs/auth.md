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
--->

# SINETStream の認証と認可の設定

## Kafka の認証

### 対応する security_protocol と sasl_mechanism

| security_protocol | Java | Python | 補足 |
| --- | --- | --- | --- |
| PLAINTEXT | サポート | サポート | 認証なし・暗号化なし |
| SSL | サポート | サポート | 認証あり(有効時のみ)・暗号化あり |
| SASL_PLAINTEXT | サポート | サポート | 認証あり・暗号化なし |
| SASL_SSL | サポート | サポート | 認証あり・暗号化あり |

| sasl_mechanism | Java | Python | 補足 |
| --- | --- | --- | --- |
| PLAIN | サポート | サポート | simple username/password |
| SCRAM-SHA-256 | サポート | 未サポート | Salted Challenge Response Authentication |
| SCRAM-SHA-512 | サポート | 未サポート | |
| GSSAPI | (未確認) | (未確認) | Kerberos |
| OAUTHBEARER |  (未確認) | (未確認) | OAuth2 |

* protocol と mechanism の組み合わせ

    | security_protocol | sasl_mechanism  | 認証 | 暗号化 |
    | --- | --- | --- | --- |
    | PLAINTEXT | (なし) | なし | なし |
    | SASL_PLAINTEXT | PLAIN | あり | なし |
    | SASL_PLAINTEXT| SCRAM-SHA-* | あり | なし |
    | SSL | (なし) | あり(クライアント証明書による)/なし | あり |
    | SASL_SSL | PLAIN | あり | あり |
    | SASL_SSL | SCRAM-SHA-* | あり | あり |

### Kafka サーバー

補足: 以下のポート番号 `9092` は変更してよい。

#### security_protocol=PLAINTEXT

* `server.properties` を編集する

    ```
    listeners=PLAINTEXT://:9092
    ```

#### security_protocol=SSL

* `server.properties` を編集する

    ```
    listeners=SSL://:9092
    ```

    if client authentication is required:

    ```
    listeners=SSL://:9092
    ssl.client.auth=required
    ```

#### security_protocol=SASL_PLAINTEXT

* `server.properties` を編集する

    ```
    listeners=SASL_PLAINTEXT://:9092
    ```

#### security_protocol=SASL_SSL

* `server.properties` を編集する

    ```
    listeners=SASL_SSL://:9092
    ```

#### sasl_mechanism=PLAIN

* `server.properties` を編集する

    ```
    sasl.mechanism.inter.broker.protocol=PLAIN
    sasl.enabled.mechanisms=PLAIN
    ```

* `kafka_server_jaas.conf` を編集する

    ブローカー間通信用のユーザ名とパスワードを設定する。

    ```
    KafkaServer {
        org.apache.kafka.common.security.plain.PlainLoginModule required
        username="admin"
        password="admin-secret";
    };
    ```

    ユーザを追加するには:

    ```
    KafkaServer {
        org.apache.kafka.common.security.plain.PlainLoginModule required
        username="admin"
        password="admin-secret"
        user_alice="alice-secret";
    };
    ```

    * フォーマットハ user_<USER>="<PASSWORD>"
    * Restart Kafka server after edit kafka_server_jaas.conf to reload.
    * kafka_server_jaas.conf を編集したらリロードするために Kafka サーバーを再起動する。

* 起動スクリプトを編集する

    ```
    EXTRA_ARGS=-Djava.security.auth.login.config=config/kafka_server_jaas.conf bin/kafka-server-start.sh config/server.properties
    ```

#### sasl_mechanism=SCRAM-SHA-*

* `server.properties` を編集する

    ```
    sasl.mechanism.inter.broker.protocol=SCRAM-SHA-256,SCRAM-SHA-512
    sasl.enabled.mechanisms=SCRAM-SHA-256,SCRAM-SHA-512
    ```

* ブローカー間通信用のユーザ名とパスワードを設定する

    ```
    $ bin/kafka-configs.sh --zookeeper localhost:2181 --alter --add-config 'SCRAM-SHA-256=[password=admin-secret],SCRAM-SHA-512=[password=admin-secret]' --entity-type users --entity-name admin
    ```

* ユーザのパスワードを設定する

    ```
    $ bin/kafka-configs.sh --zookeeper localhost:2181 --alter --add-config 'SCRAM-SHA-256=[iterations=8192,password=alice-secret],SCRAM-SHA-512=[password=alice-secret]' --entity-type users --entity-name alice
    ```

    * サーバーを再起動する必要はない。

### Kafka Client

* 以下のポート番号 `9092` はサーバーの設定に合せる。
* 以下のサーバーホスト `localhost` はサーバーのホスト名にする。
* 以下の `service-foo` は変更してよい。

#### security_protocol=PLAINTEXT

* `.sinetstream_config.yml` を編集する

    ```
    service-foo:
        broker:
            - localhost:9092
    ```

#### security_protocol=SSL

* `.sinetstream_config.yml` を編集する

    ```
    service-foo:
        broker:
            - localhost:9092
        tls: true
    ```

    If using private CA:

    ```
    service-foo:
        broker:
            - localhost:9092
        tls:
            - ca_certs: /path/to/ca.pem
    ```

#### security_protocol=SASL_PLAINTEXT

* `.sinetstream_config.yml` を編集する

    ```
    service-foo:
        broker:
            - localhost:9092
        security_protocol: SASL_PLAINTEXT
    ```

#### security_protocol=SASL_SSL

* `.sinetstream_config.yml` を編集する

    ```
    service-foo:
        broker:
            - localhost:9092
        tls: true
        security_protocol: SASL_SSL
    ```

#### sasl_mechanism=PLAIN

* `.sinetstream_config.yml` を編集する

    ```
    service-foo:
        ...
        sasl_mechanism: PLAIN
        sasl_plain_username: alice
        sasl_plain_password: alice-secret
    ```

#### sasl_mechanism=SCRAM-SHA-*

* `.sinetstream_config.yml` を編集する

    ```
    service-foo:
        ...
        sasl_mechanism: SCRAM-SHA-512
        sasl_plain_username: alice
        sasl_plain_password: alice-secret
    ```

## MQTT の認証

### Mosquitto (MQTTサーバー)

1. `mosquitto.conf` を編集する

    ```
    password_file /path/to/password
    ```

1. `password` を編集する

    ```
    $ mosquitto_passwd -c /path/to/password <USER1>
    Password: <PASSWORD1>
    reenter password: <PASSWORD1>
    ```
    
    Note: password file is hash'ed.

1. サーバーを再起動する

1. ユーザーを追加する:

    ```
    $ mosquitto_passwd -b /path/to/password <USER2> <PASSWORD2>
    ```

    SIGHUPでパスワードを再読み込みする:

    ```
    pkill -HUP mosquitto
    ```

### MQTT クライアント

* `.sinetstream_config.yml` を編集する

    ```
    service-foo:
        username_pw_set:
            username: <USER1>
            password: <PASSWORD1>
    ```

## Kafka の認可

* `server.properties` を編集する

    ```
    authorizer.class.name=kafka.security.auth.SimpleAclAuthorizer
    super.users=User:admin
    ```

* プロデューサーを追加する:

    プロデューサー <USER> は任意のトピックをパブリッシュできる。

    ```
    kafka-acls --authorizer-properties zookeeper.connect=localhost:2181 --add --allow-principal User:<USER> --producer --topic "*"
    ```

* コンシューマを追加する:

    コンシューマ <USER> は任意のトピックをサブスクライブできる。

    ```
    kafka-acls --authorizer-properties zookeeper.connect=localhost:2181 --add --allow-principal User:<USER> --consumer --topic "*" --group "*"
    ```

* List ACL

    すべての ACL を表示する:

    ```
    kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 --list
    ```

    特定の ACL だけを表示する:

    ```
    kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 --list --topic <TOPIC>
    ```

    ワイルドカードの ACL を表示する:

    ```
    kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 --list --topic "*"
    ```

## Authorization for Mosquitto (MQTT server)

* `mosquitto.conf` を編集する

    ```
    acl_file /path/to/aclfile
    ```

* サーバを再起動する

* `aclfile` を編集する:

    ```
    user <USER1>
    topic read <TOPIC-A>
    topic write <TOPIC-B>
    topic readwrite <TOPIC-C>

    user <USER2>
    topic read <TOPIC-B>
    topic read <TOPIC-C>
    ```

    Reload config after edit aclfile:

    ```
    pkill -HUP mosquitto
    ```

## 参考

* Kafka
    * https://docs.confluent.io/2.0.0/kafka/sasl.html
    * http://kafka.apache.org/documentation/#security_sasl_plain_clientconfig
    * https://docs.confluent.io/current/kafka/authorization.html
* Mosquitto
    * http://www.steves-internet-guide.com/mqtt-username-password-example/
    * https://mosquitto.org/man/mosquitto_passwd-1.html
    * https://mosquitto.org/man/mosquitto-conf-5.html
    * http://www.steves-internet-guide.com/topic-restriction-mosquitto-configuration/

## 設定例

### Kafka サーバーの設定

1. Kafka のリリース版をダウンロードして展開する。

    * [Download the code](https://kafka.apache.org/quickstart#quickstart_download)

    ```
    $ wget -q http://ftp.jaist.ac.jp/pub/apache/kafka/2.3.0/kafka_2.12-2.3.0.tgz
    $ tar -xzf kafka_2.12-2.3.0.tgz
    $ cd kafka_2.12-2.3.0
    $ ls
    bin/  config/  libs/  LICENSE  NOTICE  site-docs/
    $
    ```

1. 設定ファイルを編集する。

    1. Kafka サーバーのプロパティファイルを編集する。

        ```
        $ echo '
        # authn
        listeners=PLAINTEXT://:9092,SASL_PLAINTEXT://:9099
        security.inter.broker.protocol=SASL_PLAINTEXT
        sasl.mechanism.inter.broker.protocol=PLAIN
        sasl.enabled.mechanisms=PLAIN,SCRAM-SHA-256,SCRAM-SHA-512

        # authz
        authorizer.class.name=kafka.security.auth.SimpleAclAuthorizer
        super.users=User:admin
        ' >> config/server.properties
        $
        ```

    1. ZooKeeper のプロパティファイルを編集する。

        ```
        $ echo '
        authProvider.1 = org.apache.zookeeper.server.auth.SASLAuthenticationProvider
        ' >> config/zookeeper.properties
        $
        ```

    1. Kafka サーバーの [JAAS](https://docs.oracle.com/javase/jp/8/docs/technotes/guides/security/jaas/JAASRefGuide.html) ファイルを編集する。

        ```
        $ echo '
        KafkaServer {
            org.apache.kafka.common.security.plain.PlainLoginModule required
            username="admin"
            password="admin-secret"
            user_admin="admin-secret"
            user_alice="alice-secret";

            org.apache.kafka.common.security.scram.ScramLoginModule required
            username="admin"
            password="admin-secret";
        };

        Client {
            org.apache.zookeeper.server.auth.DigestLoginModule required
            username="kafka"
            password="kafka-secret";
        };
        ' > config/kafka_server_jaas.conf
        $
        ```

    1. ZooKeeper の [JAAS](https://docs.oracle.com/javase/jp/8/docs/technotes/guides/security/jaas/JAASRefGuide.html) ファイルを編集する。

        ```
        echo '
        Server {
            org.apache.zookeeper.server.auth.DigestLoginModule required
            user_super="admin-secret"
            user_kafka="kafka-secret";
        };
        ' > config/zookeeper_jaas.conf
        $
        ```

1. サーバーを立ち上げる。

    1. ZooKeeper を立ち上げる。

        ```
        $ env KAFKA_OPTS="-Djava.security.auth.login.config=config/zookeeper_jaas.conf" bin/zookeeper-server-start.sh config/zookeeper.properties
        [2019-10-09 16:56:04,785] INFO Reading configuration from: config/zookeeper.properties (org.apache.zookeeper.server.quorum.QuorumPeerConfig)
        ...
        [2019-10-09 16:56:04,837] INFO Server successfully logged in. (org.apache.zookeeper.Login)
        [2019-10-09 16:56:04,841] INFO binding to port 0.0.0.0/0.0.0.0:2181 (org.apache.zookeeper.server.NIOServerCnxnFactory)
        ```

    1. Kafka サーバーを立ち上げる。

        ```
        $ env EXTRA_ARGS="-Djava.security.auth.login.config=config/kafka_server_jaas.conf" bin/kafka-server-start.sh config/server.properties
        [2019-10-09 16:58:52,376] INFO Registered kafka:type=kafka.Log4jController MBean (kafka.utils.Log4jControllerRegistration$)
        ...
        ```

    1. サーバーが立ち上がっているのを確認する。

        ```
        $ ss --listening --numeric |grep -w -e 2181 -e 9092 -e 9099
        tcp    LISTEN     0      50       :::9099                 :::*
        tcp    LISTEN     0      50       :::9092                 :::*
        tcp    LISTEN     0      50       :::2181                 :::*
        $
        ```

### MQTT サーバーの設定

1. Mosquitto をダウンロードしてビルドする。

    ```
    $ wget -q https://mosquitto.org/files/source/mosquitto-1.6.2.tar.gz
    $ ls mosquitto-1.6.2.tar.gz
    $ tar -xzf mosquitto-1.6.2.tar.gz
    $ cd mosquitto-1.6.2
    $ make
    ...
    $
    ```

1. 設定ファイル

    1. 設定ファイルを編集する。

        ```
        $ echo '
        password_file ./mosquitto.passwd
        acl_file ./mosquitto.acl
        ' >> mosquitto.conf
        $
        ```

    1. ユーザーを登録する。

        ```
        $ src/mosquitto_passwd -c mosquitto.passwd alice
        Password: alice-secret
        Reenter password: alice-secret
        $
        ```

        他のユーザーを登録するには:

        ```
        $ src/mosquitto_passwd -b mosquitto.passwd bob bob-secret
        $
        ```

    1. ACL ファイルを作成する。

        ```
        $ echo '
        user alice
        topic readwrite #
        ' > mosquitto.acl
        $
        ```

1. MQTT サーバーを立ち上げる。

    ```
    $src/mosquitto -c mosquitto.conf
    1570609944: mosquitto version 1.6.2 starting
    1570609944: Config loaded from mosquitto.conf.
    1570609944: Opening ipv4 listen socket on port 1883.
    1570609944: Opening ipv6 listen socket on port 1883.
    ```

### 家訓

SINETStream はすでにインストールされているものとする。

1. `.sinetstream_config.yml` を編集する。

    ```
    $ echo '
    local-kafka-authn-plain:
      type: kafka
      brokers:
        - localhost:9099
      security_protocol: SASL_PLAINTEXT
      sasl_mechanism: PLAIN
      sasl_plain_username: alice
      sasl_plain_password: alice-secret

    local-mqtt-authn:
      type: mqtt
      brokers:
        - localhost:1883
      username_pw_set:
        username: alice
        password: alice-secret
    ' >> .sinetstream_config.yml
    $ 
    ```

1. Kafka での確認

    1. コンシューマーを実行する。

        1. Python:

            ```
            $ python36 ~/path/to/sinetstream-x.x.x/python/sample/text/consumer.py -s local-kafka-authn-plain -t test1
            # service=local-kafka-authn-plain
            # topics=test1
            # group-id=None
            ```        

            メッセージを受信すると:
            
            ```
            topic=test1 value(utf-8)='hogehoge'
            ```

        1. Java:

            ```
            $ /path/to/sinetstream-x.x.x/java/sample/text/text-producer-x.x.x/bin/text-consumer -s local-kafka-authn-plain -t test1
            ```

            メッセージを受信すると:

            ```
            hogehoge
            ```

    1. プロデューサーを実行する。

        1. Python:

            ```
            $ echo "hogehoge" | python36 ~/repo/sinetstream-x.x.x/python/sample/text/producer.py -s local-kafka-authn-plain -t test1
            # service=local-kafka-authn-plain
            # topic=test1
            $
            ```
            
        1. Java:

            ```
            $ echo "hogehoge" | /path/to/sinetstream-x.x.x/java/sample/text/text-producer-x.x.x/bin/text-producer -s local-kafka-authn-plain -t test1
            $
            ```

1. MQTT での確認

    1. コンシューマーを実行する。

        1. Python:

            ```
            $ python36 ~/repo/sinetstream-x.x.x/python/sample/text/consumer.py -s local-mqtt-authn -t test1
            # service=local-mqtt-authn
            # topics=test1
            # group-id=None
            ```        

            メッセージを受信すると:
            
            ```
            topic=test1 value(utf-8)='hogehoge'
            ```

        1. Java:

            ```
            $ /path/to/sinetstream-x.x.x/java/sample/text/text-producer-x.x.x/bin/text-consumer -s local-mqtt-authn -t test1
            ```

            メッセージを受信すると:

            ```
            hogehoge
            ```

    1. プロデューサーを実行する。

        1. Python:

            ```
            [koie@vm00 tmp]$ echo "hogehoge" | python36 ~/repo/sinetstream-x.x.x/python/sample/text/producer.py -s local-mqtt-authn -t test1
            # service=local-mqtt-authn
            # topic=test1
            $
            ```

        1. Java:

            ```
            $ echo "hogehoge" | /path/to/sinetstream-x.x.x/java/sample/text/text-producer-x.x.x/bin/text-producer -s local-mqtt-authn -t test1
            $
            ```

## 制限事項

* エラー時の処理は使用しているライブラリに依存しており、どのようにユーザに通知されるか統一されていない。

| Platform | When | Library | Behavior |
| --- | --- | --- | --- |
| Kafka | AuthN | Python | Exception thrown (kafka.errors.NoBrokersAvailable -> sinetstream.api.ConnectionError) |
| Kafka | AuthN | Java | nothing |
| Kafka | AuthZ | Python | [producer] Exception thrown on metadata updating failrue (kafka.errors.KafkaTimeoutError), [consumer] nothing |
| Kafka | AuthZ | Java | nothing |
| MQTT | AuthN | Python | just logged |
| MQTT | AuthN | Java | Exception thrown (jp.ad.sinet.stream.api.ConnectionException) |
| MQTT | AuthZ | Python | nothing |
| MQTT | AuthZ | Java | nothing |
