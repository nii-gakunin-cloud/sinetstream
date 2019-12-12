**準備中** (2019-12-12 15:15:40 JST)

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

# 設定ファイル

* 概要
    * 設定ファイルの配置場所
    * 設定値の優先順位
* 共通のパラメータ
    * 基本的なパラメータ
    * SSL/TLS に関するパラメータ
    * 暗号化に関するパラメータ
* メッセージングシステム固有のパラメータ
* 注意事項
    * Python APIとJava APIの違い
    * パラメータの別名
    * 未対応

## 概要

SINETStream API では、メッセージングシステムに接続するためのパラメータなどを
設定ファイルに記述することにより、API に指定するパラメータを簡略化できる。
設定ファイルには、サービス名とそれに結び付いたパラメータを記述する。
API でサービス名を指定することで、設定ファイルに記述されているパラメータが
読み込まれる。

設定ファイルのフォーマットは YAML である。
設定ファイルのなかで一つのサービスを記述するブロックは以下のようになっている。

```
{サービス名}:
  type: {メッセージングシステムのタイプ}
  brokers:
    - {ホスト名1}:{ポート番号1}
    - {ホスト名2}:{ポート番号2}
  {その他のパラメータ1}: {設定値1}
  {その他のパラメータ2}: {設定値2}
```

`type` にはメッセージングシステムのタイプを指定する。
`brokers` にはメッセージングシステムのブローカーのアドレスを指定する。

その他のパラメータとして指定できる値はメッセージングシステムによって
異なる。例えば以下のような値が指定できる。

* 通信プロトコルに関するパラメータ
    * MQTT のプロトコルバージョン (3.1, 3.1.1)
    * MQTT のトランスポート層 (TCP, WebSocket)
* TLS 接続に関するパラメータ
    * CA 証明書に関する設定
    * クライアント証明書、秘密鍵に関する設定
* ブローカーに接続するための認証情報に関するパラメータ
    * ユーザ名
    * パスワード

### 設定ファイルの配置場所

設定ファイルは以下の順序で検索され、最初に見つかったファイルのみが読み込まれる。

> 設定ファイルのカスケードは不可。
> 例えば `.sinetstream_config.yml` に `パラメータ1:値1` が、
> `$HOME/.config/sinetstream/config.yml` に `パラメータ2:値2` が書かれているとき、`パラメータ2:値2` は読み込まれない。

1. 環境変数 `SINETSTREAM_CONFIG_URL` に指定された場所(URL)
    * 設定ファイルをリモートの web サーバに置くことも可能
    * ローカルファイルを指定する場合は `file://{設定ファイルの絶対パス}` の形式で指定する。
1. カレントディレクトリの `.sinetstream_config.yml`
1. `$HOME/.config/sinetstream/config.yml`
    * Windows 10 では `C:\Users\{userXX}\.config\sinetstream\config.yml`

### 設定値の優先順位

設定ファイルに記述されたパラメータと API に指定されたパラメータが競合する場合や、
共通のパラメータとメッセージングシステム固有のパラメータが競合する場合は、
以下の優先順位で最初に見つかった値が使用される。

1. API に指定されたメッセージングシステム固有のパラメータ値
1. API に指定された共通のパラメータ値
1. 設定ファイルに記述されたメッセージングシステム固有のパラメータ値
1. 設定ファイルに記述された共通のパラメータ値

## 共通のパラメータ

メッセージングシステムの種類によらず共通で指定できるパラメータを以下に示す。

* 基本的なパラメータ
* API のパラメータ
* SSL/TLS に関するパラメータ
* 暗号化に関するパラメータ

### 基本的なパラメータ

* type
    * メッセージングシステムの種別を指定する文字列
    * 現在のバージョンで指定できる値を以下に示す
        * `kafka`
        * `mqtt`
* brokers
    * ブローカーのアドレスを `{ホスト名}` または `{ホスト名}:{ポート番号}` の形で指定する
        * `:` の前後に空白文字を入れてはならない
    * ポート番号の指定を省略した場合は、以下に示すデフォルトのポート番号が使用される
        * Kafka: 9092
        * MQTT
            * TCP: 1883 (平文), 8883 (TLS)
            * WebSocket: 80 (平文), 443 (TLS)
    * 複数のブローカーを指定する場合は、以下のいずれかの方法を用いる
        * YAML のシーケンスとして列挙する
            ```
            brokers:
              - {ホスト名1}:{ポート番号1}
              - {ホスト名2}:{ポート番号2}
            ```
        * `,` で連結する
            ```
            brokers:
              - {ホスト名1}:{ポート番号1},{ホスト名2}:{ポート番号2}
            ```

