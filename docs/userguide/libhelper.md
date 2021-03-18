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

[English](https://translate.google.com/translate?hl=en&sl=ja&tl=en&u=https://nii-gakunin-cloud.github.io/sinetstream/docs/userguide/libhelper.html "google translate")

# SINETStreamHelper ユーザガイド

**目次**

<pre>
1. 概要
2. モジュール構成
3. 作業準備
    3.1 開発環境の導入
    3.2 実行環境の用意
4. 作業手順
    4.1 ビルド環境設定
        4.1.1 リポジトリ追加
        4.1.2 依存関係追加
    4.2 マニフェストファイルの記述
        4.2.1 利用者権限追加
    4.3 開発物のAndroid機材への導入
        4.3.1 Androidエミュレータに導入する場合
    4.3.2 Android実機に導入する場合
5. まとめ
</pre>


## 1. 概要

IoTアプリケーションの一つとして、Android端末が備える様々なセンサー
情報を収集して
[SINETStream](https://nii-gakunin-cloud.github.io/sinetstream)
に送出する「パプリッシャ」機能が考えられる。  

Android OSの
[SensorManager](https://developer.android.com/reference/android/hardware/SensorManager)
経由で個々のセンサーデバイスを制御
したり生の読取値を受け取ったりする仕掛けが用意されている。
ユーザアプリケーションがこれを直接利用することでセンサーデバイス
を扱うことはもちろん可能であるが、ハードウェア制御に関わる実装上
の細かな点を隠蔽し、簡易に利用できるような補助ライブラリがあると
使い出が良い。
上記の一つの解として`SINETStreamHelper`ライブラリを用意する。


## 2. モジュール構成

`SINETStreamHelper`ライブラリを利用するユーザアプリケーションの
モジュール構成を以下に示す。

```
        #---------------------------------------+
        | User Application                      |
        +---------------------------------------+
             |                      A
    =========|======================|=============== API functions
             |                      |
      +------|----------------------|-------------+
      | SINETStreamHelper           |             |
      |      V                      | [JSON]      |
      | +------------------+   +----------------+ |
      | | SensorController |-->| SensorListener | |
      | +------------------+   +----------------+ |
      |      |      A                             |
      |      V      |                             |
      | +------------------+                      |
      | |  SensorService   |                      |
      | +------------------+                      |
      +------|------A-----------------------------+
             |      |
             V      | [SensorEvent]
        +------------------+
        |  SensorManager   |
        +------------------+
               |  A
               V  | [Raw Data]
            +--------+
            | Sensor |+
            | Device ||+
            +--------+||
              +-------+|
               +-------+
```
〈凡例〉
* SensorController
    * `SINETStreamHelper`ライブラリのフロントエンドとしてセンサー
制御用のAPI関数一式を提供する。
* SensorService
    * `SINETStreamHelper`ライブラリのバックエンドとして、Androidの
`SensorManager`経由でセンサーデバイスを制御する。
* SensorListener
    * `SINETStreamHelper`ライブラリからの非同期通知を受けるための
コールバックインタフェースを定義する。


## 3. 作業準備
### 3.1 開発環境の導入

Google社が提供する統合開発環境`AndroidStudio`を入手して手元の
作業機材に導入する\[1\]。

\[1\]:
[Download Android Studio and SDK tools](https://developer.android.com/studio)  

### 3.2 実行環境の用意

Android実機、または`AndroidStudio`のエミュレータを用意する。

* Android 8.0 (APIレベル26）以上


## 4. 作業手順
### 4.1 ビルド環境設定

Android開発環境では、`Gradle`によるビルド管理を行なっている\[1\]。
ユーザアプリケーションが参照する外部ライブラリなどの設定を`Gradle`
制御ファイル`build.gradle`で指定する\[2\]\[3\]。

\[1\]:
[Gradle Build Tool](https://gradle.org/)
<br>
\[2\]:
[モジュールレベルビルドファイル](https://developer.android.com/studio/build#module-level)
<br>
\[3\]:
[ビルド依存関係の追加](https://developer.android.com/studio/build/dependencies)


#### 4.1.1 リポジトリ追加

`SINETStreamHelper`ライブラリの取得先（ここでは`maven`リポジトリ）
をモジュールレベルの`build.gradle`に記述する。

----------（$TOP/app/build.gradle: 抜粋ここから）----------
```build.gradle
repositories {
    maven {
        // For SINETStreamHelper library
        url "https://gitlab.vcp-handson.org/api/v4/projects/39/packages/maven"
        name "GitLab"
        credentials(HttpHeaderCredentials) {
            name = 'Private-Token'
            value = gitLabPrivateToken
        }
        authentication {
            header(HttpHeaderAuthentication)
        }
    }
}
```
----------（$TOP/app/build.gradle: 抜粋ここまで）----------


#### 4.1.2 依存関係追加

前述のリポジトリから参照する`SINETStreamHelper`ライブラリをバージョン
込みでモジュールレベルの`build.gradle`に記述する。

----------（$TOP/app/build.gradle: 抜粋ここから）----------
```build.gradle
dependencies {
    // SINETStreamHelper
    implementation 'jp.ad.sinet.stream.android.helper:libhelper:1.5.0'
}
```
----------（$TOP/app/build.gradle: 抜粋ここまで）----------


### 4.2 マニフェストファイルの記述
#### 4.2.1 利用者権限追加

多くのセンサー種別では特別な利用者権限\[1\]は不要だが、一部例外がある。
歩数計アプリケーションなど、センサー種別「step_counter, step_detector」
を利用する場合、`ACTIVITY_RECOGNITION`を明示的に指定する必要がある\[2\]。

-----（$TOP/app/src/main/AndroidManifest.xml: 抜粋ここから）-----
```xml
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
```
-----（$TOP/app/src/main/AndroidManifest.xml: 抜粋ここまで）-----

<em>注記：</em><br>
上記の利用者権限は、アプリケーション実行時に利用する可能性がある
ものをビルド時の設定として宣言するものである。
Android8.0以降の場合、アプリケーションの初回起動時に権限検査が実施
されるだけでなく、その利用権限は利用者の手動操作によりいつでもOn/Off
可能となる\[3\]。

\[1\]:
[パーミッション](https://developer.android.com/guide/topics/manifest/manifest-intro#perms)
<br>
\[2\]:
[Sensor](https://developer.android.com/reference/android/hardware/Sensor)
<br>
\[3\]:
[Android 8.0での動作変更点](https://developer.android.com/about/versions/oreo/android-8.0-changes#rmp)


### 4.3 開発物のAndroid機材への導入

開発環境`AndroidStudio`を準備し、ユーザアプリケーションを実装\[1\]\[2\]
すると、`APK`（Android package）と呼ばれるアーカイブファイルが生成
される。これはコード、データ、およびリソースファイルをパッケージと
して一つにまとめたものである。
この生成物`APK`ファイルを実行環境（エミュレータや実機）に導入する
方法について述べる。

\[1\]:
[アプリの基礎](https://developer.android.com/guide/components/fundamentals)
<br>
\[2\]:
[アプリをビルドして実行する](https://developer.android.com/studio/run)


#### 4.3.1 Androidエミュレータに導入する場合

以下の要領で`APK`ファイルをエミュレータに導入する\[1\]。
1. 開発環境`AndroidStudio`付属の`Android仮想デバイス`（AVD）ツール
を用いて、所用の諸元（画面解像度、APIレベル、CPU種別など）に
沿った`AVD`を事前作成しておく。

2. この`AVD`を起動するとAndroid画面が表示され、`AndroidStudio`から
実機同様に遠隔操作（`APK`導入やデバッグなど）できるようになる。

3. エミュレータが実行中に`AndroidStudio`上の「Run」コマンドを実行
すると（必要に応じてソースが再構築されて）生成`APK`ファイルが
エミュレータに導入され、自動的に動作を開始する。

\[1\]:
[Android Emulator上でアプリを実行する](https://developer.android.com/studio/run/emulator)


#### 4.3.2 Android実機に導入する場合

以下の要領で`APK`ファイルを実機に導入する\[\1]。
1. Android実機の設定画面を操作し、開発者モードを有効化する。
2. 設定コマンドの開発者メニュー経由で「USBデバッグ」を有効化する。
3. 実機と開発機材をUSBケーブルで接続する。
4. 実機に「デバッグモードで接続して良いか」を確認するダイアログ
が表示される。接続を承認すると、`AndroidStudio`で認識される。
5. この状態で`AndroidStudio`上の「Run」コマンドを実行する。

あるいはターミナル上で`adb`コマンドを直接操作することで対象の
`APK`ファイルを実機に導入できる\[2\]。

-----（操作イメージ：ここから）-----
```console
PC% adb install -r XXX.apk
Success
```
-----（操作イメージ：ここから）-----

\[1\]:
[ハードウェアデバイス上でのアプリの実行](https://developer.android.com/studio/run/device)
<br>
\[2\]:
[Android Debug Bridge (adb)](https://developer.android.com/studio/command-line/adb)


## 5. まとめ

SINETStreamHelperを用いるアプリケーション開発者が留意すべき
項目について一通り概説した。

