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

[English](api-python.en.md)

SINETStream ユーザガイド

**準備中** (2020-06-05 18:05:13 JST)

# Python API

* 使用例
* Python API クラス一覧
    * MessageReader クラス
    * MessageWriter クラス
    * AsyncMessageReader クラス
    * AsyncMessageWriter クラス
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
バイト列以外のオブジェクトを渡すには、[MessageWriterクラス](#messagewriterクラス) のコンストラクタで `value_type` または `value_serializer` を指定する。

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
for ループから抜けるには、[MessageReaderクラス](#messagereaderクラス) のコンストラクタで `receive_timeout_ms` を指定するか、シグナル処理を行う必要がある。


## Python API クラス一覧

* sinetstream.MessageReader
    * メッセージングシステムからメッセージを取得するクラス
* sinetstream.AsyncMessageReader
    * メッセージングシステムからメッセージを取得するクラス(非同期API)
* sinetstream.MessageWriter
    * メッセージングシステムにメッセージを送信するクラス
* sinetstream.AsyncMessageWriter
    * メッセージングシステムにメッセージを送信するクラス(非同期API)
* sinetstream.Message
    * 送受信されるメッセージを表すクラス
* sinetstream.SinetError
    * SINETStreamの例外クラス全体の親クラス

### MessageReaderクラス

#### `MessageReader()`

MessageReaderクラスのコンストラクタ。

```
MessageReader(
    service,
    topics=None,
    consistency=AT_MOST_ONCE,
    client_id=DEFAULT_CLIENT_ID,
    value_type="byte_array",
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
    * メッセージのデータ本体部分（ペイロード）のタイプ名
    * ここで指定された値によって`MessageReader`が返すペイロードの型が定まる
    * 標準パッケージでは `"byte_array"`, `"text"` の何れかを指定する
        * `"byte_array"`(デフォルト値)を指定した場合、ペイロードの型は `bytes` となる
        * `"text"`を指定した場合、ペイロードの型は `str` となる
    * 追加パッケージをインストールすることにより、`value_type`に指定できるタイプ名を増やすことができる
        * SINETStream v1.1 以降では画像タイプを追加するパッケージを提供している
        * 追加されるタイプ名は `"image"` となる
        * `"image"`を指定し当た場合、ペイロードの型は `numpy.ndarray`（OpenCVの画像データ） となる
        * `numpy.ndarray`の画像データにおける色順序は OpenCV のもの（青、緑、赤）となる
* value_deserializer
    * メッセージのバイト列から値を復元（デシリアライズ）するために使用する関数
    * このパラメータを指定しない場合、`value_type`に指定した値によりデシリアライズする関数が定まる
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

**制限事項: Kafka の `consistency` に `EXACTLY_ONCE` を指定しても `AT_LEAST_ONCE` にダウングレードする。**

##### 例外

* NoServiceError
    * service に指定した値に対応するサービスが設定ファイルに存在しない
* NoConfigError
    * 設定ファイルが存在しない、あるいは読み込めない
* InvalidArgumentError
    * 指定した引数の形式が正しくない。 `consistency` の値が範囲外、 `topic` 名として許容されない文字列などの場合
* UnsupportedServiceTypeError
    * 設定ファイルに指定されている `type` に対応するメッセージングシステムのプラグインがインストールされていない

#### プロパティ

設定ファイルもしくはコンストラクタで指定したパラメータのうち、プロパティとして値を参照することが出来るものを以下に示す。

* `client_id`
* `consistency`
* `topics`
* `value_type`

#### `MessageReader.open()`

メッセージングシステムのブローカーに接続する。通常は明示的に呼び出すことはなく MessageReaderをwith文で
用いた場合に、暗黙的に呼び出されることを想定している。

##### 戻り値

メッセージングシステムとの接続状態を保持しているハンドラ

##### 例外

* ConnectionError
    * ブローカーへの接続がエラーになった
* AlreadyConnectedError
    * 既に接続状態のオブジェクトに対して、再度 open() を呼び出した場合

#### `MessageReader.close()`

メッセージングシステムのブローカーとの通信を切断する。通常は明示的に呼び出すことはなく MessageReaderをwith文で
用いた場合に、暗黙的に呼び出されることを想定している。

#### `MessageReader.__iter__()`

メッセージングシステムから取得したメッセージのイテレータを返す。

##### 例外

このメソッドが返したイテレータに対して `next()` を呼び出した場合に以下の例外が発生することがある。

* AuthorizationError
    * 認可されていないトピックに対してメッセージの取得を行った

メッセージングシステムによっては認可されていない操作をおこなっても上記の例外が発生しないことがある。
MQTT(Mosquitto)がこれに該当し、認可されていない操作を行っても例外が発生しない。これは
認可されていない操作を行った場合もブローカー側がクライアント側にエラーを返さないためである。

### AsyncMessageReaderクラス

#### `AsyncMessageReader()`

AsyncMessageReaderクラスのコンストラクタ。

```
AsyncMessageReader(
    service,
    topics=None,
    consistency=AT_MOST_ONCE,
    client_id=DEFAULT_CLIENT_ID,
    value_type="byte_array",
    value_deserializer=None,
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
    * メッセージのデータ本体部分（ペイロード）のタイプ名
    * ここで指定された値によって`AsyncMessageReader`が返すペイロードの型が定まる
    * 標準パッケージでは `"byte_array"`, `"text"` の何れかを指定する
        * `"byte_array"`(デフォルト値)を指定した場合、ペイロードの型は `bytes` となる
        * `"text"`を指定した場合、ペイロードの型は `str` となる
    * 追加パッケージをインストールすることにより、`value_type`に指定できるタイプ名を増やすことができる
        * SINETStream v1.1 以降では画像タイプを追加するパッケージを提供している
        * 追加されるタイプ名は `"image"` となる
        * `"image"`を指定し当た場合、ペイロードの型は `numpy.ndarray`（OpenCVの画像データ） となる
        * `numpy.ndarray`の画像データにおける色順序は OpenCV のもの（青、緑、赤）となる
* value_deserializer
    * メッセージのバイト列から値を復元（デシリアライズ）するために使用する関数
    * このパラメータを指定しない場合、`value_type`に指定した値によりデシリアライズする関数が定まる
* data_encryption
    * メッセージの暗号化、復号化の有効、無効を指定する
* kwargs
    * メッセージングシステム固有のパラメータを YAML のマッピングとして記述する

`kwargs` に記述されたパラメータは、バックエンドのメッセージングシステムのコンストラクタにそのまま渡される。
詳細は [メッセージングシステム固有のパラメータ](#メッセージングシステム固有のパラメータ) を参照。

`service` 以外の引数は、設定ファイルにデフォルト値を記述することができる。
設定ファイルとコンストラクタの引数の両方に同じパラメータの値を指定した場合は、コンストラクタの引数に指定した値が優先する。

**制限事項: Kafka の `consistency` に `EXACTLY_ONCE` を指定しても `AT_LEAST_ONCE` にダウングレードする。**

##### 例外

* NoServiceError
    * service に指定した値に対応するサービスが設定ファイルに存在しない
* NoConfigError
    * 設定ファイルが存在しない、あるいは読み込めない
* InvalidArgumentError
    * 指定した引数の形式が正しくない。 `consistency` の値が範囲外、 `topic` 名として許容されない文字列などの場合
* UnsupportedServiceTypeError
    * 設定ファイルに指定されている `type` に対応するメッセージングシステムのプラグインがインストールされていない

#### プロパティ

設定ファイルもしくはコンストラクタで指定したパラメータのうち、プロパティとして値を参照することが出来るものを以下に示す。

* `client_id`
* `consistency`
* `topics`
* `value_type`

#### `AsyncMessageReader.open()`

メッセージングシステムのブローカーに接続する。

##### 戻り値

メッセージングシステムとの接続状態を保持しているハンドラ

##### 例外

* ConnectionError
    * ブローカーへの接続がエラーになった
* AlreadyConnectedError
    * 既に接続状態のオブジェクトに対して、再度 open() を呼び出した場合

#### `AsyncMessageReader.close()`

メッセージングシステムのブローカーとの通信を切断する。通常は明示的に呼び出すことはなく AsyncMessageReaderをwith文で
用いた場合に、暗黙的に呼び出されることを想定している。

#### プロパティ: `AsyncMessageReader.on_message`

メッセージを受信した際に呼び出されるコールバック関数を設定する。

### MessageWriterクラス

#### `MessageWriter()`

```
MessageWriter(
    service,
    topic,
    consistency=AT_MOST_ONCE,
    client_id=DEFAULT_CLIENT_ID,
    value_type="byte_array",
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
    * メッセージのデータ本体部分（ペイロード）のタイプ名
    * ここで指定された値によって `MessageWriter.publish()` の引数に渡すデータの型が定まる
    * 標準パッケージでは `"byte_array"`, `"text"` の何れかを指定する
        * `"byte_array"`(デフォルト値)を指定した場合、ペイロードの型は `bytes` となる
        * `"text"`を指定した場合、ペイロードの型は `str` となる
    * 追加パッケージをインストールすることにより、`value_type`に指定できるタイプ名を増やすことができる
        * SINETStream v1.1 以降では画像タイプを追加するパッケージを提供している
        * 追加されるタイプ名は `"image"` となる
        * `"image"`を指定し当た場合、ペイロードの型は `numpy.ndarray` （OpenCVの画像データ）となる
        * `numpy.ndarray`の画像データにおける色順序は OpenCV のもの（青、緑、赤）となる
* value_serializer
    * メッセージの値をバイト列に変換（シリアライズ）するための関数
    * このパラメータを指定しない場合、`value_type`に指定した値によりシリアライズする関数が定まる
* data_encryption
    * メッセージの暗号化、復号化の有効、無効を指定する
* kwargs
    * メッセージングシステム固有のパラメータを YAML のマッピングとして記述する

`kwargs` に記述されたパラメータは、バックエンドのメッセージングシステムのコンストラクタにそのまま渡される。
詳細は [メッセージングシステム固有のパラメータ](#メッセージングシステム固有のパラメータ) を参照。

`service` 以外の引数は、設定ファイルにデフォルト値を記述することができる。
設定ファイルとコンストラクタの引数の両方に同じパラメータの値を指定した場合はコンストラクタの引数に指定した値が優先する。

**制限事項: Kafka の `consistency` に `EXACTLY_ONCE` を指定しても `AT_LEAST_ONCE` にダウングレードする。**

##### 例外

* NoServiceError
    * `service` に指定した値に対応するサービスが設定ファイルに存在しない
* NoConfigError
    * 設定ファイルが存在しない、あるいは読み込めない
* InvalidArgumentError
    * 指定した引数の形式が正しくない。`consistency` の値が範囲外、`topic` 名として許容されない文字列などの場合
* UnsupportedServiceTypeError
    * 設定ファイルに指定されている `type` に対応するメッセージングシステムのプラグインがインストールされていない

#### プロパティ

設定ファイルもしくはコンストラクタで指定したパラメータのうち、プロパティとして値を参照することが出来るものを以下に示す。

* `client_id`
* `consistency`
* `topic`
* `value_type`

#### `MessageWriter.open()`

メッセージングシステムのブローカーに接続する。通常は明示的に呼び出すことはなく MessageWriterをwith文で
用いた場合に、暗黙的に呼び出されることを想定している。

##### 戻り値

メッセージングシステムとの接続状態を保持しているハンドラ

##### 例外

* ConnectionError
    * ブローカーへの接続がエラーになった
* AlreadyConnectedError
    * 既に接続状態のオブジェクトに対して、再度 open() を呼び出した場合

#### `MessageWriter.close()`

メッセージングシステムのブローカーとの通信を切断する。通常は明示的に呼び出すことはなく MessageWriterをwith文で
用いた場合に、暗黙的に呼び出されることを想定している。

#### `MessageWriter.publish(message)`

メッセージをメッセージングシステムのブローカーに送信する。`message`は`MessageWriter`の
パラメータ`value_type`あるいは`value_serializer`によってシリアライズされたうえで
ブローカーに送信される。

##### 例外

* InvalidMessageError
    * `message`の型が `value_type`あるいは`value_serializer`に指定した値と整合しない
* AuthorizationError
    * 認可されていないトピックに対してメッセージの送信を行った

メッセージングシステムによっては認可されていない操作をおこなってもAuthorizationErrorの
例外が発生しないことがある。以下のケースが該当する。

1. MQTT(Mosquitto)の場合
    * 認可されていない操作を行った場合もブローカー側がクライアント側にエラーを返さない。そのため例外が発生しない。
1. Kafkaで`Consistency`に`AT_MOST_ONCE`を指定した場合
    * ブローカーの応答を待たずにクライアント側のメッセージの送信処理が完了する。そのため、ブローカー側の認可エラーを検知できず、例外が発生しない。

### AsyncMessageWriterクラス

#### `AsyncMessageWriter()`

```
AsyncMessageWriter(
    service,
    topic,
    consistency=AT_MOST_ONCE,
    client_id=DEFAULT_CLIENT_ID,
    value_type="byte_array",
    value_serializer=None,
    **kwargs)
```

AsyncMessageWriterクラスのコンストラクタ。

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
    * メッセージのデータ本体部分（ペイロード）のタイプ名
    * ここで指定された値によって `AsyncMessageWriter.publish()` の引数に渡すデータの型が定まる
    * 標準パッケージでは `"byte_array"`, `"text"` の何れかを指定する
        * `"byte_array"`(デフォルト値)を指定した場合、ペイロードの型は `bytes` となる
        * `"text"`を指定した場合、ペイロードの型は `str` となる
    * 追加パッケージをインストールすることにより、`value_type`に指定できるタイプ名を増やすことができる
        * SINETStream v1.1 以降では画像タイプを追加するパッケージを提供している
        * 追加されるタイプ名は `"image"` となる
        * `"image"`を指定し当た場合、ペイロードの型は `numpy.ndarray` （OpenCVの画像データ）となる
        * `numpy.ndarray`の画像データにおける色順序は OpenCV のもの（青、緑、赤）となる
* value_serializer
    * メッセージの値をバイト列に変換（シリアライズ）するための関数
    * このパラメータを指定しない場合、`value_type`に指定した値によりシリアライズする関数が定まる
* data_encryption
    * メッセージの暗号化、復号化の有効、無効を指定する
* kwargs
    * メッセージングシステム固有のパラメータを YAML のマッピングとして記述する

`kwargs` に記述されたパラメータは、バックエンドのメッセージングシステムのコンストラクタにそのまま渡される。
詳細は [メッセージングシステム固有のパラメータ](#メッセージングシステム固有のパラメータ) を参照。

`service` 以外の引数は、設定ファイルにデフォルト値を記述することができる。
設定ファイルとコンストラクタの引数の両方に同じパラメータの値を指定した場合はコンストラクタの引数に指定した値が優先する。

**制限事項: Kafka の `consistency` に `EXACTLY_ONCE` を指定しても `AT_LEAST_ONCE` にダウングレードする。**

##### 例外

* NoServiceError
    * `service` に指定した値に対応するサービスが設定ファイルに存在しない
* NoConfigError
    * 設定ファイルが存在しない、あるいは読み込めない
* InvalidArgumentError
    * 指定した引数の形式が正しくない。`consistency` の値が範囲外、`topic` 名として許容されない文字列などの場合
* UnsupportedServiceTypeError
    * 設定ファイルに指定されている `type` に対応するメッセージングシステムのプラグインがインストールされていない

#### プロパティ

設定ファイルもしくはコンストラクタで指定したパラメータのうち、プロパティとして値を参照することが出来るものを以下に示す。

* `client_id`
* `consistency`
* `topic`
* `value_type`

#### `AsyncMessageWriter.open()`

メッセージングシステムのブローカーに接続する。通常は明示的に呼び出すことはなく AsyncMessageWriterをwith文で
用いた場合に、暗黙的に呼び出されることを想定している。

##### 戻り値

メッセージングシステムとの接続状態を保持しているハンドラ

##### 例外

* ConnectionError
    * ブローカーへの接続がエラーになった
* AlreadyConnectedError
    * 既に接続状態のオブジェクトに対して、再度 open() を呼び出した場合

#### `AsyncMessageWriter.close()`

メッセージングシステムのブローカーとの通信を切断する。通常は明示的に呼び出すことはなく AsyncMessageWriterをwith文で
用いた場合に、暗黙的に呼び出されることを想定している。

#### `AsyncMessageWriter.publish(message)`

メッセージをメッセージングシステムのブローカーに送信する。`message`は`AsyncMessageWriter`の
パラメータ`value_type`あるいは`value_serializer`によってシリアライズされたうえで
ブローカーに送信される。

`publish(message)`は非同期処理であり [promise](https://github.com/syrusakbary/promise)の`Promise`オブジェクトを返す。
`Promise`オブジェクトのメソッド `.then()`, `.catch()`を用いることで、 
送信結果（成功、失敗）に応じた処理を設定することができる。使用例を以下に示す。

```python
with AsyncMessageWriter('service-1') as writer:
    writer.publish("message 1").then(lambda _: print("success")).catch(lambda _: print("failure"))
```

##### 例外

* InvalidMessageError
    * `message`の型が `value_type`あるいは`value_serializer`に指定した値と整合しない
* AuthorizationError
    * 認可されていないトピックに対してメッセージの送信を行った

メッセージングシステムによっては認可されていない操作をおこなってもAuthorizationErrorの
例外が発生しないことがある。以下のケースが該当する。

1. MQTT(Mosquitto)の場合
    * 認可されていない操作を行った場合もブローカー側がクライアント側にエラーを返さない。そのため例外が発生しない。
1. Kafkaで`Consistency`に`AT_MOST_ONCE`を指定した場合
    * ブローカーの応答を待たずにクライアント側のメッセージの送信処理が完了する。そのため、ブローカー側の認可エラーを検知できず、例外が発生しない。

### Messageクラス

メッセージングシステムのメッセージオブジェクトのラッパークラス。

#### プロパティ

全て読み取りアクセスのみ。

* value
    * メッセージのデータ本体部分（ペイロード）
    * `MessageWriter` の `value_type` に指定した値により`value`が返すデータの型が定まる
        * `value_type`に `"byte_array"`（デフォルト値)を指定した場合、データの型は `bytes` となる
        * `value_type`に `"text"`を指定した場合、データの型は `str` となる
* topic
    * トピック名
* timestamp
    * メッセージ送信時刻(Unix時間)
         * 単位は秒
         * 型はfloat
    * 値 `0` は時刻が設定されてないことを示す
* timestamp_us
    * メッセージ送信時刻(Unix時間)
        * 単位はマイクロ秒
        * 型はint
    * 値 `0` は時刻が設定されてないことを示す
* raw
    * メッセージングシステムのメッセージオブジェクト

### 例外一覧

|例外|発生元メソッド|理由|
|---|---|---|
|`NoServiceError`|`MessageReader()`, `MessageWriter()`, `AsyncMessageReader()`, `AsyncMessageWriter()`|指定したサービス名が設定ファイルで定義されていない。|
|`UnsupportedServiceTypeError`|`MessageReader()`, `MessageWriter()`, `AsyncMessageReader()`, `AsyncMessageWriter()`|サービスの定義で指定されているサービスタイプをサポートしていない。または対応するプラグインがインストールされていない。|
|`NoConfigError`|`MessageReader()`, `MessageWriter()`, `AsyncMessageReader()`, `AsyncMessageWriter()`|設定ファイルがない。|
|`InvalidArgumentError`|`MessageReader()`, `MessageWriter()`, `AsyncMessageReader()`, `AsyncMessageWriter()`, `MessageReader.open()`, `MessageWriter.open()`, `MessageWriter.publish()`, `AsyncMessageReader().open()`, `AsyncMessageWriter().open()`|引数が間違っている。|
|`ConnectionError`|`MessageReader.open()`, `MessageWriter.open()`, `MessageWriter.publish()`, `AsyncMessageReader().open()`, `AsyncMessageWriter().open()`|ブローカーとの接続に問題がある。|
|`AlreadyConnectedError`|`MessageReader.open()`, `MessageWriter.open()`, `AsyncMessageReader().open()`, `AsyncMessageWriter().open()`|すでにブローカと接続している。|
|`InvalidMessageError`|`MessageWriter.publish()`, `MessageReader.__iter__().__next__()`|メッセージのフォーマットが間違っている。|
|`AuthorizationError`|`MessageWriter.publish()`, `MessageReader.__iter__().__next__()`|権限のない操作を行った。|

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

### MQTT (Eclipse Paho)

基本的に
[paho.mqtt.client.Client](https://www.eclipse.org/paho/clients/python/docs/#client) の
コンストラクタと設定関数 (`XXX_set`) などの引数に指定できるパラメータを指定できる。

[MQTT固有のパラメータ](config-mqtt.md)

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
    value_type="byte_array",         # The type of message.
    value_deserializer=None          # If not specified, use default deserializer according to valueType.
)
MessageWriter(
    service=SERVICE,                 # Service name defined in the configuration file. (REQUIRED)
    topic=TOPIC,                     # The topic to send.
    consistency=AT_MOST_ONCE,        # consistency: AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE
    client_id=DEFAULT_CLIENT_ID,     # If not specified, the value is automatically generated.
    value_type="byte_array",         # The type of message.
    value_serializer=None            # If not specified, use default serializer according to valueType.
)
```
