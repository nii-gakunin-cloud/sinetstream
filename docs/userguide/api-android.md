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

SINETStream ユーザガイド

# Android版のAPI

* パッケージ
    * jp.ad.sinet.stream.android.api

* 公開インタフェース
    * SinetStreamReader.SinetStreamReaderListener
    * SinetStreamWriter.SinetStreamWriterListener

* 公開クラス
    * SinetStreamReader
    * SinetStreamWriter


## インタフェース SinetStreamReader.SinetStreamReaderListener

### メソッド概要
* onError
    * エラー検出時に呼ばれる。
* onMessageReceived
    * 購読トピックのメッセージを受信したときに呼ばれる。
* onReaderStatusChanged
    * 受信可否状態が変化したときに呼ばれる。

### メソッド詳細
#### onReaderStatusChanged

```
void onReaderStatusChanged(boolean isReady)
```

* 説明：
    * 受信可否状態が変化したときに呼ばれる。
* 引数:
    * isReady - ブローカに「接続済みかつ登録済み」であれば真、さもなくば偽


#### onMessageReceived

```
void onMessageReceived(@NonNull
                       java.lang.String data)
```

* 説明：
    * 購読トピックのメッセージを受信したときに呼ばれる。
* 引数:
    * data - 受信メッセージ内容

#### onError

```
void onError(@NonNull
             java.lang.String description)
```

* 説明：
    * 何らかのエラー条件を満たしたときに呼ばれる。そのエラーはAndroid版のSINETStreamライブラリ内部、あるいはより下位層で検出したものかもしれない。
* 引数:
    * description - エラー内容の簡単な説明


## インタフェース SinetStreamWriter.SinetStreamWriterListener

### メソッド概要
* onError
    * エラー検出時に呼ばれる。
* onWriterStatusChanged
    * 送信可否状態が変化したときに呼ばれる。

### メソッド詳細
#### onWriterStatusChanged

```
void onWriterStatusChanged(boolean isReady)
```

* 説明：
    * 送信可否状態が変化したときに呼ばれる。
* 引数:
    * isReady - ブローカに「接続済み」であれば真、さもなくば偽

#### onError

```
void onError(@NonNull
             java.lang.String description)
```

* 説明：
    * 何らかのエラー条件を満たしたときに呼ばれる。そのエラーはAndroid版のSINETStreamライブラリ内部、あるいはより下位層で検出したものかもしれない。
* 引数:
    * description - エラー内容の簡単な説明


## クラスSinetStreamReader

* SINETStreamシステムにおいて、リーダ（= サブスクライバ）として機能するためのAPI関数一式を提供する。
* メッセージングシステムの性質上、下記のメソッドはいずれも非同期型の要求として扱う必要がある。
    * initialize
    * terminate
* 本クラスの利用者は、呼び出し側の[Activity](https://developer.android.com/guide/components/activities/intro-activities)において`SinetStreamReader.SinetStreamReaderListener`を実装し、処理結果やエラーの非同期通知を受けられるようにしなければならない。


### 入れ子クラス概要
* SinetStreamReader.SinetStreamReaderListener
    * 内部インタフェース


### コンストラクタ概要
* SinetStreamReader
    * SinetStreamReaderのインスタンスを生成する。


### メソッド概要
* initialize
    * ブローカに接続し、サブスクライバとしての初期処理を実施する。
* terminate
    * ブローカから切断し、確保した資源を解放する。


### コンストラクタ詳細

```
public SinetStreamReader(@NonNull
                         android.content.Context context)
```

* 説明：
    * SinetStreamReaderのインスタンスを生成する。
* 引数:
    * context - `SinetStreamReader.SinetStreamReaderListener`を実装したアプリケーション[コンテクスト](https://developer.android.com/reference/android/content/Context)、すなわち呼出側のActivityそのもの。
* 例外:
    * java.lang.RuntimeException - 付与のコンテクストが所用のリスナーを実装していない


### メソッド詳細
#### initialize

```
public void initialize(@NonNull
                       java.lang.String serviceName)
```

* 説明：
    * ブローカに接続し、サブスクライバとしての初期処理を実施する。
    * 接続パラメータは外部の[設定ファイル](config.md)で規定される。
* 引数:
    * serviceName - 設定ファイルで検索鍵となるサービス名

#### terminate

```
public void terminate()
```

* 説明：
    * ブローカから切断し、確保した資源を解放する。

## クラスSinetStreamWriter

* SINETStreamシステムにおいて、ライタ（= パブリッシャ）として機能するためのAPI関数一式を提供する。
* メッセージングシステムの性質上、下記のメソッドはいずれも非同期型の要求として扱う必要がある。
    * initialize
    * publish
    * terminate
* 本クラスの利用者は、呼び出し側の[Activity](https://developer.android.com/guide/components/activities/intro-activities)において`SinetStreamWriter.SinetStreamWriterListener`を実装し、処理結果やエラーの非同期通知を受けられるようにしなければならない。


### 入れ子クラス概要
* SinetStreamWriter.SinetStreamWriterListener
    * 内部インタフェース


### コンストラクタ概要
* SinetStreamWriter
    * SinetStreamWriterのインスタンスを生成する。


### メソッド概要
* initialize
    * ブローカに接続し、パブリッシャとしての初期処理を実施する。
* terminate
    * ブローカから切断し、確保した資源を解放する。
* publish
    * 付与のメッセージをブローカに発行する。


### コンストラクタ詳細

```
public SinetStreamWriter(@NonNull
                         android.content.Context context)
```

* 説明：
    * SinetStreamWriterのインスタンスを生成する。
* 引数:
    * context - `SinetStreamWriter.SinetStreamWriterListener`を実装したアプリケーション[コンテクスト](https://developer.android.com/reference/android/content/Context)、すなわち呼出側のActivityそのもの。
* 例外:
    * java.lang.RuntimeException - 付与のコンテクストが所用のリスナーを実装していない

### メソッド詳細
#### initialize

```
public void initialize(@NonNull
                       java.lang.String serviceName)
```

* 説明：
    * ブローカに接続し、パブリッシャとしての初期処理を実施する。
    * 接続パラメータは外部の[設定ファイル](config.md)で規定される。
* 引数:
    * serviceName - 設定ファイルで検索鍵となるサービス名

#### terminate

```
public void terminate()
```

* 説明：
    * ブローカから切断し、確保した資源を解放する。


#### publish

```
public void publish(@NonNull
                    java.lang.String message,
                    @Nullable
                    java.lang.Object userData)
```

* 説明：
    * 付与のメッセージをブローカに発行する。
* 引数:
    * message - 発行対象のメッセージ
    * userData - ユーザ指定の不透明オブジェクト。`WriterMessageCallback#onPublished()`でそのまま返却される。

