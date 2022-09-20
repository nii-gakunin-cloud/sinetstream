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
[English](sinetstream_reader_bytes.en.md)
-->

SINETStream ユーザガイド: SinetStreamReaderBytes

# Android版のAPI

* パッケージ
    * jp.ad.sinet.stream.android.api

* 公開インタフェース
    * SinetStreamReaderBytes.SinetStreamReaderBytesListener

* 公開クラス
    * SinetStreamReaderBytes


## インタフェース SinetStreamReaderBytes.SinetStreamReaderBytesListener

<em>注意：<em><br>
基底クラスのインタフェース`SinetStreamReader.SinetStreamReaderListener\<T\>`をバイト列型（`byte[]`）に紐付けして継承したものであり、ここでの独自拡張はない。


### メソッド概要
* onError
    * エラー検出時に呼ばれる。
* onMessageReceived
    * 購読トピックのメッセージを受信したときに呼ばれる。
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
                       @NonNull byte[] data)
```

* 説明：
    * 購読トピックのメッセージを受信したときに呼ばれる。
* 引数:
    * topic - 受信メッセージが所属するトピック
    * timestamp - メッセージ発行日時を`UnixTime`形式で表現したもの
    * data - バイト列型データ


#### onError

```java
void onError(@NonNull java.lang.String description)
```

* 説明：
    * 何らかのエラー条件を満たしたときに呼ばれる。そのエラーはAndroid版のSINETStreamライブラリ内部、あるいはより下位層で検出したものかもしれない。
* 引数:
    * description - エラー内容の簡単な説明


## クラスSinetStreamReaderBytes

<em>注意：<em><br>
基底クラス`SinetStreamReader\<T\>`をバイト列型(`byte[]`)に紐付けして継承したものであり、ここでの独自拡張はない。


* SINETStreamシステムにおいて、リーダ（= サブスクライバ）として機能するためのAPI関数一式を提供する。
* メッセージングシステムの性質上、下記のメソッドはいずれも非同期型の要求として扱う必要がある。
    * initialize
    * terminate
* 本クラスの利用者は、呼び出し側の[Activity](https://developer.android.com/guide/components/activities/intro-activities)、
あるいは[Fragment](https://developer.android.com/guide/components/fragments)において、
`SinetStreamReaderBytes.SinetStreamReaderBytesListener\<byte[]\>`を実装し、処理結果やエラーの非同期通知を受けられるようにしなければならない。


### 入れ子クラス概要
* SinetStreamReaderBytes.SinetStreamReaderBytesListener
    * 内部インタフェース


### コンストラクタ概要
* SinetStreamReaderBytes
    * SinetStreamReaderBytesのインスタンスを生成する。


### メソッド概要
* initialize
    * ブローカに接続し、サブスクライバとしての初期処理を実施する。
* terminate
    * ブローカから切断し、確保した資源を解放する。


### コンストラクタ詳細

```java
public SinetStreamReaderBytes(@NonNull android.content.Context context)
```

* 説明：
    * SinetStreamReaderBytesのインスタンスを生成する。
* 引数:
    * context - `SinetStreamReaderBytes.SinetStreamReaderBytesListener`を実装したアプリケーション[コンテクスト](https://developer.android.com/reference/android/content/Context)、すなわち呼出側の`Activity`または`Fragment`そのもの。
* 例外:
    * java.lang.RuntimeException - 付与のコンテクストが所用のリスナーを実装していない


### メソッド詳細
#### initialize

```java
public void initialize(@NonNull java.lang.String serviceName,
                       @NonNull java.lang.String alias)
```

* 説明：
    * ブローカに接続し、サブスクライバとしての初期処理を実施する。
    * 接続パラメータは外部の[設定ファイル](../config.md)で規定される。
    * ブローカ接続、およびトピック購読処理が完了したら、リスナー関数「onReaderStatusChanged(true)」でユーザインタフェースに通知する。
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


# 付録
## ライフサイクル

基底クラス`SinetStreamReader`から継承したいくつかのクラスを内部使用とすることにより、初期処理の扱いを簡素化する。

```
    ( constructor )
          |
          V
      initialize()
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

