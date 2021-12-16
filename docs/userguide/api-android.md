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

[English](api-android.en.md)

SINETStream ユーザガイド：Android版SINETStreamライブラリ

**目次**
<pre>
1. 概要
2. Android版SINETStreamライブラリの位置づけ
3. 総称型（Generics）の利用
4. Android版SINETStreamライブラリのAPI詳細
4.1 SinetStreamReader
4.2 SinetStreamReaderStream
4.3 SinetStreamReaderBytes
4.4 SinetStreamWriter
4.5 SinetStreamWriterStream
4.6 SinetStreamWriterBytes
</pre>


# 1. 概要

Android版SINETStreamライブラリが提供する公開API関数群およびインタフェース群に関して述べる。


# 2. Android版SINETStreamライブラリの位置づけ

ユーザアプリケーションがAndroid版SINETStreamライブラリを用いてMQTTメッセージの送受信を行う場合、概略以下のような3階層構成となる。
図中、二重線（=）で区切られた中央部分がAndroid版SINETStreamライブラリである。

```
===========================================================================
< User Application >

      #---------------------------+     #---------------------------+
      | UserAPP (Publisher)       |     | UserApp (Subscriber)      |
      +---------------------------+     +---------------------------+
              |              A                  |              A
==============|==============|==================|==============|===========
< SINETStream for Android >  |                  |              |
              |              |                  |              |
              V              |                  V              |
      +-------------+ +-----------+     +-------------+ +-----------+
      | SinetStream | | WriterXXX |     | SinetStream | | WriterXXX |
      | WriterXXX   | | Listener  |     | ReaderXXX   | | Listener  |
      +-------------+ +-----------+     +-------------+ +-----------+
                ...                                  ...
      +---------------------------+     +---------------------------+
      | MqttAsyncMessageWriter    |     | MqttAsyncMessageReader    |
      +---------------------------+     +---------------------------+
                 |                                    A
=================|====================================|====================
< Paho MQTT library for Android >                     |
                 |                                    |
                 V                                    |
      +---------------------------+     +---------------------------+
      | MqttAndroidClient         |     | MqttAndroidClient         |
      +---------------------------+     +---------------------------+
      +-------------------------------------------------------------+
      | MqttService                                                 |
      +-------------------------------------------------------------+
                 |                                    A
                 V                                    |
           MQTT messages                        MQTT messages
```


# 3. 総称型（Generics）の利用

`Reader`や`Writer`を表現する基本クラスは総称型（Generics）の抽象クラスとする。
これらを文字列型やバイト列型など特定のデータ型用に拡張した具象クラスを併せて用意し、ユーザアプリケーションは用途に応じて後者を使うことを想定する。

例えば、`SinetStreamWriter<T>`クラスで定義するメッセージ発行メソッド`publish(T data)`は「総称型」の引数`data`を取る。

```java
      public abstract class
      SinetStreamWriter<T>
      +---------------------+
      | initialize()        |
      | terminate()         |
      | publish(T data)     |
      | ...                 |
      +---------------------+
```

この`SinetStreamWriter<T>`クラスを「文字列型」として拡張した`SinetStreamWriterString`クラスでは、上記`publish`の引数は「文字列型」を取るものとして再定義される。
同様に、「バイト列型」として拡張した`SinetStreamWriterBytes`の場合は`publish`の引数は「バイト列型」を取るものとして再定義される。

```java
       public class
       SinetStreamWriterString
         extends SinetStreamWriter<String>
      +-------------------------+
      |                         |
      |  public abstract class  |
      |  SinetStreamWriter<T>   |
      | +---------------------+ |
      | | initialize()        | |
      | | terminate()         | |
      | | publish(T data)   <------ publish(String data)
      | | ...                 | |
      | +---------------------+ |
      +-------------------------+

       public class
       SinetStreamWriterBytes
         extends SinetStreamWriter<byte[]>
      +-------------------------+
      |                         |
      |  public abstract class  |
      |  SinetStreamWriter<T>   |
      | +---------------------+ |
      | | initialize()        | |
      | | terminate()         | |
      | | publish(T data)   <------ publish(byte[] data)
      | | ...                 | |
      | +---------------------+ |
      +-------------------------+
```

インタフェース定義に関しても考え方は同様である。
例えば、インタフェース`SinetStreamReaderListener<T>`は、受信メッセージ通知用のコールバック関数`onReceiveData`の第3引数で「総称型」の`data`を持つ。
これを文字列に拡張した`SinetStreamReaderStringListener`では、同じ`onReceiveData`の第3引数は「文字列型」の`data`と再定義される。

```java
       public interface
       SinetStreamReaderStringListener
         extends SinetStreamReaderListener<String>
      +------------------------------------------+
      |                                          |
      |  public interface                        |
      |  SinetStreamReaderListener<T>            |
      | +--------------------------------------+ |
      | | ...                                  | |
      | | void                                 | |
      | | onReceiveData(@NonNull String topic, | |
      | |               long timestamp,        | |
      | |               @NonNull T data);   <-------- "@NonNull String data"
      | | ...                                  | |
      | +--------------------------------------+ |
      +------------------------------------------+
```


# 4. Android版SINETStreamライブラリのAPI詳細

## 4.1 SinetStreamReader

受信側の抽象クラスおよびインタフェース：
[SinetStreamReader](sinetstream_android_api/sinetstream_reader.md)

## 4.2 SinetStreamReaderStream

文字列型データ受信用の派生クラスおよびインタフェース：
[SinetStreamReaderStream](sinetstream_android_api/sinetstream_reader_string.md)

## 4.3 SinetStreamReaderBytes

バイト型データ受信用の派生クラスおよびインタフェース：
[SinetStreamReaderBytes](sinetstream_android_api/sinetstream_reader_bytes.md)

## 4.4 SinetStreamWriter

送信側の抽象クラスおよびインタフェース：
[SinetStreamWriter](sinetstream_android_api/sinetstream_writer.md)

## 4.5 SinetStreamWriterStream

文字列型データ送信用の派生クラスおよびインタフェース：
[SinetStreamWriterStream](sinetstream_android_api/sinetstream_writer_string.md)

## 4.6 SinetStreamWriterBytes

バイト列型データ送信用の派生クラスおよびインタフェース：
[SinetStreamWriterBytes](sinetstream_android_api/sinetstream_writer_bytes.md)

