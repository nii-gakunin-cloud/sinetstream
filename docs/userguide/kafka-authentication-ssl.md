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

[English](kafka-authentication-ssl.en.md)

# SINETStreamからKafkaのSSL/TLS認証（クライアント認証）を利用する

## 概要

SSL/TLSの双方向認証をおこなうように設定をされた Kafaka ブローカーを
SINETStreamから利用する方法について説明する。

この文書のおもな記述の流れを以下に示す。

1. 設定手順の前提条件について
1. KafkaブローカーにSSL/TLSの双方向認証を設定する手順について
1. SINETStreamからSSL/TLSの双方向認証を行うKafkaブローカーにアクセスする手順について
1. 認証エラーとなった場合のSINETStreamの挙動について

## 前提条件

Kafkaブローカーの構成やSSL/TLSの設定については様々な状況が想定される。
設定手順の記述を簡潔にするために、ここでは以下の前提条件をおく。

* Kafkaブローカーは1ノード構成とする
* KafkaブローカーのトラストストアにCA証明書を登録し、このCA証明書によって署名された証明書を全て信頼する
* CA証明書は事前にプライベート認証局において作成されているものとする
* PEM形式の証明書は事前に作成されているものとする(*)

(*) 動作確認用の証明書の作成手順については「[プライベート認証局における証明書の作成手順](certificate.md)」を参照のこと。

設定例を示す場合のホスト名などの値を以下に示す。
> 実際に設定を行う際は、以下の値に対応する箇所を環境に合わせて適宜変更すること。

* Kafkaブローカー
    * ホスト名
        * broker.example.org
    * ポート番号
        * 9093
    * Kafkaブローカーをインストールしたディレクトリ
        * /srv/kafka
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
    * クライアント認証の証明書
        * /home/user01/certs/client0.crt
    * クライアント認証の秘密鍵
        * /home/user01/certs/client0.key

## ブローカー側の設定手順

KafkaブローカーにSSL/TLSの双方向認証を設定する手順について説明する。

以下の手順で設定を行う。
1. Kafkaブローカーで読み込めるようにするためにPEM形式の証明書、秘密鍵をPKCS#12形式に変換する
1. KafkaブローカーのSSL/TLS認証に関するプロパティを設定ファイルに記述する

### 証明書、秘密鍵のファイルを PKCS#12 形式に変換する

Kafkaブローカーが読み込む形式に合わせて証明書の形式を変換する。

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

### SSL/TLS認証に関するプロパティを設定ファイルに記述する

Kafkaブローカーのプロパティファイル `/srv/kafka/config/server.properties` に
以下の内容を追加する。

```properties
listeners=SSL://:9093
advertised.listeners=SSL://broker.example.org:9093
ssl.truststore.location=/srv/kafka/config/cert/truststore.p12
ssl.truststore.password=trust-pass-00
ssl.truststore.type=pkcs12
ssl.keystore.location=/srv/kafka/config/cert/keystore.p12
ssl.keystore.password=key-pass-00
ssl.keystore.type=pkcs12
ssl.client.auth=required
```

追加したプロパティに関する簡単な説明を以下に記す。

* `listeners`, `advertised.listeners`
    * `SSL://` のエントリを追加することによりKafkaブローカーはSSL/TLS接続のサービスをKafkaクライアントに提供する
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
* `ssl.client.auth`
    * クライアント認証を行うかどうかの設定
        * `required`: クライアント認証を行う
        * `requested`: クライアント認証をオプションとする
        * `none`: クライアント認証を行わない

プロパティファイルの変更内容をKafkaブローカーに反映させるため、ブローカーを再起動する。

```bash
$ sudo /srv/kafka/bin/kafka-server-stop.sh
$ sudo /srv/kafka/bin/kafka-server-start.sh /srv/kafka/config/server.properties
```

> サービスを中断することなく設定変更を行うにはKafkaブローカーを複数台構成とし、
> ローリング再起動による設定変更の反映を行う必要がある。

## SINETStream（クライアント側）の設定手順

SINETStreamからSSL/TLS認証を行うKafkaブローカーを利用するための設定について説明する。
以下の作業が必要となる。

1. 証明書の準備
1. SINETStreamの設定ファイルの作成
1. SINETStreamを利用するプログラムの作成

### 証明書の準備

SSL/TLSの双方向認証を利用するのに必要となる証明書類をクライアント環境に準備する。
以下のものが必要となる。

* クライアント認証の証明書
* クライアント認証の秘密鍵
* CA証明書

プライベート認証局などで作成した証明書をクライアント環境に配置する。
SINETStreamでは証明書の配置場所を定めてはいないので、配置する場所は利用者の判断に
ゆだねられる。SINETStreamは、設定ファイルに記されたパスから証明書を読み込む。

### SINETStreamの設定ファイルを作成する

設定ファイルの例を以下に示す。

```yaml
service-kafka-ssl:
  brokers: broker.example.org:9093
  type: kafka
  topic: topic-001
  tls:
    ca_certs: /opt/certs/cacert.pem
    certfile: /home/user01/certs/client0.crt
    keyfile: /home/user01/certs/client0.key
```

`brokers`, `type`, `topic` については認証を利用しない場合の設定ファイルと同様なので
説明を省く。 SSL/TLS認証に関わる設定は `tls:` 以降の行が該当する。
各パラメータの意味を以下に示す。

* `ca_certs`
    * CA 証明書ファイル (PEM) のパス
* `certfile`
    * クライアント証明書 (PEM) のパス
* `keyfile`
    * 秘密鍵 (PEM) のパス

### SINETStreamを利用するプログラムを作成する

SINETStreamを利用するプログラム自体は、SSL/TLS認証を行うKafkaブローカーを利用する場合
と認証なしのKafkaブローカーを利用する場合で変わりはない。

Python APIの　`MessageWriter` を利用する場合の例を以下に示す。認証に関わる処理は存在していない。

```python
with sinetstream.MessageWriter(service='service-kafka-ssl') as writer:
    writer.publish(b'message 001')
```

認証情報をプログラムから設定したい場合は、コンストラクタの引数に認証情報のパラメータを追加すればよい。

```python
tls = {
    'ca_certs': '/opt/certs/cacert.pem',
    'certfile': '/home/user01/certs/client0.crt',
    'keyfile': '/home/user01/certs/client0.key',
}
with sinetstream.MessageWriter(service='service-kafka', tls=tls) as writer:
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
