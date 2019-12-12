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

# Java API

* 使用例
* Java API クラス一覧
    * MessageWriterFactory クラス
    * MessageWriter クラス
    * MessageReaderFactory クラス
    * MessageReader クラス
    * 例外一覧
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

```
MessageWriterFactory<String> factory =
    MessageWriterFactory.<String>builder()
        .service("service-1")
        .topic("topic-1")
        .consistency(AT_LEAST_ONCE)
        .build();

try(MessageWriter<String> writer = factory.getWriter()) {
    writer.write("Hello! This is the 1st message.");
    writer.write("Hello! This is the 2nd message.");
}
```

まず、パラメータ `service`, `topic`, `consistency` を指定してファクトリオブジェクト `factory` を作成する。
この `factory` に対して `getWriter()` を呼び出し、メッセージを送信するためのライターを得る。
その後、ライターの `write()` を呼び出してメッセージをブローカーに送信する。

### メッセージ受信

サービス名 `service-1` に対応するメッセージングシステムのトピック `topic-1` からメッセージを受信する例を示す。

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

まず、パラメータ `service`, `topic`, `consistency`, `receiveTimeout` を指定してファクトリオブジェクト `factory` を作成する。
この `factory` に対して `getReader()` を呼び出し、メッセージを受信するためのリーダーを得る。
その後、リーダーの `read()` を呼び出してブローカーからメッセージを受信する。
リーダーの `read()` を呼び出したあと、`receiveTimeout` に指定した時間メッセージが取得できなかった場合、`read()` が `null` を返しループが終了する。


## Java API クラス一覧

### 主要クラス

* jp.ad.sinet.stream.api.MessageWriter
    * メッセージを送信するクラス
* jp.ad.sinet.stream.api.MessageReader
    * メッセージを受信するクラス
* jp.ad.sinet.stream.utils.MessageWriterFactory
    * `MessageWriter` オブジェクトを作成するためのファクトリクラス
* jp.ad.sinet.stream.utils.MessageReaderFactory
    * `MessageReader` オブジェクトを作成するためのファクトリクラス

### MessageWriterFactory

`MessageWriter` を取得するためのファクトリクラス。

複数のパラメータを指定して `MessageWriter` のインスタンスを構築するための
ビルダークラス `MessageWriterFactoryBuilder` が内部クラスとして用意されている。
ビルダークラスでは以下のパラメータを指定できる。

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
    * 列挙型で `text`, ``byte_array` のいずれかの値をとる
    * デフォルト値は `text`
    * この設定値によって、デフォルトのシリアライザが設定される
* serializer(Serializer\<T\>)
    * メッセージのシリアライザ
    * 指定されなかった場合は、`valueType`の値に応じたデフォルトのシリアライザを利用する
* dataEncryption(Boolean)
    * メッセージを暗号化の有効、無効の指定
    * 暗号化を有効にする場合は、暗号化に関するパラメータ `crypto` が設定ファイルなどで指定されている必要がある
* parameter(String key, Object value)
    * メッセージングシステム固有のパラメータを指定する
* parameters(Map\<String, Object\> parameters)
    * メッセージングシステム固有のパラメータを指定する

ビルダークラスのインスタンスを取得するには `MessageWriterFactory.builder()` を呼び出す。
また、ビルダーオブジェクトからファクトリオブジェクトを得るには `build()` を呼び出す。
以下に例を示す。

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

ファクトリクラスのインスタンスに対して `getWriter()` を呼び出すことで、ライタークラス
`MessageWriter` のインスタンスが取得できる。
`MessageWriter` には `AutoClosable` が実装されているので try-with-resources 文を利用できる。
以下に例を示す。

```
MessageWriterFactory<String> factory = MessageWriterFactory.<String>builder()
        .service("service-1").build();

