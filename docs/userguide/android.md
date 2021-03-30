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
    3.2 実行用の用意
4. 作業手順
    4.1 ビルド環境設定
        4.1.1 ライブラリファイルの取得
        4.1.2 開発ソースへの組み込み
        4.1.3 リポジトリ追加
        4.1.4 依存関係追加
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
    4.5 開発成果物のAndroid機材への導入
        4.5.1 Androidエミュレータに導入する場合
        4.5.2 Android実機に導入する場合
5. まとめ
</pre>

## 1. 概要

Android向けSINETStream APIの実装「SINETStream for Android」（以降、
簡素化のため「SINETStreamAndroid」と表記）を提供するにあたり、
これを利用するユーザアプリケーションの開発者が考慮すべきことを概説する。


## 2. モジュール構成

SINETStreamAndroidを利用するユーザアプリケーションは、
一般的に下図のようなモジュール構成および依存関係となる。

<pre>
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
             | IP network
             V
      [ SINETStream ]
</pre>

＜凡例＞
1. 開発対象のユーザアプリケーション
2. 参照する外部ライブラリ群
3. SINETStream設定ファイル
4. SSL/TLS接続用の証明書ファイル群

> 上図の外部ライブラリ群の内訳:
> * Android版のSINETStream（WriterおよびReader機能）
> * Paho MQTT Android （Android版MQTTクライアント機能）[1]

