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

[English](mqtt-authorization.en.md)

# SINETStreamから認可の設定が行われているMQTTブローカーを利用する

## 概要

認可をおこなうように設定をされた MQTT ブローカーを
SINETStreamから利用する方法について説明する。

**注意: MQTTブローカーは認可のエラーをクライアントに返さない。そのためSINETStreamでは認可のエラーを検出できない。**

この文書のおもな記述の流れを以下に示す。

1. 設定手順の前提条件について
1. MQTTブローカー(Mosquitto)に認可の設定を行う手順について
1. SINETStreamから認可が設定されたMQTTブローカーにアクセスする手順について
1. 認可エラーとなった場合のSINETStreamの挙動について

## 前提条件

MQTTブローカーの構成や設定については様々な状況が想定される。
設定手順の記述を簡潔にするために、ここでは以下の前提条件をおく。

* MQTTブローカーは[Mosquitto](https://mosquitto.org/)を用いる
* MQTTブローカーは既にパスワード認証の設定が行われているものとする(*)
* MQTTブローカーは既にSSL/TLS接続の設定が行われているものとする(*)
* ACLの設定対象となるユーザは既に登録されているものとする(*)

(*) パスワード認証の設定手順については
「[SINETStreamからMQTTのパスワード認証を利用する](mqtt-authentication-password.md)」を参照のこと。

設定例を示す場合のホスト名などの値を以下に示す。
> 実際に設定を行う際は、以下の値に対応する箇所を環境に合わせて適宜変更すること。

* MQTTブローカー
    * ホスト名
        * broker.example.org
    * ポート番号
        * 8884
    * MQTTブローカーをインストールしたディレクトリ
        * /srv/mqtt
    * ユーザ名/パスワード
        * `user01`/`user01-pass`
            * 権限: write
        * `user02`/`user02-pass`
            * 権限: read
        * `user03`/`user03-pass`
            * 権限: read, write
    * Mosquittoの設定ファイル
        * /etc/mosquitto/mosquitto.conf
    * ACLファイル
        * /etc/mosquitto/aclfile
* 証明書（クライアント環境）
    * CA証明書
        * /opt/certs/cacert.pem

## ブローカー側の設定手順

MQTTブローカー(Mosquitto)に認可の設定する手順について説明する。

以下の手順で設定を行う。
1. Mosquittoの設定ファイルに認可に関する設定を追加する
1. ACLファイルに設定を追加する

### 認可に関するプロパティを設定ファイルに記述する

MQTTブローカーの設定ファイル `/etc/mosquitto/mosquitto.conf` に
以下の内容を追加する。

```properties
acl_file /etc/mosquitto/aclfile
allow_anonymous false
```

上記のパラメータに関する簡単な説明を以下に記す。

* `acl_file`
    * ACL(アクセス制御リスト)のファイルを指定する
* `allow_anonymous`
    * 匿名ユーザの接続を許可するか否か

### ACLファイルに設定を追加する

ここでは、各ユーザに以下の権限を設定する手順のみを示す。

| ユーザ名 | 権限 |
| --- | --- |
| user01 | write |
| user02 | read |
| user03 | read, write|

ACLファイルに以下の内容を追加する。
```
user user01
topic write #

user user02
topic read #

user user03
topic readwrite #
```

ACLファイルの記述内容について以下に簡単な説明を行う。
* `user <username>`
    * 対象となるユーザ名を指定する
* `topic [read|write|readwrite] <topic>`
    * `<topic>`に指定したトピックに対するアクセス権を設定する
    * `read`, `write`, `readwrite` のいずれかを指定でき、省略すると `readwrite` となる
    * `<topic>`には`#`（マルチレベルワイルドカード）, `+`（シングルレベルワイルドカード）を指定することができる

ACLファイルの詳細についてはMosquittoのドキュメント「[mosquitto.conf man page](https://mosquitto.org/man/mosquitto-conf-5.html)」を参照のこと。

最後に、設定ファイルの再読み込みを行わせるために`SIGHUP`シグナルを送る。

```bash
$ sudo killall -HUP mosquitto
```

## SINETStream（クライアント側）の設定手順

SINETStreamから認可を行うMQTTブローカーを利用するための設定について説明する。
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

認可の設定が行われているMQTTブローカーを利用するには、
認可の主体を定めるために認証設定が必要となる。

設定ファイルの例を以下に示す。
> 設定自体は パスワード認証の設定と同じ内容になる。

```yaml
service-mqtt:
  brokers: broker.example.org:9094
  type: mqtt
  topic: topic-001
  consistency: AT_LEAST_ONCE
  tls:
    ca_certs: /opt/certs/cacert.pem
  username_pw_set:
    username: user03
    password: user03-pass
```

`brokers`, `type`, `topic`, `consistency`, `tls` についてはパスワード認証を利用しない場合の設定ファイルと同様なので説明を省く。
パスワード認証に関わる設定は `username_pw_set:` 以降の行が該当する。
各パラメータの意味を以下に示す。

* `username`
    * ユーザ名
* `password`
    * パスワード

### SINETStreamを利用するプログラムを作成する

SINETStreamを利用するプログラム自体は、認可を行うMQTTブローカーを利用する場合
と認可なしのMQTTブローカーを利用する場合で変わりはない。

Python APIの　`MessageWriter` を利用する場合の例を以下に示す。認可に関わる処理は存在していない。

```python
with sinetstream.MessageWriter(service='service-mqtt') as writer:
    writer.publish(b'message 001')
```

## 認可エラーとなった場合の挙動について

MQTTブローカーは認可エラーになった場合でもクライアント側にエラーを返さない。
そのため、認可エラーとなる操作を行った場合でもSINETStreamでは例外が発生しない。

