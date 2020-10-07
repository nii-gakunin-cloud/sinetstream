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

[English](kafka-authorization.en.md)

# SINETStreamから認可の設定が行われているKafkaブローカーを利用する

## 概要

認可をおこなうように設定をされた Kafaka ブローカーを
SINETStreamから利用する方法について説明する。

この文書のおもな記述の流れを以下に示す。

1. 設定手順の前提条件について
1. Kafkaブローカーに認可の設定を行う手順について
1. SINETStreamから認可が設定されたKafkaブローカーにアクセスする手順について
1. 認可エラーとなった場合のSINETStreamの挙動について

## 前提条件

Kafkaブローカーの構成や設定については様々な状況が想定される。
設定手順の記述を簡潔にするために、ここでは以下の前提条件をおく。

* Kafkaブローカーは1ノード構成とする
* ZooKeeperはKafkaブローカーと同一ホストで実行している
* Kafkaブローカーは既にSASL/SCRAM認証の設定が行われているものとする(*1)(*2)
* Kafkaブローカーは既にSSL/TLS接続の設定が行われているものとする(*1)
* ACLの設定対象となるユーザは既に登録されているものとする(*1)

(*1) SASL/SCRAM認証の設定手順については
「[SINETStreamからKafkaのSASL/SCRAM認証を利用する](kafka-authentication-sasl-scram.md)」を参照のこと。

(*2) Kafkaブローカーでの認可の設定手順については SASL/SCRAM認証以外の認証方法を利用する場合も同様の手順となる。

設定例を示す場合のホスト名などの値を以下に示す。
> 実際に設定を行う際は、以下の値に対応する箇所を環境に合わせて適宜変更すること。

* Kafkaブローカー
    * ホスト名
        * broker.example.org
    * ポート番号
        * 9094
    * Kafkaブローカーをインストールしたディレクトリ
        * /srv/kafka
    * ブローカーのプロパティファイルのパス
        * /srv/kafka/config/server.properties
    * ユーザ名/パスワード
        * `user01`/`user01-pass`
            * 権限: write
        * `user02`/`user02-pass`
            * 権限: read
        * `user03`/`user03-pass`
            * 権限: read, write
* ZooKeeper
    * ホスト名
        * broker.example.org
    * ポート番号
        * 2181
* 証明書（クライアント環境）
    * CA証明書
        * /opt/certs/cacert.pem

## ブローカー側の設定手順

Kafkaブローカーに認可の設定を行う手順について説明する。

以下の手順で設定を行う。
1. Kafkaブローカーの認可に関するプロパティを設定ファイルに記述する
1. ZooKeeperにACL設定を登録する

### 認可に関するプロパティを設定ファイルに記述する

Kafkaブローカーのプロパティファイル `/srv/kafka/config/server.properties` に
以下の内容を追加する。

```properties
authorizer.class.name=kafka.security.auth.SimpleAclAuthorizer
```

`authorizer.class.name`にはKafkaブローカーの認可で使用するクラス名を指定する。
ZooKeeperに記録されているACL設定による認可を行うには
`kafka.security.auth.SimpleAclAuthorizer`を指定する。

必要に応じて以下のプロパティを追加することができる。

```properties
allow.everyone.if.no.acl.found=true
super.users=User:Admin1;User:Admin2
```

上記のプロパティに関する簡単な説明を以下に記す。

* `allow.everyone.if.no.acl.found`
    * ACL設定がない場合、全てのユーザにアクセスを許可するか否か
* `super.users`
    * スーパーユーザのリスト
    * 区切り文字は`;`

プロパティファイルの変更内容をKafkaブローカーに反映させるため、ブローカーを再起動する。

```bash
$ sudo /srv/kafka/bin/kafka-server-stop.sh
$ sudo /srv/kafka/bin/kafka-server-start.sh /srv/kafka/config/server.properties
```

> サービスを中断することなく設定変更を行うにはKafkaブローカーを複数台構成とし、
> ローリング再起動による設定変更の反映を行う必要がある。

### ZooKeeperにACL設定を登録する

ここでは、各ユーザに以下の権限を設定する手順を示す。

| ユーザ名 | 権限 |
| --- | --- |
| user01 | write |
| user02 | read |
| user03 | read, write |

`kafka-acls.sh`コマンドを用いてACLの設定を行う。おもなコマンド引数についての簡単な説明を以下に示す。
* `--authorizer-properties`
    * `zookeeper.connect`
        * ACLの保存先となる ZooKeeperのアドレスを指定する
* `--allow-principal`
    * 認可の対象となるユーザ名を指定する
* `--topic`
    * 認可の対象となるトピック名を指定する
    * `*`を指定すると全てのトピック名が対象となる
* `--group`
    * 認可の対象となるコンシューマグループ名を指定する
    * `*`を指定すると全てのグループ名対象となる
* `--producer`
    * プロデューサーとして利用する場合に必要となる権限がまとめて指定される
* `--consumer`
    * コンシューマとして利用する場合に必要となる権限がまとめて指定される

実際にACLを設定するコマンドを以下に示す。
`user01`に write 権限を設定する。
```bash
$ sudo /srv/kafka/bin/kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 \
       --add --allow-principal User:user03 --producer --topic '*'
```
`user02`に read 権限を設定する。
```bash
$ sudo /srv/kafka/bin/kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 \
       --add --allow-principal User:user02 --consumer --topic '*' --group '*'
```
`user03`に read, write 権限を設定する。
```bash
$ sudo /srv/kafka/bin/kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 \
       --add --allow-principal User:user01 --producer --topic '*'
$ sudo /srv/kafka/bin/kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 \
       --add --allow-principal User:user01 --consumer --topic '*' --group '*'
```