try (MessageWriter<String> writer = factory.getWriter()) {
    writer.write("message-1");
}
```

### MessageReaderFactory

`MessageReader` を取得するためのファクトリクラス。

複数のパラメータを指定して `MessageReader` のインスタンスを構築するための
ビルダークラス `MessageReaderFactoryBuilder` が内部クラスとして用意されている。
ビルダークラスでは以下のパラメータを指定できる。

* service(String)
    * サービス名
    * 対応するサービスが設定ファイルに記述されている必要がある
* topic(String)
    * メッセージの受信元となるトピック名
* topics(Collection\<String\>)
    * メッセージの受信元となるトピックのコレクション
    * `MessageReader` では複数のトピックからメッセージを受信することができる
* clientId(String)
    * クライアントID
    * 指定されなかった場合は、値をライブラリ内部で自動生成する
* consistency(Consistency)
    * 列挙型で `AT_MOST_ONCE`, `AT_LEAST_ONCE`, `EXACTLY_ONCE` のいずれかの値をとる
    * デフォルト値は `AT_MOST_ONCE`
* valueType(ValueType)
    * メッセージのタイプ
    * 列挙型で `text`, ``byte_array` のいずれかの値をとる
    * デフォルト値は `text`
    * この設定値によって、デフォルトのデシリアライザが設定される
* deserializer(Serializer\<T\>)
    * メッセージのデシリアライザ
    * 指定されなかった場合は、`valueType` の値に応じたデフォルトのデシリアライザを利用する
* dataEncryption(Boolean)
    * 暗号化されたメッセージの復号処理の有効、無効の指定
    * 暗号化を有効にする場合は、暗号化に関するパラメータ `crypto` が設定ファイルなどで指定されている必要がある
* receiveTimeout(Duration)
    * `MessageReader` の `read()` メソッドがメッセージの到着を待つ最大待ち時間
* parameter(String key, Object value)
    * 設定ファイルと異なる値を指定したい場合にマッピングを指定する
* parameters(Map\<String, Object\> parameters)
    * 設定ファイルと異なる値を複数のマッピングペアで指定する

ビルダークラスのインスタンスを取得するには `MessageReaderFactory.builder()` を呼び出す。
また、ビルダーオブジェクトからファクトリオブジェクトを得るには `build()` を呼び出す。
以下に例を示す。

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

ファクトリクラスのインスタンスに対して `getReader()` を呼び出すことで、リーダークラス `MessageReader` のインスタンスが取得できる。
`MessageReader` には `AutoClosable` が実装されているので try-with-resources 文を利用できる。
以下に例を示す。

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

`read()` メソッドの返り値は `Message<T>` クラスのインスタンスになる。
`getTopic()` でトピック名が、`getValue()` でメッセージの値が取得できる。

### 例外一覧

| 例外名 | メソッド名 | |
| ---  | --- | --- |
| NoConfigException | MessageReaderFactory#getReader() MessageWriterFactory#getWriter() | 設定ファイルを読み込めない |
| NoServiceException | MessageReaderFactory#getReader() MessageWriterFactory#getWriter() | 指定したサービス名に対応するエントリが設定ファイルに存在しない |
| UnsupportedServiceException | MessageReaderFactory#getReader() MessageWriterFactory#getWriter() | サポートしていないメッセージングシステムが指定された |
| ConnectionException | MessageReaderFactory#getReader() MessageWriterFactory#getWriter() | ブローカーに接続できない |
| InvalidConfigurationException | MessageReaderFactory#getReader() MessageWriterFactory#getWriter()  | 設定ファイルの記述内容に誤りがある |
| SinetStreamIOException | MessageReaderFactory#getReader() MessageWriterFactory#getWriter() MessageReader\<T\>#read() MessageReader\<T\>#close() MessageWriter\<T\>#write(T) MessageWriter\<T\>#close() | メッセージングシステムとのIOでエラーが発生した |
| SinetStreamException | MessageReaderFactory#getReader() MessageWriterFactory#getWriter() MessageReader\<T\>#read() MessageReader\<T\>#close() MessageWriter\<T\>write(T) MessageWriter\<T\>close() | SINETStreamに関するその他のエラー |

## メッセージングシステム固有のパラメータ

* [Kafka固有のパラメータ](config-kafka.md)
* [MQTT固有のパラメータ](config-mqtt.md)

## チートシートの表示方法

APIのjarファイルを `java -jar` の後に指定して実行すると、チートシートが表示される。

```
$ java -jar SINETStream-api-1.0.0.jar

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
        The topic to send.
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
