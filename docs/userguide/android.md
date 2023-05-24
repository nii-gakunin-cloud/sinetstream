<!--
Copyright (C) 2020-2021 National Institute of Informatics

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

<pre>
1. 概要
2. モジュール構成
3. 作業準備
    3.1 開発環境の導入
    3.2 実行環境の用意
4. 作業手順
    4.1 ビルド環境設定
        4.1.1 ライブラリファイルの取得
        4.1.2 開発ソースへの組み込み
        4.1.3 リポジトリ追加
        4.1.4 依存関係追加
            4.1.4.1 SINETStreamライブラリ
            4.1.4.2 Paho MQTT Androidライブラリ
    4.2 マニフェストファイルの記述
        4.2.1 利用者権限追加
        4.2.2 サービスコンポーネント追加
    4.3 SINETStreamライブラリの動作設定
        4.3.1 SINETStream設定ファイルの配置
        4.3.2 基本設定
        4.3.3 接続先URIの決定方法
        4.3.4 接続先ホスト・ポートが複数ある場合の振る舞い
    4.4 SSL/TLSの利用設定
        4.4.1 設定ファイルを用いる運用の場合
        4.4.2 コンフィグサーバを利用する運用の場合
    4.5 開発成果物のAndroid端末への導入
        4.5.1 Androidエミュレータに導入する場合
        4.5.2 Android実機に導入する場合
5. まとめ
</pre>


## 1. 概要

国立情報学研究所（NII）開発によるAndroid版の`SINETStreamライブラリ`を提供するにあたり、
これを利用するユーザアプリケーションの開発者が考慮すべきことを概説する。


## 2. モジュール構成

`SINETStreamライブラリ`、およびそれを利用するユーザアプリケーションの依存関係を簡略的に図示すると以下のように表現される。


```
      #---------------------------------------------+
      | User Application using MQTT              (1)|
      +---------------------------------------------+
    =================================================== SINETStream API
      +---------------------------------------------+
      | SINETStream Android Library              (2)|
      |                                             |
      +---------------------------------------------+
      +------------------+  +-----------------------+
      | Paho MQTT    (3a)|  | Paho MQTT         (3b)|
      | client library   |  | client library        |
      | (Paho ORIGINAL)  |  | (BUGFIX: Android 12+) |
      +------------------+  +-----------------------+
    =================================================== System API
      +---------------------------------------------+
      | Android System                           (4)|
      |       ........... ............ ............ |
      |       : Network : : File I/O : : KeyChain : |
      |       :.........: :..........: :..........: |
      +------------A--------------------------------+
                   |
                   | IP network
                   V
            [ MQTT Broker ]
```

＜凡例＞

(1) ユーザが開発するアプリケーション

(2) `SINETStreamライブラリ`本体

