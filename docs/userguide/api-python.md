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

# Python API

* 使用例
* Python API クラス一覧
    * MessageReader クラス
    * MessageWriter クラス
    * Message クラス
* メッセージングシステム固有のパラメータ
    * Apache Kafka
    * MQTT (Eclipse Paho)
* チートシートの表示方法


## 使用例

はじめに簡単な使用例を示す。

この例では、異なるメッセージングシステムをバックエンドとする二つのサービス `service-1` と `service-2` を利用する。
`service-1` のバックエンドは Apache Kafka で、4台のブローカー `kafka-1` ～ `kafka-4` で構成される。
`service-2` のバックエンドは MQTT で、1台のブローカー `192.168.2.105` で構成される。

### 設定ファイル作成

設定ファイルは、クライアントがブローカーに接続するための設定が記述されたファイルである。
詳細は [設定ファイル](config.md) を参照すること。

この例では、以下の内容の設定ファイル `.sinetstream_config.yml` をクライアントマシンのカレントディレクトリに作成する。

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

### メッセージ送信

サービス名 `service-1` に対応するメッセージングシステムのトピック `topic-1` に対してメッセージを送信する例を示す。

```python
from sinetstream import MessageWriter

writer = MessageWriter('service-1', 'topic-1')
with writer as f:
    f.publish(b'Hello! This is the 1st message.')
    f.publish(b'Hello! This is the 2nd message.')
```

はじめに、サービス名とトピック名を指定して MessageWriter オブジェクトのインスタンスを作成する。
このインスタンスを with 文で開き、ブロック内で `publish()` メソッドを呼び出すことで、メッセージをブローカーに送信する。

> MessageWriter オブジェクトは、with ブロックに入ると自動的にメッセージングシステムに接続され、
> with ブロックを抜けると自動的にメッセージングシステムとの接続がクローズされる。

