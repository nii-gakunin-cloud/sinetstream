**準備中** (2020-06-04 18:27:50 JST)

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

[English](kafka-authentication-sasl-scram.en.md)

# SINETStreamからKafkaのSASL/SCRAM認証を利用する

## 概要

SASL/SCRAM認証をおこなうように設定をされた Kafaka ブローカーを
SINETStreamから利用する方法について説明する。

この文書のおもな記述の流れを以下に示す。

1. 設定手順の前提条件について
1. KafkaブローカーにSASL/SCRAM認証を設定する手順について
1. SINETStreamからSASL/SCRAM認証を行うKafkaブローカーにアクセスする手順について
1. 認証エラーとなった場合のSINETStreamの挙動について

## 前提条件

Kafkaブローカーの構成や設定については様々な状況が想定される。
設定手順の記述を簡潔にするために、ここでは以下の前提条件をおく。

* Kafkaブローカーは1ノード構成とする
* KafkaブローカーはSASL/SCRAM-SHA-256認証のサービスだけを提供する
* Kafkaブローカーに対する接続はSSL/TLSとする
* KafkaブローカーのトラストストアにCA証明書を登録し、このCA証明書によって署名された証明書を全て信頼する
* CA証明書は事前にプライベート認証局において作成されているものとする
* PEM形式の証明書は事前に作成されているものとする(*)
* ZooKeeperはKafkaブローカーと同じホストで実行しているものとする

(*) 動作確認用の証明書の作成手順については「[プライベート認証局における証明書の作成手順](certificate.md)」を参照のこと。

設定例を示す場合のホスト名などの値を以下に示す。
> 実際に設定を行う際は、以下の値に対応する箇所を環境に合わせて適宜変更すること。
> また `SCRAM-SHA-512`を利用する場合は `SCRAM-SHA-256` となっている箇所を`SCRAM-SHA-512`に変更すること。

* Kafkaブローカー
    * ホスト名
        * broker.example.org
    * ポート番号
        * 9094
    * 認証
        * SCRAM-SHA-256
    * Kafkaブローカーをインストールしたディレクトリ
        * /srv/kafka
    * ユーザ名/パスワード
        * `user01`/`user01-pass`
        * `user02`/`user02-pass`
        * `user03`/`user03-pass`
    * ブローカーのプロパティファイルのパス
        * /srv/kafka/config/server.properties
    * トラストストア
        * ファイルのパス
            * /srv/kafka/config/cert/truststore.p12
        * パスワード
            * trust-pass-00
        * CA証明書をトラストストアに登録する際の名前
            * private-ca
    * キーストア
        * ファイルのパス
            * /srv/kafka/config/cert/keystore.p12
        * パスワード
            * key-pass-00
        * Kafkaブローカーのサーバ証明書を登録する際の名前
            * broker
* ZooKeeper
    * ホスト名
        * broker.example.org
    * ポート番号
        * 2181
* 証明書（Kafkaブローカー環境）
    * CA証明書
        * 証明書ファイルのパス
            * /etc/pki/CA/cacert.pem
        * 秘密鍵のパス
            * /etc/pki/CA/private/cakey.pem
    * Kafkaブローカーのサーバ証明書
        * 証明書ファイルのパス
            * /etc/pki/CA/certs/broker.crt
        * 秘密鍵のパス
            * /etc/pki/CA/private/broker.key
* 証明書（クライアント環境）
    * CA証明書
        * /opt/certs/cacert.pem

## ブローカー側の設定手順

KafkaブローカーにSASL/SCRAM-SHA-256 認証を設定する手順について説明する。
また、SSL/TLS接続に関する設定もあわせて行う。

以下の手順で設定を行う。
1. KafkaブローカーのSASL認証に関するプロパティを設定ファイルに記述する
1. ZooKeeperにSCRAM資格情報を登録する
1. Kafkaブローカーで読み込めるようにするためにPEM形式の証明書、秘密鍵をPKCS#12形式に変換する
1. KafkaブローカーのSSL/TLS接続に関するプロパティを設定ファイルに記述する

### SASL認証に関するプロパティを設定ファイルに記述する

