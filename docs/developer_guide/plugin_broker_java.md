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
--->

[English](https://translate.google.com/translate?hl=en&sl=ja&tl=en&u=https://nii-gakunin-cloud.github.io/sinetstream/docs/developer_guide/plugin_broker_java.html "google translate")

# プラグイン開発ガイド(Messaging system / Java)

新たなメッセージングシステムをSINETStream (Java)で扱えるようにするためのプラグインを開発する手順について説明します。

## 1. はじめに

SINETStream v1.2では以下に示すメッセージングシステムに対応しています。

* [Apache Kafka](https://kafka.apache.org/)
* [MQTT](http://mqtt.org/)

上記のメッセージングシステムに対応するための処理はプラグインとして実装されています。
さらに、新たなプラグインを実装することで、上記に示したメッセージングシステム以外のものを
SINETStreamで扱えるようになります。

このドキュメントでは新たなメッセージングシステムをサポートするためのプラグインを開発する手順について説明します。

### 1.1 対象者

このドキュメントが対象としている読者を以下に示します。

* SINETStreamで新たなメッセージングシステムを利用できるようにしたい開発者
* SINETStream内部の詳細について理解したい開発者

### 1.2 前提知識

このドキュメントの説明は、以下の知識を有していることを前提としています。

* Java 8 
* [ServiceLoader](https://docs.oracle.com/javase/jp/8/docs/api/java/util/ServiceLoader.html)の利用方法
* SINETStream の Java APIの利用方法、設定ファイルの記述方法

## 2. SINETStream の内部構造について

プラグインを開発する手順を説明する前に、開発の際に必要となるSINETStreamの内部構造について説明します。

### 2.1 モジュール構成

SINETStreamのモジュール構成を以下の図に示します。

![モジュール構成](images/SINETStream-module.png)

青枠で示した部分がSINETStream本体です。
この部分はメッセージングシステムによらない機能を提供しています。
緑枠で示した部分がSINETStreamのプラグインです。
メッセージングシステムとのインターフェイスはこの部分に閉じた実装となります。
各メッセージングシステムに対する操作は、SPIを実装したプラグインを介して行われます。

モジュールの各部分についての簡単な説明を以下に記します。

* SINETStream本体
    * API
        * ライブラリとしてSINETStreamを利用する場合のインターフェースを定める
    * SPI
        * SINETStream本体とプラグインとの間のインターフェースを定める
    * シリアライザ
        * APIが受け取ったオブジェクトをバイト列に変換する機能
    * デシリアライザ
        * メッセージングシステムから取得したバイト列をオブジェクトに変換する機能
    * メタデータの埋め込み
        * メッセージにメタデータを付加する機能
        * SINETStream v1.1 で付加するメタデータはメッセージ送信のタイムスタンプとなる
    * メタデータの取り出し
        * メッセージに付加されているメタデータを取り出す機能
    * 暗号化
        * メッセージの暗号化を行う機能
    * 復号化
        * 暗号化されたメッセージを復号する機能
    * 設定ファイル読み込み
        * SINETStreamの設定ファイルを読み込む機能
    * プラグイン管理
        * SINETStreamのプラグインを管理する機能
* プラグイン
    * Kafkaプラグイン
        * Kafkaブローカとの間でメッセージの送受信を行う機能
        * 実際の処理はApache Kafka [Producer](https://kafka.apache.org/24/javadoc/index.html?org/apache/kafka/clients/producer/KafkaProducer.html), [Consumer](https://kafka.apache.org/24/javadoc/index.html?org/apache/kafka/clients/consumer/KafkaConsumer.html)の機能を呼び出すことで実現している
    * MQTTプラグイン
        * MQTTブローカとの間でメッセージの送受信を行う機能
        * 実際の処理は[Eclipse Paho](https://www.eclipse.org/paho/clients/java/)の機能を呼び出すことで実現している

### 2.2 処理シーケンス

SINETStreamでメッセージの送受信を行うためのクラスMessageReader, MesageWriter, AsyncMessageReader, AsyncMessageWriter の処理シーケンスについて説明します。

#### 2.2.1 MessageReader

以下に示すメッセージ受信処理を SINETStreamで行った場合のシーケンス図を示します。
ここではKafkaブローカーからメッセージを受信することを想定しています。

```java
MessageReaderFactory<String> factory =
    MessageReaderFactory.<String>builder()
            .service("kafka-service")
            .build();

try (MessageReader<String> reader = factory.getReader()) {
    Message<String> msg;
    while (Objects.nonNull(msg = reader.read())) {
        System.out.print(msg.getValue());
    }
}
```

![MessageReader](images/reader_sequence_java.png)

図中のクラスについての簡単な説明を以下に記します。

* ユーザプログラム
    * SINETStreamを利用するユーザプログラム
* MessageReaderFactory
    * `MessageReader`のファクトリクラス
* MessageReader
    * ユーザプログラムに対して、SINETStreamのメッセージ受信APIを提供するインタフェース
* KafkaMessageReader
    * Kafkaブローカーからメッセージを受信するKafkaプラグインのクラス
* KafkaMessageProvider
    * Kafkaのサービスクラス
* ServiceLoader
    * 登録されているサービスをロードする

#### 2.2.2 MessageWriter

以下に示すメッセージ送信処理を SINETStreamで行った場合のシーケンス図を示します。
ここではKafkaブローカーにメッセージを送信することを想定しています。

```java
MessageWriterFactory<String> factory =
    MessageWriterFactory.<String>builder()
            .service("kafka-service")
            .build();

try (MessageWriter<String> writer = factory.getWriter()) {
    for (String msg : messages) {
        writer.write(msg);
    }
}
```

![MessageWriter](images/writer_sequence_java.png)

図中のクラスについての簡単な説明を以下に記します。

* ユーザプログラム
    * SINETStreamを利用するユーザプログラム
* MessageWriterFactory
    * `MessageWriter`のファクトリクラス
* MessageWriter
    * ブローカーにメッセージを送信するSINETStreamのクラス
* KafkaMessageWriter
    * ユーザプログラムに対して、SINETStreamのメッセージ送信APIを提供するインタフェース
* KafkaMessageProvider
    * Kafkaのサービスクラス
* ServiceLoader
    * 登録されているサービスをロードする

#### 2.2.3 AsyncMessageReader

以下に示す非同期APIのメッセージ受信処理を SINETStreamで行った場合のシーケンス図を示します。
ここではKafkaブローカーからメッセージを受信することを想定しています。

```java
MessageReaderFactory<String> factory =
    MessageReaderFactory.<String>builder()
            .service("kafka-service")
            .build();

try (AsyncMessageReader<String> reader = factory.getAsyncReader()) {
    reader.addOnMessageCallback((message) -> {
        System.out.print(msg.getValue());
    });

    // 他の処理
    otherTask();
}
```

![AsyncMessageReader](images/async_reader_sequence_java.png)

図中のクラスについての簡単な説明を以下に記します。

* ユーザプログラム
    * SINETStreamを利用するユーザプログラム
* MessageReaderFactory
    * `AsyncMessageReader`のファクトリクラス
* AsyncMessageReader
    * ユーザプログラムに対して、SINETStreamの非同期メッセージ受信APIを提供するインタフェース
* KafkaAsyncMessageReader
    * Kafkaブローカーからメッセージを非同期受信するKafkaプラグインのクラス
* KafkaMessageProvider
    * Kafkaのサービスクラス
* ServiceLoader
    * 登録されているサービスをロードする

#### 2.2.4 AsyncMessageWriter

以下に示す非同期APIのメッセージ送信処理を SINETStreamで行った場合のシーケンス図を示します。
ここではKafkaブローカーにメッセージを送信することを想定しています。

```java
MessageWriterFactory<String> factory =
    MessageWriterFactory.<String>builder()
            .service("kafka-service")
            .build();

try (AsyncMessageWriter<String> writer = factory.getAsyncWriter()) {
    for (String msg : messages) {
        writer.write(msg)
            .then(r -> System.err.println("success"));
    }
}
```

![MessageWriter](images/async_writer_sequence_java.png)

図中のクラスについての簡単な説明を以下に記します。

* ユーザプログラム
    * SINETStreamを利用するユーザプログラム
* MessageWriterFactory
    * `MessageWriter`のファクトリクラス
* AsyncMessageWriter
    * ブローカーにメッセージを非同期で送信するSINETStreamのクラス
* KafkaMessageWriter
    * ユーザプログラムに対して、SINETStreamの非同期メッセージ送信APIを提供するインタフェース
* KafkaMessageProvider
    * Kafkaのサービスクラス
* ServiceLoader
    * 登録されているサービスをロードする

## 3. プラグインの実装方法

### 3.1 概要

SINETStreamでは [ServiceLoader](https://docs.oracle.com/javase/jp/8/docs/api/java/util/ServiceLoader.html) を用いてプラグインを実現しています。
プラグインを作成するためには以下の作業が必要となります。

* プロバイダ構成ファイルの作成
* サービスプロバイダの実装

それぞれの作業項目の詳細について以下に記します。

### 3.2 プロバイダ構成ファイルの作成

プロバイダ構成ファイルにサービスプロバイダを登録することで、
ServiceLoaderがプラグインを見つけることができるようになります。

構成ファイルはリソースディレクトリの`META-INF/services/`に配置します。
ファイル名はサービスプロバイダの完全修飾クラス名にする必要があります。
SINETStreamのメッセージ受信、送信に対応するサービスプロバイダの場合、以下のファイル名となります。

* メッセージ受信(同期API)に対応するサービスプロバイダ
    * `jp.ad.sinet.stream.spi.MessageReaderProvider`
* メッセージ送信(同期API)に対応するサービスプロバイダ
    * `jp.ad.sinet.stream.spi.MessageWriterProvider`
* メッセージ受信(非同期API)に対応するサービスプロバイダ
    * `jp.ad.sinet.stream.spi.AsyncMessageReaderProvider`
* メッセージ送信(非同期API)に対応するサービスプロバイダ
    * `jp.ad.sinet.stream.spi.AsyncMessageWriterProvider`

構成ファイルには、サービスプロバイダの実装クラスを完全修飾名で１クラス1行で記述します。

例えば Kafkaブローカーにメッセージ送信を行うクラス`jp.ad.sinet.stream.plugins.kafka.KafkaMessageProvider`を登録する場合、
以下の内容を構成ファイル`META-INF/services/jp.ad.sinet.stream.spi.MessageWriterProvider`に記します。

```
jp.ad.sinet.stream.plugins.kafka.KafkaMessageProvider
```

SINETStreamには４つのサービスプロバイダがありますが、１つのプラグインで全てのサービスプロバイダに対応する必要はありません。
サポートするものに対応する構成ファイルのみを作成してください。

### 3.3 サービスプロバイダの実装

#### 3.3.1 メッセージ送信(同期API)のためのクラス

メッセージ送信(同期API)を行うサービスプロバイダを実装するには、
以下に示すインターフェースの実装クラスを作成する必要があります。

* `jp.ad.sinet.stream.spi.MessageWriterProvider`
    * サービスプロバイダインタフェース
* `jp.ad.sinet.stream.spi.PluginMessageWriter`
    * メッセージ送信処理のインタフェース

`MessageWriterProvider`のメソッドを以下に示します。

* `PluginMessageWriter getWriter(WriterParameters params)`
    * メッセージングシステム固有の送信処理を行うWriterクラスを返す
    * 引数の`params`を通してSINETStreamの設定ファイルまたはファクトリクラス`MessageWriterFactory`で設定したパラメータが渡される
* `String getType()`
    * メッセージングシステムのタイプを表す名前を返す
    * メッセージングシステム固有の処理については、このメソッドが返す値と設定ファイルの `type` に指定された値が一致したプラグインによって処理される

`PluginMessageWriter`の主なメソッドを以下に示します。

* `void write(byte[] message)`
    * ブローカーにメッセージを送信する
* `void close()`
    * ブローカーとの接続を切断する
* `Object getMetrics()`
    * サービスプロバイダ固有のメトリクス情報を返す
    * 固有の情報がなければ`null`を返す
* `void resetMetrics()`
    * サービスプロバイダ固有のメトリクス情報をリセットする

#### 3.3.2 メッセージ受信(同期API)のためのクラス

メッセージ受信(同期API)を行うサービスプロバイダを実装するには、
以下に示すインターフェースの実装クラスを作成する必要がある。

* `jp.ad.sinet.stream.spi.MessageReaderProvider`
    * メッセージングシステム固有の受信処理を行うReaderクラスを返す
* `jp.ad.sinet.stream.spi.PluginMessageReader`
    * メッセージ受信処理のインタフェース

`MessageReaderProvider`のメソッドを以下に示します。

* `PluginMessageReader getReader(ReaderParameters params)`
    * メッセージングシステム固有の受信処理を行うReaderクラスを返す
    * 引数の`params`を通してSINETStreamの設定ファイルまたはファクトリクラス`MessageReaderFactory`で設定したパラメータが渡される
* `String getType()`
    * メッセージングシステムのタイプを表す名前を返す
    * メッセージングシステム固有の処理については、このメソッドが返す値と設定ファイルの `type` に指定された値が一致したプラグインによって処理される

`PluginMessageReader`の主なメソッドを以下に示します。

* `PluginMessageWrapper read()`
    * ブローカーのメッセージを受信する
* `void close()`
    * ブローカーとの接続を切断する
* `Object getMetrics()`
    * サービスプロバイダ固有のメトリクス情報を返す
    * 固有の情報がなければ`null`を返す

#### 3.3.3 メッセージ送信(非同期API)のためのクラス

メッセージ送信(非同期API)を行うサービスプロバイダを実装するには、
以下に示すインターフェースの実装クラスを作成する必要があります。

* `jp.ad.sinet.stream.spi.AsyncMessageWriterProvider`
    * サービスプロバイダインタフェース
* `jp.ad.sinet.stream.spi.PluginAsyncMessageWriter`
    * メッセージ送信処理のインタフェース

`AsyncMessageWriterProvider`のメソッドを以下に示します。

* `PluginAsyncMessageWriter getAsyncWriter(WriterParameters params)`
    * メッセージングシステム固有の送信処理を行うWriterクラスを返す
    * 引数の`params`を通してSINETStreamの設定ファイルまたはファクトリクラス`MessageWriterFactory`で設定したパラメータが渡される
* `String getType()`
    * メッセージングシステムのタイプを表す名前を返す
    * メッセージングシステム固有の処理については、このメソッドが返す値と設定ファイルの `type` に指定された値が一致したプラグインによって処理される

`PluginAsyncMessageWriter`の主なメソッドを以下に示します。

* `Promise<?, ? extends Throwable, ?> write(byte[] message)`
    * ブローカーにメッセージを送信する
    * 戻り値は[JDeferred](https://github.com/jdeferred/jdeferred) の`Promise`を返す
* `void close()`
    * ブローカーとの接続を切断する
* `Object getMetrics()`
    * サービスプロバイダ固有のメトリクス情報を返す
    * 固有の情報がなければ`null`を返す

#### 3.3.4 メッセージ受信(非同期API)のためのクラス

メッセージ受信(非同期API)を行うサービスプロバイダを実装するには、
以下に示すインターフェースの実装クラスを作成する必要がある。

* `jp.ad.sinet.stream.spi.AsyncMessageReaderProvider`
    * メッセージングシステム固有の受信処理を行うReaderクラスを返す
* `jp.ad.sinet.stream.spi.PluginAsyncMessageReader`
    * メッセージ受信処理のインタフェース

`AsyncMessageReaderProvider`のメソッドを以下に示します。

* `PluginAsyncMessageReader getAsyncReader(ReaderParameters params)`
    * メッセージングシステム固有の受信処理を行うReaderクラスを返す
    * 引数の`params`を通してSINETStreamの設定ファイルまたはファクトリクラス`MessageReaderFactory`で設定したパラメータが渡される
* `String getType()`
    * メッセージングシステムのタイプを表す名前を返す
    * メッセージングシステム固有の処理については、このメソッドが返す値と設定ファイルの `type` に指定された値が一致したプラグインによって処理される

`PluginAsyncMessageReader`の主なメソッドを以下に示します。

* `void addOnMessageCallback(Consumer<PluginMessageWrapper> onMessage)`
    * メッセージを受信したときに呼び出すコールバック関数`onMessage`を登録する
    * コールバック関数はSINETStreamがデシリアライズする前のメッセージをラップした`PluginMessageWrapper`のオブジェクトを引数で渡す

* `void addOnMessageCallback(Consumer<PluginMessageWrapper> onMessage, Consumer<Throwable> onFailure)`
    * メッセージ受信したときのコールバック関数`onMessage`、エラーが発生したときのコールバック関数`onFailure`を登録する
    * 引数に`null`が指定された場合、対応するコールバック関数は登録されない
    * コールバック関数`onFailure`はエラーが発生した際の例外オブジェクトが引数で渡される
* `void close()`
    * ブローカーとの接続を切断する
* `Object getMetrics()`
    * サービスプロバイダ固有のメトリクス情報を返す
    * 固有の情報がなければ`null`を返す

## 4. プラグインの実装例

プラグイン実装の具体的な手順を示すために実装例を示します。

ここで示す実装例では実際のブローカーにアクセスするのではなく、プロセス内で
`java.util.Queue`オブジェクトを利用したデータの受け渡しを行う処理をSINETStreamのプラグインとして実現します。

### 4.1 ファイル構成

以下のファイルを作成します。

* src/main/java/ssplugin/
    * QueueMessageProvider.java
    * QueueMessage.java
    * QueueMessageReader.java
    * QueueMessageWriter.java
    * QueueAsyncMessageReader.java
    * QueueAsyncMessageWriter.java
* src/main/resources/META-INF/services/
    * jp.ad.sinet.stream.spi.MessageReaderProvider
    * jp.ad.sinet.stream.spi.MessageWriterProvider
    * jp.ad.sinet.stream.spi.AsyncMessageReaderProvider
    * jp.ad.sinet.stream.spi.AsyncMessageWriterProvider
* build.gradle
* settings.gradle

### 4.2 実装クラス

プラグインとして実装するクラスについて説明します。

> ここでは主な処理についてのみの説明となります。サンプルコード全体を確認する場合は「[ソースコード](#ソースコード)」のリンク先を参照してください。

#### 4.2.1 QueueMessageProvider.java

プラグインのプロバイダインタフェース`MessageReaderProvider`, `MessageWriterProvider`を実装したクラスになります。

```java
public class QueueMessageProvider implements MessageReaderProvider, MessageWriterProvider,
        AsyncMessageReaderProvider, AsyncMessageWriterProvider {

    private static final ConcurrentMap<String, BlockingQueue<QueueMessage>> queues = new ConcurrentHashMap<>();

    @Override
    public String getType() {
        return "queue";
    }

    @Override
    public PluginMessageReader getReader(ReaderParameters params) {
        String topic = params.getTopics().get(0);
        BlockingQueue<QueueMessage> queue = queues.computeIfAbsent(topic, key -> new LinkedBlockingQueue<>());
        return new QueueMessageReader(params, queue);
    }

    @Override
    public PluginMessageWriter getWriter(WriterParameters params) {
        String topic = params.getTopic();
        BlockingQueue<QueueMessage> queue = queues.computeIfAbsent(topic, key -> new LinkedBlockingQueue<>());
        return new QueueMessageWriter(params, queue);
    }

    @Override
    public PluginAsyncMessageReader getAsyncReader(ReaderParameters params) {
        String topic = params.getTopics().get(0);
        BlockingQueue<QueueMessage> queue = queues.computeIfAbsent(topic, key -> new LinkedBlockingQueue<>());
        return new QueueAsyncMessageReader(params, queue);
    }

    @Override
    public PluginAsyncMessageWriter getAsyncWriter(WriterParameters params) {
        String topic = params.getTopic();
        BlockingQueue<QueueMessage> queue = queues.computeIfAbsent(topic, key -> new LinkedBlockingQueue<>());
        return new QueueAsyncMessageWriter(params, queue);
    }
}
```

`getType()`でメッセージングシステムのタイプ名を返します。
`getReader()`でプラグインの`PluginMessageReader`実装となる`QueueMessageReader`オブジェクトを返します。
`getWriter()`でプラグインの`PluginMessageWriter`実装となる`QueueMessageWriter`オブジェクトを返します。
`getAsyncReader()`でプラグインの`PluginAsyncMessageReader`実装となる`QueueAsyncMessageReader`オブジェクトを返します。
`getAsyncWriter()`でプラグインの`PluginAsyncMessageWriter`実装となる`QueueAsyncMessageWriter`オブジェクトを返します。

`QueueMessageReader`, `QueueMessageWriter`, `QueueAsyncMessageReader`, `QueueAsyncMessageWriter`のコンストラクタには
`BlockingQueue`のオブジェクト `queue`を引数で渡します。`queue`を通してメッセージが受け渡されことになります。

#### 4.2.2 QueueMessageReader.java

`PluginMessageReader`を実装したクラスになります。

```java
public class QueueMessageReader implements PluginMessageReader {
(中略)
    @Override
    public PluginMessageWrapper read() {
        try {
            return queue.poll(receiveTimeout.getSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new SinetStreamIOException(e);
        }
    }
(中略)
}
```

`read()`はメッセージングシステムからメッセージの取得を行い、その値を返すメソッドになります。
ここでは `queue` からメッセージを取得して、その値を返しています。

#### 4.2.3 QueueMessageWriter.java

`PluginMessageWriter`を実装したクラスになります。

```java
public class QueueMessageWriter implements PluginMessageWriter {
(中略)
    @Override
    public void write(byte[] aByte) {
        QueueMessage msg = new QueueMessage(topic, aByte);
        try {
            queue.put(msg);
        } catch (InterruptedException e) {
            throw new SinetStreamIOException(e);
        }
    }
(中略)
}
```

`write()`は引数で渡されたバイト列をメッセージングシステムに送信するメソッドになります。
ここでは、引数で受け取ったバイト列を`QueueMessage`クラスでラップして `queue` に送っています。

#### 4.2.4 QueueAsyncMessageReader.java

`PluginAsyncMessageReader`を実装したクラスになります。

```java
public class QueueAsyncMessageReader implements PluginAsyncMessageReader {
(中略)
    public QueueAsyncMessageReader(ReaderParameters params, BlockingQueue<QueueMessage> queue) {
(中略)
        executor = Executors.newSingleThreadExecutor();
        future = executor.submit(this::pollingTask);
    }

    private void pollingTask() {
        try {
            while (!closed.get()) {
                onMessage(queue.take());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void onMessage(PluginMessageWrapper message) {
        for (Consumer<PluginMessageWrapper> callback : onMessageCallbacks) {
            try {
                callback.accept(message);
            } catch (Throwable ex) {
                onFailure(ex);
            }
        }
    }
(中略)
    @Override
    public void addOnMessageCallback(Consumer<PluginMessageWrapper> onMessage, Consumer<Throwable> onFailure) {
        if (Objects.nonNull(onMessage)) {
            onMessageCallbacks.add(onMessage);
        }
        if (Objects.nonNull(onFailure)) {
            onFailureCallbacks.add(onFailure);
        }
    }
(中略)
}
```

`addOnMessageCallback()`は、メッセージングシステムからメッセージの取得した際に呼び出すコールバック関数を登録します。
また、コンストラクタで起動した`executor`のスレッドで`pollingTask()` を実行し `queue` からのメッセージの取得を行います。
メッセージの取得に成功すると登録されているコールバック関数をメッセージを引数にして呼び出します。

#### 4.2.5 QueueAsyncMessageWriter.java

`PluginAsyncMessageWriter`を実装したクラスになります。

```java
public class QueueAsyncMessageWriter implements PluginAsyncMessageWriter {
(中略)
    private final DefaultDeferredManager manager =
            new DefaultDeferredManager(Executors.newFixedThreadPool(4));
(中略)
    @Override
    public Promise<?, ? extends Throwable, ?> write(byte[] bytes) {
        if (closed.get()) {
            throw new SinetStreamIOException();
        }
        return manager.when(() -> enqueue(bytes));
    }

    private void enqueue(byte[] bytes) {
        QueueMessage msg = new QueueMessage(topic, bytes);
        try {
            queue.put(msg);
        } catch (InterruptedException e) {
            throw new SinetStreamIOException(e);
        }
    }
(中略)
}
```

`PluginAsyncMessageWriter`は非同期APIを想定しているので、メッセージ送信処理の`write()`では`queue`への追加を直接は行っていません。
`manager.when()`を呼び出すことで`manager`が管理するスレッドプールに`queue`へのメッセージ追加のタスクを依頼しています。
そのため`write()`はブロックせずに直ぐに返ります。

### 4.3 プロバイダ構成ファイルの作成

リソースディレクトリの`META-INF/services/`に４つの構成ファイルを以下の内容で作成します。

* `jp.ad.sinet.stream.spi.MessageReaderProvider`
```
ssplugin.QueueMessageProvider
```
* `jp.ad.sinet.stream.spi.MessageWriterProvider`
```
ssplugin.QueueMessageProvider
```
* `jp.ad.sinet.stream.spi.AsyncMessageReaderProvider`
```
ssplugin.QueueMessageProvider
```
* `jp.ad.sinet.stream.spi.AsyncMessageWriterProvider`
```
ssplugin.QueueMessageProvider
```

### 4.4 jarファイルの作成

プラグインのjarファイルを作成する手順を以下に示します。

1. [Gradle](https://gradle.org)をインストールする
    * 参考: [インストール手順](https://gradle.org/install/)
2. gradle を実行して jar ファイルを作成する
```bash
$ gradle jar
```
3. `build/libs/`にjarファイルが作成されたことを確認する
```bash
$ ls build/libs/
SINETStream-queue-1.2.0.jar
```

### 4.5 ソースコード
プラグインの実装例となるファイルへのリンクを以下に示します。

* src/main/java/ssplugin/
    * [QueueMessageProvider.java](https://github.com/nii-gakunin-cloud/sinetstream/blob/master/docs/developer_guide/sample/messaging-system/java/src/main/java/ssplugin/QueueMessageProvider.java)
    * [QueueMessage.java](https://github.com/nii-gakunin-cloud/sinetstream/blob/master/docs/developer_guide/sample/messaging-system/java/src/main/java/ssplugin/QueueMessage.java)
    * [QueueMessageReader.java](https://github.com/nii-gakunin-cloud/sinetstream/blob/master/docs/developer_guide/sample/messaging-system/java/src/main/java/ssplugin/QueueMessageReader.java)
    * [QueueMessageWriter.java](https://github.com/nii-gakunin-cloud/sinetstream/blob/master/docs/developer_guide/sample/messaging-system/java/src/main/java/ssplugin/QueueMessageWriter.java)
    * [QueueAsyncMessageReader.java](https://github.com/nii-gakunin-cloud/sinetstream/blob/master/docs/developer_guide/sample/messaging-system/java/src/main/java/ssplugin/QueueAsyncMessageReader.java)
    * [QueueAsyncMessageWriter.java](https://github.com/nii-gakunin-cloud/sinetstream/blob/master/docs/developer_guide/sample/messaging-system/java/src/main/java/ssplugin/QueueAsyncMessageWriter.java)
* src/main/resources/META-INF/services/
    * [jp.ad.sinet.stream.spi.MessageReaderProvider](https://github.com/nii-gakunin-cloud/sinetstream/blob/master/docs/developer_guide/sample/messaging-system/java/src/main/resources/META-INF/services/jp.ad.sinet.stream.spi.MessageReaderProvider)
    * [jp.ad.sinet.stream.spi.MessageWriterProvider](https://github.com/nii-gakunin-cloud/sinetstream/blob/master/docs/developer_guide/sample/messaging-system/java/src/main/resources/META-INF/services/jp.ad.sinet.stream.spi.MessageWriterProvider)
    * [jp.ad.sinet.stream.spi.AsyncMessageReaderProvider](https://github.com/nii-gakunin-cloud/sinetstream/blob/master/docs/developer_guide/sample/messaging-system/java/src/main/resources/META-INF/services/jp.ad.sinet.stream.spi.AsyncMessageReaderProvider)
    * [jp.ad.sinet.stream.spi.AsyncMessageWriterProvider](https://github.com/nii-gakunin-cloud/sinetstream/blob/master/docs/developer_guide/sample/messaging-system/java/src/main/resources/META-INF/services/jp.ad.sinet.stream.spi.AsyncMessageWriterProvider)
* [build.gradle](https://github.com/nii-gakunin-cloud/sinetstream/blob/master/docs/developer_guide/sample/messaging-system/java/build.gradle)
* [settings.gradle](https://github.com/nii-gakunin-cloud/sinetstream/blob/master/docs/developer_guide/sample/messaging-system/java/settings.gradle)