### API のパラメータ

SINETStream API を呼び出すときに指定するパラメータのデフォルト値を設定する。
API にパラメータを指定しなかった場合は、設定ファイルに記述した値がデフォルト値として使用される。

* topic
    * トピック名
* client_id
    * クライアントID
* consistency
    * メッセージ配信の信頼性を指定する
    * 指定できる値
        * AT_MOST_ONCE
            * メッセージは届かないかもしれない
        * AT_LEAST_ONCE
            * メッセージは必ず届くが何度も届くかもしれない
        * EXACTLY_ONCE
            * メッセージは必ず一度だけ届く
* value_type
    * メッセージの種別
    * 指定できる値
        * text
        * byte_array
    * デフォルト値: byte_array
* value_serializer
    * メッセージのシリアライズを行うクラス名
    * 指定したクラスにはパブリックなデフォルトコンストラクタが必要
    * `MessageWriter` のみで意味をもつ
* value_deserializer
    * メッセージのデシリアライズを行うクラス名
    * 指定したクラスにはパブリックなデフォルトコンストラクタが必要
    * `MessageReader` のみで意味をもつ
* data_encryption
    * メッセージの暗号化/復号化の有効/無効を指定する
* receive_timeout_ms
    * `MessageReader` がメッセージ到着を待つ最大待ち時間 (ms)

`value_serializer`/`value_deserializer` は `value_type` よりも優先される。

> `value_deserializer` と `value_type` を指定し、`value_serializer` を指定しなかった場合、
> MessageReader では value_deserializer が有効になり、MessageWriter では value_type が有効になる。

> Python API の制限:
> SINETStream v1.0 では、`value_serializer`/`value_deserializer` の指定はAPIのパラメータでのみ指定可能で設定ファイルには記述できない。

### SSL/TLS に関するパラメータ

SSL/TLS に関するパラメータはメッセージングシステムによって名称が異なるが、
SINETStream ではそれらを共通の `tls` パラメータによって統一的に記述できる。
ここで指定したパラメータは、SINETStream 内部でメッセージングシステム固有のパラメータにマッピングされる。

* tls
    * 以下のいずれか
        * TLS 接続を使用するかどうかを真偽値で指定する
        * TLS 接続に関するパラメータを YAML のマッピング (`{キー}: {値}`) として指定する

`tls` の子要素となるマッピングに指定できる値を以下に示す。

* ca_certs
    * CA 証明書ファイル (PEM) のパス
* certfile
    * クライアント証明書 (PEM) のパス
* keyfile
    * 秘密鍵 (PEM) のパス
* keyfilePassword
    * 秘密鍵 (PEM) のパスワード
* ciphers
    * SSL/TLS 接続に利用可能な暗号を指定する文字列
* check_hostname
    * 証明書がブローカーのホスト名と一致することを SSL ハンドシェイクで検証するかどうかを示す真偽値
* trustStore
    * トラストストアのパス※
* trustStoreType
    * トラストストアのファイルフォーマット (jks, pkcs12, ...) ※
* trustStorePassword
    * トラストストアのパスワード※
* keyStore
    * キーストアのパス※
* keyStoreType
    * キーストアのファイルフォーマット (jks, pkcs12, ...) ※
* keyStorePassword
    * キーストアのパスワード※

> ※ `trustStore`, `trustStoreType`, `trustStorePassword`, `keyStore`, `keyStoreType`, `keyStorePassword`, `keyfilePassword` は
> Java APIのみで指定できるパラメータである。Python API では指定できない。

<!-- 
共通のパラメータ名と各メッセージングシステム固有のパラメータ名との対応を以下の表に示す。

|SSL/TLS|型|Kafka|MQTT|
|---|---|---|---|
|ca_certs|str(Path)|ssl_cafile|tls_set:ca_certs|
|certfile|str(Path)|ssl_certfile|tls_set:certfile|
|keyfile|str(Path)|ssl_keyfile|tls_set:keyfile|
|ciphers|str|ssl_ciphers |tls_set:ciphers|
|check_hostname|bool|ssl_check_hostname|tls_insecure|
 -->

#### 設定例

`tls` パラメータに真偽値を指定する例を以下に示す。
この場合、設定値はメッセージングシステム固有のデフォルト値が使用される。

```
service-tls-1:
  type: mqtt
  brokers: mqtt.example.org
  tls: true
```

`tls` パラメータにマッピングを指定する例を以下に示す。