Kafkaブローカーのプロパティファイル `/srv/kafka/config/server.properties` に
以下の内容を追加する。

```properties
listeners=SASL_SSL://:9094
advertised.listeners=SASL_SSL://broker.example.org:9094
sasl.enabled.mechanisms=SCRAM-SHA-256
listener.name.sasl_ssl.scram-sha-256.sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required;
```

追加したプロパティに関する簡単な説明を以下に記す。

* `listeners`, `advertised.listeners`
    * `SASL_SSL://` のエントリを追加することによりKafkaブローカーはSASL認証かつSSL/TLS接続のサービスをKafkaクライアントに提供する
* `sasl.enabled.mechanisms`
    * 有効にするSASLのメカニズムを指定する
    * SASL/SCRAM-SHA-256 の場合 `SCRAM-SHA-256` を指定する
* `listener.name.sasl_ssl.scram-sha-256.sasl.jaas.config`
    * SASL接続のJAASログインコンテキストパラメータを指定する
    * `SASL_SSL`接続のSASL/SCRAM-SHA-256認証の場合、プロパティ名のプレフィックスに`listener.name.sasl_ssl.scram-sha-256`がつく

### ZooKeeperにSCRAM資格情報を登録する

`kafka-configs.sh`コマンドを用いてZooKeeperにSCRAM資格情報を登録する。
`--entity-name`にユーザ名を`--add-config`の`password`パラメータにパスワードを指定する。

```bash
$ /srv/kafka/bin/bin/kafka-configs.sh --zookeeper broker.example.org:2181 --alter --entity-type users \
      --entity-name user01 --add-config 'SCRAM-SHA-256=[iterations=8192,password=user01-pass]'
$ /srv/kafka/bin/bin/kafka-configs.sh --zookeeper broker.example.org:2181 --alter --entity-type users \
      --entity-name user02 --add-config 'SCRAM-SHA-256=[iterations=8192,password=user02-pass]'
$ /srv/kafka/bin/bin/kafka-configs.sh --zookeeper broker.example.org:2181 --alter --entity-type users \
      --entity-name user03 --add-config 'SCRAM-SHA-256=[iterations=8192,password=user03-pass]'
```

### 証明書、秘密鍵のファイルを PKCS#12 形式に変換する

SSL/TLS接続のための証明書を、Kafkaブローカーが読み込む形式に合わせて変換する。

トラストストアにはCA証明書とその秘密鍵を格納する。
キーストアにはKafkaブローカーのサーバ証明書とその秘密鍵を格納する。
トラストストア、キーストアともに PKCS#12形式のファイルとする。

まずトラストストアを作成する。`openssl pkcs12`コマンドを用いてPEM形式の証明書、秘密鍵をPKCS#12形式のファイルに変換する。
`-in`, `-inkey` でCA証明書とその秘密鍵のファイル名を指定する。`-out`で出力先のファイル名を指定し、`-passout`
でトラストストアに設定するパスワードを指定する。

```bash
$ sudo mkdir -p /srv/kafka/config/cert
$ sudo openssl pkcs12 -export -in /etc/pki/CA/cacert.pem \
         -inkey /etc/pki/CA/private/cakey.pem -name private-ca \
         -CAfile /etc/pki/CA/cacert.pem -caname private-ca \
         -out /srv/kafka/config/cert/truststore.p12 \
         -passout pass:trust-pass-00
```

次に同様の手順でキーストアを作成する。キーストアにはブローカーのサーバ証明書を格納するので、
`-in`, `-inkey`にはサーバ証明書とその秘密鍵のファイル名を指定する。また`-passout`には
キーストアに設定するパスワードを指定する。

```bash
$ sudo openssl pkcs12 -export -in /etc/pki/CA/certs/broker.crt \
         -inkey /etc/pki/CA/private/broker.key -name broker \
         -CAfile /etc/pki/CA/cacert.pem -caname private-ca \
         -out /srv/kafka/config/cert/keystore.p12 \
         -passout pass:key-pass-00
```

### SSL/TLS接続に関するプロパティを設定ファイルに記述する

Kafkaブローカーのプロパティファイル `/srv/kafka/config/server.properties` に
以下の内容を追加する。

