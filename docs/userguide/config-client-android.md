<!--
Copyright (C) 2023 National Institute of Informatics

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
[English](config-client-android.en.md)
-->
[English](https://translate.google.com/translate?hl=en&sl=ja&tl=en&u=https://nii-gakunin-cloud.github.io/sinetstream/docs/userguide/config-client-android.html "google translate")

# SINETStream ユーザガイド

## Android版の設定クライアント

<em>目次</em>
```
1. 概要
2. システム構成
3. 利用方法
  3.1 コンフィグサーバ側の準備
  3.2 Android端末側の操作（事前準備）
    3.2.1 コンフィグサーバからアクセストークンの取得
    3.2.2 手元の鍵対の生成
    3.2.3 公開鍵をコンフィグサーバに登録
  3.3 Android端末側の操作（設定情報のダウンロード）
  3.4 Android端末側の操作（ブローカとの通信）
4. まとめ
```


## 1. 概要

従来、[Android版のSINETStream設定](config-android.md)に関しては、
`YAML`形式の設定ファイルを規定の場所に配置する方式としていた。

しかし、この「Android端末で個別に設定ファイルを管理する」方式だと、
以下に示すような運用上の課題がある。

* パラメータ変更の度に設定ファイル内容を更新する必要がある\[1\]。
  * 対応策1：手動でファイル編集する。
  * 対応策2：ユーザアプリケーションが設定ファイルを自動生成して置き換える。
* SSL/TLS証明書や暗号化鍵など秘匿情報の扱いが煩雑となる。
  * 規定の場所にパスワード付きで手動導入する必要がある。
  * 失効時の入れ替え操作も同様。
* 複数台のAndroid端末で同様の作業をする場合に人海戦術となる。
  * 作業が煩雑で間違いの元になる。

> \[1\]
> 別紙[チュートリアル](../tutorial-android/index.md)で
> 紹介しているサンプルアプリケーションは`対応策2`を採用している。

そこで、`SINETStream設定情報`や`秘匿情報`などを一元的に管理する`設定サーバ`（以降、[コンフィグサーバ](http://manual.config-server.sinetstream.net/manual/docs/home/)と記述）を導入する。
システム管理者が`コンフィグサーバ`側で諸々の管理を実施する一方、個々のAndroid端末側は`コンフィグサーバ`に接続するための接続情報だけを手元で管理する。
この「クライアント／サーバ」協調動作により、システム管理や運用上の負担を減らせるとともに、作業の効率化を図ることができる。

本書では、`コンフィグサーバ`を利用するAndroid端末の`設定クライアント`動作について概説する。


## 2. システム構成

本書で扱う3つの機能要素（Android端末、ブローカ、コンフィグサーバ）を以下に示す。
```
   Broker      Android Device                                 Config Server
  +-----+     +-----------------------------------------+     +-----------+
  |     |     | #--------------------+  #-------------+ |     | #-------+ |
  | ... | <=> | | User Application   |  | Web Browser | | <=> | |Server | |
  |     |     | +--------------------+  +-------------+ |     | |App    | |
  +-----+     +=========================================+     | +-------+ |
              |                                         |     |       |   |
              | +-------------+ +----------+ +--------+ |     |      [DB] |
              | | SINETStream | | pub/priv | | access | |     |           |
              | | Library     | | keypair  | | token  | |     +-----------+
              | +-------------+ +----------+ +--------+ |
              | | MQTT        |                         |
              | | Library     |                         |
              | +-------------+          Android System |
              +-----------------------------------------+
```
〈判例〉
* Android端末
  * ユーザアプリケーション
    * [Android版のSINETStreamライブラリ](./android.md)経由で`コンフィグサーバ`から設定情報を取得する。
    * 必要に応じて`公開鍵／秘密鍵の対`（以降、`鍵対`と表記）を作成する。
    * 取得した設定情報を基に対向ブローカと接続し、MQTT通信を行う。

  * ウェブブラウザ
    * コンフィグサーバから`アクセストークン`を手動で取得する。
    * コンフィグサーバに秘匿情報を取得するための公開鍵を手動で登録する。

* ブローカ
  * Android端末とのMQTT通信の相手となる。

* コンフィグサーバ
  * ウェブサービスとしてのUI/UXを提供し、`SINETStream設定情報`などをデータベースに反映する。
  * ユーザごとの`アクセストークン`や秘匿情報を管理する。


## 3. 利用方法

### 3.1 コンフィグサーバ側の準備

本章では、システム管理者が`コンフィグサーバ`側で実施すべき事項について述べる。
```
                                        Management PC         Config Server
                                      +-----------------+     +-----------+
                                      | #-------------+ |     | #-------+ |
                                      | | Web Browser |-------->|Server | |
                                      | +-------------+ |     | |App    | |
                                      +-----------------+     | +-------+ |
                                                              |       |   |
                                                              |      [DB] |
                                                              |           |
                                                              +-----------+
```

まずは管理端末上の適当なウェブブラウザで`コンフィグサーバ`のURLに接続し、
指定のユーザアカウントで[ログイン](http://manual.config-server.sinetstream.net/manual/docs/guide-01/)する。

当該ユーザアカウントに対して、以下の各項目を準備する。

* SINETStream設定の記述
  * ウェブインタフェース操作により、`SINETStream設定情報`の[登録／編集／削除](http://manual.config-server.sinetstream.net/manual/docs/guide-02/)作業を実施する。
  * 記法の詳細は[Android版のSINETStream設定](config-android.md)を参照のこと。

* アクセストークンの管理
  * `アクセストークン`とは、操作対象`コンフィグサーバ`への接続情報の組である。
    * サーバURL
    * ユーザアカウント
    * アクセスキー
    * 有効期限
  * ウェブインタフェース操作により、`アクセストークン`設定内容の[作成](http://manual.config-server.sinetstream.net/manual/docs/screen-711/)または[削除](http://manual.config-server.sinetstream.net/manual/docs/screen-721/)を実施する。
  * 情報セキュリティの観点から`アクセストークン`には有効期限が設定される。これが失効した場合は再発行する必要がある。

* ユーザから預かった公開鍵の登録
  * `SINETStream設定情報`に秘匿情報（SSL/TLS接続時の証明書など）を含める場合、
その内容は`ハイブリッド暗号化方式`で暗号化された状態で`コンフィグサーバ`のデータベースに保持される。
  * この暗号化処理のため、ユーザ側で作成した`鍵対`のうち`公開鍵`をコンフィグサーバに
[登録](http://manual.config-server.sinetstream.net/manual/docs/screen-211/)
または
[更新](http://manual.config-server.sinetstream.net/manual/docs/screen-231/)
を実施する。


### 3.2 Android端末側の操作（事前準備）

#### 3.2.1 コンフィグサーバからアクセストークンの取得

Android端末から`コンフィグサーバ`に接続するためには、前述の`アクセストークン`（実体はJSONファイル`auth.json`）に記載された有効な`アクセスキー`が必要である。

```
               Android Device                                 Config Server
              +-----------------------------------------+     +-----------+
              |                         #-------------+ |     | #-------+ |
              |                         | Web Browser |<--------|Server | |
              |                         +-------------+ |     | |App    | |
              +==================================|======+     | +-------+ |
              |                                  V      |     |       |   |
              |                              +--------+ |     |      [DB] |
              |                              | access | |     |           |
              |                              | token  | |     +-----------+
              |                              +--------+ |
              |                                         |
              |                          Android System |
              +-----------------------------------------+
```

このため、作業対象のAndroid端末に何らかの方法で上記`アクセストークン`のファイルを配置する。

* 手法1：ネットワーク経由の導入
  * Android端末上のウェブブラウザ（[Google Chrome](https://play.google.com/store/apps/details?id=com.android.chrome&hl=ja)など）で`コンフィグサーバ`に接続／ログインし、ウェブインタフェース操作でダウンロードする。
  * [Google Drive](https://www.google.com/intl/ja_jp/drive/)に配置した`アクセストークン`ファイルを手元にコピーする。
* 手法2：ファイルコピーによる導入
  * 外部記憶媒体（SDカード）に格納された`アクセストークン`ファイルを手元にコピーする。

> [Android版のSINETStreamライブラリ](./android.md)を`コンフィグサーバ`から設定情報を取得するよう指定して初期化すると、上記`アクセストークン`ファイルを選択するよう利用者に促すダイアログが表示される。
> ここで指定された`アクセストークン`の記載内容を基に、`コンフィグサーバ`への接続処理が走ることになる。


#### 3.2.2 手元の`鍵対`の生成

`コンフィグサーバ`から取得を想定している`SINETStream設定情報`で何らかの秘匿情報（ブローカとのSSL/TLS接続のための証明書など）を扱う場合に必要な手順である。

> ブローカとの通信路をSSL/TLSで暗号化しないなど、秘匿情報を扱わない場合は本章の記述は飛ばして構わない。

```
               Android Device
              +-----------------------------------------+
              | #--------------------+                  |
              | | User Application   |                  |
              | +--------------------+                  |
              +=====|===================================+
              |     |            ...................... |
              |     |            : Android Keystore   : |
              |     V            :                    : |
              | +-------------+  :  +----------+      : |
              | | SINETStream |---->| pub/priv |      : |
              | | Library     |  :  | keypair  |      : |
              | +-------------+  :  +----------+      : |
              |                  :....................: |
              |                          Android System |
              +-----------------------------------------+
```
[Android版のSINETStreamライブラリ](./android.md)が用意するAPI関数により、
Androidシステムの秘匿領域（[Android Keystore](https://developer.android.com/training/articles/keystore?hl=ja)）内部に`鍵対`を作成する。
この`鍵対`は、`エイリアス`（利用者指定の任意文字列）で識別する。

> <em>注意</em><br>
>
> 上記`Android Keystore`で管理される`鍵対`はアプリケーションごとに固有の領域が割り当てられる。すなわち他のアプリケーションと`鍵対`が干渉することはない。
> 同アプリケーションの削除と同時に`Android Keystore`内部も削除される。


#### 3.2.3 公開鍵をコンフィグサーバに登録

本章の記述内容はAndroid端末と`コンフィグサーバ`が協調動作する部分である。

前述の`鍵対`のうち公開鍵は`コンフィグサーバ`に登録する必要がある。
ウェブインタフェースの操作により手動で[登録](http://manual.config-server.sinetstream.net/manual/docs/screen-211/)する手段は用意されているが、
ここでは[Android版のSINETStreamライブラリ](./android.md)が用意するAPI関数によりネットワーク経由で`コンフィグサーバ`に公開鍵を登録する手法を紹介する。

```
               Android Device                                 Config Server
              +-----------------------------------------+     +-----------+
              | #--------------------+                  |     | #-------+ |
              | | User Application   |                  |     | |Server | |
              | +--------------------+                  |     | |App    | |
              +=====|===================================+     | +-------+ |
              |     |            ...................... |     |   A   |   |
              |     |   [token]  : Android Keystore   : |     |   | [DB]  |
              |     V     |      :                    : |     +---|-------+
              | +-------------+  :  +----------+      : |         |
              | | SINETStream |<----| pub/priv |      : |         |
              | | Library     |  :  | keypair  |      : |         |
              | +-------------+  :  +----------+      : |         |
              |        |         :....................: |         |
              |        |                 Android System |         |
              +--------|--------------------------------+         |
                       |                                          |
                       +---------------( NETWORK )----------------+
```

ユーザ指定の`エイリアス`を基に`Android Keystore`に格納した`鍵対`を抽出し、
その`公開鍵`を得る。
これと`アクセストークン`を引数として
[Android版のSINETStreamライブラリ](./android.md)
の`暗号鍵登録`用のAPI関数に処理を依頼する。
同ライブラリは、REST-API経由で`コンフィグサーバ`に公開鍵を登録する。


### 3.3 Android端末側の操作（設定情報のダウンロード）

本章の記述内容はAndroid端末と`コンフィグサーバ`が協調動作する部分である。

```
               Android Device                                 Config Server
              +-----------------------------------------+     +-----------+
              | #--------------------+                  |     | #-------+ |
              | | User Application   |                  |     | |Server | |
              | +--------------------+                  |     | |App    | |
              +=====|===================================+     | +-------+ |
              |     |            ...................... |     |   |   |   |
              |     |   [token]  : Memory             : |     |   | [DB]  |
              |     V     |      :                    : |     +---|-------+
              | +-------------+  :  +------+ +------+ : |         |
              | | SINETStream |---> |config| |secert| : |         |
              | | Library     |  :  |data  | |data  | : |         |
              | +-------------+  :  +------+ +------+ : |         |
              |        A         :....................: |         |
              |        |                 Android System |         |
              +--------|--------------------------------+         |
                       |                                          |
                       +---------------( NETWORK )----------------+

```

`アクセストークン`を引数として
[Android版のSINETStreamライブラリ](./android.md)の初期化用の
API関数に処理を依頼する。
同ライブラリは、REST-API経由で`コンフィグサーバ`から`SINETStream設定情報`
（SSL/TLS証明書のような秘密情報があればそれも）をダウンロードする。
ここで取得した内容はファイルに書き出すことなく、メモリ上で扱う。

> 設定項目に選択肢がある場合、利用者に指示を仰ぐためダイアログが表示される。



### 3.4 Android端末側の操作（ブローカとの通信）

`コンフィグサーバ`から取得した`SINETStream設定情報`を基に対向ブローカと接続し、MQTT通信を行う。

```
   Broker      Android Device
  +-----+     +-----------------------------------------+
  |     |     | #--------------------+                  |
  | ... |     | | User Application   |                  |
  |     |     | +--------------------+                  |
  +-----+     +=====|=====A=============================+
     A        |     |     |      ...................... |
     |        |     |     |      : Memory             : |
     |        |     V     |      :                    : |
     |        | +-------------+  :  +------+ +------+ : |
     |        | | SINETStream |<----|config| |secert| : |
     |        | | Library     |  :  |data  | |data  | : |
     |        | +-------------+  :  +------+ +------+ : |
     |        | | MQTT        |  :....................: |
     |        | | Library     |                         |
     |        | +-------------+                         |
     |        |        |                 Android System |
     |        +--------|--------------------------------+
     |                 |
     +---( NETWORK )---+
```

## 4. まとめ

[Android版のSINETStreamライブラリ](./android.md)が用意する
`設定クライアント`機能により、`SINETStream設定情報`などをダウンロードしてブローカに接続するまでの一連の操作内容を概説した。