(3) [Eclipse Pahoプロジェクト](https://www.eclipse.org/paho)が提供するAndroid版の[MQTTクライアントライブラリ](https://github.com/eclipse/paho.mqtt.android)

> <em>注意: Pahoプロジェクト活動停止(?)による不具合放置への暫定対応</em>
>
> (3a) Android 12未満はオリジナル版をそのまま使用可能
>
> (3b) Android 12以降は動作不具合があるため、NIIによる暫定改修版を使用する

(4) Androidシステム

> MQTTを扱うネットワーク処理、設定情報を扱うファイルI/O、秘匿情報を扱うKeyChainなどの機能モジュールを利用する。


## 3. 作業準備
### 3.1 開発環境の導入

Google社が提供する統合開発環境
[Android Studio](https://developer.android.com/studio)
を入手して、手元の作業機材に導入する。

`Android Studio`自体は、`Windows`, `MacOS`, `Linux`, `ChromeOS` の各プラットフォームに対応しているが、その動作環境として高性能な開発機材が必要となる。
特に実装メモリ量やCPU/GPUの性能が効いてくる。
少なくとも上記プラットフォーム毎の
[システム要件](https://developer.android.com/studio/install?hl=ja)
を満足するものを選定すること。

また、開発機材上で
[エミュレータ](https://developer.android.com/studio/run/emulator?hl=ja)
を動作させる場合は、その
[システム要件](https://developer.android.com/studio/run/emulator?hl=ja#requirements)
にも留意すること。

> <em>注意: `Android Studio`の版数</em>
>
> Android開発環境は継続的に更新されており、時としてビルド環境と設定ファイルの不整合によりビルドに失敗することがある。
> その場合、
[Android Studio のダウンロード アーカイブ](https://developer.android.com/studio/archive?hl=ja)より、
> `Android Studio Dolphin | 2021.3.1 ベータ版 3`を取得されたい。


### 3.2 実行環境の用意

Android端末の実機、または`AndroidStudio`と連携する
[エミュレータ](https://developer.android.com/studio/run/emulator?hl=ja)
環境にて所用の
[Android仮想デバイス(AVD)](https://developer.android.com/studio/run/managing-avds?hl=ja)
を用意する。

> <em>注意: Android動作環境の制約</em>
>
> ユーザアプリケーションのビルド設定、および対象Android端末のいずれも、Android 8.0（APIレベル26）以上であること。
> Android版の`SINETStreamライブラリ`は、メッセージのシリアライザ／デシリアライザ機能で`Apache Avro`ライブラリを利用しており、後者の実装内容が動作上の制約条件となっている。


## 4. 作業手順
### 4.1 ビルド環境設定

ユーザアプリケーションにとって、`SINETStreamライブラリ`も`Paho MQTT Android`も外部ライブラリとして参照するものである。
すなわち「どこに何があるか」というリポジトリ参照先と、「どの版のものを参照するか」という二種類の情報を開発環境に設定する必要がある。

`Android Studio`では、[Gradle Build Tool](https://gradle.org/)によるビルド管理を行なっている。
よって、ユーザアプリケーションが参照する外部ライブラリなどの設定情報は`Gradle`制御ファイルの一つである
[モジュールレベルの`build.gradle`](https://developer.android.com/studio/build#module-level?hl=ja)にて設定する。
[ビルド依存関係の追加](https://developer.android.com/studio/build/dependencies?hl=ja)の記述を参照されたい。

以降では、ユーザのAndroid開発ソースから`SINETStreamライブラリ`を利用するための具体的な手順について記述する。

> <em>注意: ライブラリ参照方法の違い</em>
>
> `SINETStreamライブラリ`自身、およびその依存ライブラリである
> `Paho MQTT Android`とでは参照方法が異なる。
> 前者は
[Android ARchive (AAR)](https://developer.android.com/studio/projects/android-library?hl=ja)ファイルを手元にダウンロードして、それを直接参照する。
> 後者はネット上のmavenリポジトリに配置されるのでそれを参照する。


#### 4.1.1 ライブラリファイルの取得

`SINETStreamライブラリ`は、ソースおよびバイナリの形式で`GitHub`上で公開される。

国立情報学研究所（NII）が管理するリポジトリ
[sinetstream-android](https://github.com/nii-gakunin-cloud/sinetstream-android/releases)
より、最新版の`sinetstream-android-x.y.z.aar`を手元にダウンロードする。
> 便宜的に、ダウンロード先を`$HOME/Downloads`として話を進める。


#### 4.1.2 開発ソースへの組み込み

Android開発環境で作業する対象プログラムを仮に`testapp`とすると、
概略以下のようなディレクトリ内容になっているはずである。

```console
% cd $(WORKDIR)/testapp
% ls -FC
app/          gradle/            gradlew      local.properties
build.gradle  gradle.properties  gradlew.bat  settings.gradle
```

アプリケーションソース格納用の`app`サブディレクトリ直下に、
ローカルライブラリ格納用のディレクトリ`libs`を用意する。
既にあればそれを使い、なければ作成する。
前項で取得した`SINETStreamライブラリ`をそこに格納する。

```console
% cd app
% mkdir libs
% cd libs
% cp $HOME/Downloads/sinetstream-android-x.y.z.aar .
```


#### 4.1.3 リポジトリ追加

`SINETStreamライブラリ`、およびその依存ライブラリ`Paho MQTT Android`ではそれぞれ参照方法が異なるため、個別に記述する。

`SINETStreamライブラリ`は、目的のものを手動で手元にダウンロードして、
ローカルライブラリ格納用の`libs`ディレクトリに配置する。
それを参照するよう、
モジュールレベルの`build.gradle`に記述する。

```gradle:app/build.gradle
repositories {
    flatDir {
        // For SINETStream library
        dirs "libs"
    }
}
```

`Paho MQTT Android`ライブラリは、ネット上の公開先（mavenブロック）を
モジュールレベルの`build.gradle`に
[記述](https://github.com/eclipse/paho.mqtt.android#gradle)
する。

```gradle:app/build.gradle
repositories {
    maven {
        // The Paho MQTT Android
        url "https://repo.eclipse.org/content/repositories/paho-snapshots/"
    }
    mavenCentral()
}
```


#### 4.1.4 依存関係追加

`SINETStreamライブラリ`、およびその依存ライブラリ`Paho MQTT Android`ではそれぞれ参照方法が異なるため、個別に記述する。


##### 4.1.4.1 SINETStreamライブラリ

`SINETStreamライブラリ`は、
前述のようにローカルライブラリ格納用の`libs`ディレクトリに配置する。
当該ファイル名の本体部分と拡張子部分に分けて、
モジュールレベルの`build.gradle`に[ビルド依存関係を追加](https://developer.android.com/studio/build/dependencies?hl=ja)する。

```gradle:app/build.gradle
dependencies {
    // SINETStream for Android
    implementation(name: 'sinetstream-android-x.y.z', ext: 'aar')
}
```


##### 4.1.4.2 Paho MQTT Androidライブラリ

`Paho MQTT Android`ライブラリは、ネット上の公開先（`maven`リポジトリ）を以下の形式で指定する。
```gradle
    implementation '<group-id>:<artifact-id>:<version>'
```

さて、第2章「モジュール構成」で述べたとおり、`Paho MQTT Android`ライブラリはAndroid 12以降に対応しておらず、そのままではビルドできない。
NII開発の暫定ライブラリで代替するか否か、ユーザアプリケーションの動作環境に応じてモジュールレベルの`build.gradle`に[ビルド依存関係を追加](https://developer.android.com/studio/build/dependencies?hl=ja)する。

* Android 12 (APIレベル31)未満の場合：
```gradle:app/build.gradle
dependencies {
    // The Paho MQTT Android
    implementation 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5'
    implementation 'org.eclipse.paho:org.eclipse.paho.android.service:1.1.1'
}
```

* Android 12 (APIレベル31)以降の場合：
```gradle:app/build.gradle
dependencies {
    /* NOP: Use the client module as is */
    implementation 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5'
    /* DEL: Comment out the service module until fixed */
    //implementation 'org.eclipse.paho:org.eclipse.paho.android.service:1.1.1'

    /* ADD: A temporary fix for the Paho Android Service */
    implementation 'net.sinetstream:pahomqttandroid-bugfix:1.0.0'
}
```

> 将来`Paho MQTT`プロジェクトの活動が再開され、`Paho MQTT Android`ライブラリの不具合が修正されることを期待したい。


### 4.2 マニフェストファイルの記述
#### 4.2.1 利用者権限追加

`SINETStreamライブラリ`を利用するユーザアプリケーションは、
所用の[権限](https://developer.android.com/guide/topics/manifest/manifest-intro?hl=ja#perms)（`uses-permission`句）をビルド情報として宣言する必要がある。
さもないと当該アプリケーション実行時に`権限エラー`が発生する。

以下の内容をマニフェストファイル（AndroidManifest.xml）に設定すること。

```xml:app/src/main/AndroidManifest.xml
<!-- Permissions for the external network (such like INTERNET) access -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Required for the Paho Android Service (Android 12+) -->
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
```


#### 4.2.2 サービスコンポーネント追加

`SINETStreamライブラリ`の下位層に位置する`Paho MQTT Android`ライブラリは、以下の2つの機能要素

* ユーザインタフェースを提供するクライアントAPI部分
* 実際にMQTTメッセージ処理を担当する常駐型サービス

から構成される。

すなわち、`Paho MQTT Android`ライブラリは[アプリコンポーネント](https://developer.android.com/guide/topics/manifest/manifest-intro?hl=ja#components)として`サービス`機能を提供している。
ユーザアプリケーションが同サービスと連携するため、その名称（`service`句）をマニフェストに定義する必要がある。

> <em>注意<em>
>
> この設定を忘れても「Paho MQTT Android」を利用するユーザプログラム自体は
> 起動するが、Androidシステムが当該サービスを起動しない。
> このため、publishやsubscribeなどのAPI関数の処理が空回りする。
> アプリケーションからの要求は受け付けられるものの、ネットワークとやりとり
> されない状態なのでエラーに気づきにくい。

以下の内容をマニフェストファイル（AndroidManifest.xml）に設定すること。

```xml:app/src/main/AndroidManifest.xml
<application ... >
    <!-- MQTT service provided by Paho MQTT Android -->
    <service android:name="org.eclipse.paho.android.service.MqttService" />
</application>
```


### 4.3 SINETStreamライブラリの動作設定

本ライブラリの設定方法として、以下の2通りを用意する。

* 規定場所に事前に配置された設定ファイルを読み込む
* 設定サーバ(コンフィグサーバ)から設定内容を動的にダウンロードする

次節以降では、前者の設定ファイル方式について記述する。
後者の設定サーバ方式については別途記述する。

#### 4.3.1 SINETStream設定ファイルの配置

本方式による設定の場合、アプリケーション固有のデータ領域に[YAML](https://yaml.org/)形式の設定ファイル

* /data/data/__PACKAGE__/files/sinetstream_config.yml

を事前に用意しておく。ファイル名は固定である。
書式など詳細は、別紙[Android版のSINETStream設定ファイル](config-android.md)を参照のこと。

> [Android版クイックスタートガイド](../tutorial-android/index.md)で示したサンプルアプリケーション2種では、
> GUIによる設定画面を用意して「利用者の操作内容を元にSINETStream設定
> ファイルを動的に自動生成する」手法を採用している。
> こちらの実装内容を参照されたい。


#### 4.3.2 基本設定

設定ファイルの記述内容は必須項目とオプション項目がある。
以下に示す項目は必ず記述する必要がある。

* サービス名（任意文字列、検索鍵を兼ねる）
* メッセージシステム種別（type）
* 接続先ホスト・ポート（brokers）
* トピック名（topic）

オプション項目が省略された場合、[MqttConnectOptionsのコンストラクタ既定値](https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttConnectOptions.html#MqttConnectOptions--)が使われる。


#### 4.3.3 接続先URIの決定方法

接続先サーバ（ブローカ）URIは一般的に以下のように表現される。

```
schema://host[:port]
^^^^^ 1) ^^^^^^^^^^^ 2)
```

SINETStream設定ファイルにおける項目「brokers」で指定するのは、
後述のように上記2)の「ホスト（＋ポート）部分の羅列」である。
なお、上記1)のスキーマ部分はいくつかの条件の組み合わせで決まる。

| transport  | tls        | schema  |
|------------|------------|---------|
| 省略       | 省略       | tcp     |
| tcp        | なし       | tcp     |
| tcp        | あり       | ssl     |
| websockets | なし       | ws      |
| websockets | あり       | wss     |

さらに、SSL/TLS通信を行うか否かの判定は、
後述するように設定項目「tls」の記述方法で変わる。
上記の「transport」および「tls」ともオプション項目であるため、
記述のバリエーションを頭に入れて設定しないと所望のスキーマが得られず、
サーバとの接続失敗で苦労することになる。


#### 4.3.4 接続先ホスト・ポートが複数ある場合の振る舞い

SINETStream設定ファイルにおける項目「brokers」で指定する場合、
次の3通りの書式を受け付ける。

**1) 単一の接続先**

```YAML
brokers: host1
```

**2) 複数の接続先をコンマ接続でリスト指定**

```YAML
brokers: host1,host2:port2,host3
```

**3) 複数の接続先をYAMLリスト形式で指定**

```YAML
brokers:
  - host1
  - host2:port2
  - host3
```

```YAML
brokers: [host1, host2:port2, host3]
```

複数の接続先を指定した場合の振る舞いは以下のようになる。

* サーバURIの配列として受け取った候補を上から順に試し、
接続に失敗した場合は次の接続先を試すことを繰り返す。
全滅の場合にエラーで返る。

> <em>出典</em>：[MqttConnectOptions: setServerURIs](https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttConnectOptions.html#setServerURIs-java.lang.String:A-)


### 4.4 SSL/TLSの利用設定

`SINETStreamライブラリ`を利用するユーザアプリケーションは、
対向の相手群（ブローカ）との通信路にSSL/TLSを利用することができる。

#### 4.4.1 設定ファイルを用いる運用の場合

以下の各項目を実施する。

* 接続先ブローカの管理者から発行された`SSL/TLS証明書ファイル`を動作対象Android機材に手動で導入する。
* 別紙[Android版のSINETStream設定ファイル](config-android.md)の「SSL/TLSに関するパラメータ」の項を参照し、所用の設定内容を記述する。

> <em>注意</em>
>
> 事前導入したSSL/TLS証明書ファイル形式、および接続先ブローカの設定内容に相互矛盾がないよう留意する。
> さもないとブローカ接続時のSSL/TLSハンドシェーク処理で失敗する。


#### 4.4.2 コンフィグサーバを利用する運用の場合

SSL/TLSの設定はサーバ側で準備され、またSSL/TLS証明書データ自体も自動的にダウンロードされる。
すなわちAndroid端末側で特に対処すべきことはない。


### 4.5 開発成果物のAndroid端末への導入

開発環境`AndroidStudio`を準備し、ユーザアプリケーションを実装\[1\]\[2\]すると、
`APK`（Android package）と呼ばれるアーカイブファイルが生成される。

APKとは、アプリケーションを構成する「コード、データ、およびリソースファイル」をパッケージとして一つにまとめたものである。
この生成物`APK`ファイルを実行環境（エミュレータや実機）に導入する方法について述べる。

\[1\]:
[アプリの基礎](https://developer.android.com/guide/components/fundamentals)
<br>
\[2\]:
[アプリをビルドして実行する](https://developer.android.com/studio/run)

#### 4.5.1 Androidエミュレータに導入する場合

以下の要領で`APK`ファイルをエミュレータに導入し、[同エミュレータ上でアプリケーションを実行](https://developer.android.com/studio/run/emulator?hl=ja)する。

1. Android仮想デバイス（AVD: Android Virtual Device）の用意

> 開発環境`AndroidStudio`付属の`Android仮想デバイス`（AVD）ツールを用いて、
> 所用の諸元（画面解像度、APIレベル、CPU種別など）に
> 沿った`AVD`を事前作成しておく。

2. AVDの起動

> 目的の諸元を満たす`AVD`を起動するとAndroid端末イメージが表示される。
> GUI経由で実機同様に操作したり、
> 遠隔操作（`APK`導入やデバッグなど）ができるようになる。

3. アプリケーションの導入と起動

> エミュレータが実行中に`AndroidStudio`上の「Run」コマンドを実行する。
> 必要に応じてソースが再構築され、生成`APK`ファイルがエミュレータに導入される。
> 続けて、当該プログラムが自動的に動作を開始する。


#### 4.5.2 Android実機に導入する場合

以下の要領で`APK`ファイルを実機に導入し、[同機上でアプリケーションを実行する](https://developer.android.com/studio/run/device)。

1. Android実機の設定画面を操作し、開発者モードを有効化する。
2. 設定コマンドの開発者メニュー経由で「USBデバッグ」を有効化する。
3. 実機と開発機材をUSBケーブルで接続する。
4. 実機に「デバッグモードで接続して良いか」を確認するダイアログが表示される。
デバッグ接続を承認すると、`AndroidStudio`で認識される。
5. この状態で`AndroidStudio`上の「Run」コマンドを実行する。

あるいは、開発機材上で[Android Debug Bridge (adb)](https://developer.android.com/studio/command-line/adb)コマンドを直接操作することで、
対象`APK`ファイルを実機に導入できる。

```console
PC% adb install -r XXX.apk
Success
```


## 5. まとめ

`SINETStreamライブラリ`を用いるアプリケーション開発者が留意すべき項目について、
一通り概説した。