```properties
ssl.truststore.location=/srv/kafka/config/cert/truststore.p12
ssl.truststore.password=trust-pass-00
ssl.truststore.type=pkcs12
ssl.keystore.location=/srv/kafka/config/cert/keystore.p12
ssl.keystore.password=key-pass-00
ssl.keystore.type=pkcs12
```

追加したプロパティに関する簡単な説明を以下に記す。

* `ssl.truststore.location`
    * トラストストアのファイル名
* `ssl.truststore.password`
    * トラストストアのパスワード
* `ssl.truststore.type`
    * トラストストアのファイル形式
* `ssl.keystore.location`
    * キーストアのファイル名
* `ssl.keystore.password`
    * キーストアのパスワード
* `ssl.keystore.type`
    * キーストアのファイル形式

### Kafkaブローカーの再起動

プロパティファイルの変更内容をKafkaブローカーに反映させるため、ブローカーを再起動する。

```bash
$ sudo /srv/kafka/bin/kafka-server-stop.sh
$ sudo /srv/kafka/bin/kafka-server-start.sh /srv/kafka/config/server.properties
```

> サービスを中断することなく設定変更を行うにはKafkaブローカーを複数台構成とし、
> ローリング再起動による設定変更の反映を行う必要がある。

## SINETStream（クライアント側）の設定手順

SINETStreamからSASL/SCRAM-SHA-256認証を行うKafkaブローカーを利用するための設定について説明する。

以下の作業が必要となる。

1. 証明書の準備
1. SINETStreamの設定ファイルの作成
1. SINETStreamを利用するプログラムの作成

### 証明書の準備

認証には直接関係しないが、SSL/TLS接続を利用するので必要となる証明書をクライアント環境に準備する。
以下のものが必要となる。

* CA証明書

SINETStreamでは証明書の配置場所を定めてはいないので、配置する場所は利用者の判断に
ゆだねられる。SINETStreamは、設定ファイルに記されたパスから証明書を読み込む。

### SINETStreamの設定ファイルを作成する

設定ファイルの例を以下に示す。

```yaml
service-kafka-sasl-scram:
  brokers: broker.example.org:9094
  type: kafka
  topic: topic-001
  tls:
    ca_certs: /opt/certs/cacert.pem
  security_protocol: SASL_SSL
  sasl_mechanism: SCRAM-SHA-256
  sasl_plain_username: user01
  sasl_plain_password: user01-pass
```

`brokers`, `type`, `topic`, `tls` についてはSASL認証を利用しない場合の設定ファイルと同様なので説明を省く。

SASL認証に関するパラメータの設定内容について以下に示す。

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

SINETStreamを利用するプログラム自体は、SASL/SCRAM認証を行うKafkaブローカーを利用する場合
と認証なしのKafkaブローカーを利用する場合で変わりはない。

Python APIの　`MessageWriter` を利用する場合の例を以下に示す。認証に関わる処理は存在していない。

```python
with sinetstream.MessageWriter(service='service-kafka-sasl-scram') as writer:
    writer.publish(b'message 001')
```

認証情報をプログラムから設定したい場合は、コンストラクタの引数に認証情報のパラメータを追加すればよい。

```python
user_passwd = {
    'sasl_plain_username': 'user01',
    'sasl_plain_password': 'user01-pass',
}
with sinetstream.MessageWriter(service='service-kafka-sasl-scram', **user_passwd) as writer:
    writer.publish(b'message 001')
```

## 認証エラーとなる場合の挙動について

### Python API

認証でエラーになった場合、例外 `sinetstream.error.ConnectionError` が発生する。
例外が発生するメソッドを以下に示す。

* `sinetstream.MessageWriter.__enter__()`
* `sinetstream.MessageWriter.open()`
* `sinetstream.MessageReader.__enter__()`
* `sinetstream.MessageReader.open()`

### Java API

認証でエラーになった場合、例外 `jp.ad.sinet.stream.api.AuthenticationException`
が発生する。例外が発生するメソッドを以下に示す。

* `jp.ad.sinet.stream.utils.MessageWriterFactory#getWriter()`
* `jp.ad.sinet.stream.utils.MessageReaderFactory#getReader()`
