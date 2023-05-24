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

[English](api-java.en.md)

SINETStream ユーザガイド

# Java API

<pre>
1. 使用例
2. Java API クラス一覧
 2.1 MessageWriterFactory クラス
 2.2 MessageWriter クラス
 2.3 AsyncMessageWriter クラス
 2.4 MessageReaderFactory クラス
 2.5 MessageReader クラス
 2.6 AsyncMessageReader クラス
 2.7 Message クラス
 2.8 Metrics クラス
 2.9 例外一覧
3. メッセージングシステム固有のパラメータ
4. チートシートの表示方法
</pre>

## 1. 使用例

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

try (MessageWriter<String> writer = factory.getWriter()) {
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

try (MessageReader<String> reader = factory.getReader()) {
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

## 2. Java API クラス一覧

### 主要クラス

* jp.ad.sinet.stream.api.MessageWriter
    * メッセージを送信するクラス
* jp.ad.sinet.stream.api.AsyncMessageWriter
    * メッセージを送信するクラス(同非期API)
* jp.ad.sinet.stream.api.MessageReader
    * メッセージを受信するクラス
* jp.ad.sinet.stream.api.AsyncMessageReader
    * メッセージを受信するクラス(非同期API)
* jp.ad.sinet.stream.utils.MessageWriterFactory
    * `MessageWriter` オブジェクトを作成するためのファクトリクラス
* jp.ad.sinet.stream.utils.MessageReaderFactory
    * `MessageReader` オブジェクトを作成するためのファクトリクラス

### 2.1 MessageWriterFactory クラス

`MessageWriter` を取得するためのファクトリクラス。

複数のパラメータを指定して `MessageWriter` のインスタンスを構築するためのビルダークラス `MessageWriterFactoryBuilder` が内部クラスとして用意されている。
ビルダークラスでは以下のパラメータを指定できる。

* service(String)
    * サービス名
    * 対応するサービスが設定ファイルに記述されている必要がある
* topic(String)
    * メッセージの送信先となるトピック名
* configName(String)
    * コンフィグ名
    * コンフィグ名が指定されるとコンフィグサーバからコンフィグ情報を取得する。
    * コンフィグ情報のなかで定義されているサービスが1つしかないとわかっている場合はサービス名にnullを指定してもよい。
    * コンフィグ名が指定されなかった場合、コンフィグ情報を得るために設定ファイルが読み込まれる。
* clientId(String)
    * クライアントID
    * 指定されなかった場合は、値をライブラリ内部で自動生成する
* consistency(Consistency)
    * 列挙型で `AT_MOST_ONCE`, `AT_LEAST_ONCE`, `EXACTLY_ONCE` のいずれかの値をとる
    * デフォルト値は `AT_MOST_ONCE`
* valueType(ValueType)
    * メッセージのデータ本体部分（ペイロード）のタイプ
    * `MessageWriter.write()` が送信するメッセージは、このパラメータに対応するシリアライザによってバイト列に変換される
    * 標準パッケージでは `SimpleValueType.BYTE_ARRAY`, `SimpleValueType.TEXT` の何れかを指定する
        * `SimpleValueType.BYTE_ARRAY`(デフォルト値)を指定した場合、ペイロードの型は `byte[]` として処理する
        * `SimpleValueType.TEXT`を指定した場合、ペイロードの型は `java.lang.String` として処理する
    * 追加パッケージをインストールすることにより、サポートする`ValueType`の種類を増やすことができる
        * SINETStream v1.1 以降では画像タイプを追加するパッケージを提供している
        * 画像タイプを指定する場合は`valueType()`の引数に `new ValueTypeFactory().get("image")` を設定する
        * 画像タイプを指定した場合、ペイロードの型は `java.awt.image.BufferedImage` として処理する
* serializer(Serializer\<T\>)
    * メッセージのデータ本体のシリアライザ
    * このパラメータを指定しない場合、シリアライザは`valueType`の値から決定される
* dataEncryption(Boolean)
    * メッセージを暗号化の有効、無効の指定
    * 暗号化を有効にする場合は、暗号化に関するパラメータ `crypto` が設定ファイルなどで指定されている必要がある
* parameter(String key, Object value)
    * メッセージングシステム固有のパラメータを指定する。設定ファイルと異なる値を指定したい場合にマッピングを指定する
* parameters(Map\<String, Object\> parameters)
    * メッセージングシステム固有のパラメータを指定する。設定ファイルと異なる値を複数のマッピングペアで指定する

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

### 2.2 MessageWriter クラス

ブローカーにメッセージを送信するクラス。

ファクトリクラスのインスタンスに対して `getWriter()` を呼び出すことで、ライタークラス `MessageWriter` のインスタンスが取得できる。
`MessageWriter` には `AutoCloseable` が実装されているので try-with-resources 文を利用できる。
メッセージを送信するメソッド`write()`は送信処理が完了するまでブロックする。

以下に例を示す。

```
MessageWriterFactory<String> factory = MessageWriterFactory.<String>builder()
        .service("service-1").build();

try (MessageWriter<String> writer = factory.getWriter()) {
    writer.write("message-1");
}
```

### 2.3 AsyncMessageWriter クラス

ブローカーにメッセージを送信するクラス。

ファクトリクラスのインスタンスに対して `getAsyncWriter()` を呼び出すことで、ライタークラス `AsyncMessageWriter` のインスタンスが取得できる。
`AsyncMessageWriter` には `AutoCloseable` が実装されているので try-with-resources 文を利用できる。
メッセージを送信するメソッド`write()`は非同期処理であり [JDeferred](http://jdeferred.org/)の`Promise`オブジェクトを返す。

以下に例を示す。

```
MessageWriterFactory<String> factory = MessageWriterFactory.<String>builder()
        .service("service-1").build();

try (AsyncMessageWriter<String> writer = factory.getAsyncWriter()) {
    writer.write("message-1")
          .done(result -> System.out.println("write task done"))
          .fail(result -> System.out.println("write task failed"))
}
```

`write()`メソッドが返す`Promise`オブジェクトのメソッド `.done()`, `.fail()`を用いることで、
送信結果（成功、失敗）に応じた処理を設定することができる。`Promise`の主なメソッドを以下に示す。

* `done()`
    –  遅延オブジェクトの処理が正常に完了した場合にトリガーされる
* `fail()`
    –  遅延オブジェクトの処理中に例外が発生したした場合にトリガーされる
* `always()`
    –  遅延オブジェクトの処理結果によらず全ての場合にトリガーされる

### 2.4 MessageReaderFactory クラス

`MessageReader` を取得するためのファクトリクラス。

複数のパラメータを指定して `MessageReader` のインスタンスを構築するためのビルダークラス
`MessageReaderFactoryBuilder` が内部クラスとして用意されている。
ビルダークラスでは以下のパラメータを指定できる。

* service(String)
    * サービス名
    * 対応するサービスが設定ファイルに記述されている必要がある
* topic(String)
    * メッセージの受信元となるトピック名
    * topicsパラメータが指定されているときは無視される
* configName(String)
    * コンフィグ名
    * コンフィグ名が指定されるとコンフィグサーバからコンフィグ情報を取得する。
    * コンフィグ情報のなかで定義されているサービスが1つしかないとわかっている場合はサービス名にnullを指定してもよい。
    * コンフィグ名が指定されなかった場合、コンフィグ情報を得るために設定ファイルが読み込まれる。
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
    * メッセージのデータ本体部分（ペイロード）のタイプ
    * `MessageReader.read()` で受信するメッセージは、このパラメータに対応するデシリアライザによってバイト列から変換される
    * 標準パッケージでは `SimpleValueType.BYTE_ARRAY`, `SimpleValueType.TEXT` の何れかを指定する
        * `SimpleValueType.BYTE_ARRAY`(デフォルト値)を指定した場合、ペイロードの型は `byte[]` として処理する
        * `SimpleValueType.TEXT`を指定した場合、ペイロードの型は `java.lang.String` として処理する
    * 追加パッケージをインストールすることにより、サポートする`ValueType`の種類を増やすことができる
        * SINETStream v1.1 以降では画像タイプを追加するパッケージを提供している
        * 画像タイプを指定する場合は`valueType()`の引数に `new ValueTypeFactory().get("image")` を設定する
        * 画像タイプを指定した場合、ペイロードの型は `java.awt.image.BufferedImage` として処理する
* deserializer(Serializer\<T\>)
    * メッセージのデータ本体のデシリアライザ
    * このパラメータを指定しない場合、デシリアライザは`valueType`の値から決定される
* dataEncryption(Boolean)
    * 暗号化されたメッセージの復号処理の有効、無効の指定
    * 暗号化を有効にする場合は、暗号化に関するパラメータ `crypto` が設定ファイルなどで指定されている必要がある
* receiveTimeout(Duration)
    * `MessageReader` の `read()` メソッドがメッセージの到着を待つ最大待ち時間
* parameter(String key, Object value)
    * メッセージングシステム固有のパラメータを指定する。設定ファイルと異なる値を指定したい場合にマッピングを指定する
* parameters(Map\<String, Object\> parameters)
    * メッセージングシステム固有のパラメータを指定する。設定ファイルと異なる値を複数のマッピングペアで指定する

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

### 2.5 MessageReader クラス

ブローカーからメッセージを受信するクラス。

ファクトリクラスのインスタンスに対して `getReader()` を呼び出すことで、リーダークラス `MessageReader` のインスタンスが取得できる。
`MessageReader` には `AutoCloseable` が実装されているので try-with-resources 文を利用できる。
メッセージを受信するメソッド`read()`はメッセージを受信するか`receiveTimeout()`に指定されているタイムアウト時間が経過するまではブロックする。

以下に例を示す。

```
MessageReaderFactory<String> factory = MessageReaderFactory.<String>builder()
        .service("service-1").receiveTimeout(Duration.ofSeconds(60)).build();

try (MessageReader<String> reader = factory.getReader()) {
    Message<String> msg;
    while (Objects.nonNull(msg = reader.read())) {
        System.out.println("TOPIC: " + msg.getTopic() + " MESSAGE: " + msg.getValue());
    }
}
```

`read()` メソッドの返り値は `Message<T>` クラスのインスタンスになる。

### 2.6 AsyncMessageReader クラス

ブローカーからメッセージを受信するクラス。

ファクトリクラスのインスタンスに対して `getAsyncReader()` を呼び出すことで、
リーダークラス `AsyncMessageReader` のインスタンスが取得できる。

受信したメッセージを処理するには`addOnMessageCallback()`メソッドでメッセージを受信した際に呼び出されるコールバックを設定する。
コールバックの引数によって受信したメッセージが受け渡される。

以下に例を示す。
```
MessageReaderFactory<String> factory = MessageReaderFactory.<String>builder()
        .service("service-1").receiveTimeout(Duration.ofSeconds(60)).build();

AsyncMessageReader<String> reader = factory.getAsyncReader();
reader.addOnMessageCallback((msg) -> {
    System.out.println("TOPIC: " + msg.getTopic() + " MESSAGE: " + msg.getValue());
});

// 他の処理など

reader.close();
```

### 2.7 Message クラス

ブローカーから受信したメッセージのクラス。

* getTopic()
    * トピック名を取得する。
* getValue()
    * メッセージの値を取得する。
* getTimestamp()
    * メッセージ送信時刻(Unix時間;単位は秒)を取得する。
    * 値 `0` は時刻が設定されてないことを示す。
* getTimestampMicroseconds()
    * メッセージ送信時刻(Unix時間;単位はマイクロ秒単位)を取得する。
    * 値 `0` は時刻が設定されてないことを示す。

### 2.8 Metrics クラス

メトリクス情報のクラス。
Reader/Writerオブジェクトに対してgetMetrics()メソッドを呼び出すと得られる。

* MessageReader#getMetrics()
* AsyncMessageReader#getMetrics()
* MessageWriter#getMetrics()
* AsyncMessageWriter#getMetrics()

Reader/Writerオブジェクトに対してresetMetrics()メソッドを呼び出すとReader/Writerの統計情報がリセットされる。
引数 `reset_raw` にtrueを指定した場合に限り、SINETStreamの統計情報だけでなくメッセージングシステム固有の統計情報もリセットされる(可能であれば)。

* MessageReader#resetMetrics(boolean reset_raw)
* MessageWriter#resetMetrics(boolean reset_raw)
* AsyncMessageReader#resetMetrics(boolean reset_raw)
* AsyncMessageWriter#resetMetrics(boolean reset_raw)

引数 `reset_raw` なしのresetMetrics()メソッドはreset_raw=falseを指定したのと同じである。

* MessageReader#resetMetrics()
* MessageWriter#resetMetrics()
* AsyncMessageReader#resetMetrics()
* AsyncMessageWriter#resetMetrics()

> Eclipse Paho(SINETStreamのMQTTプラグインで使用しているMQTTクライアントライブラリ)は統計情報を提供してない。
> Kafkaにはメッセージングシステム固有の統計情報があるがリセット機能はない。

統計情報はSINETStreamメインライブラリとメッセージングシステムプラグインの境界で測定した値が使われる。
したがって、SINETStreamの暗号化機能が有効の場合は暗号化されたメッセージが測定される。
統計情報の更新タイミングはWriterではメッセージングシステムプラグインにデータ渡す直前(メッセージングシステムが実際に送信したかは関知しない)、
Readerではメッセージングシステムプラグインからデータを受け取った直後である。
圧縮に関する統計統計情報は例外で圧縮処理の前後で測定される。

```
  <writer>                      <reader>
  Application                   Application
    ↓                            ↑
  value serializer              value deserializer
    ↓                            ↑                ←msg_uncompressed_bytes_total
  compressor                    decompressor
    ↓                            ↑                ←msg_compressed_bytes_total
  Avro serializer               Avro deserializer
    ↓                            ↑
  encrypt                       decrypt
- - ↓  - - - - - - - - - - - - - ↑ - - - - - - - -←メトリクス測定境界
  messaging system → broker → messaging system
```

#### プロパティ

* getStartTime(), getStartTimeMillis()
    * 測定を開始した時刻(Unix時間)
        * getStartTime()が返す時刻の単位は秒
        * getStartTimeMillis()が返す時刻の単位はミリ秒
    * Reader/Writerオブジェクトを作成した時刻、またはリセットした時刻。
* getEndTime(), getEndTimeMillis()
    * 測定を終了した時刻(Unix時間)
        * getEndTime()が返す時刻の単位は秒
        * getEndTimeMillis()が返す時刻の単位はミリ秒
    * getMetrics()メソッドを呼んだ時刻
* getTime(), getTimeMillis()
    * 測定時間 (= EndTime - StartTime)
        * getTime()が返す時間の単位は秒
        * getTimeMillis()が返す時間の単位はミリ秒
* getMsgCountTotal()
    * 累積送受信メッセージ数
* getMsgCountRate()
    * 送受信メッセージ数レート
    * = msg_count_total / time
    * timeが0のときは0を返す。
* getMsgUncompressedBytesTotal()
    * ユーザデータ累積送受信メッセージ量(bytes)
    * value serializerを通した直後・value deserializerを通す直前
* getMsgCompressedBytesTotal()
    * ユーザデータ圧縮後累積送受信メッセージ量(bytes)
* getMsgCompressionRatio()
    * メッセージ圧縮率 (0に近い方が高圧縮率、1に近い方が低圧縮率)
    * = msg_compression_ratio / msg_uncompressed_bytes_total
* getMsgBytesTotal()
    * 累積送受信メッセージ量(bytes)
* getMsgBytesRate()
    * 送受信メッセージ量レート
    * = msg_bytes_total / time
    * timeが0のときは0を返す。
* getMsgSizeMin()
    * 最小送受信メッセージサイズ(bytes)
* getMsgSizeAvg()
    * 平均送受信メッセージサイズ(bytes)
    * = msg_bytes_total / msg_count_total
    * msg_count_totalが0のときは0を返す。
* getMsgSizeMax()
    * 最大送受信メッセージサイズ(bytes)
* getErrorCountTotal()
    * 累積エラー数
* getErrorCountRate()
    * エラーレート
    * = error_count_total / time
    * timeが0のときは0を返す。

* getRaw()
    * メッセージングシステム固有の統計情報

#### 使用例

受信したメッセージ数・バイト数を表示する。

```
try (MessageReader<String> reader = factory.getReader()) {
    // (1)
    Message<String> msg;
    while (Objects.nonNull(msg = reader.read())) {
        ;
    }
    Metrics metrics = reader.getMetrics();  // (1) からの累積の統計情報が得られる
    System.out.println("COUNT: " + metrics.getMsgCountTotal());
    System.out.println("BYTES: " + metrics.getMsgBytesTotal());
}
```

10メッセージごとに受信レートを表示する。

```
try (MessageReader<String> reader = factory.getReader()) {
    Message<String> msg;
    int count = 0;
    while (Objects.nonNull(msg = reader.read())) {
        count++;
        if (count == 10) {
            count = 0;
            Metrics metrics = reader.getMetrics();
            reader.resetMetrics();
            System.out.println("COUNT/s: " + metrics.getMsgCountRate());
            System.out.println("BYTES/s: " + metrics.getMsgBytesRate());
        }
    }
}
```

### 2.9 例外一覧

| 例外名 | メソッド名 | |
| ---  | --- | --- |
| NoConfigException | MessageReaderFactory#getReader() MessageReaderFactory#getAsyncReader() MessageWriterFactory#getWriter() MessageWriterFactory#getAsyncWriter()  | 設定ファイルを読み込めない |
| NoServiceException | MessageReaderFactory#getReader() MessageReaderFactory#getAsyncReader() MessageWriterFactory#getWriter() MessageWriterFactory#getAsyncWriter() | 指定したサービス名に対応するエントリが設定ファイルに存在しない |
| UnsupportedServiceException |MessageReaderFactory#getReader() MessageReaderFactory#getAsyncReader() MessageWriterFactory#getWriter() MessageWriterFactory#getAsyncWriter() | サポートしていないメッセージングシステムが指定された |
| ConnectionException |MessageReaderFactory#getReader() MessageReaderFactory#getAsyncReader() MessageWriterFactory#getWriter() MessageWriterFactory#getAsyncWriter() | ブローカーに接続できない |
| InvalidConfigurationException |MessageReaderFactory#getReader() MessageReaderFactory#getAsyncReader() MessageWriterFactory#getWriter() MessageWriterFactory#getAsyncWriter() | 設定ファイルの記述内容に誤りがある |
| SinetStreamIOException | MessageReaderFactory#getReader() MessageReaderFactory#getAsyncReader() MessageWriterFactory#getWriter() MessageWriterFactory#getAsyncWriter()  MessageReader\<T\>#read() MessageReader\<T\>#close() MessageWriter\<T\>#write(T) MessageWriter\<T\>#close() | メッセージングシステムとのIOでエラーが発生した |
| SinetStreamException | MessageReaderFactory#getReader() MessageReaderFactory#getAsyncReader() MessageWriterFactory#getWriter() MessageWriterFactory#getAsyncWriter() MessageReader\<T\>#read() MessageReader\<T\>#close() MessageWriter\<T\>write(T) MessageWriter\<T\>close() | SINETStreamに関するその他のエラー |
| InvalidMessageException | MessageReader\<T\>#read() | メッセージのフォーマットが間違っている |
| AuthenticationException | MessageReaderFactory#getReader() MessageReaderFactory#getAsyncReader() MessageWriterFactory#getWriter() MessageWriterFactory#getAsyncWriter() | ブローカーとの接続で認証エラーになった |
| AuthorizationException | MessageReader\<T\>#read() MessageWriter\<T\>#write() | 認可されない操作を行った(*) |

(*) メッセージングシステムのタイプや`Consistency`のパラメータによっては認可されない操作を行っても例外が発生しない場合がある。
1. MQTT(Mosquitto)では認可されない操作を行ってもブローカーがエラーを返さないため例外が発生しない
2. Kafkaのブローカーに対して`Consistency`に`AT_MOST_ONCE`を指定してメッセージの送信を行った場合、ブローカーからの応答を待たずに送信処理が完了するため、認可されない操作を行った場合も例外が発生しない

## 3. メッセージングシステム固有のパラメータ

* [Kafka固有のパラメータ](config-kafka.md)
* [MQTT固有のパラメータ](config-mqtt.md)
* [S3固有のパラメータ](config-s3.md)

## 4. チートシートの表示方法

APIのjarファイルを `java -jar` の後に指定して実行すると、チートシートが表示される。

```
$ java -jar SINETStream-api-1.6.0.jar

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
    configName(String configName)
        configuration name.
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
    valueType(ValueType valueType[=BYTE_ARRAY])
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
    configName(String configName)
        configuration name.
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
    valueType(ValueType valueType[=BYTE_ARRAY])
        The type of message.
```