ACL設定がZooKeeperに記録されたことを確認するために、以下のコマンドを実行する。

```bash
$ sudo /srv/kafka/bin/kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 --list
```

以下のような表示が出力されれば、ACLの設定が記録されたことが確認できる。

```
Current ACLs for resource `Group:LITERAL:*`:
        User:user02 has Allow permission for operations: Read from hosts: *
        User:user03 has Allow permission for operations: Read from hosts: *

Current ACLs for resource `Topic:LITERAL:*`:
        User:user03 has Allow permission for operations: Write from hosts: *
        User:user03 has Allow permission for operations: Create from hosts: *
        User:user02 has Allow permission for operations: Read from hosts: *
        User:user01 has Allow permission for operations: Create from hosts: *
        User:user01 has Allow permission for operations: Write from hosts: *
        User:user03 has Allow permission for operations: Describe from hosts: *
        User:user03 has Allow permission for operations: Read from hosts: *
        User:user01 has Allow permission for operations: Describe from hosts: *
        User:user02 has Allow permission for operations: Describe from hosts: *
```

ACL設定方法の詳細についてはApache Kafkaのドキュメント「[Authorization and ACLs](https://kafka.apache.org/documentation/#security_authz)」を参照のこと。

## SINETStream（クライアント側）の設定手順

SINETStreamから認可を行うKafkaブローカーを利用するための設定について説明する。
以下の作業が必要となる。

1. 証明書の準備
1. SINETStreamの設定ファイルの作成
1. SINETStreamを利用するプログラムの作成

### 証明書の準備

認可には直接関係しないが、前提としてSSL/TLS接続を行うブローカーを利用するので
必要となる証明書をクライアント環境に準備する。

* CA証明書

SINETStreamでは証明書の配置場所を定めてはいないので、配置する場所は利用者の判断に
ゆだねられる。SINETStreamは、設定ファイルに記されたパスから証明書を読み込む。

### SINETStreamの設定ファイルを作成する

認可の設定が行われているKafkaブローカーを利用するには、
認可の主体を定めるために認証設定が必要となる。

設定ファイルの例を以下に示す。
> 設定自体は SASL/SCRAM認証の設定と同じ内容になる。

```yaml
service-kafka:
  brokers: broker.example.org:9094
  type: kafka
  topic: topic-001
  consistency: AT_LEAST_ONCE
  tls:
    ca_certs: /opt/certs/cacert.pem
  security_protocol: SASL_SSL
  sasl_mechanism: SCRAM-SHA-256
  sasl_plain_username: user03
  sasl_plain_password: user03-pass
```

`brokers`, `type`, `topic`, `consistency`, `tls` についてはSASL認証を利用しない場合の設定ファイルと同様なので説明を省く。
SASL認証、SSL/TLS接続に関するパラメータの設定内容について以下に示す。

* `security_protocol`
    * ブローカーとの通信プロトコル
    * SSL/TLS接続のSASL認証を利用する場合 `SASL_SSL`を指定する
* `sasl_mechanism`
    * SASLの認証メカニズムの指定
    * SASL/SCRAM-SHA-256の場合 `SCRAM-SHA-256` を指定する
* `sasl_plain_username`
    * SASL/SCRAM認証のユーザ名
* `sasl_plain_password`
    * SASL/SCRAM認証のパスワード

### SINETStreamを利用するプログラムを作成する

SINETStreamを利用するプログラム自体は、認可を行うKafkaブローカーを利用する場合
と認可なしのKafkaブローカーを利用する場合で変わりはない。

Python APIの `MessageWriter` を利用する場合の例を以下に示す。認可に関わる処理は存在していない。

```python
with sinetstream.MessageWriter(service='service-kafka') as writer:
    writer.publish(b'message 001')
```

## 認可エラーとなった場合の挙動について

### Python API

読み込み権限がないトピックに対して `MessageReader`がメッセージの読み込み処理を行うと、例外
`sinetstream.error.AuthorizationError` が発生する。

書き込み権限がないトピックに対して `MessageWriter`が`publish()`でメッセージの書き込み処理を行った場合、
`consistency`の値によって動作が異なる。

* `consistency`=`AT_MOST_ONCE`の場合
    * 権限がないため、メッセージはトピックに書き込まれない
    * ブローカーからの応答を待たずに `publish()` の処理が完了するので、例外が発生しない
* それ以外の場合
    * 権限がないため、メッセージはトピックに書き込まれない
    * 例外 `sinetstream.error.AuthorizationError` が発生する

認可エラーになった場合、例外 `sinetstream.error.AuthorizationError` が発生する。
例外が発生するメソッドを以下に示す。

* `sinetstream.MessageWriter.publish()`
* `sinetstream.MessageReader.__iter___().__next__()`

### Java API

読み込み権限がないトピックに対して `jp.ad.sinet.stream.api.MessageReader#read`が
メッセージの読み込み処理を行うと、例外 `jp.ad.sinet.stream.api.AuthorizationException`
が発生する。

書き込み権限がないトピックに対して `jp.ad.sinet.stream.api.MessageWriter#write`が
メッセージの書き込み処理を行う行った場合、`consistency`の値によって動作が異なる。

* `consistency`=`AT_MOST_ONCE`の場合
    * 権限がないため、メッセージはトピックに書き込まれない
    * ブローカーからの応答を待たずに `write()` の処理が完了するので、例外が発生しない
* それ以外の場合
    * 権限がないため、メッセージはトピックに書き込まれない
    * 例外 `jp.ad.sinet.stream.api.AuthorizationException`が発生する
