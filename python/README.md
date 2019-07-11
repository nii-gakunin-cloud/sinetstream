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

# About: SINETStream API

各種メッセージングサーバ（ブローカー）に対して同一のAPIでアクセスすることを可能にするライブラリ。

# メッセージングシステムに対する操作

## 概要

メッセージングシステムのブローカーに対して以下の操作を行う機能を提供する。

1. ブローカーへの接続
1. ブローカーからの切断
1. ブローカーへのメッセージ送信
1. ブローカーからのメッセージ受信

SINETStream API では以下のメッセージングシステムをサポートする。

* [Apache Kafka](https://kafka.apache.org/) 2.2.1
* MQTT v3.1, v3.1.1
    * [Eclipse Mosquitto](https://mosquitto.org/) v1.6.2

** 制限事項: v0.9ではMQTTをサポートしない**

将来、上記のメッセージングシステム以外をサポートする場合に備えて、
各メッセージングシステムを処理するモジュールはプラグインとしてSINETStream API本体とは分離できるように実装を行う。

SINETStream APIでは以下の言語をサポートする。

* Java 8
* Python 3.7

**制限事項: v0.9 ではPython 3のライブラリのみ提供する。**

以下のプラットフォームでの動作確認を行う。

* CentOS 7.6
* Windows 10
* Android (MQTTのみ)

**制限事項: v0.9 ではAndroid をサポートしない**

## 設定ファイル

### 概要

SINETStream API ではメッセージングシステムに接続するためのパラメータをAPIに
直接指定しなくて済むように設定ファイルを用意する。設定ファイルには各メッセー
ジングシステムに接続するためのパラメータを記述する。APIでは設定ファイルの
どのパラメータセットを用いるかを指し示すラベル（サービス名）のみを指定する。
設定ファイルのフォーマットは YAML とする。

設定ファイルの例を以下に示す。

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

設定ファイルのなかで一つのサービスを記述するブロックは以下のようになる。

```
{サービス名}:
  type: {メッセージングシステムのタイプ}
  brokers:
    - {ホスト名1}:{ポート番号1}
    - {ホスト名2}:{ポート番号2}
  {その他のパラメータ1}: {設定値1}
  {その他のパラメータ2}: {設定値2}
```

`type`にはメッセージングシステムのタイプを指定する。
SINETStream API v0.9 では`type`に指定可能な値は `kafka`のみとする。

`brokers` にはメッセージングシステムのブローカーのアドレスを指定する。
複数のサーバを指定する場合は、リストまたは `,` で連結した文字列を指定する。
ブローカーのポート番号は、ホスト名の後に `:` でつなげて指定する。
ポート番号の指定を省略した場合は、各メッセージングシステムのデフォルトの
ポート番号を使用する。 kafkaのデフォルトのポート番号は9092になっている。

その他のパラメータを指定する箇所は、メッセージングシステム毎に設定できる
パラメータが異なる。
以下のようなパラメータが指定できる。詳細については後述する。

* 通信の暗号化に関するパラメータ
    * SSL/TLSのための証明書、秘密鍵ファイルのパス
    * CA証明書のパス
* ブローカーに接続するための認証情報に関するパラメータ
    * ユーザ名
    * パスワード
* 通信プロトコルに関するパラメータ
    * Kafka APIのバージョン
    * MQTTのプロトコルバージョン(3.1, 3.1.1)
    * MQTTのトランスポート層(TCP, WebSocket)

### 配置場所

設定ファイルは、以下の順で検索して最初に見つかったファイルのみを読み込む。

1. 環境変数 `SINETSTREAM_CONFIG_URL` に指定されたアドレスから YAML ファイルを取得する
<!-- * 設定ファイルをHTTPサーバなどのリモートに配置することも許容する -->
1. カレントディレクトリの `.sinetstream_config.yml`
1. `$HOME/.config/sinetstream/config.yml`
    * Windows 10 では `~/.config` は `C:\Users\{userXX}\.config` となる

### その他のパラメータについて

設定ファイルにその他のパラメータとして指定するものについては基本的に抽象化は行わず、
下位層のクラスに指定するパラメータを透過的に指定できるものとする。
ただし、SSL/TLS に関するパラメータは共通部分が多いので統一した指定方法も用意する。

#### Apache Kafka

基本的に
[kafka-python](https://kafka-python.readthedocs.io/en/master/)の
[KafkaConsumer](https://kafka-python.readthedocs.io/en/master/apidoc/KafkaConsumer.html),
[KafkaProducer](https://kafka-python.readthedocs.io/en/master/apidoc/KafkaProducer.html)
のコンストラクタ引数をパラメータとして指定できる。
`KafkaConsumer`のみ、または`KafkaProducer`のみで意味を持つパラメータについては、
それぞれ `MessageReader`, `MessageWriter`の対応するクラスのみに影響を与える。

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

`MessageReader`, `MessageWriter`で指定されている型と異なる値が設定されている場合は、
設定ファイルを読み込んだときに `Warning` を出力してそのパラメータに関する設定を無視する（エラーとせずに処理を継続する）。
Warningの出力先は、ログ設定に従う。

#### MQTT(Eclipse Paho)

**制限事項: v0.9ではMQTTをサポートしない**

基本的に
[paho.mqtt.client.Client](https://pypi.org/project/paho-mqtt/#client)
のコンストラクタと設定関数(`XXX_set`)などの引数に指定できるパラメータを指定できる。

* clean_session
* userdata
* qos
* retain
* protocol
    * MQTTv31, MQTTv311
* transport
    * websockets, tcp
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

`username_pw_set()`などのコンストラクタとは別の関数で設定するパラメータについては、
関数名がキー、その関数で設定するパラメータ名と値の組からなる mapping が値となるように設定ファイルに記述する。

##### 例:

```
service-2:
  type: mqtt
  brokers: 192.168.2.105:1883
  username_pw_set:
    username: user01
    password: pass01
```

`paho.mqtt.client.Client`で指定されている型と異なる値が設定されている場合は、
設定ファイルを読み込んだときに `Warning` を出力してそのパラメータに関する設定を無視する（エラーとせずに処理を継続する）。
Warningの出力先は、ログ設定に従う。

`MQTTv311` などの指定は、指定された文字列をSINETStream APIの MQTTプラグインが適切に処理して対応する定数に変換する。

#### SSL/TLSに関するパラメータ

各メッセージングシステムのパラメータによらず、共通のパラメータ名でSSL/TLSに関する設定値を指定することができる。

* ca_certs
    * `str`: CA証明書ファイルのパス
* certfile
    * `str`: クライアント証明書のパス
* keyfile
    * `str`: クライアントの秘密鍵のパス
* ciphers
    * `str`: SSL/TLS接続に利用可能な暗号を指定する文字列
* check_hostname
    * `bool`: 証明書がブローカーのホスト名と一致することをsslハンドシェイクで検証するかどうかを設定するためのフラグ

共通のパラメータ名で指定する場合は、パラメータと値の組からなる mapping を、キー `tls`の値として設定ファイルに記述する。

##### 例:

```
service-3:
  type: kafka
  brokers:
    - kafka-1:9093
    - kafka-2:9093
  tls:
    ca_certs: ~/.config/sinetstream/ca.pem
    certfile: ~/.config/sinetstream/client.pem
    keyfile: ~/.config/sinetstream/client.key
```

共通のパラメータ名と各メッセージングシステム固有のパラメータ名との対応を以下の表に示す。

|SSL/TLS|型|Kafka|MQTT|
|---|---|---|---|
|ca_certs|str(Path)|ssl_cafile|tls_set:ca_certs|
|certfile|str(Path)|ssl_certfile|tls_set:certfile|
|keyfile|str(Path)|ssl_keyfile|tls_set:keyfile|
|ciphers|str|ssl_ciphers |tls_set:ciphers|
|check_hostname|bool|ssl_check_hostname|tls_insecure|

１つのサービスに対して共通のパラメータ名とメッセージングシステム固有のパラメータ名の両方が指定された場合は、
メッセージングシステム固有のパラメータの設定値の方が優先される。

SSL/TLSの共通パラメータが１つでも指定された場合、
SSL/TLS による接続を有効にするメッセージングシステム固有のパラメータを暗黙的に設定する。
このようなパラメータとして、Kakfaの `security_protocol` がある。
SSL/TLSの共通パラメータが指定された場合は`security_protocol`の値を`SSL`に設定する。

#### コンストラクタのデフォルト値

SINETStream APIの MessageReader, MessageWriterのコンストラクタ引数のデフォルト値を設定ファイルで指定することができる。

指定できるパラメータを以下に示す。

* consistency
* client_id
* value_deserializer
* value_serializer
* topic

MessageReader, MessageWriterのどちらかにしか存在しないパラメータは、対応するクラスのみに影響を与える。

`consistency`と MQTTの`qos`などのように対応関係のあるパラメータの両方が設定ファイルに記述されていた場合は、
メッセージングシステム固有のパラメータの方が優先される。
例えば`consistency`と`qos`の両方が記述されていた場合は `qos`に指定されている値の方が優先する。

MessageReader, MessageWriterのコンストラクタの引数に値を指定した場合は、
コンストラクタの引数の値が設定ファイルに記述した値に優先する。
パラメータの優先度を高いほうから順に並べると以下のようになる。

1. コンストラクタの引数
1. 設定ファイルのメッセージングシステム固有のパラメータ指定
1. 設定ファイルのコンストラクタのデフォルト値

# Python API クラス一覧

* sinetstream.MessageReader
    * メッセージングシステムからメッセージを取得するクラス
* sinetstream.MessageWriter
    * メッセージングシステムにメッセージを送信するクラス
* sinetstream.Message
    * 送受信されるメッセージを表すクラス
* sinetstream.SinetError
    * SINETStreamの例外クラス全体の親クラス

## 使用例

### MessageReader

サービス名: `service-1` のトピック `topic-001` からメッセージを取得する例。

```python
from sinetstream import MessageReader

reader = MessageReader('service-1', 'topic-001')
with reader as f:
    for msg in f:
        print(msg.value)
```

MessageReaderオブジェクトは with 文をサポートしている。
ブロックに入る際に暗黙的にメッセージングシステムとの接続を行い、
ブロックから抜け出す際にメッセージングシステムとの接続のクローズ処理を
行う。 またwith文のターゲット`f` から取得できるイテレータを通してメッセージングシステムからメッセージを取得することができる。
デフォルトのパラメータではメッセージ取得の最大待ち時間は `inf` に設定されているため、
通常の処理で`for` ループから抜け出すことはない。
`for`ループから抜け出すことができるようにするにはMessageReaderコンストラクタの引数でメッセージの最大待ち時間の指定するか、
シグナル処理を行う必要がある。

### MessageWriter

サービス名: `service-2` のトピック `topic-002` にメッセージを送る例。

```python
from sinetstream import MessageWriter

writer = MessageWriter('service-2', 'topic-002')
with writer as f:
    f.publish(b'test message 001')
    f.publish(b'test message 002')
```

MessageWriterオブジェクトは with 文をサポートしている。
ブロックに入る際に暗黙的にメッセージングシステムとの接続を行い、
ブロックから抜け出す際にメッセージングシステムとの接続のクローズ処理を行う。

`value_serializer` を設定しない場合は publish() にメッセージの値として渡すパラメータはバイト列を指定する必要がある。
バイト列以外のオブジェクトを渡す場合は MessageWriter() のパラメータ `value_serializer` でメッセージの直列化を行う関数を指定する必要がある。

## MessageReaderクラス

### MessageReader()

MessageReaderクラスのコンストラクタ。

```
MessageReader(
    service,
    topic,
    consistency=EXACTLY_ONCE,
    client_id=DEFAULT_CLIENT_ID,
    value_deserializer=None,
    receive_timeout_ms=float("inf"),
    **kwargs)
```

**パラメータ:**

* `service`
    * サービス名
    * 設定ファイルに対応するサービス名が記述されている必要がある
* `topic`
    * トピック名
    * `str` または `list` を指定できる
    * 複数のトピックをsubscribeする場合は`list`を指定すること
* `consistency`
    * AT_MOST_ONCE (=0)
    * AT_LEAST_ONCE (=1)
    * EXACTLY_ONCE (=2)
* `client_id`
    * クライアントの名前
    * DEFAULT_CLIENT_ID, None, 空文字のいずれかが指定された場合はライブラリが値を自動生成する
    * 自動生成した値は、このオブジェクトのプロパティとして取得できる
* `value_deserializer`
    * メッセージのバイト列から値を復元するために使用する関数
    * None(デフォルト値)が指定された場合は何もしない
* `receive_timeout_ms`
    * メッセージの到着を待つ最大時間(ms)
    * 一度タイムアウトするとこのコネクションからメッセージを読み込むことはできない。
* `kwargs`
    * 各メッセージングシステムを操作する際に用いるオブジェクトに設定する個別のパラメータ

`kwargs` に指定があった場合は、
メッセージングシステムを操作するための下位層のクラス `kafka.KafkConsumer`などのコンストラクタにパラメータをそのまま渡す事ができる。
実際にどのようなパラメータを渡せるかは下位のライブラリに依存する。
このクラスでは指定されたパラメータの妥当性のチェックは行わず、そのままの形で下位のクラスに渡すこととする。

`service`以外の引数は、設定ファイルにデフォルト値を記述することができる。
設定ファイルとコンストラクタの引数の両方に同じパラメータの値を指定した場合はコンストラクタの引数に指定した値が優先する。

** 制限事項: v0.9では`consistency`に`EXACTLY_ONCE`を指定しても`AT_LEAST_ONCE`にダウングレードする**

##### 例外:

* NoServiceError
    * `service`に指定した値に対応するサービスが設定ファイルに存在しない
* NoConfigError
    * 設定ファイルが存在しない、あるいは読み込めない
* InvalidArgumentError
    * 指定した引数の形式が正しくない。`consistency`の値が範囲外、`topic`名として許容されない文字列などの場合
* UnsupportedServiceTypeError
    * 設定ファイルに指定されている`type`に対応するメッセージングシステムのプラグインがインストールされていない

##### パラメータ:

* `revice_timeout_ms`
    * メッセージの到着を待つ最大時間(ms)
    * 一度タイムアウトするとこのコネクションからメッセージを読み込むことはでない。

##### 戻り値:

メッセージングシステムとの接続状態を保持しているハンドラ

##### 例:
```
reader = MessageReader('service-1', 'topic-001')
with reader as f:
    for msg in f:
        print(msg.value.decode('utf-8'))
```

##### 例外:

* ConnectionError
    * ブローカーへの接続がエラーになった
* AlreadyConnectedError
    * 既に接続状態のオブジェクトに対して、再度 open() を呼び出した場合

### プロパティ

コンストラクタの引数に指定した `service` などの値は読み取りのみのプロパティとしてアクセスできる。
`client_id`などのようにライブラリが値を自動生成するものについては、生成した値がプロパティとして取得できる。

### 読み取り位置の変更

メッセージングシステムによっては読み取り位置を変更できるものがある。
実際 Apache KafkaではConsumerがメッセージを読み取る位置を変更できる。

MessageReaderのサービスが Apache Kafka だった場合、読み取り位置の変更を指定することができる

* seek_to_beginning()
* seek_to_end()

** 制限事項: v0.9ではこの機能をサポートしない**

##### 例:
```
reader = MessageReader('service-1', 'topic-001')
with reader.open() as f:
    f.seek_to_beginning()
    for msg in f:
        print(msg.value.decode('utf-8'))
```

> 開発中のデバッグ、テストを目的の非公開API。将来のリリースで削除する可能性がある。

## MessageWriterクラス

### MessageWriter()

```
MessageWriter(
    service,
    topic,
    consistency=EXACTLY_ONCE,
    client_id=DEFAULT_CLIENT_ID,
    value_serializer=None,
    **kwargs)
```

MessageWriterクラスのコンストラクタ。

##### パラメータ:

* `service`
    * サービス名
    * 設定ファイルに対応するサービス名が記述されている必要がある
* `topic`
    * トピック名
* `consistency`
    * AT_MOST_ONCE (=0)
    * AT_LEAST_ONCE (=1)
    * EXACTLY_ONCE (=2)
* `client_id`
    * クライアントの名前
    * DEFAULT_CLIENT_ID, None, 空文字のいずれかが指定された場合はライブラリが値を自動生成する
* `value_serializer`
    * メッセージの値をバイト列に変換するための関数
* `kwargs`
    * 各メッセージングシステムを操作する際に用いるオブジェクトに設定する個別のパラメータ

`kwargs` に指定があった場合は、
メッセージングシステムを操作するための下位層のクラス `kafka.KafkProducer`などのコンストラクタにパラメータをそのまま渡す事ができる。
実際にどのようなパラメータを渡せるかは下位のライブラリに依存する。
このクラスでは指定されたパラメータの妥当性のチェックは行わず、そのままの形で下位のクラスに渡すこととする。

`service`以外の引数は、設定ファイルにデフォルト値を記述することができる。
設定ファイルとコンストラクタの引数の両方に同じパラメータの値を指定した場合はコンストラクタの引数に指定した値が優先する。

** 制限事項: v0.9では`consistency`に`EXACTLY_ONCE`を指定しても`AT_LEAST_ONCE`にダウングレードする**

##### 例外:

* NoServiceError
    * `service`に指定した値に対応するサービスが設定ファイルに存在しない
* NoConfigError
    * 設定ファイルが存在しない、あるいは読み込めない
* InvalidArgumentError
    * 指定した引数の形式が正しくない。`consistency`の値が範囲外、`topic`名として許容されない文字列などの場合
* UnsupportedServiceTypeError
    * 設定ファイルに指定されている`type`に対応するメッセージングシステムのプラグインがインストールされていない

##### 戻り値:

メッセージングシステムとの接続状態を保持しているハンドラ

**例:**
```
writer = MessageWriter('service-2', 'topic-002')
with writer as f:
    f.publish(b'test message 001')
    f.publish(b'test message 002')
```

##### 例外:

* ConnectionError
    * ブローカーへの接続がエラーになった
* AlreadyConnectedError
    * 既に接続状態のオブジェクトに対して、再度 open() を呼び出した場合

### プロパティ

コンストラクタの引数に指定した `service` などの値は読み取りのみのプロパティとしてアクセスできる。
`client_id`などのようにライブラリが値を自動生成するものについては、生成した値がプロパティとして取得できる。

## Messageクラス

各メッセージングシステムのメッセージオブジェクトのラッパークラス。

### プロパティ

全て読み取りアクセスのみ。

* value
    * メッセージの値部分
    * Kafka では raw.value, MQTT では raw.payload
    * デフォルトではメッセージの値のバイト列が得られる
    * MessageReaderに value_deserializer が設定されている場合は、その関数によってバイト列から変換されたオブジェクトが得られる
* topic
    * トピック名
* raw
    * 各メッセージングシステムのメッセージオブジェクト

<!--
# 確認事項

API Design の資料では、以下のパラメータが設定できることになっている。

* persistent_storage
* data_encryption

`persistent_storage`はメッセージを S3に保存するか否かの指定になっている。
これはメッセージングシステムとの間とのやり取りではなく、
-->

# 依存関係にあるライブラリ

* [kafka-python](https://kafka-python.readthedocs.io/en/master/)
* [mqtt client](https://www.eclipse.org/paho/clients/python/docs/)
