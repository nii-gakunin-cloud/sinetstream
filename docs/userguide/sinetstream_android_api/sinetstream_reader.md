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
[English](sinetstream_reader.en.md)
-->

[English](https://translate.google.com/translate?hl=en&sl=ja&tl=en&u=https://nii-gakunin-cloud.github.io/sinetstream/docs/userguide/sinetstream_android_api/sinetstream_reader.html "google translate")

SINETStream ユーザガイド: SinetStreamReader\<T\>

# Android版のAPI

* パッケージ
    * jp.ad.sinet.stream.android.api

* 公開インタフェース
    * SinetStreamReader.SinetStreamReaderListener\<T\>

* 公開抽象クラス
    * SinetStreamReader\<T\>


## インタフェース SinetStreamReader.SinetStreamReaderListener\<T\>

### メソッド概要
* onError
    * エラー検出時に呼ばれる。
* onMessageReceived
    * 購読トピックに関するメッセージを受信したときに呼ばれる。
* onReaderStatusChanged
    * ブローカからのメッセージ受信可否状態が変化したときに呼ばれる。

### メソッド詳細
#### onReaderStatusChanged

```java
void onReaderStatusChanged(boolean isReady)
```

* 説明：
    * ブローカからのメッセージ受信可否状態が変化したときに呼ばれる。
    * 通知対象となる事象として以下の可能性がある。
        * ブローカへの購読完了（ブローカと接続しただけでは通知されない）
        * ブローカからの購読解除
        * ブローカとの通信路切断
* 引数:
    * isReady - ブローカに「接続済みかつ登録済み」であれば真、さもなくば偽


#### onMessageReceived

```java
void onMessageReceived(@NonNull java.lang.String topic,
                       long timestamp,
                       @NonNull T data)
```

* 説明：
    * 購読トピックのメッセージを受信したときに呼ばれる。
* 引数:
    * topic - 受信メッセージが所属するトピック
    * timestamp - メッセージ発行日時を`UnixTime`形式で表現したもの
    * data - 総称型（`\<T\>`）データ


#### onError

```java
void onError(@NonNull java.lang.String description)
```

* 説明：
    * 何らかのエラー条件を満たしたときに呼ばれる。そのエラーはAndroid版のSINETStreamライブラリ内部、あるいはより下位層で検出したものかもしれない。
* 引数:
    * description - エラー内容の簡単な説明


## クラスSinetStreamReader\<T\>

* SINETStreamシステムにおいて、リーダ（= サブスクライバ）として機能するためのAPI関数一式を提供する。
* メッセージングシステムの性質上、下記のメソッドはいずれも非同期型の要求として扱う必要がある。
    * initialize + setup
    * terminate
* 本抽象クラスの利用者は、呼び出し側の[Activity](https://developer.android.com/guide/components/activities/intro-activities)、
あるいは[Fragment](https://developer.android.com/guide/components/fragments)において、
`SinetStreamReader.SinetStreamReaderListener\<T\>`を実装し、処理結果やエラーの非同期通知を受けられるようにしなければならない。


### 入れ子クラス概要
* SinetStreamReader.SinetStreamReaderListener\<T\>
    * 内部インタフェース


### コンストラクタ概要
* SinetStreamReader
    * SinetStreamReader\<T\>のインスタンスを生成する。


### メソッド概要
* initialize
    * メッセージリーダのインスタンスを確保する。
* terminate
    * ブローカから切断し、確保した資源を解放する。
* isInitializationSuccess
    * 初期化処理が正常に完了したか否かを検査する。
* setup
    * ブローカに接続し、サブスクライバとしての初期処理を実施する。
* abort
    * 足回りで検出したエラー内容をリスナー関数「onError()」で通知する。
* getValueType
    * 本抽象クラスの拡張時に型付けされたValueTypeオブジェクトを取得する。


### コンストラクタ詳細

```java
public SinetStreamReader(@NonNull android.content.Context context)
```

* 説明：
    * SinetStreamReaderのインスタンスを生成する。
* 引数:
    * context - `SinetStreamReader.SinetStreamReaderListener\<T\>`を実装したアプリケーション[コンテクスト](https://developer.android.com/reference/android/content/Context)、すなわち呼出側の`Activity`または`Fragment`そのもの。
* 例外:
    * java.lang.RuntimeException - 付与のコンテクストが所用のリスナーを実装していない


### メソッド詳細
#### initialize

```java
public void initialize(@NonNull java.lang.String serviceName,
                       @NonNull java.lang.String alias)
```

* 説明：
    * 本メソッドは、ユーザ指定のメッセージングシステム（現状ではMQTTのみに対応）の受信側、すなわち「メッセージリーダ」のインスタンスを確保する。
    * 技術的には「メッセージリーダ」の初期化処理は2段階で実施される。
      * `initialize()` -- 本メソッド：リーダインスタンスを確保する。
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
    * ブローカから（購読解除および）切断し、確保した資源を解放する。
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
    * ブローカへの接続が完了したら、続けて（ユーザインタフェースに通知することなく）指定トピックの購読をブローカに要求する。
    * トピック購読処理が完了したら、リスナー関数「onReaderStatusChanged(true)」でユーザインタフェースに通知する。
    * 処理中に何らかのエラーが発生した場合は、リスナー関数「onError()」で通知する。


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
          |-----> onReaderStatusChanged(true)
          |
          |-----> onMessageReceived()
          //              :
          |-----> onMessageReceived()
          V
      terminate()
          |
          |-----> onReaderStatusChanged(false)
          V
```

