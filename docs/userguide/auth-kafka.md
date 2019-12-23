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

SINETStream ユーザガイド

# Kafkaの認証と認可

## Kafkaの認証の設定

Kafka で認証を行うには、以下の作業が必要である。

* Kafka ブローカの設定ファイル `server.properties` に認証方式の設定を書く
* Kafka ブローカにユーザを追加する
* SINETStream クライアントの設定ファイル `.sinetstream_config.yml` を書く

Kafka では、クライアントとブローカの間の通信方式を指定する security_protocol と、
認証方式を指定する sasl_mechanism の、二つのパラメータを組み合せて設定する。

* SINETStream がサポートする security_protocol と sasl_mechanism

    | security_protocol | Java | Python | 補足 |
    | --- | --- | --- | --- |
    | PLAINTEXT | サポート | サポート | 認証なし・暗号化なし |
    | SSL | サポート | サポート | 認証あり (有効時のみ)・暗号化あり |
    | SASL_PLAINTEXT | サポート | サポート | 認証あり・暗号化なし |
    | SASL_SSL | サポート | サポート | 認証あり・暗号化あり |

* SINETStream がサポートする sasl_mechanism

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
    | SSL | (なし) | あり (クライアント証明書による)/なし | あり |
    | SASL_SSL | PLAIN | あり | あり |
    | SASL_SSL | SCRAM-SHA-* | あり | あり |

### Kafka ブローカの認証方式の設定

Kafka ブローカの設定ファイル `server.properties` を認証方式に合わせて編集する。
以下のポート番号 `9092` は変更してよい。

#### security_protocol=PLAINTEXT

```
listeners=PLAINTEXT://:9092
```

#### security_protocol=SSL

```
listeners=SSL://:9092
```

もしクライアント認証が必要なら:

```
listeners=SSL://:9092
ssl.client.auth=required
```

#### security_protocol=SASL_PLAINTEXT

```
listeners=SASL_PLAINTEXT://:9092
```

#### security_protocol=SASL_SSL

```
listeners=SASL_SSL://:9092
```

#### sasl_mechanism=PLAIN

```
sasl.mechanism.inter.broker.protocol=PLAIN
sasl.enabled.mechanisms=PLAIN
```

起動スクリプトを編集して `kafka_server_jss.conf` を読み込むようにする。

```
EXTRA_ARGS=-Djava.security.auth.login.config=config/kafka_server_jaas.conf bin/kafka-server-start.sh config/server.properties
```

`kafka_server_jaas.conf` を編集して、ブローカー間通信用のユーザ名とパスワードを設定する。
パスワードは適切なものに変更すること。

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

ユーザを登録する行のフォーマットは `user_<USER>="<PASSWORD>"` の形式である。
上記例ではユーザーが alice 、パスワードが alice-secret である。

ユーザを追加/削除するために `kafka_server_jaas.conf` を編集したあとは、設定を再読み込みするために Kafka ブローカーを再起動しなければならない。

#### sasl_mechanism=SCRAM-SHA-*

```
sasl.mechanism.inter.broker.protocol=SCRAM-SHA-256,SCRAM-SHA-512
sasl.enabled.mechanisms=SCRAM-SHA-256,SCRAM-SHA-512
```

ブローカー間通信用のユーザ名とパスワードを設定する。

```
$ bin/kafka-configs.sh --zookeeper localhost:2181 --alter --add-config 'SCRAM-SHA-256=[password=admin-secret],SCRAM-SHA-512=[password=admin-secret]' --entity-type users --entity-name admin
```

ユーザとそのパスワードを設定するには専用のスクリプトを利用して ZooKeeper に認証情報を登録する。

```
$ bin/kafka-configs.sh --zookeeper localhost:2181 --alter --add-config 'SCRAM-SHA-256=[iterations=8192,password=alice-secret],SCRAM-SHA-512=[password=alice-secret]' --entity-type users --entity-name alice
```

sasl_mechanism=PLAIN と異なり、ブローカーを再起動する必要はない。

### Kafka 使用時の SINETStream クライアントの設定

`.sinetstream_config.yml` に設定を書く。

* 以下のポート番号 `9092` はブローカーの設定に合わせること。
* 以下のブローカーホスト `localhost` はブローカーのホスト名に合わせること。
* 以下の `service-foo` は変更してよい。

#### security_protocol=PLAINTEXT

```
service-foo:
    broker:
	- localhost:9092
```

#### security_protocol=SSL

```
service-foo:
    broker:
	- localhost:9092
    tls: true
```

もしプライベート CA を使っている場合は以下のように CA 証明書を指定する。

```
service-foo:
    broker:
	- localhost:9092
    tls:
	- ca_certs: /path/to/ca.pem
```

#### security_protocol=SASL_PLAINTEXT

```
service-foo:
    broker:
	- localhost:9092
    security_protocol: SASL_PLAINTEXT
```

#### security_protocol=SASL_SSL

```
service-foo:
    broker:
	- localhost:9092
    tls: true
    security_protocol: SASL_SSL
```

#### sasl_mechanism=PLAIN

```
service-foo:
    ...
    sasl_mechanism: PLAIN
    sasl_plain_username: alice
    sasl_plain_password: alice-secret
```

#### sasl_mechanism=SCRAM-SHA-*