デフォルトでは、`publish()` の引数はバイト列である。
バイト列以外のオブジェクトを渡すには、[MessageWriter クラス](#MessageWriter クラス) のコンストラクタで `value_type` または `value_serializer` を指定する。

### メッセージ受信

サービス名 `service-1` に対応するメッセージングシステムのトピック `topic-1` からメッセージを受信する例を示す。

```python
from sinetstream import MessageReader

reader = MessageReader('service-1', 'topic-001')
with reader as f:
    for msg in f:
        print(msg.value)
```

はじめに、サービス名とトピック名を指定して MessageReader オブジェクトのインスタンスを作成する。
このインスタンスを with 文で開き、ブロック内でターゲット `f` に対しイテレータを回し、イテレータの `value` プロパティを参照することで、メッセージをブローカーから受信する。

> MessageReader オブジェクトは、with ブロックに入ると自動的にメッセージングシステムに接続され、
> with ブロックを抜けると自動的にメッセージングシステムとの接続がクローズされる。

デフォルトでは、メッセージ受信処理はタイムアウトせず、for 文は無限ループとなる。
for ループから抜けるには、[MessageReader クラス](#MessageReader クラス) のコンストラクタで `receive_timeout_ms` を指定するか、シグナル処理を行う必要がある。


## Python API クラス一覧

* sinetstream.MessageReader
    * メッセージングシステムからメッセージを取得するクラス
* sinetstream.MessageWriter
    * メッセージングシステムにメッセージを送信するクラス
* sinetstream.Message
    * 送受信されるメッセージを表すクラス
* sinetstream.SinetError
    * SINETStreamの例外クラス全体の親クラス

### MessageReaderクラス

#### MessageReader()

MessageReaderクラスのコンストラクタ。

```
MessageReader(
    service,
    topics=None,
    consistency=AT_MOST_ONCE,
    client_id=DEFAULT_CLIENT_ID,
    value_type=None,
    value_deserializer=None,
    receive_timeout_ms=float("inf"),
    **kwargs)
```

##### パラメータ

* service
    * サービス名
    * 設定ファイルに対応するサービス名が記述されている必要がある
* topics
    * トピック名
    * `str` または `list` を指定できる
    * 複数のトピックをsubscribeする場合は `list` を指定すること
    * 指定を行わなかった場合は設定ファイルに記述されている値が用いられる
* consistency
    * メッセージ配信の信頼性を指定する
    * AT_MOST_ONCE (=0)
        * メッセージは届かないかもしれない
    * AT_LEAST_ONCE (=1)
        * メッセージは必ず届くが何度も届くかもしれない
    * EXACTLY_ONCE (=2)
        * メッセージは必ず一度だけ届く
* client_id
    * クライアントの名前
    * DEFAULT_CLIENT_ID, None, 空文字のいずれかが指定された場合はライブラリが値を自動生成する
    * 自動生成した値は、このオブジェクトのプロパティとして取得できる
* value_type
    * メッセージの値の種類
    * TEXT (='text'), BYTE_ARRAY (='byte_array') のいずれかの値をとる
    * None(デフォルト値) が指定された場合は何もしない
* value_deserializer
    * メッセージのバイト列から値を復元するために使用する関数
    * None (デフォルト値) が指定された場合は何もしない
* receive_timeout_ms
    * メッセージの到着を待つ最大時間 (ms)
    * 一度タイムアウトするとこのコネクションからメッセージを読み込むことはできない。
* data_encryption
    * メッセージの暗号化、復号化の有効、無効を指定する
* kwargs
    * メッセージングシステム固有のパラメータを YAML のマッピングとして記述する

`kwargs` に記述されたパラメータは、バックエンドのメッセージングシステムのコンストラクタにそのまま渡される。
詳細は [メッセージングシステム固有のパラメータ](#メッセージングシステム固有のパラメータ) を参照。

`service` 以外の引数は、設定ファイルにデフォルト値を記述することができる。
設定ファイルとコンストラクタの引数の両方に同じパラメータの値を指定した場合は、コンストラクタの引数に指定した値が優先する。

** 制限事項: SINETStream v1.0 では、Kafka の `consistency` に `EXACTLY_ONCE` を指定しても `AT_LEAST_ONCE` にダウングレードする。**

##### 戻り値

メッセージングシステムとの接続状態を保持しているハンドラ

##### 例外

* NoServiceError
    * service に指定した値に対応するサービスが設定ファイルに存在しない
* NoConfigError
    * 設定ファイルが存在しない、あるいは読み込めない
* InvalidArgumentError
    * 指定した引数の形式が正しくない。 `consistency` の値が範囲外、 `topic` 名として許容されない文字列などの場合
* UnsupportedServiceTypeError
    * 設定ファイルに指定されている `type` に対応するメッセージングシステムのプラグインがインストールされていない
* ConnectionError
    * ブローカーへの接続がエラーになった
* AlreadyConnectedError
    * 既に接続状態のオブジェクトに対して、再度 open() を呼び出した場合

<!-- 
#### プロパティ

コンストラクタの引数に指定した `service` などの値は読み取り専用のプロパティとしてアクセスできる。
`client_id` などのようにライブラリが値を自動生成するものについては、生成された値をプロパティとして取得できる。

**制限事項: SINETStream v1.0 ではプロパティの取得は未実装である。**
 -->

<!-- 
#### 読み取り位置の変更

メッセージングシステムによっては読み取り位置を変更できるものがある。
実際 Apache KafkaではConsumerがメッセージを読み取る位置を変更できる。

MessageReaderのサービスが Apache Kafka だった場合、読み取り位置の変更を指定することができる

* seek_to_beginning()
* seek_to_end()

##### 例
```
reader = MessageReader('service-1', 'topic-001')
with reader.open() as f:
    f.seek_to_beginning()
    for msg in f:
        print(msg.value.decode('utf-8'))
```

**開発中のデバッグ、テストを目的の非公開API。将来のリリースで削除する可能性がある。**
 -->

### MessageWriterクラス

#### MessageWriter()

```
MessageWriter(
    service,
    topic,
    consistency=AT_MOST_ONCE,
    client_id=DEFAULT_CLIENT_ID,
    value_serializer=None,
    **kwargs)
```

MessageWriterクラスのコンストラクタ。

##### パラメータ

* service
    * サービス名
    * 設定ファイルに対応するサービス名が記述されている必要がある
* topic
    * トピック名
    * 指定を行わなかった場合は設定ファイルに記述されている値が用いられる
* consistency
    * メッセージ配信の信頼性を指定する
    * AT_MOST_ONCE (=0)
        * メッセージは届かないかもしれない
    * AT_LEAST_ONCE (=1)
        * メッセージは必ず届くが何度も届くかもしれない
    * EXACTLY_ONCE (=2)
        * メッセージは必ず一度だけ届く
* client_id
    * クライアントの名前
    * DEFAULT_CLIENT_ID, None, 空文字のいずれかが指定された場合はライブラリが値を自動生成する
* value_type
    * メッセージの値の種類
    * TEXT (='text'), BYTE_ARRAY (='byte_array') のいずれかの値をとる
    * None(デフォルト値) が指定された場合は何もしない
* value_serializer
    * メッセージの値をバイト列に変換するための関数
    * None (デフォルト値) が指定された場合は何もしない
* data_encryption
    * メッセージの暗号化、復号化の有効、無効を指定する
* kwargs
    * メッセージングシステム固有のパラメータを YAML のマッピングとして記述する

`kwargs` に記述されたパラメータは、バックエンドのメッセージングシステムのコンストラクタにそのまま渡される。
詳細は [メッセージングシステム固有のパラメータ](#メッセージングシステム固有のパラメータ) を参照。

`service` 以外の引数は、設定ファイルにデフォルト値を記述することができる。
設定ファイルとコンストラクタの引数の両方に同じパラメータの値を指定した場合はコンストラクタの引数に指定した値が優先する。

**制限事項: SINETStream v1.0 では、Kafka の `consistency` に `EXACTLY_ONCE` を指定しても `AT_LEAST_ONCE` にダウングレードする。**

##### 戻り値

メッセージングシステムとの接続状態を保持しているハンドラ

##### 例外

* NoServiceError
    * `service` に指定した値に対応するサービスが設定ファイルに存在しない
* NoConfigError
    * 設定ファイルが存在しない、あるいは読み込めない
* InvalidArgumentError
    * 指定した引数の形式が正しくない。`consistency` の値が範囲外、`topic` 名として許容されない文字列などの場合
* UnsupportedServiceTypeError
    * 設定ファイルに指定されている `type` に対応するメッセージングシステムのプラグインがインストールされていない
* ConnectionError
    * ブローカーへの接続がエラーになった
* AlreadyConnectedError
    * 既に接続状態のオブジェクトに対して、再度 open() を呼び出した場合

<!-- 
#### プロパティ

コンストラクタの引数に指定した `service` などの値は読み取り専用のプロパティとしてアクセスできる。
`client_id` などのようにライブラリが値を自動生成するものについては、生成した値がプロパティとして取得できる。

**制限事項: SINETStream v1.0 ではプロパティの取得は未実装。**
 -->

### Messageクラス

メッセージングシステムのメッセージオブジェクトのラッパークラス。

#### プロパティ

全て読み取りアクセスのみ。

* value
    * メッセージの値部分
    * Kafka では raw.value, MQTT では raw.payload
    * デフォルトではメッセージの値のバイト列が得られる
    * MessageReader に value_deserializer が設定されている場合は、その関数によってバイト列から変換されたオブジェクトが得られる
* topic
    * トピック名
* raw
    * メッセージングシステムのメッセージオブジェクト

### 例外一覧

|例外|発生元メソッド|理由|
|---|---|---|
|`NoServiceError`|`MessageReader()`, `MessageWriter()`|指定したサービス名が設定ファイルで定義されていない。|
|`UnsupportedServiceTypeError`|`MessageReader()`, `MessageWriter()`|サービスの定義で指定されているサービスタイプをサポートしていない。または対応するプラグインがインストールされていない。|
|`NoConfigError`|`MessageReader()`, `MessageWriter()`|設定ファイルがない。|
|`InvalidArgumentError`|`MessageReader()`, `MessageWriter()`, `MqttReader.open()`, `MqttWriter.open()`, `MqttWriter.publish()`|引数が間違っている。|
|`ConnectionError`|`KafkaReader.open()`, `KafkaWriter.open()`, `MqttReader.open()`, `MqttWriter.open()`, `MqttWriter.publish()`|ブローカーとの接続に問題がある。|
|`AlreadyConnectedError`|`KafkaReader.open()`, `KafkaWriter.open()`, `MqttReader.open()`, `MqttWriter.open()`|すでにブローカと接続している。|


## メッセージングシステム固有のパラメータ

`kwargs` を用いて、バックエンドのメッセージングシステム固有のパラメータを透過的に指定できる。
実際にどのようなパラメータを渡せるかはバックエンドによって異なる。
`kwargs` に指定されたパラメータの妥当性チェックは行われない。

### Apache Kafka

基本的に
[kafka-python](https://kafka-python.readthedocs.io/en/master/) の
[KafkaConsumer](https://kafka-python.readthedocs.io/en/master/apidoc/KafkaConsumer.html) と
[KafkaProducer](https://kafka-python.readthedocs.io/en/master/apidoc/KafkaProducer.html) の
コンストラクタ引数をパラメータとして指定できる。
`KafkaConsumer` のみ、または `KafkaProducer` のみで意味を持つパラメータについては、
それぞれ `MessageReader`, `MessageWriter` の対応するクラスのみに影響を与える。

[Kafka固有のパラメータ](config-kafka.md)
<!--
* group_id
* key_deserializer
* key_serializer
* value_deserializer
* value_serializer
* fetch_min_bytes
* fetch_max_wait_ms
* fetch_max_bytes
* max_partition_fetch_bytes
* request_timeout_ms
* max_in_flight_requests_per_connection
* auto_offset_reset
* enable_auto_commit
* auto_commit_interval_ms
* check_crcs
* metadata_max_age_ms
* partition_assignment_strategy
* max_poll_records
* max_poll_interval_ms
* session_timeout_ms
* heartbeat_interval_ms
* receive_buffer_bytes
* send_buffer_bytes
* socket_options
* consumer_timeout_ms
* security_protocol
* ssl_context
* ssl_check_hostname
* ssl_cafile
* ssl_certfile
* ssl_keyfile
* ssl_password
* ssl_crlfile
* ssl_ciphers
* api_version
* api_version_auto_timeout_ms
* connections_max_idle_ms
* metric_reporters
* metrics_num_samples
* metrics_sample_window_ms
* selector
* exclude_internal_topics
* sasl_mechanism
* sasl_plain_username
* sasl_plain_password
* sasl_kerberos_service_name
* sasl_kerberos_domain_name
* sasl_oauth_token_provider
* acks
* compression_type
* retries
* batch_size
* linger_ms
* buffer_memory
* connections_max_idle_ms
* max_block_ms
* max_request_size
* retry_backoff_ms
* reconnect_backoff_ms
* reconnect_backoff_max_ms
* max_in_flight_requests_per_connection
-->

### MQTT (Eclipse Paho)

<!-- 
基本的に
[paho.mqtt.client.Client](https://pypi.org/project/paho-mqtt/#client) の
コンストラクタと設定関数 (`XXX_set`) などの引数に指定できるパラメータを指定できる。

[MQTT固有のパラメータ](config-mqtt.md)

* clean_session
* userdata
* qos
* retain
* protocol
* transport
* max_inflight_messages_set
* max_queued_messages_set
* message_retry_set
* ws_set_options
    * path
    * headers
* tls_set
    * ca_certs
    * certfile
    * keyfile
    * cert_reqs
    * tls_version
    * ciphers
* tls_set_context
* tls_insecure_set
* enable_logger
* username_pw_set
    * username
    * password
* will_set
    * topic
    * payload
    * qos
    * retain
* reconnect_delay_set
    * min_delay
    * max_delay
* connect
    * keepalive
    * bind_address

`username_pw_set()` などのコンストラクタとは別の関数で設定するパラメータについては、
関数名がキー、その関数で設定するパラメータ名と値の組からなるマッピングが値となるように設定ファイルに記述する。
 -->
**制限事項: SINETStream v1.0 では MQTT のパラメータ設定は未実装。**

<!--
#### 例

```
service-2:
  type: mqtt
  brokers: 192.168.2.105:1883
  username_pw_set:
    username: user01
    password: pass01
```

`MQTTv311` などの指定は、指定された文字列を SINETStream API の MQTT プラグインが適切に処理して対応する定数に変換する。
-->

## チートシートの表示方法

SINETStreamをインストール後 `python3 -m sinetstream` を実行するとチートシートが表示される。

```
$ python3.6 -m sinetstream
==================================================
Default parameters:
MessageReader(
    service=SERVICE,                 # Service name defined in the configuration file. (REQUIRED)
    topics=TOPICS,                   # The topic to receive.
    consistency=AT_MOST_ONCE,        # consistency: AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE
    client_id=DEFAULT_CLIENT_ID,     # If not specified, the value is automatically generated.
    value_type=None,                 # The type of message.
    value_deserializer=None          # If not specified, use default deserializer according to valueType.
)
MessageWriter(
    service=SERVICE,                 # Service name defined in the configuration file. (REQUIRED)
    topic=TOPIC,                     # The topic to send.
    consistency=AT_MOST_ONCE,        # consistency: AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE
    client_id=DEFAULT_CLIENT_ID,     # If not specified, the value is automatically generated.
    value_type=None,                 # The type of message.
    value_serializer=None            # If not specified, use default serializer according to valueType.
)
```
