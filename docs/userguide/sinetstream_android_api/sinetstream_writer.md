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

<!-- NOTYET
[English](sinetstream_writer.en.md)
-->

SINETStream ユーザガイド: SinetStreamWriter\<T\>

# Android版のAPI

* パッケージ
    * jp.ad.sinet.stream.android.api

* 公開インタフェース
    * SinetStreamWriter.SinetStreamWriterListener\<T\>

* 公開抽象クラス
    * SinetStreamWriter\<T\>


## インタフェース SinetStreamWriter.SinetStreamWriterListener\<T\>

### メソッド概要
* onError
    * エラー検出時に呼ばれる。
* onPublished
    * ユーザ指定のメッセージ発行が完了した時に呼ばれる。
* onWriterStatusChanged
    * ブローカへのメッセージ送信可否状態が変化したときに呼ばれる。


### メソッド詳細
#### onWriterStatusChanged

```java
void onWriterStatusChanged(boolean isReady)
```

* 説明：
    * ブローカへのメッセージ送信可否状態が変化したときに呼ばれる。
    * 通知対象となる事象として以下の可能性がある。
        * ブローカへの接続完了
        * ブローカとの通信路切断
* 引数:
    * isReady - ブローカに「接続済み」であれば真、さもなくば偽


#### onPublished

```java
void onPublished(@NonNull T message,
                 @Nullable java.lang.Object userData)
```

* 説明：
    * ユーザ指定のメッセージ発行が完了した時に呼ばれる。
* 引数:
    * message - メソッド`publish()`で指定された総称型データ
    * userData - メソッド`publish()`で指定された任意のユーザデータ


#### onError

```java
void onError(@NonNull java.lang.String description)
```

* 説明：
    * 何らかのエラー条件を満たしたときに呼ばれる。そのエラーはAndroid版のSINETStreamライブラリ内部、あるいはより下位層で検出したものかもしれない。
* 引数:
    * description - エラー内容の簡単な説明


## クラスSinetStreamWriter\<T\>

* SINETStreamシステムにおいて、ライタ（= パブリッシャ）として機能するためのAPI関数一式を提供する。
* メッセージングシステムの性質上、下記のメソッドはいずれも非同期型の要求として扱う必要がある。
    * initialize
    * publish
    * terminate
* 本抽象クラスの利用者は、呼び出し側の[Activity](https://developer.android.com/guide/components/activities/intro-activities)、
あるいは[Fragment](https://developer.android.com/guide/components/fragments)において、
`SinetStreamWriter.SinetStreamWriterListener\<T\>`を実装し、処理結果やエラーの非同期通知を受けられるようにしなければならない。


### 入れ子クラス概要
* SinetStreamWriter.SinetStreamWriterListener\<T\>
    * 内部インタフェース


### コンストラクタ概要
* SinetStreamWriter
    * SinetStreamWriter\<T\>のインスタンスを生成する。


### メソッド概要
* initialize
    * メッセージライタのインスタンスを確保する。
* terminate
    * ブローカから切断し、確保した資源を解放する。
* isInitializationSuccess
    * 初期化処理が正常に完了したか否かを検査する。
* setup
    * ブローカに接続し、パブリッシャとしての初期処理を実施する。
* abort
    * 足回りで検出したエラー内容をリスナー関数「onError()」で通知する。
* publish
    * ユーザ指定のメッセージ発行を要求する。


### コンストラクタ詳細

```java
public SinetStreamWriter(@NonNull android.content.Context context)
```

* 説明：
    * SinetStreamWriterのインスタンスを生成する。
* 引数:
    * context - `SinetStreamWriter.SinetStreamWriterListener\<T\>`を実装したアプリケーション[コンテクスト](https://developer.android.com/reference/android/content/Context)、すなわち呼出側の`Activity`または`Fragment`そのもの。
* 例外:
    * java.lang.RuntimeException - 付与のコンテクストが所用のリスナーを実装していない


### メソッド詳細
#### initialize

```java
public void initialize(@NonNull java.lang.String serviceName,
                       @NonNull java.lang.String alias)
```

* 説明：
    * ユーザ指定のメッセージングシステム（現状ではMQTTのみに対応）の送信側、すなわち「メッセージライタ」のインスタンスを確保する。
    * 技術的には「メッセージライタ」の初期化処理は2段階で実施される。
      * `initialize()` -- 本メソッド：ライタインスタンスを確保する。
      * `setup()` -- 次メソッド：メッセージングシステムを介してブローカへの接続要求を発行する。
    * 接続パラメータは外部の[設定ファイル](../config.md)で規定される。
    * 処理中に何らかのエラーが発生した場合は、リスナー関数「onError()」で通知する。
* 引数:
    * serviceName - 設定ファイルで検索鍵となるサービス名
    * alias - システム秘匿領域に格納した「秘密鍵と証明書の対」に対応するエイリアス

#### terminate

```java
public void terminate()
```

* 説明：
    * ブローカから切断し、確保した資源を解放する。
    * 処理中に何らかのエラーが発生した場合は、リスナー関数「onError()」で通知する。


#### isInitializationSuccess

```java
public boolean isInitializationSuccess()
```

* 説明：
    * 初期化処理が正常に完了したか否かを検査する。
    * メソッド`initialize()`が正常終了した場合は特にイベント通知がないので、本メソッドで判定する。
    * 本メソッドの戻り値が「真」であれば、初期処理の後半部`setup()`を発行し、さもなくば中断処理`abort()`を発行すること。


#### setup

```java
public void setup()
```

* 説明：
    * 足回りのメッセージングシステム（MQTT）を介してブローカへの接続要求を発行する。
    * ブローカへの接続が完了したら、リスナー関数「onWriterStatusChanged(true)」でユーザインタフェースに通知する。
    * 処理中に何らかのエラーが発生した場合は、リスナー関数「onError()」で通知する。


#### publish

```java
public void publish(@NonNull T message, @Nullable java.lang.Object userData)
```

* 説明：
    * ユーザ指定の総称型データに関するメッセージ発行を要求する。
    * メッセージ発行処理が完了したら、リスナー関数「onPublished()」で通知する。
    * 処理中に何らかのエラーが発生した場合は、リスナー関数「onError()」で通知する。
* 引数:
    * message - 発行対象の総称型データ
    * userData - ユーザ指定の任意オブジェクト。リスナー関数「onPublished()」でそのまま返却される。


#### abort

```java
public void abort(@NonNull java.lang.String description)
```

* 説明：
    * 本抽象クラスが利用する足回りで何らかのエラーを検出した場合、当該エラー内容をリスナー関数「onError()」で通知する。
* 引数:
    * description - エラーメッセージ


#### getValueType

```java
public ValueType getValueType()
```

* 説明：
    * 本抽象クラスの拡張時に型付けされたValueTypeオブジェクトを取得する。
* 戻値:
    * valueType - ValueTypeオブジェクト


# 付録
## ライフサイクル

```
    ( constructor )
          |
          V
      initialize()
          |
          V
      isInitializationSuccess()
          |
          <>-------------------------------------------------+
      YES |                                               NO |
          V                                                  V
        setup()                                            abort()
          |
          |-----> onWriterStatusChanged(true)
          V
       publish()
          |
          |-----> onPublished()
          //           :
          |-----> onPublished()
          V
      terminate()
          |
          |-----> onWriterStatusChanged(false)
          V
```