```
service-foo:
    ...
    sasl_mechanism: SCRAM-SHA-512
    sasl_plain_username: alice
    sasl_plain_password: alice-secret
```

## Kafkaの認可の設定

Kafka ブローカの設定ファイル `server.properties` に ACL を使う設定を追加して、
個々のユーザごとに許可する操作を設定 (ACL に追加) する。

認可の情報は ZooKeeper に登録されるので、認可を追加したあとに Kafka ブローカーを再起動する必要はない。

### `server.properties` に設定を追加したあとブローカーを再起動する

```
authorizer.class.name=kafka.security.auth.SimpleAclAuthorizer
super.users=User:admin
```

### Writerを追加する方法

`USER` が任意のトピックをWriteできるようにするには以下のコマンドを実行する。

```
kafka-acls --authorizer-properties zookeeper.connect=localhost:2181 --add --allow-principal User:<USER> --producer --topic "*"
```

### Readerを追加する方法

`USER` が任意のトピックをReadできるようにするには以下のコマンドを実行する。

```
kafka-acls --authorizer-properties zookeeper.connect=localhost:2181 --add --allow-principal User:<USER> --consumer --topic "*" --group "*"
```

### 設定したACLを確認する方法

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

## 参考

* https://docs.confluent.io/2.0.0/kafka/sasl.html
* http://kafka.apache.org/documentation/#security_sasl_plain_clientconfig
* https://docs.confluent.io/current/kafka/authorization.html

## Kafka設定例

### Kafka ブローカーの設定

#### Kafka のリリース版をダウンロードして展開する

[Download the code](https://kafka.apache.org/quickstart#quickstart_download)

```
$ wget -q http://ftp.jaist.ac.jp/pub/apache/kafka/2.3.0/kafka_2.12-2.3.0.tgz
$ tar -xzf kafka_2.12-2.3.0.tgz
$ cd kafka_2.12-2.3.0
$ ls
bin/  config/  libs/  LICENSE  NOTICE  site-docs/
$
```

#### 設定ファイルを編集する

##### Kafka ブローカーのプロパティファイルを編集する

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

##### ZooKeeper のプロパティファイルを編集する

```
$ echo '
authProvider.1 = org.apache.zookeeper.server.auth.SASLAuthenticationProvider
' >> config/zookeeper.properties
$
```

##### Kafka ブローカーの [JAAS](https://docs.oracle.com/javase/jp/8/docs/technotes/guides/security/jaas/JAASRefGuide.html) ファイルを編集する

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

##### ZooKeeper の [JAAS](https://docs.oracle.com/javase/jp/8/docs/technotes/guides/security/jaas/JAASRefGuide.html) ファイルを編集する

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

#### ブローカーを立ち上げる

##### ZooKeeper を立ち上げる

```
$ env KAFKA_OPTS="-Djava.security.auth.login.config=config/zookeeper_jaas.conf" bin/zookeeper-server-start.sh config/zookeeper.properties
[2019-10-09 16:56:04,785] INFO Reading configuration from: config/zookeeper.properties (org.apache.zookeeper.server.quorum.QuorumPeerConfig)
...
[2019-10-09 16:56:04,837] INFO Server successfully logged in. (org.apache.zookeeper.Login)
[2019-10-09 16:56:04,841] INFO binding to port 0.0.0.0/0.0.0.0:2181 (org.apache.zookeeper.server.NIOServerCnxnFactory)
```

##### Kafka ブローカーを立ち上げる

```
$ env EXTRA_ARGS="-Djava.security.auth.login.config=config/kafka_server_jaas.conf" bin/kafka-server-start.sh config/server.properties
[2019-10-09 16:58:52,376] INFO Registered kafka:type=kafka.Log4jController MBean (kafka.utils.Log4jControllerRegistration$)
...
```

##### ブローカーが立ち上がっているのを確認する

```
$ ss --listening --numeric |grep -w -e 2181 -e 9092 -e 9099
tcp    LISTEN     0      50       :::9099                 :::*
tcp    LISTEN     0      50       :::9092                 :::*
tcp    LISTEN     0      50       :::2181                 :::*
$
```

### 動作確認

SINETStream はすでにインストールされているものとする。

#### `.sinetstream_config.yml` を編集する

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
  topic: test1
  value_type: text
' >> .sinetstream_config.yml
$
```

#### Readerを実行する

Python:

```
$ python3 ~/path/to/sinetstream-x.x.x/python/sample/text/consumer.py -s local-kafka-authn-plain
# service=local-kafka-authn-plain
# topics=test1
# group-id=None
```

メッセージを受信すると:

```
topic=test1 value(utf-8)='hogehoge'
```

Java:

```
$ /path/to/sinetstream-x.x.x/java/sample/text/text-producer-x.x.x/bin/text-consumer -s local-kafka-authn-plain
```

メッセージを受信すると:

```
hogehoge
```

#### Writerを実行する

Python:

```
$ echo "hogehoge" | python3 ~/repo/sinetstream-x.x.x/python/sample/text/producer.py -s local-kafka-authn-plain
# service=local-kafka-authn-plain
# topic=test1
$
```

Java:

```
$ echo "hogehoge" | /path/to/sinetstream-x.x.x/java/sample/text/text-producer-x.x.x/bin/text-producer -s local-kafka-authn-plain
$
```