\[1\]:
[Eclipse Paho Android Client](https://www.eclipse.org/paho/index.php?page=clients/java/index.php)

## 3. 作業準備
### 3.1 開発環境の導入

Google社が提供する統合開発環境`AndroidStudio`を入手\[1\]して、
手元の作業機材に導入する。

\[1\]:
[Download Android Studio and SDK tools](https://developer.android.com/studio)
> ページ最下部に記載されたシステム要件に注意すること

### 3.2 実行環境の用意

Android実機、または`AndroidStudio`のエミュレータを用意する。

* Android 8.0 (APIレベル26）以上


## 4. 作業手順
### 4.1 ビルド環境設定

ユーザアプリケーションにとって、`SINETStreamAndroid`も`Paho MQTT Android`も外部ライブラリとして参照するものである。
すなわち、どこに何があるかという参照先と、どのバージョンのものを参照するかという二種類の情報を開発環境に設定する必要がある。

Android開発環境では、`Gradle`によるビルド管理を行なっている\[1\]。
ユーザアプリケーションが参照する外部ライブラリなどの設定情報を、
`Gradle`制御ファイル`build.gradle`で指定する\[2\]\[3\]。

以降では、ユーザのAndroid開発ソースから`SINETStreamAndroid`ライブラリを利用するための具体的な手順について記述する。

> <em>注意</em>
>
> SINETStreamAndroid自身、およびその依存ライブラリである
> `Paho MQTT Android`とでは参照方法が異なる。
> 前者はAARファイルを手元にダウンロードして、それを直接参照する。
> 後者はネット上のmavenリポジトリに配置されるのでそれを参照する。

\[1\]:
[Gradle Build Tool](https://gradle.org/)
<br>
\[2\]:
[モジュールレベルビルドファイル](https://developer.android.com/studio/build#module-level)
<br>
\[3\]:
[ビルド依存関係の追加](https://developer.android.com/studio/build/dependencies)


#### 4.1.1 ライブラリファイルの取得

`SINETStreamAndroid`ライブラリは、
ソースおよびバイナリの形式で`GitHub`上で公開される。

国立情報学研究所（NII）が管理するリポジトリ
[sinetstream-android](https://github.com/nii-gakunin-cloud/sinetstream-android/releases)
より、最新バージョンの`sinetstream-android-x.y.z.aar`を手元にダウンロードする。
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
前項で取得した`SINETStreamAndroid`ライブラリをそこに格納する。

```console
    % cd app
    % mkdir libs
    % cd libs
    % cp $HOME/Downloads/sinetstream-android-x.y.z.aar .
```

#### 4.1.3 リポジトリ追加

SINETStreamAndroid、およびその依存ライブラリ`Paho MQTT Android`ではそれぞれ参照方法が異なるため、個別に記述する。

`SINETStreamAndroid`ライブラリは、目的のものを手動で手元にダウンロードして、
ローカルライブラリ格納用の`libs`ディレクトリに配置する。
それを参照するよう、
モジュールレベル（この場合`app`）の`build.gradle`に記述する。

----------（$TOP/app/build.gradle: 抜粋ここから）----------
```build.gradle
repositories {
    flatDir {
        // For SINETStreamAndroid library
        dirs "libs"
    }
}
```
----------（$TOP/app/build.gradle: 抜粋ここまで）----------

`Paho MQTT Android`ライブラリは、ネット上の公開先（mavenブロック）を
モジュールレベルの`build.gradle`に記述する\[1\]。

----------（$TOP/app/build.gradle: 抜粋ここから）----------
```build.gradle
repositories {
    maven {
        // The Paho MQTT Android
        url "https://repo.eclipse.org/content/repositories/paho-snapshots/"
    }
    mavenCentral()
}
```
----------（$TOP/app/build.gradle: 抜粋ここまで）----------

\[1\]:
[Using the Paho Android Client](https://github.com/eclipse/paho.mqtt.android#gradle)

#### 4.1.4 依存関係追加

SINETStreamAndroid、およびその依存ライブラリ`Paho MQTT Android`ではそれぞれ参照方法が異なるため、個別に記述する。

`SINETStreamAndroid`ライブラリは、
前述のようにローカルライブラリ格納用の`libs`ディレクトリに配置する。
当該ファイル名の本体部分と拡張子部分に分けて、
モジュールレベルの`build.gradle`に記述する\[1\]。

----------（$TOP/app/build.gradle: 抜粋ここから）----------
```build.gradle
dependencies {
    // SINETStreamAndroid
    implementation(name: 'sinetstream-android-x.y.z', ext: 'aar')
}
```
----------（$TOP/app/build.gradle: 抜粋ここまで）----------

`Paho MQTT Android`ライブラリは、
ネット上の公開先（mavenブロック）をリポジトリとして指定した。
そこから参照するライブラリのMavenアーティファクト
```
    group-id:artifact-id:version
```
をモジュールレベルの`build.gradle`に記述する\[1\]\[2\]。

ライブラリ開発元での実装状況の進展により、
ここで記述したバージョンより進んでいることがあるかもしれない。
その場合、`AndroidStudio`により該当ライブラリのバージョン更新を促される。

----------（$TOP/app/build.gradle: 抜粋ここから）----------
```build.gradle
dependencies {
    // The Paho MQTT Android
    implementation 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5'
    implementation 'org.eclipse.paho:org.eclipse.paho.android.service:1.1.1'
}
```
----------（$TOP/app/build.gradle: 抜粋ここまで）----------

\[1\]:
[ビルド依存関係を追加する](https://developer.android.com/studio/build/dependencies)
<br>
\[2\]:
[Maven依存関係](https://pleiades.io/help/idea/work-with-maven-dependencies.html#generate_maven_dependency)


### 4.2 マニフェストファイルの記述

#### 4.2.1 利用者権限追加

SINETStreamでは内部でメッセージングシステム（例えば`MQTT`）を利用するため、
外部との通信が発生する。
このため、開発対象のユーザアプリケーションでは外部ネットワーク接続に必要な権限（`uses-permission`句）\[1\]を定義する。
さもないとアプリケーション実行時に権限エラーが発生する。

設定内容を以下に示す。

-----（$TOP/app/src/main/AndroidManifest.xml: 抜粋ここから）-----
```xml
<!-- Permissions for the external network (such like INTERNET) access -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```
-----（$TOP/app/src/main/AndroidManifest.xml: 抜粋ここまで）-----

\[1\]:
[パーミッション](https://developer.android.com/guide/topics/manifest/manifest-intro#perms)

#### 4.2.2 サービスコンポーネント追加

SINETStreamAndroidの下位層に位置する「Paho MQTT Android」ライブラリは、
ユーザインタフェースを提供するクライアント部分、
および実際にMQTTメッセージ処理を担当する常駐型サービス\[1\]から構成される。
このため「Paho MQTT Android」が提供するサービス名（service句）をマニフェストに定義する必要がある。

> <em>注意<em>
>
> この設定を忘れても「Paho MQTT Android」を利用するユーザプログラム自体は
> 起動するが、Androidシステムが当該サービスを起動しない。
> このため、publishやsubscribeなどのAPI関数の処理が空回りする。
> アプリケーションからの要求は受け付けられるものの、ネットワークとやりとり
> されない状態なのでエラーに気づきにくい。

設定内容を以下に示す。

-----（$TOP/app/src/main/AndroidManifest.xml: 抜粋ここから）-----
```xml
<application ... >
    <!-- MQTT service provided by Paho MQTT Android -->
    <service android:name="org.eclipse.paho.android.service.MqttService" />
</application>
```
-----（$TOP/app/src/main/AndroidManifest.xml: 抜粋ここまで）-----

\[1\]:
[アプリのコンポーネント](https://developer.android.com/guide/topics/manifest/manifest-intro#components)


### 4.3 SINETStream設定ファイル

#### 4.3.1 配置

SINETStreamを利用するユーザアプリケーションは、
その動作内容を規定する`YAML`形式\[1\]の設定ファイル「sinetstream_config.yml」を所定の位置に配置する必要がある。
具体的には、開発Androidアプリケーションソースのリソース領域
```
$TOP/app/src/main/res
```
配下にサブディレクトリ「raw」を手動で追加\[2\]し、そこに配置する。
```
$TOP/app/src/main/res/raw/sinetstream_config.yml
                      ^^^^^^^^^^^^^^^^^^^^^^^^^^
```

\[1\]:
[The Official YAML Web Site](https://yaml.org/)
<br>
\[2\]:
[リソースディレクトリを追加する](https://developer.android.com/studio/write/add-resources#add_a_resource_directory)

具体的な操作手順を以下に示す。

**1) `res` ディレクトリ選択：**

`AndroidStudio`画面左側の`Project`ペインにて「res」ディレクトリアイコンを選択（カーソル表示された状態）とする。

**2. `raw` サブディレクトリ作成：**

上記1)の状態でマウス右クリックまたは画面上部の`File`メニューから「New -> Android Resource Directory」を選択する。
ポップアップウィンドウ「New Resource Directory」にて、
上から２番目のプルダウンメニュー「Resource type」から「raw」を選択する。
その状態で「OK」ボタンを押下すると「res/raw」ディレクトリが作成される。

**3. `sinetstream_config.yml` ファイル作成：**

`AndroidStudio`画面左側の`Project`ペインにて「raw」ディレクトリアイコンが選択された状態（カーソル表示）とする。
この状態でマウス右クリックまたは画面上部の`File`メニューから「New -> File」を選択するとファイル名の入力を促される。
ここでは「sinetstream_config.yml」を指定する。
Enterキー押下により同名の空ファイルが作成される。

**＜既知の問題＞**
> 上記のように、SINETStream設定ファイルはAndroidアプリケーション
> ソースの一部として組み込まれる。
> このため、設定内容を変更するたびにユーザアプリケーションを再度
> 構築してAndroid実行環境に導入し直す必要がある。

**＜別解＞**
> Android版のチュートリアル[1]で示したサンプルアプリケーション2種では、
> GUIによる設定画面を用意して「利用者の操作内容を元にSINETStream設定
> ファイルを動的に自動生成する」手法を採用している。
> こちらの実装内容も参照されたい。

\[1\]:
[Android版クイックスタートガイド](../tutorial-android/index.md)


#### 4.3.2 基本設定

SINETStream接続に必要なパラメータ群を記述する。
ここで書かれた内容は、アプリケーション固有のデータ領域
```
/data/data/<PACKAGE>/files
```
配下に転記され、これが「SINETStreamAndroid」内部で参照される。

設定ファイルの記述内容は必須項目とオプション項目がある。
以下に示す項目は必ず記述する必要がある。

* サービス名（任意文字列、検索鍵を兼ねる）
* メッセージシステム種別（type）
* 接続先ホスト・ポート（brokers）
* トピック名（topic）

オプション項目が省略された場合は「Paho MQTT Android」の既定値\[1\]が使われる。

\[1\]:
[MqttConnectOptions: Constructor Detail](https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttConnectOptions.html)

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
「Paho MQTT Android」API仕様の公開文書\[1\]によれば、
サーバURIの配列として受け取った候補を上から順に試し、
接続に失敗した場合は次の接続先を試すことを繰り返す。
全滅の場合にエラーで返る。

\[1\]:
[MqttConnectOptions: setServerURIs](https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttConnectOptions.html)


### 4.4 SSL/TLSの利用設定

SINETStreamを利用するユーザアプリケーションは、
対向の相手群（ブローカ）との通信路にSSL/TLSを利用することができる。

この場合、ブローカの動作設定と合わせてユーザアプリケーションのアセット領域にSSL/TLS証明書を組み込むとともに、その内容を設定ファイルに追記する必要がある。

```
$(TOP)
  +-- app/
       +-- src/
            +-- main/
                 +-- assets/    <-- SSL/TLS証明書ファイル
                 +-- res/
                      +-- raw/  <-- SINETStream設定ファイル
```

SSL/TLS証明書ファイル（規定のファイル形式）の所用の場所への配置、
およびSINETStream設定ファイルへのパラメータ設定を妥当にしないと、
SSL/TLS接続時のハンドシェークで失敗する。

#### 4.4.1 SINETStream設定ファイルへのSSL/TLS項目追記

SINETStream設定ファイルにおいてSSL/TLS設定をキーワード「tls」の論理値、
または複数項目を扱うブロックとして追記する。

**1) 論理値としての設定**

```YAML
# a) SSL/TLSを使わない場合：
tls: false （または記述省略）
```

```YAML
# b) SSL/TLSを使う場合：
tls: true
```

上記b)の設定は「商用のサーバ証明書を購入利用、かつクライアント証明書なし」で運用する場合限定となる。

**2) ブロックで設定（SSL/TLSを使う場合のみ）**

   -> クライアント証明書を使う場合はこちらの書式を用いる。

```YAML
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

ここでは、SINETStream設定ファイルにSSL/TLS証明書の情報を記載した。
次項でこれら実体ファイルの導入方法を示す。

#### 4.4.2 SSL/TLS証明書ファイル群の導入

既述したSINETStream設定ファイルとは異なり、
SSL/TLS証明書ファイル群は開発ソースのアセット領域
```
$TOP/app/src/main/assets
```
配下に配置する。

**1）自己署名サーバCA証明書**
--> 内容はPEM形式ファイル（xxx.crt）
--> 商用のサーバ証明書を購入利用する場合は不要

**2）クライアント証明書**
--> 内容はPKCS#12/PFX形式ファイル（xxx.pfx）
--> クライアント認証を必要とする場合に用意する

導入手順はSINETStream設定ファイルの場合と同様である。
開発環境`AndroidStudio`上の操作でサブディレクトリ「assets」を作成し、
そこに所用のSSL/TLS証明書ファイルを配置する。

**＜既知の問題＞**
> 上記のように、SSL/TLS証明書ファイル群はソースの一部として組
> み込まれる。証明書は頻繁に変更されるものではないため再導入の
> 面倒はないが、開発者の操作で証明書ファイルを容易に抜かれてし
> まうため、セキュリティの面では脆弱である。
> 将来的には、管理者がAndroidの秘密領域（キーストア）に手動で
> 配置してそれをSINETStreamAndroidライブラリが直接参照するよう
> な方式に改める。

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

以下の要領で`APK`ファイルをエミュレータに導入する\[1\]。

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

\[1\]:
[Android Emulator上でアプリを実行する](https://developer.android.com/studio/run/emulator)


#### 4.5.2 Android実機に導入する場合

以下の要領で`APK`ファイルを実機に導入する\[1\]。
1. Android実機の設定画面を操作し、開発者モードを有効化する。
2. 設定コマンドの開発者メニュー経由で「USBデバッグ」を有効化する。
3. 実機と開発機材をUSBケーブルで接続する。
4. 実機に「デバッグモードで接続して良いか」を確認するダイアログが表示される。
デバッグ接続を承認すると、`AndroidStudio`で認識される。
5. この状態で`AndroidStudio`上の「Run」コマンドを実行する。

あるいは開発機材上で`adb`コマンドを直接操作することで、
対象`APK`ファイルを実機に導入できる\[2\]。

```console
PC% adb install -r XXX.apk
Success
```

\[1\]:
[ハードウェアデバイス上でのアプリの実行](https://developer.android.com/studio/run/device)
<br>
\[2\]:
[Android Debug Bridge (adb)](https://developer.android.com/studio/command-line/adb)


## 5. まとめ

SINETStreamAndroidを用いるアプリケーション開発者が留意すべき項目について、
一通り概説した。