```
service-tls-2:
  type: kafka
  brokers:
    - kafka-1:9092
  tls:
    ca_certs: /etc/sinetstream/ca.pem
    certfile: certs/client.pem
    keyfile: certs/client.key
```

#### 優先順位

SINETStream の `tls` パラメータを使用せず、メッセージングシステム固有のパラメータを直接指定することもできる。

一つのサービスに対して `tls` パラメータとメッセージングシステム固有のパラメータを両方指定した場合は、以下の優先順位で最初に見つかった値が使用される。

1. API に指定されたメッセージングシステム固有のパラメータ
1. API に指定された `tls` パラメータ
1. 設定ファイルに記述されたメッセージングシステム固有のパラメータ
1. 設定ファイルに記述された `tls` パラメータ


### 暗号化に関するパラメータ

SINETStream では、バックエンドの SSL/TLS による通信の暗号化とは別に、
メッセージ内容を暗号化することができる。
これにより、ブローカーに蓄積されたメッセージを第三者に見られても情報を保護することができる。

* crypto
    * メッセージの暗号化に関するパラメータを YAML のマッピング (`{キー}: {値}`) で設定する
    * `crypto` を設定しただけでは暗号化処理は有効にならない。
      メッセージ内容を暗号化するには、別途 `data_encryption` パラメータまたは
      API のパラメータで暗号化処理を有効にする必要がある。

`crypto` の子要素となるマッピングに指定できる値を以下に示す。

* algorithm
    * 暗号のアルゴリズムを指定する
    * 指定可能な値: "AES"
* key_length
    * 鍵長 (bit) を指定する
    * 指定可能な値: 128, 192, 256
    * デフォルト値: 128
* mode
    * 暗号利用モードを指定する
    * 指定可能な値: "CBC", "OFB", "CTR", "EAX", "GCM"
* padding
    * パディング方法を指定する
    * 指定可能な値: "pkcs7"
* password
    * パスワードを指定する
* key_derivation
    * 鍵導出関数に関するパラメータを YAML のマッピングで指定する
    * algorithm
        * 鍵導出関数のアルゴリズムを指定する
        * 指定可能な値: "pbkdf2"
    * salt_bytes
        * ソルトのバイト数を指定する
    * iteration
        * 反復回数を指定する

#### 設定例

`crypto` を設定する例を以下に示す。

```
service-aes-1:
  type: kafka
  brokers:
    - kafka0.example.org:9092
  crypto:
    algorithm: AES
    key_length: 256
    mode: EAX
    key_derivation:
      algorithm: pbkdf2
      iteration: 10000
    password: secret-000
```

## メッセージングシステム固有のパラメータ

バックエンドのメッセージングシステム固有のパラメータを透過的に指定することができる。

* [Kafka固有のパラメータ](config-kafka.md)
* [MQTT固有のパラメータ](config-mqtt.md)


## 注意事項

### Java API と Python API の違い

以下のパラメータは Python API でのみ有効である。Java API で指定しても無視される。

* socket_options
* consumer_timeout_ms
* ssl_context
* ssl_crlfile
* api_version
* api_version_auto_timeout_ms
* selector
* value_serializer
* value_deserializer

以下のパラメータは Java API でのみ有効である。Python API で指定しても無視される。

* delivery_timeout_ms
* enable_idempotence
* transaction_timeout_ms
* transactional_id
* allow_auto_create_topics
* auto_offset_reset
* default_api_timeout_ms
* group_instance_id
* isolation_level
* client_rack
* client_dns_lookup
* ssl_truststore_location
* ssl_truststore_password
* ssl_truststore_type
* ssl_keystore_location
* ssl_keystore_password
* ssl_keystore_type

<!--
* partitioner_class
* interceptor_classes
* ssl_key_password
* ssl_enabled_protocols
* ssl_protocol
* ssl_provider
* ssl_cipher_suites
-->

### パラメータの別名

`ssl_check_hostname`, `ssl_cafile`, `ssl_certfile`, `ssl_keyfile`, `ssl_password`, `ssl_ciphers`
以外のパラメータについては、`_` をすべて `.` に置き換えたパラメータ名も受け付ける。
両方の名前が設定された場合は、`.` に置き換えたパラメータ名に設定されている値が優先される。

### 未対応

SINETStream v1.0 は、以下のパラメータをサポートしていない。

* metric_reporters
* metrics_num_samples
* metrics_sample_window_ms
* sasl_kerberos_service_name
* sasl_kerberos_domain_name
* sasl_oauth_token_provider
