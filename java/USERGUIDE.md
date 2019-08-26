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

# SINETStreamユーザガイド

この文書では SINETStream の Java API の利用方法について記します。

## 概要

SINETStream では メッセージングシステムに対して以下の操作を行う機能を提供します。

1. ブローカーへの接続
1. ブローカーからの切断
1. ブローカーへのメッセージ送信
1. ブローカーからのメッセージ受信

### 環境など

SINETStream v0.9.x Java API では以下のバージョンのJavaをサポートします。

* Java 8

SINETStream v0.9.xでは以下のメッセージングシステムをサポートします。

* [Apache Kafka](https://kafka.apache.org/) 2.2.1
* MQTT v3.1, v3.1.1
    
SINETStreamの動作環境は以下の通りです。

* CentOS 7.6
* Windows 10


## 使用例

### メッセージ送信

サービス名`service-1`に対応するメッセージングシステムのトピック`topic-1`
に対してメッセージを送信する例を示します。

```
MessageWriterFactory<String> factory =
    MessageWriterFactory.<String>builder()
        .service("service-1")
        .topic("topic-1")
        .consistency(AT_LEAST_ONCE)
        .build();

try(MessageWriter<String> writer = factory.getWriter()) {
    writer.write("message 001");
}
```

まずパラメータ `service`, `topic`, `consistency` を指定してファクトリオブ
ジェクト`factory`を作成します。 `factory`に対して `getWriter()`を呼び出す
ことでメッセージを送信するためのライターが得られます。

### メッセージ受信

サービス名`service-1`に対応するメッセージングシステムのトピック`topic-1`
からメッセージを受信する例を示します。

```
MessageReaderFactory<String> factory =
    MessageReaderFactory.<String>builder()
        .service("service-1")
        .topic("topic-1")
        .consistency(AT_LEAST_ONCE)
        .receiveTimeout(Duration.ofSeconds(60))
        .build();

try(MessageReader<String> reader = factory.getReader()) {
    Message<String> msg;
    while (Objects.nonNull(msg = reader.read())) {
        System.out.println(msg.getValue());
    }
}
```

まずパラメータ `service`, `topic`, `consistency`, `receiveTimeout` を指定
してファクトリオブジェクト`factory`を作成します。
`factory`に対して `getReader()`を呼び出すことでメッセージを受信するための
リーダーが得られます。`read()`を呼び出した後`receiveTimeout`に 指定した時間
メッセージが取得できないと`read()`が`null` を返しループが終了します。

## 設定ファイル

### 概要

SINETStream API では、メッセージングシステムに接続するためのパラメータなどを
設定ファイルに記述することにより、APIに指定するパラメータを簡略化できます。
設定ファイルには、サービス名とそれに結び付いたパラメータを記述します。
APIでサービス名を指定することで、設定ファイルに記述されているパラメータが
読み込まれます。

設定ファイルのフォーマットは YAML を使用します。 例を以下に示します。

```
service-1:
  type: kafka
  brokers:
    - kafka-1:9092
    - kafka-2:9092
    - kafka-3:9092
    - kafka-4:9092
service-2:
  type: mqtt
  brokers: 192.168.2.105:1883
  username_pw_set:
    username: user01
    password: pass01
```

設定ファイルのなかで一つのサービスを記述するブロックは以下のようになっています。

```
{サービス名}:
  type: {メッセージングシステムのタイプ}
  brokers:
    - {ホスト名1}:{ポート番号1}
    - {ホスト名2}:{ポート番号2}
  {その他のパラメータ1}: {設定値1}
  {その他のパラメータ2}: {設定値2}
```

`type`にメッセージングシステムのタイプを指定します。
`brokers` にはメッセージングシステムのブローカーのアドレスを指定します。

その他のパラメータとして指定できる値は各メッセージングシステムによって
異なります。例えば以下のような値が指定できます。

* 通信プロトコルに関するパラメータ
    * MQTTのプロトコルバージョン(3.1, 3.1.1)
    * MQTTのトランスポート層(TCP, WebSocket)
* TLS接続に関するパラメータ
    * CA証明書に関する設定
    * クライアント証明書、秘密鍵に関する設定
* ブローカーに接続するための認証情報に関するパラメータ
    * ユーザ名
    * パスワード

### 設定ファイルの配置場所

以下の順で設定ファイルを検索して最初に見つかったものを読み込みます。

1. 環境変数 `SINETSTREAM_CONFIG_URL` に指定されたアドレス(URL)から YAML ファイルを取得する
1. カレントディレクトリの `.sinetstream_config.yml`
1. `$HOME/.config/sinetstream/config.yml`
    * Windows 10 では `$HOME/.config/` は `C:\Users\{userXX}\.config/` となる

### 共通のパラメータ

この節ではメッセージングシステムの種類によらず共通で指定できるパラメータについて記します。
共通のパラメータとして指定できるものを以下に示します。

* 基本的なパラメータ
* API呼び出しで指定するパラメータ
* SSL/TLSに関するパラメータ
* 暗号化に関するパラメータ

#### 基本的なパラメータ

* type
    * メッセージングシステムの種別を指定する文字列
    * 現在のバージョンで指定できる値を以下に示す
        * `kafka`
        *  `mqtt` 
* brokers
    * ブローカーのアドレス
    * 複数のサーバを指定する場合は、リストまたは `,` で連結した文字列を指定する
    * ブローカーのポート番号はホスト名の後に空白文字を入れずに `:` でつなげて指定する
    * ポート番号の指定を省略した場合は、各メッセージングシステムのデフォルトの値が使用される
    * デフォルトのポート番号を以下に示す
        * kafka: 9092
        * MQTT
            * TCP: 1883(平文), 8883(TLS)
            * WebSocket: 80(平文), 443(TLS)
    
#### APIのパラメータ

SINETStream APIを呼び出すときに指定するパラメータ。APIを呼び出す際に指定が
無かった場合は、設定ファイルに記述されている値をデフォルトの値として使用
します。

* topic
    * トピック名
* client_id
    * クライアントID
* consistency
    * コンシステンシー
    * 指定できる値
        * AT_MOST_ONCE
        * AT_LEAST_ONCE
        * EXACTLY_ONCE
* value_type
    * メッセージの種別
    * 指定できる値
        * text
        * image
        * byte_array
* value_serializer
    * メッセージのシリアライズを行うクラス名
    * 指定されたクラスはパブリックなデフォルトコンストラクタが必要
    * `MessageWriter` のみで意味をもつ
* value_deserializer
    * メッセージのデシリアライズを行うクラス名
    * 指定されたクラスはパブリックなデフォルトコンストラクタが必要
    * `MessageReader` のみで意味をもつ
* data_encryption
    * メッセージの暗号化、復号化の有効、無効を指定する
* receive_timeout_ms
    * `MessageReader` がメッセージ到着を待つ最大待ち時間(ms)

#### SSL/TLSに関するパラメータ

* tls
    * TLS接続を使用するか否かの真偽値、または
    * TLS接続に関するパラメータをキー、バリューで指定する
    * 指定された値は、最終的には各メッセージングシステム固有の値にマッピングされる
    * ここで指定した値は、 各メッセージングシステム固有のパラメータよりも優先度が低い
    
`tls` の子要素となるキー、バリューに指定できる値を以下に記します。

* ca_certs
    * CA証明書ファイル(PEM)のパス
* certfile
    * クライアント証明書(PEM)のパス
* keyfile
    * 秘密鍵(PEM)のパス
* keyfilePassword
    * 秘密鍵(PEM)のパスワード
* ciphers
    * SSL/TLS接続に利用可能な暗号を指定する文字列
* check_hostname
    * 証明書がブローカーのホスト名と一致することをsslハンドシェイクで検証するかどうか
* trustStore
    * トラストストアのパス
* trustStoreType
    * トラストストアのファイルフォーマット(jks, pkcs12, ...)
* trustStorePassword
    * トラストストアのパスワード
* keyStore
    * キーストアのパス
* keyStoreType
    * キーストアのファイルフォーマット(jks, pkcs12, ...)
* keyStorePassword
    * キーストアのパスワード

> `trustStore`, `trustStoreType`, `trustStorePassword`, `keyStore`, `keyStoreType`, `keyStorePassword`, `keyfilePassword` については
> Java APIのみで指定できるパラメータです。Python API では指定できません。

`tls` パラメータに真偽値を指定する例を以下に示します。

```
service-tls-1:
  type: mqtt
  brokers: mqtt.example.org
  tls: true
```

`tls` パラメータにキー、バリューを指定する例を以下に示します。
キー、バリューを指定した場合は、暗黙的に `tls`に`true`を指定したものとみなします。

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

#### 暗号化に関するパラメータ

* crypto
    * メッセージの暗号化に関するパラメータをキー、バリューで指定する

`crypto` の子要素となるキー、バリューに指定できる値を以下に記します。

* algorithm
    * 暗号のアルゴリズムを指定する
* key_length
    * 鍵長(bit)を指定する
    * デフォルト値は 128
* mode
    * 暗号利用モードを指定する
* padding
    * パディング方法を指定する
* password
    * パスワードを指定する
* key_derivation
    * 鍵導出関数に関するパラメータをキー、バリューで指定する
    * algorithm
        * 鍵導出関数のアルゴリズムを指定する
    * salt_bytes
        * ソルトのバイト数を指定する
    * iteration
        * 反復回数を指定する
        
`crypto` を指定しただけでは暗号化処理は有効になりません。別途
`data_encryption` パラメータまたは API のパラメータで暗号化処理を有効
にする必要があります。

`crypto`　を指定した設定ファイルの例を以下に示します。

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

### 各メッセージングシステム固有のパラメータ

#### Apache Kafka

##### `MessageWriter` に関するパラメータ

* acks
    * 要求が完了したと見なすのに必要な ack の数
* compression_type
    * データの圧縮タイプ
    * 有効な値は `none`, `gzip`, `snappy`, `lz4`, `zstd`
* retries
    * ゼロより大きい値を設定すると、一時的なエラーで送信が失敗したレコードを再送信する
* batch_size
    * 送信処理のバッチサイズ
* linger_ms
    * 送信処理をまとめておこなうための遅延時間（ミリ秒）
* buffer_memory
    * バッファリングするため使用するメモリの合計バイト数
* max_block_ms
    * `send()`, `partitionsFor()` がブロックする時間（ミリ秒）
* max_request_size
    * リクエストの最大サイズ
* max_in_flight_requests_per_connection
    * ackの応答なしでリクエストする最大数
* delivery_timeout_ms
    * `send()` が呼び出されてから結果を報告するまでの時間の上限（ミリ秒）
* enable_idempotence
    * プロデューサーからの書き込みをべき等となるようにするかどうか
* transaction_timeout_ms
    * トランザクションコーディネータがプロデューサからの更新を待機してからの最大待ち時間（ミリ秒）
* transactional_id
    * トランザクションID
* key_serializer
    * メッセージキーのシリアライザ
    
<!--
* partitioner_class
-->

##### `MessageReader` に関するパラメータ

* group_id
    * コンシューマグループの名前
* fetch_min_bytes
    * フェッチリクエストに対してサーバーが返す必要があるデータの最小量
* fetch_max_wait_ms
    *  サーバが応答を返す際に `fetch_min_bytes`の条件を満たすことを待つ、最大時間(ミリ秒）
* fetch_max_bytes
    * フェッチリクエストに対してサーバーが返すデータの最大量
* max_partition_fetch_bytes
    * サーバーが返すパーティションごとのデータの最大量
* enable_auto_commit
    *  オフセットをバックグラウンドで定期的にコミットするかどうか
* auto_commit_interval_ms
    * 自動オフセットコミットの時間間隔（ミリ秒）
* check_crcs
    *  消費したレコードのCRC32を自動的にチェックするかどうか
* partition_assignment_strategy
    * コンシューマグループのパーティション割り当て戦略のクラス名のリスト
* max_poll_records
    * `poll()` で返される最大のレコード数
* max_poll_interval_ms
    * `poll()`の最大遅延時間（ミリ秒）
* session_timeout_ms
    * コンシューマーの障害を検出するために使用されるタイムアウト（ミリ秒）
* heartbeat_interval_ms
    * コンシューマコーディネーター間のハートビートの期待値（ミリ秒）
* allow_auto_create_topics
    * トピックの自動作成を行うかどうか
* auto_offset_reset
    * Kafkaに初期オフセットがない場合の振舞いを指定する
    * `latest`, `earliest`, `none` のいずれかの値を指定する
* default_api_timeout_ms
    * コンシューマAPIのデフォルトのタイムアウト値（ミリ秒）
* group_instance_id
    * コンシューマインスタンスのID
* isolation_level
    * アイソレーションレベルを指定する
    * `read_committed`, `read_uncommitted` のいずれかの値を指定する
* key_deserializer
    * メッセージキーのデシリアライザ

##### `MessageWriter`, `MessageReader` に共通するパラメータ
    
* request_timeout_ms
    * クライアントからのリクエストのタイムアウト値（ミリ秒）
* retry_backoff_ms
    * エラー時に再試行するときのバックオフまでの時間（ミリ秒）
* reconnect_backoff_ms
    * 特定のホストへの再接続を試行する前に待機する時間（ミリ秒）
* reconnect_backoff_max_ms
    * 接続に繰り返し失敗したブローカーに再接続するときに待機する最大時間（ミリ秒）
* receive_buffer_bytes
    * TCP受信バッファーサイズ
* send_buffer_bytes
    * TCP送信バッファーサイズ
* metadata_max_age_ms
    * メタデータの更新を強制する時間（ミリ秒）
* security_protocol
    * ブローカーとの通信に使用されるプロトコル
* connections_max_idle_ms
    * 指定された時間後にアイドル接続を閉じる（ミリ秒）
* exclude_internal_topics
    * 内部トピックをコンシューマーに公開するかどうか
* client_dns_lookup
    * クライアントがDNSルックアップする方法を指定する
* ssl_check_hostname
    * 証明書がホスト名と一致することを検証するかどうか
* ssl_cafile
    * CA証明書ファイルのパス
* ssl_certfile
    * クライアント証明書ファイルのパス
* ssl_keyfile
    *  クライアントの秘密鍵ファイルのパス
* ssl_password
    *  証明書ファイルをロードするときに使用するパスワード
* ssl_ciphers
    * SSL接続に使用可能な暗号を指定する
* ssl_truststore_location
    * トラストストアファイルのパス
* ssl_truststore_password
    * トラストストアファイルのパスワード
* ssl_truststore_type
    * トラストストアファイルのファイルフォーマット
* ssl_keystore_location
    * キーストアファイルのパス
* ssl_keystore_password
    * キーストアファイルのパスワード
* ssl_keystore_type
    * キーストアファイルのファイルフォーマット
    
<!--
* interceptor_classes
* ssl_key_password
* ssl_enabled_protocols
* ssl_protocol
* ssl_provider
* ssl_cipher_suites
-->

##### Python APIとの差異

以下のパラメータはJava API では無効になっています。指定されても処理に影響を与えません。

* socket_options
* consumer_timeout_ms
* ssl_context
* ssl_crlfile
* api_version
* api_version_auto_timeout_ms
* selector
* value_serializer
* value_deserializer

以下のパラメータは Java API でのみ有効で Python API では意味をもちません。

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


`ssl_check_hostname`, `ssl_cafile`, `ssl_certfile`, `ssl_keyfile`, 
`ssl_password`, `ssl_ciphers` 以外のパラメータについては
名前の `_` を全て `.` に置き換えたパラメータ名も受け付けます。
両方の名前が設定された場合は`.`に置き換えたパラメータ名に設定されている値
が優先されます。

###### 制限

v0.9.x では以下のパラメータは制限になります。指定されても処理に影響を与えません。

* metric_reporters
* metrics_num_samples
* metrics_sample_window_ms
* sasl_mechanism
* sasl_plain_username
* sasl_plain_password
* sasl_kerberos_service_name
* sasl_kerberos_domain_name
* sasl_oauth_token_provider

#### MQTT(Eclipse Paho)

* clean_session
    * 再起動、再接続で状態を記憶するかどうか
* qos
    * メッセージを送受信する際の QoS
    * 0, 1, 2 が指定でき、それぞれ `Consistency` の `AT_MOST_ONCE`, `AT_LEAST_ONCE`, `EXACTLY_ONCE` に対応する
    * `Consistency`の設定値より、`qos` の設定値が優先される
    * `qos` の値が設定されると `MessageReader`, `MessageWriter`から `getConsistency()`で取得する値に影響を及ぼす
* retain
    * サーバーがこのメッセージを保持するかどうか
* protocol
    * MQTTバージョン
    * 指定できる値は `MQTTv31`, `MQTTv311` のどちらか
* transport
    * 指定できる値は `tcp`, `websocket` のどちらか
* max_inflight_messages_set
    * ネットワークフローを一度に通過できる QoS > 0のメッセージの最大数
* ws_set_options
    * WebSocket接続のオプションを指定します
    * 以下のパラメータを指定できる
        * path
            * WebSocket のパスを指定する
        * headers
            * 標準の WebSocketヘッダーに追加するヘッダーをキー、バリューで指定する
* tls_set
    * TLS接続に関するパラメータをキー、バリューで指定する
    * 以下のパラメータを指定できる
        * ca_certs
            * CA証明書ファイル(PEM)のパス
        * certfile
            * クライアント証明書ファイル(PEM)のパス
        * keyfile
            * クライアント証明書の秘密鍵ファイル(PEM)のパス
        * keyfilePassword
            * クライアント証明書の秘密鍵ファイル(PEM)のパスワード
        * tls_version
            * TLSプロトコルのバージョン
        * ciphers
            * この接続で許可する暗号
        * trustStore
            * トラストストアのパス
        * trustStoreType
            * トラストストアのファイルフォーマット(jks, pkcs12, ...)
        * trustStorePassword
            * トラストストアのパスワード
        * keyStore
            * キーストアのパス
        * keyStoreType
            * キーストアのファイルフォーマット(jks, pkcs12, ...)
        * keyStorePassword
            * キーストアのパスワード
* tls_insecure_set
    * TLS接続でホスト名の検証を無視するかどうか
* username_pw_set
    * 認証用のユーザ、パスワードをキー、バリューで指定する
    * 以下のパラメータを指定する
        * username
        * password
* will_set
    * Last Will and Testament(LWT)に関するパラメータをキー、バリューで指定する
    * クライアントが予期せず切断された場合、ブローカーがLWTに設定されているメッセージを代わりに発行する
    * 以下のパラメータを指定できる
        * topic(必須項目)
        * payload(必須項目)
        * qos
        * retain
* reconnect_delay_set
    * 再接続するまでの待機時間に関するパラメータをキー、バリューで指定する
    * 以下のパラメータを指定できる
        * max_delay
            * 最大待ち時間（秒）
        * min_delay
            * 最小待ち時間（秒）
* connect
    * 接続に関するパラメータをキー、バリューで指定する
    * 以下のパラメータを指定できる
        * keepalive
            * キープアライブの間隔（秒）を指定する
        * automatic_recoonnect
            * 接続が切れた場合に、自動的に再接続を行うかどうか
        * connection_timeout
            * 接続タイムアウト値（秒）を指定します
    
> `userdata`, `enable_logger`, `tls_set_context`, `max_queued_messages_set`,
> `message_retry_set`, `tls_set_context`, `enable_logger`, `connect/bind_address`,
> `tls_set/cert_reqs` はPython API で定義されていますが、Java APIでは指定できないパラメータです。

> `trustStore`, `trustStoreType`, `trustStorePassword`, `keyStore`, `keyStoreType`, `keyStorePassword`, `keyfilePassword` 
> `automatic_reconnect`, `connection_timeout` はJava APIのみで指定できるパラメータです。Python API では指定できません。

## SINETStream Java API

### 主要クラス

* jp.ad.sinet.stream.api.MessageWriter
    * メッセージを送信するクラス
* jp.ad.sinet.stream.api.MessageReader
    * メッセージを受信するクラス
* jp.ad.sinet.stream.utils.MessageWriterFactory
    * `MessageWriter`オブジェクトを作成するためのファクトリクラス
* jp.ad.sinet.stream.utils.MessageReaderFactory
    * `MessageReader`オブジェクトを作成するためのファクトリクラス
    
### MessageWriterFactory

`MessageWriter`を取得するためのファクトリクラス。

このインスタンスを作成するためには複数のパラメータを指定します。そのため
インスタンスを構築するためのビルダークラス`MessageWriterFactoryBuilder`を
内部クラスとして用意しています。ビルダークラスでは以下のパラメータが指定できます。

* service(String)
    * サービス名
    * 対応するサービスが設定ファイルに記述されている必要がある
* topic(String)
    * メッセージの送信先となるトピック名
* clientId(String)
    * クライアントID
    * 指定されなかった場合は、値をライブラリ内部で自動生成する
* consistency(Consistency)
    * 列挙型で `AT_MOST_ONCE`, `AT_LEAST_ONCE`, `EXACTLY_ONCE` のいずれかの値をとる
    * デフォルト値は `AT_MOST_ONCE`
* valueType(ValueType)
    * メッセージのタイプ
    * 列挙型で `text`, `image`, `byte_array` のいずれかの値をとる
    * デフォルト値は `text`
    * この設定値によって、デフォルトのシリアライザが設定される
* serializer(Serializer<T>)
    * メッセージのシリアライザ
    * 指定されなかった場合は、`valueType`の値に応じたデフォルトのシリアライザを利用する
* dataEncryption(Boolean)
    * メッセージを暗号化の有効、無効の指定
    * 暗号化を有効にする場合は、暗号化に関するパラメータ`crypto`が設定ファイルなどで指定されている必要がある
* parameter(String key, Object value)
    * このインスタンスに限り、設定ファイルと異なる値を指定したい場合にキー、バリューを指定する
* parameters(Map<String, Object> parameters)
    * このインスタンスに限り、設定ファイルと異なる値を複数のキー、バリューペアで指定する
    

ビルダークラスのインスタンスを取得するには `MessageWriterFactory.builder()` を呼び出して
下さい。また、ビルダーオブジェクトからファクトリオブジェクトを得るには `build()` を呼び出して
下さい。以下に例を示します。

```
MessageWriterFactory<String> factory =
    MessageWriterFactory.<String>builder()
        .service("service-1")
        .topic("topic-1")
        .consistency(AT_LEAST_ONCE)
        .build();
```


### MessageWriter

ブローカーにメッセージを送信するクラス。

ファクトリクラスのオブジェクトに対して `getWriter()`を呼び出すことで、ライタークラス
`MessageWriter` のオブジェクトが取得できます。`MessageWriter`は `AutoClosable`を実装しているので、
try-with-resources文を利用できます。以下に例を示します。


```
MessageWriterFactory<String> factory = MessageWriterFactory.<String>builder()
        .service("service-1").build();

try (MessageWriter<String> writer = factory.getWriter()) {
    writer.write("message-1");
}
```

### MessageReaderFactory

`MessageReader`を取得するためのファクトリクラス。

このインスタンスを作成するためには複数のパラメータを指定します。そのため
インスタンスを構築するためのビルダークラス`MessageReaderFactoryBuilder`を
内部クラスとして用意しています。ビルダークラスでは以下のパラメータが指定できます。

* service(String)
    * サービス名
    * 対応するサービスが設定ファイルに記述されている必要がある
* topic(String)
    * メッセージの受信元となるトピック名
* topics(Collection<String>)
    * メッセージの受信元となるトピックのコレクション
    * `MessageReader`では複数のトピックからメッセージを受信することができる
* clientId(String)
    * クライアントID
    * 指定されなかった場合は、値をライブラリ内部で自動生成する
* consistency(Consistency)
    * 列挙型で `AT_MOST_ONCE`, `AT_LEAST_ONCE`, `EXACTLY_ONCE` のいずれかの値をとる
    * デフォルト値は `AT_MOST_ONCE`
* valueType(ValueType)
    * メッセージのタイプ
    * 列挙型で `text`, `image`, `byte_array` のいずれかの値をとる
    * デフォルト値は `text`
    * この設定値によって、デフォルトのデシリアライザが設定される
* deserializer(Serializer<T>)
    * メッセージのデシリアライザ
    * 指定されなかった場合は、`valueType`の値に応じたデフォルトのデシリアライザを利用する
* dataEncryption(Boolean)
    * 暗号化されたメッセージの復号処理の有効、無効の指定
    * 暗号化を有効にする場合は、暗号化に関するパラメータ`crypto`が設定ファイルなどで指定されている必要がある
* receiveTimeout(Duration)
    * `MessageReader`の`read()`メソッドがメッセージの到着を待つ最大待ち時間
* parameter(String key, Object value)
    * このインスタンスに限り、設定ファイルと異なる値を指定したい場合にキー、バリューを指定する
* parameters(Map<String, Object> parameters)
    * このインスタンスに限り、設定ファイルと異なる値を複数のキー、バリューペアで指定する
    

ビルダークラスのインスタンスを取得するには `MessageReaderFactory.builder()` を呼び出して
下さい。また、ビルダーオブジェクトからファクトリオブジェクトを得るには `build()` を呼び出して
下さい。以下に例を示します。

```
MessageReaderFactory<String> factory =
    MessageReaderFactory.<String>builder()
        .service("service-1")
        .topic("topic-1")
        .consistency(AT_LEAST_ONCE)
        .build();
```
### MessageReader

ブローカーからメッセージを受信するクラス。

ファクトリクラスのオブジェクトに対して `getReader()`を呼び出すことで、リーダークラス
`MessageReader` のオブジェクトが取得できます。`MessageReader`は `AutoClosable`を実装しているので、
try-with-resources文を利用できます。以下に例を示します。

```
MessageReaderFactory<String> factory = MessageReaderFactory.<String>builder()
        .service("service-1").receiveTimeout(Duration.ofSecondsG(60)).build();

try (MessageReader<String> reader = factory.getReader()) {
    Message<String> msg;
    while (Objects.nonNull(msg = reader.read())) {
        System.out.println("TOPIC: " + msg.getTopic() + " MESSAGE: " + msg.getValue());
    }
}
```

`read()`メソッドの返り値は `Message<T>`クラスのインスタンスになります。`getTopic()`でトピック名が、
`getValue()`でメッセージの値が取得できます。

### 例外一覧

| 例外名 | メソッド名 | |
| ---  | --- | --- |
| NoConfigException | MessageReaderFactory#getReader() MessageWriterFactory#getWriter() | 設定ファイルを読み込めない |
| NoServiceException | MessageReaderFactory#getReader() MessageWriterFactory#getWriter() | 指定したサービス名に対応するエントリが設定ファイルに存在しない |
| UnsupportedServiceException | MessageReaderFactory#getReader() MessageWriterFactory#getWriter() | サポートしていないメッセージングシステムが指定された |
| ConnectionException | MessageReaderFactory#getReader() MessageWriterFactory#getWriter() | ブローカーに接続できない |
| InvalidConfigurationException | MessageReaderFactory#getReader() MessageWriterFactory#getWriter()  | 設定ファイルの記述内容に誤りがある |
| SinetStreamIOException | MessageReaderFactory#getReader() MessageWriterFactory#getWriter() MessageReader<T>#read() MessageReader<T>#close() MessageWriter<T>#write(T) MessageWriter<T>#close() | メッセージングシステムとのIOでエラーが発生した |
| SinetStreamException | MessageReaderFactory#getReader() MessageWriterFactory#getWriter() MessageReader<T>#read() MessageReader<T>#close() MessageWriter<T>#write(T) MessageWriter<T>#close() | SINETStreamに関するその他のエラー |

### チートシートの表示方法

APIのjarファイルを `java -jar` の後に指定して実行すると、チートシートが表示されます。

```
$ java -jar SINETStream-api-0.9.5.jar

==================================================
MessageWriter example
--------------------------------------------------
MessageWriterFactory<String> factory = MessageWriterFactory.<String>builder()
        .service("service-1")
        .topic("topic-1")
        .build();
try (MessageWriter<String> writer = factory.getWriter()) {
    writer.writer("message");
}
--------------------------------------------------
MessageWriterFactory parameters:
    service(String service)
        Service name defined in the configuration file. (REQUIRED)
    clientId(String clientId)
        If not specified, the value is automatically generated.
    consistency(Consistency consistency[=AT_MOST_ONCE])
        consistency: AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE
    dataEncryption(Boolean dataEncryption[=false])
        Message encryption.
    parameter(String key, Object parameter)
        Rewrites the parameters described in the configuration file only for the specified key / value pairs.
    parameters(Map parameters)
        Overwrites the parameters described in the configuration file with the specified values.
    serializer(Serializer serializer)
        If not specified, use default serializer according to valueType.
    topic(String topic)
        The topic to receive.
    valueType(ValueType valueType[=TEXT])
        The type of message.
==================================================
MessageReader example
--------------------------------------------------
MessageReaderFactory<String> factory = MessageReaderFactory.<String>builder()
        .service("service-1")
        .topic("topic-1")
        .build();
try (MessageReader<String> reader = factory.getReader()) {
    Message<String> msg;
    while (Objects.nonNull(msg = reader.read)) {
        System.out.println(msg.getValue());
    }
}
--------------------------------------------------
MessageReaderFactory parameters:
    service(String service)
        Service name defined in the configuration file. (REQUIRED)
    clientId(String clientId)
        If not specified, the value is automatically generated.
    consistency(Consistency consistency[=AT_MOST_ONCE])
        consistency: AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE
    dataEncryption(Boolean dataEncryption[=false])
        Message encryption.
    deserializer(Deserializer deserializer)
        If not specified, use default deserializer according to valueType.
    parameter(String key, Object parameter)
        Rewrites the parameters described in the configuration file only for the specified key / value pairs.
    parameters(Map parameters)
        Overwrites the parameters described in the configuration file with the specified values.
    topic(String topic)
        The topic to receive.
    topics(Collection topics)
        A list of topics to receive.
    valueType(ValueType valueType[=TEXT])
        The type of message.
```
