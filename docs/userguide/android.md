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

[English](https://translate.google.com/translate?hl=en&sl=ja&tl=en&u=https://nii-gakunin-cloud.github.io/sinetstream/docs/userguide/android.html "google translate")

# SINETStream for Android ユーザガイド

**目次**

```
1. 概要
2. 動作環境
    2.1 開発環境
    2.2 実行用のAndroid実機またはエミュレータ
    2.3 SINET接続環境
    2.4 接続相手（ブローカ）
3. 本書の記述範囲
4. 作業手順
    4.1 ビルド環境設定
        4.1.1 リポジトリ追加
        4.1.2 依存関係追加
    4.2 マニフェストファイルの記述
        4.2.1 利用者権限追加
        4.2.2 サービスコンポーネント追加
    4.3 SINETStream設定ファイル
        4.3.1 配置
        4.3.2 基本設定
        4.3.3 接続先URIの決定方法
        4.3.4 接続先ホスト・ポートが複数ある場合の振る舞い
    4.4 SSL/TLSの利用設定
        4.4.1 SINETStream設定ファイルへのSSL/TLS項目追記
        4.4.2 SSL/TLS証明書ファイル群の導入
    4.5 開発物のAndroid機材への導入
        4.5.1 Androidエミュレータに導入する場合
        4.5.2 Android実機に導入する場合
5. まとめ
```

## 概要

Android向けSINETStream APIの実装「SINETStream for Android」
（以降、簡素化のため「SINETStreamAndroid」と表記）を提供する
にあたり、これを利用するユーザアプリケーションの開発者が考慮
すべきことを概説する。

## 動作環境

### 開発環境

Google社が提供する統合開発環境「AndroidStudio」を導入する。

* [Download Android Studio and SDK tools](https://developer.android.com/studio)  
  ページ最下部に記載されたシステム要件に注意すること

### 実行用のAndroid実機またはエミュレータ

Android 8.0 (APIレベル26）以上

### SINET接続環境

SINET-SIM装着端末を利用、あるいはローカルネットワーク経由で
SINETに接続できる経路があること。

### 接続相手（ブローカ）

SINET内に所用のサーバを構築し実行しておく。

## 本書の記述範囲

SINETStreamAndroidを利用するユーザアプリケーションは、一般的に
下図のようなモジュール構成および依存関係となる。

```
                              ........................
                              : Option               :
                              :    +-----------+     :
                              :   / SSL/TLS     \    :
                           +--:--+  Cert File(s) +   :
                           |  :   \ 4)          /    :
                           |  :    +-----------+     :
                           |  :......................:
                           |
  #---------------------+  |       +-----------+
  | User Application    |--+      / SINETStream \
  | 1)                  |--------+  Config File  +
  +---------------------+         \ 3)          /
===========================        +-----------+
  +---------------------+   ---
  | SINETStreamAndroid  |    A
  +---------------------+    | External Libraries
  +---------------------+    | 2)
  | Paho MQTT Android   |    V
  +---------------------+   ---
===========================
  +---------------------+
  | Android OS / HW     |
  +---------------------+
             A
             | Internet
             V
      [ SINETStream ]


        ＜凡例＞
        1) 開発対象のユーザアプリケーション
        2) 参照する外部ライブラリ群
        3) SINETStream設定ファイル
        4) SSL/TLS接続用の証明書ファイル群
```

以降では、これらの各要素に関してアプリケーション開発者が設定
すべきことを記述する。

## 作業手順

### ビルド環境設定

Android開発環境では、Gradleによるビルド管理を行なっている。
ユーザアプリケーションがどの外部ライブラリを参照するかなどの
設定は制御ファイル「build.gradle」で指定する。

* [Gradle Build Tool](https://gradle.org/)
* [モジュールレベルビルドファイル](https://developer.android.com/studio/build#module-level)

#### リポジトリ追加

SINETStreamAndroid自身、およびその依存ライブラリであるPaho
MQTT Androidの取得先を定義する。

* [Using the Paho Android Client](https://github.com/eclipse/paho.mqtt.android#gradle)

「maven」ブロックに複数のURIをまとめて記述したくなるが、個別
に記述しないと文法エラーとなるので注意すること。
また「mavenCentral()」は「repositories」の最後に記述する。


```
----------（$TOP/app/build.gradle: 抜粋ここから）----------
repositories {
    [...]

    maven {
        // SINETStreamAndroid
        url "https://niidp.pages.vcp-handson.org/sinetstream-android/"
    }
    maven {
        // The Paho MQTT Android
        url "https://repo.eclipse.org/content/repositories/paho-snapshots/"
    }
    mavenCentral()
}
----------（$TOP/app/build.gradle: 抜粋ここまで）----------
```

#### 依存関係追加

リポジトリから参照するライブラリ名とバージョンを定義する。

* [ビルド依存関係の追加](https://developer.android.com/studio/build/dependencies)

ライブラリ開発元での実装状況の進展により、ここで記述したバー
ジョンより進んでいることがあるかもしれない。
その場合、AndroidStudioにより該当ライブラリのバージョン更新
を促される。

```
----------（$TOP/app/build.gradle: 抜粋ここから）----------
dependencies {
    // SINETStreamAndroid
    implementation 'jp.ad.sinet.stream.android:sinetstream-android:0.2.4'

    // The Paho MQTT Android
    implementation 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5'
    implementation 'org.eclipse.paho:org.eclipse.paho.android.service:1.1.1'
}
----------（$TOP/app/build.gradle: 抜粋ここまで）----------
```


### マニフェストファイルの記述

#### 利用者権限追加

SINETStreamでは内部でメッセージングシステム（例えばMQTT）を
用いてネットワーク接続が発生する。
このため、開発対象のユーザアプリケーションではインターネット
接続に必要な権限（uses-permission句）を定義する。
さもないと実行時エラーが発生する。

* [パーミッション](https://developer.android.com/guide/topics/manifest/manifest-intro#perms)

設定内容を以下に示す。

```
-----（$TOP/app/src/main/AndroidManifest.xml: 抜粋ここから）-----
<!-- インターネット接続に必要な権限 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
-----（$TOP/app/src/main/AndroidManifest.xml: 抜粋ここまで）-----
```

#### サービスコンポーネント追加

SINETStreamAndroidの下位層に位置する「Paho MQTT Android」ラ
イブラリは、ユーザインタフェースを提供するクライアント部分、
および常駐型サービスから構成される。
このため「Paho MQTT Android」が提供するサービス名（service句）
をマニフェストに定義する必要がある。

* [アプリのコンポーネント](https://developer.android.com/guide/topics/manifest/manifest-intro#components)

この設定を忘れてもユーザプログラム自体は起動するが、Androidが
当該サービスを起動しないため、publish/subscribeの要求が空回り
する。アプリケーションからの要求は受け付けられるもののネット
ワークとやりとりされない状態なのでエラーに気づきにくい。
ともあれ記述を忘れないこと。

設定内容を以下に示す。

```
-----（$TOP/app/src/main/AndroidManifest.xml: 抜粋ここから）-----
<application ... >
    <!-- 「Paho MQTT Android」のサービスプロセスを利用 -->
    <service android:name="org.eclipse.paho.android.service.MqttService" />
</application>
-----（$TOP/app/src/main/AndroidManifest.xml: 抜粋ここまで）-----
```

### SINETStream設定ファイル

#### 配置

SINETStreamを利用するユーザアプリケーションは、その動作内容
を規定するYAML形式の設定ファイル「sinetstream_config.yml」
を所定の位置に配置する必要がある。
開発ソースのリソース領域（$TOP/app/src/main/res）配下にサブ
ディレクトリ「raw」を手動で追加し、そこに配置する。

* [The Official YAML Web Site](https://yaml.org/)
* [リソースディレクトリを追加する](https://developer.android.com/studio/write/add-resources#add_a_resource_directory)

具体的な操作手順を以下に示す。

**1) `res` ディレクトリ選択：**

AndroidStudio画面左側のProjectペインにて「res」ディレクトリ
アイコンを選択（カーソル表示された状態）とする。

**2. `raw` サブディレクトリ作成：**

上記1)の状態でマウス右クリックまたは画面上部のFileメニュー
から「New -> Android Resource Directory」を選択する。
ポップアップウィンドウ「New Resource Directory」にて、上から
２番目のプルダウンメニュー「Resource type」から「raw」を選択
する。
その状態で「OK」ボタンを押下すると「res/raw」ディレクトリが
作成される。

**3. `sinetstream_config.yml` ファイル作成：**

AndroidStudio画面左側のProjectペインにて「raw」ディレクトリ
アイコンが選択された状態（カーソル表示）とする。
この状態でマウス右クリックまたは画面上部のFileメニューから
「New -> File」を選択するとファイル名の入力を促される。
ここでは「sinetstream_config.yml」を指定する。
Enterキー押下により同名の空ファイルが作成される。

**＜既知の問題＞**
> 上記のように、設定ファイルはソースの一部として組み込まれる。
> このため設定内容を変更するたびにユーザアプリケーションを再度
> 構築してAndroid実行環境に導入し直す必要がある。

#### 基本設定

SINETStream接続に必要なパラメータ群を記述する。
ここで書かれた内容は、アプリケーション固有のデータ領域
（/data/data/\<PACKAGE\>/files）配下に転記され、これが
「SINETStreamAndroid」内部で参照される。

設定ファイルの記述内容は必須項目とオプション項目がある。以下
に示す項目は必ず記述する必要がある。

* サービス名（任意文字列、検索鍵を兼ねる）
* メッセージシステム種別（type）
* 接続先ホスト・ポート（brokers）
* トピック名（topic）

オプション項目が省略された場合は「Paho MQTT Android」の既定
値が使われる。

#### 接続先URIの決定方法

接続先サーバ（ブローカ）URIは一般的に以下のように表現される。

```
schema://host[:port]
^^^^^ 1) ^^^^^^^^^^^ 2)
```

SINETStream設定ファイルにおける項目「brokers」で指定するのは
上記2)のホスト（＋ポート）部分の羅列であり、上記1)のスキーマ
部分はいくつかの条件の組み合わせで決まる。

| transport  | tls        | schema  |
|------------|------------|---------|
| 省略       | 省略       | tcp     |
| tcp        | なし       | tcp     |
| tcp        | あり       | ssl     |
| websockets | なし       | ws      |
| websockets | あり       | wss     |

さらに、SSL/TLS通信を行うか否かの判定は、後述するように設定
項目「tls」の記述方法で変わる。
上記の「transport」および「tls」ともオプション項目であるため
記述のバリエーションを頭に入れて設定しないと所望のスキーマが
得られず、サーバとの接続失敗で苦労することになる。


#### 接続先ホスト・ポートが複数ある場合の振る舞い

SINETStream設定ファイルにおける項目「brokers」で指定する場合、
次の3通りの書式を受け付ける。

**1) 単一の接続先**

```
brokers: host1
```

**2) 複数の接続先をコンマ接続でリスト指定**

```
brokers: host1,host2:port2,host3
```

**3) 複数の接続先をYAMLリスト形式で指定**

```
brokers:
  - host1
  - host2:port2
  - host3
```

```
brokers: [host1, host2:port2, host3]
```

複数の接続先を指定した場合の振る舞いは以下のようになる。
「Paho MQTT Android」API仕様の公開文書によれば、サーバURIの
配列として受け取った候補を上から順に試し、接続に失敗した場合
は次の接続先を試すことを繰り返す。全滅の場合にエラーで返る。

* [MqttConnectOptions: setServerURIs](https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttConnectOptions.html)


### SSL/TLSの利用設定

SINETStreamを利用するユーザアプリケーションは、対向の相手群
（ブローカ）との通信路にSSL/TLSを利用することができる。

この場合、ブローカの動作設定と合わせてユーザアプリケーション
のアセット領域にSSL/TLS証明書を組み込むとともに、その内容を
設定ファイルに追記する必要がある。

```
$(TOP)
  +-- app/
       +-- src/
            +-- main/
                 +-- assets/    <-- SSL/TLS証明書ファイル
                 +-- res/
                      +-- raw/  <-- SINETStream設定ファイル
```

SSL/TLS証明書ファイル（規定のファイル形式）の所用の場所への
配置、およびSINETStream設定ファイルへのパラメータ設定を妥当
にしないとSSL/TLS接続時のハンドシェークで失敗する。

#### SINETStream設定ファイルへのSSL/TLS項目追記

SINETStream設定ファイルにおいてSSL/TLS設定をキーワード「tls」
の論理値、または複数項目を扱うブロックとして追記する。

**1) 論理値としての設定**

```
a) SSL/TLSを使わない場合：
tls: false （または記述省略）
```

```
b) SSL/TLSを使う場合：
tls: true
```

上記b)の設定は商用のサーバ証明書を購入利用かつクライ
アント証明書なしで運用する場合限定となる。

**2) ブロックで設定（SSL/TLSを使う場合のみ）**

   -> クライアント証明書を使う場合はこちらの書式を用いる。

```
# SSL/TLS設定ブロック
# この書式の場合はSSL/TLSを使うものと看做す。
tls:
  # 自己署名サーバCA証明書のファイル名
  # オプション項目
  ca_certs: xxx.crt

  # クライアント証明書のファイル名
  # オプション項目
  certfile: xxx.pfx

  # クライアント証明書のパスワード文字列
  # (これを秘匿できないのはセキュリティ上の大きな欠陥)
  # オプション項目
  keyfilePassword: xxx
```

ここでは、SINETStream設定ファイルにSSL/TLS証明書の情報を記載
した。次項でこれら実体ファイルの導入方法を示す。

#### SSL/TLS証明書ファイル群の導入

既述したSINETStream設定ファイルとは異なり、SSL/TLS証明書ファ
イル群は開発ソースのアセット領域（$TOP/app/src/main/assets）
配下に配置する。

**1）自己署名サーバCA証明書**
--> 内容はPEM形式ファイル（xxx.crt）
--> 商用のサーバ証明書を購入利用する場合は不要

**2）クライアント証明書**
--> 内容はPKCS#12/PFX形式ファイル（xxx.pfx）
--> クライアント認証を必要とする場合に用意する

導入手順はSINETStream設定ファイルの場合と同様である。
開発環境AndroidStudio上の操作でサブディレクトリ「assets」を
作成し、そこに所用のSSL/TLS証明書ファイルを配置する。

**＜既知の問題＞**
> 上記のように、SSL/TLS証明書ファイル群はソースの一部として組
> み込まれる。証明書は頻繁に変更されるものではないため再導入の
> 面倒はないが、開発者の操作で証明書ファイルを容易に抜かれてし
> まうため、セキュリティの面では脆弱である。
> 将来的には、管理者がAndroidの秘密領域（キーストア）に手動で
> 配置してそれをSINETStreamAndroidライブラリが直接参照するよう
> な方式に改める。

### 開発物のAndroid機材への導入

環境構築やソース実装を経てユーザアプリケーションを構築すると
APK（Android package）アーカイブファイルが生成される。これは
コード、データ、およびリソースファイルを一つにまとめたもので
ある。
最終的にはこのAPKを実行環境（エミュレータや実機）に導入して
実行することになる。

* [アプリの基礎](https://developer.android.com/guide/components/fundamentals)
* [アプリをビルドして実行する](https://developer.android.com/studio/run)


#### Androidエミュレータに導入する場合

開発環境AndroidStudio付属のAndroid仮想デバイス（AVD）ツール
を用いて、所用の仕様（画面解像度、APIレベル、CPU種別など）に
沿ったAVDを事前作成しておく。
このAVDを起動するとAndroid画面が表示され、AndroidStudioから
実機同様に遠隔操作（APK導入やデバッグなど）できるようになる。
エミュレータが実行中にAndroidStudio上の「Run」コマンドを実行
すると（必要に応じてソースが再構築されて）生成APKファイルが
エミュレータに導入され、自動的に動作を開始する。

* [Android Emulator上でアプリを実行する](https://developer.android.com/studio/run/emulator)

#### Android実機に導入する場合

以下の要領でAPKファイルを実機に導入する。
Android実機の設定画面を操作し、開発者モードを有効化する。
設定コマンドの開発者メニュー経由で「USBデバッグ」を有効化する。
実機と開発機材をUSBケーブルで接続する。
実機に「デバッグモードで接続して良いか」を確認するダイアログ
が表示される。接続を承認すると、AndroidStudioで認識される。
この状態でAndroidStudio上の「Run」コマンドを実行する。

あるいはターミナル上でadbコマンドを直接操作することで対象の
APKファイルを実機に導入できる。

```
-----（操作イメージ：ここから）-----
PC% adb devices
...  <-- デバイス識別子

PC% adb install -r XXX.apk
Success
-----（操作イメージ：ここから）-----
```


* [ハードウェアデバイス上でのアプリの実行](https://developer.android.com/studio/run/device)
* [Android Debug Bridge (adb)](https://developer.android.com/studio/command-line/adb)


## まとめ

SINETStreamAndroidを用いるアプリケーション開発者が留意すべき
項目について一通り概説した。
