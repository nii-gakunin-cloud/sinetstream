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

[English](mqtt-authentication-password.en.md)

**準備中** (2020-06-05 18:05:13 JST)

# SINETStreamからMQTTのパスワード認証を利用する

## 概要

パスワード認証をおこなうように設定をされた MQTT ブローカーを
SINETStreamから利用する方法について説明する。

この文書のおもな記述の流れを以下に示す。

1. 設定手順の前提条件について
1. MQTTブローカー(Mosquitto)にパスワード認証を設定する手順について
1. SINETStreamからパスワード認証を行うMQTTブローカーにアクセスする手順について
1. 認証エラーとなった場合のSINETStreamの挙動について

## 前提条件

MQTTブローカーの設定については様々な状況が想定される。
設定手順の記述を簡潔にするために、ここでは以下の前提条件をおく。

* MQTTブローカーは[Mosquitto](https://mosquitto.org/)を用いる
* MQTTブローカーに対する接続はSSL/TLSとする
* CA証明書は事前にプライベート認証局において作成されているものとする
* PEM形式の証明書は事前に作成されているものとする(*)

(*) 動作確認用の証明書の作成手順については「[プライベート認証局における証明書の作成手順](certificate.md)」を参照のこと。

設定例を示す場合のホスト名などの値を以下に示す。
> 実際に設定を行う際は、以下の値に対応する箇所を環境に合わせて適宜変更すること。

* MQTTブローカー
    * ホスト名
        * broker.example.org
    * ポート番号
        * 8884
    * ユーザ名/パスワード
        * `user01`/`user01-pass`
        * `user02`/`user02-pass`
        * `user03`/`user03-pass`
    * Mosquittoの設定ファイル
        * /etc/mosquitto/mosquitto.conf
    * パスワードファイル
        * /etc/mosquitto/pwfile
* 証明書（MQTTブローカー環境）
    * CA証明書
        * 証明書ファイルのパス
            * /etc/pki/CA/cacert.pem
    * MQTTブローカーのサーバ証明書
        * 証明書ファイルのパス
            * /etc/pki/CA/certs/broker.crt
        * 秘密鍵のパス
            * /etc/pki/CA/private/broker.key
* 証明書（クライアント環境）
    * CA証明書
        * /opt/certs/cacert.pem

## ブローカー側の設定手順

MQTTブローカー(Mosquitto)にパスワード認証を設定する手順について説明する。
また、SSL/TLS接続に関する設定もあわせて行う。

以下の手順で設定を行う。
1. MQTTブローカーの設定ファイルを更新する
1. パスワードファイルにユーザ名、パスワードを登録する
1. MQTTブローカーに設定ファイルの再読み込みを行わせる

### 設定ファイルにパスワード認証の設定を追加する

以下の内容を追加する。

```
per_listener_settings true
listener 8884
password_file /etc/mosquitto/pwfile
cafile /etc/pki/CA/cacert.pem
certfile /etc/pki/CA/certs/broker.crt
keyfile /etc/pki/CA/private/broker.key
require_certificate false
```

各項目の簡単な設定内容について以下に示す。

* `per_listener_settings`
    * `true`の場合、認証とアクセス制御の設定がリスナー毎に制御される
* `listener`
    * リッスンポート番号の指定
* `password_file`
    * パスワードファイルの指定
* `cafile`
    * CA証明書(PEM)のファイル名
* `certfile`
    * サーバ証明書(PEM)のファイル名
* `keyfile`
    * サーバ証明書の秘密鍵
* `require_certificate`
    * `false`の場合、接続時にクライアント証明書を要求しない

### パスワードファイルにユーザ名、パスワードを登録する

`mosquitto_passwd`コマンドを利用しユーザ名、パスワードを登録する。

```bash
$ sudo touch /etc/mosquitto/pwfile
$ sudo mosquitto_passwd -b /etc/mosquitto/pwfile user01 user01-pass
$ sudo mosquitto_passwd -b /etc/mosquitto/pwfile user02 user02-pass
$ sudo mosquitto_passwd -b /etc/mosquitto/pwfile user03 user03-pass
```

### 設定ファイルの再読み込みを行わせる

設定ファイルの再読み込みを行わせるために`SIGHUP`シグナルを送る。

```bash
$ sudo killall -HUP mosquitto
```

## SINETStream（クライアント側）の設定手順

SINETStreamからパスワード認証を行うMQTTブローカーを利用するための設定について説明する。
以下の作業が必要となる。

1. 証明書の準備
1. SINETStreamの設定ファイルの作成
1. SINETStreamを利用するプログラムの作成

### 証明書の準備

認証には直接関係しないが、SSL/TLS接続を利用するので必要となる証明書をクライアント環境に準備する。
以下のものが必要となる。

* CA証明書

プライベート認証局などで作成した証明書をクライアント環境に配置する。
SINETStreamでは証明書の配置場所を定めてはいないので、配置する場所は利用者の判断に
ゆだねられる。SINETStreamは、設定ファイルに記されたパスから証明書を読み込む。

### SINETStreamの設定ファイルを作成する

設定ファイルの例を以下に示す。

```yaml
service-mqtt-password:
  brokers: broker.example.org:9093
  type: mqtt
  topic: topic-001
  tls:
    ca_certs: /opt/certs/cacert.pem
  username_pw_set:
    username: user01
    password: user01-pass
```

`brokers`, `type`, `topic` `tls` については認証を利用しない場合の設定ファイルと同様なので
説明を省く。 パスワード認証に関わる設定は `username_pw_set:` 以降の行が該当する。
各パラメータの意味を以下に示す。

* `username`
    * ユーザ名
* `password`
    * パスワード

### SINETStreamを利用するプログラムを作成する

SINETStreamを利用するプログラム自体は、パスワード認証を行うMQTTブローカーを利用する場合
と認証なしのMQTTブローカーを利用する場合で変わりはない。

Python APIの　`MessageWriter` を利用する場合の例を以下に示す。認証に関わる処理は存在していない。

```python
with sinetstream.MessageWriter(service='service-mqtt-password') as writer:
    writer.publish(b'message 001')
```

認証情報をプログラムから設定したい場合は、コンストラクタの引数に認証情報のパラメータを追加すればよい。

```python
user_password = {
    'username': 'user01',
    'password': 'user01-pass',
}
with sinetstream.MessageWriter(service='service-mqtt', username_pw_set=user_password) as writer:
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
