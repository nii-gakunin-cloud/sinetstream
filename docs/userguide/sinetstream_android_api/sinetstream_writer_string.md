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
[English](sinetstream_writer_string.en.md)
-->

[English](https://translate.google.com/translate?hl=en&sl=ja&tl=en&u=https://nii-gakunin-cloud.github.io/sinetstream/docs/userguide/sinetstream_android_api/sinetstream_writer_string.html "google translate")

SINETStream ユーザガイド: SinetStreamWriterString

# Android版のAPI

* パッケージ
    * jp.ad.sinet.stream.android.api

* 公開インタフェース
    * SinetStreamWriterString.SinetStreamWriterStringListener

* 公開クラス
    * SinetStreamWriterString


## インタフェース SinetStreamWriterString.SinetStreamWriterStringListener

<em>注意：<em><br>
基底クラスのインタフェース`SinetStreamWriter.SinetStreamWriterListener\<T\>`を文字列型（`String`）に紐付けして継承したものであり、ここでの独自拡張はない。


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
void onPublished(@NonNull java.lang.String message,
                 @Nullable java.lang.Object userData)
```

* 説明：
    * ユーザ指定のメッセージ発行が完了した時に呼ばれる。
* 引数:
    * message - メソッド`publish()`で指定された文字列データ
    * userData - メソッド`publish()`で指定された任意のユーザデータ


#### onError

```java
void onError(@NonNull java.lang.String description)
```

* 説明：
    * 何らかのエラー条件を満たしたときに呼ばれる。そのエラーはAndroid版のSINETStreamライブラリ内部、あるいはより下位層で検出したものかもしれない。
* 引数:
    * description - エラー内容の簡単な説明



## クラスSinetStreamWriterString

<em>注意：<em><br>
基底クラス`SinetStreamWriter\<T\>`を文字列型（`String`）に紐付けして継承したものであり、ここでの独自拡張はない。


* SINETStreamシステムにおいて、「文字列データ」送信用のライタ（= パブリッシャ）として機能するためのAPI関数一式を提供する。
* メッセージングシステムの性質上、下記のメソッドはいずれも非同期型の要求として扱う必要がある。
    * initialize
    * publish
    * terminate
* 本クラスの利用者は、呼び出し側の[Activity](https://developer.android.com/guide/components/activities/intro-activities)、
あるいは[Fragment](https://developer.android.com/guide/components/fragments)において、
`SinetStreamWriterString.SinetStreamWriterStringListener\<String\>`を実装し、処理結果やエラーの非同期通知を受けられるようにしなければならない。


### 入れ子クラス概要
* SinetStreamWriterString.SinetStreamWriterStringListener
    * 内部インタフェース


### コンストラクタ概要
* SinetStreamWriterString
    * SinetStreamWriterStringのインスタンスを生成する。


### メソッド概要
* initialize
    * ブローカに接続し、パブリッシャとしての初期処理を実施する。
* terminate
    * ブローカから切断し、確保した資源を解放する。
* publish
    * 付与のメッセージをブローカに発行する。


### コンストラクタ詳細

```java
public SinetStreamWriterString(@NonNull android.content.Context context)
```

* 説明：
    * SinetStreamWriterStringのインスタンスを生成する。
* 引数:
    * context - `SinetStreamWriterString.SinetStreamWriterStringListener`を実装したアプリケーション[コンテクスト](https://developer.android.com/reference/android/content/Context)、すなわち呼出側の`Activity`または`Fragment`そのもの。
* 例外:
    * java.lang.RuntimeException - 付与のコンテクストが所用のリスナーを実装していない


### メソッド詳細
#### initialize

```java
public void initialize(@NonNull java.lang.String serviceName,
                       @NonNull java.lang.String alias)
```

* 説明：
    * ブローカに接続し、文字列送信用パブリッシャとしての初期処理を実施する。
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


#### publish

```java
public void publish(@NonNull java.lang.String message,
                    @Nullable java.lang.Object userData)
```

* 説明：
    * ユーザ指定の文字列データに関するメッセージ発行を要求する。
    * メッセージ発行処理が完了したら、リスナー関数「onPublished()」で通知する。
    * 処理中に何らかのエラーが発生した場合は、リスナー関数「onError()」で通知する。
* 引数:
    * message - 発行対象の文字列データ
    * userData - ユーザ指定の任意オブジェクト。リスナー関数「onPublished()」でそのまま返却される。


# 付録
## ライフサイクル

基底クラス`SinetStreamWriter`から継承したいくつかのクラスを内部使用とすることにより、初期処理の扱いを簡素化する。

```
    ( constructor )
          |
          V
      initialize()
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

