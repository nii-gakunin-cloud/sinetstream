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
    2.1 センサ情報の収集
    2.2 位置情報の収集
        2.2.1 GPSサービスを利用
        2.2.2 FLPサービスを利用
    2.3 携帯電話の電波受信状況の収集
3. 作業準備
4. 作業手順
    4.1 ビルド環境設定
        4.1.1 ライブラリファイルの取得
        4.1.2 開発ソースへの組み込み
        4.1.3 リポジトリ追加
        4.1.4 依存関係追加
    4.2 マニフェストファイルの記述
        4.2.1 センサ情報取得に関する利用権限追加
        4.2.2 端末位置情報の追跡取得に関する利用権限追加
        4.2.3 電話状態の参照に関する利用権限追加
    4.3 開発成果物のAndroid端末への導入
        4.3.1 Androidエミュレータに導入する場合
        4.3.2 Android実機に導入する場合
5. アプリケーション実行時の権限の扱い
    5.1 センサ情報取得
        5.1.1 システム要件
        5.1.2 ユーザアプリケーション設定
    5.2 位置情報取得
        5.2.1 システム要件
        5.2.2 ユーザアプリケーション設定
    5.3 携帯電話網の電波受信状況取得
        5.3.1 システム要件
        5.3.2 ユーザアプリケーション設定
6. まとめ
</pre>


## 1. 概要

`IoT（Internet of Things）`アプリケーションの一つとして、
Android端末が実装する様々な[センサ](https://developer.android.com/guide/topics/sensors/sensors_overview?hl=ja)
デバイスから読取値を収集して
[SINETStream](https://nii-gakunin-cloud.github.io/sinetstream)
上の`ブローカ`に送出する`パプリッシャ`機能が考えられる。

ユーザアプリケーションは、
[SensorManager](https://developer.android.com/reference/android/hardware/SensorManager)を経由して個々のセンサデバイスの動作パラメータを設定したり、センサ読取値を参照したりすることができる。
一方で、電力消費の考慮などハードウェア制御に関わる実装上の細かな点を隠蔽し、
簡易に利用できるような補助ライブラリがあると使い勝手が良い。

また、Android 8.0頃から権限管理が厳しくなってきており、多段に渡る動作条件の判定をアプリケーションを構成するアクティビティやフラグメントのライフサイクルを考慮しながら実装するのは難易度が高い。

上記課題の一つの解として`SINETStreamHelper`ライブラリ（以降、本ライブラリと呼称）を用意する。
本ライブラリは、基本機能としてAndroid端末から得られるセンサ読取値をJSON形式に成形して出力する。ユーザ指定により、端末属性（ユーザ情報、端末位置、電波受信状況）の任意の組み合わせをJSON出力に含めることも可能とする。

1) ユーザ情報: [アカウント、備考]

> 複数のAndroid端末で一斉に測定するときに互いを識別するための情報である。

2) 端末位置情報: [緯度、経度、(取得時刻)]

> Android端末を任意の場所に固定するならその位置情報、あるいは同端末を移動しながら間歇的に取得した位置情報とその取得時刻を記録する。

3) 電波受信状況: [ネットワーク種別、信号情報、取得時刻]

> Android端末が接続中の携帯電話網の種別と受信信号強度（`4G/LTE`なら`RSRP`など）、その取得時刻を記録する。


## 2. モジュール構成

本ライブラリは、基本機能として`センサ情報`を収集するモジュール、付加機能として`位置情報`および`電波情報`をそれぞれ収集するサブモジュール群から構成される。

さらに、上記の各機能モジュールの使用にあたり[実行時の権限検査](https://developer.android.com/training/permissions/requesting?hl=ja)を一元的に処理するための権限処理モジュールも用意する。

```
        #--------------------------------------+
        |           User Application           |
        +--------------------------------------+
              A          A           A
    ==========|==========|===========|==================== API functions
              |          |           |
      +-------|----------V-----------|----------------------+
      |       |    +------------+    |    SINETStreamHelper |
      |       |    | Permission |    |                      |
      |       |    | Handler    |    |                      |
      |       |    +------------+    |                      |
      |       V                      V                      |
      | +---------------+  +------------------------------+ |
      | | BASE function |  | OPTIONAL function            | |
      | |               |  |                              | |
      | | +-----------+ |  | +-----------+  +-----------+ | |
      | | | Sensor    | |  | | Location  |  | Celluar   | | |
      | | | Module    | |  | | Submodule |  | Submodule | | |
      | | +-----------+ |  | +-----------+  +-----------+ | |
      | +---------------+  +------------------------------+ |
      +-----------------------------------------------------+
```
〈凡例〉
* 基本機能
  * センサ情報モジュール
    * センサ群の動作制御、測定値の読み取り、JSON形式での出力を担当する。
* 付加機能
  * 位置情報サブモジュール
    * GPSなどの情報源経由でAndroid端末の位置情報を追跡取得する。
  * 電話情報サブモジュール
    * 携帯電話網の電波受信状況を読み取る。
* 権限処理モジュール
  * 実行時の権限検査を一元的に処理する。


### 2.1 センサ情報の収集

ほとんどのAndroid端末は様々な[センサ](https://developer.android.com/guide/topics/sensors?hl=ja)素子が実装されており、[SensorManager](https://developer.android.com/reference/android/hardware/SensorManager)経由でこれらの読み取り値を取得することができる。

センサ情報収集に関する機能モジュール構成の概略は下図のようになる。

```
        #---------------------------------------+
        | User Application                      |
        +---------------------------------------+
             |                      A
    =========|======================|=============== API functions
             |                      | [JSON]
      +------|----------------------|-------------+
      |      V                      |             |
      | +------------------+   +----------------+ |
      | | SensorController |-->| SensorListener | |
      | +------------------+   +----------------+ |
      |      |      A                             |
      |      V      |                             |
      | +------------------+                      |
      | |  SensorService   |                      |
      | +------------------+    SINETStreamHelper |
      +------|------A-----------------------------+
             |      |
    =========|======|==================================== Android System
             |      |
             V      | [SensorEvent]
        +------------------+
        |  SensorManager   |
        +------------------+
               |  A
    ===========|==|============================================= Devices
               |  |
               V  | [Raw Data]
            +--------+
            | Sensor |+
            | Device ||+
            +--------+||   [pressure, temperature, gravity, ...]
              +-------+|
               +-------+
```
〈凡例〉
* SensorController
  * センサ制御情報の受け渡しなど、ユーザインタフェースを担当する。
  * `SensorListener`のコールバックでセンサ読み取り値をJSON形式で返却する。
* SensorService
  * Androidの[SensorManager](https://developer.android.com/reference/android/hardware/SensorManager)経由でセンサ制御を依頼する。
  * `SensorManager`より[SensorEvent](https://developer.android.com/reference/android/hardware/SensorEvent)としてセンサ読み取り値の非同期通知を受ける。
* SensorListener
  * ユーザアプリケーションが実装すべきコールバックインタフェースを定義する。


センサ種別によりスカラ値、またはベクタ値が返却される。
値の定義や単位の詳細は[SensorEvent](https://developer.android.com/reference/android/hardware/SensorEvent)の記述を参照されたい。

> <em>注意</em><br>
> 着目のAndroid端末でどのセンサ種別を使えるかは実行環境依存である。
> ハードウェアとして実装されるセンサ素子、および稼働Android OSの版数の組み合わせにより`SensorManager`で対応可能なセンサ種別が異なるためである。


### 2.2 位置情報の収集

Android端末の位置情報（緯度経度）は、手動あるいは自動更新で設定可能とする。

* ユーザアプリケーション側からの設定指示
  * 指定された位置情報（緯度経度）を出力JSONデータに反映する。
* 位置情報源の参照による自動更新
  * 方式1：`GPS`サービス経由で随時更新される位置情報をJSONデータに反映する。
  * 方式2：`FLP`サービス経由で随時更新される位置情報をJSONデータに反映する。

以下では、後者の2通りの自動更新について概説する。


#### 2.2.1 GPSサービスを利用

着目のAndroid端末に`GPS`受信機が搭載されていれば、上空を飛び交う`GPS`衛星から直接受信した（あるいはトンネル内部や地下街などの場所では地上から中継された）`GPS`信号から端末位置情報を取得できる可能性がある。

`GPS`サービスにはAndroid初版（APIレベル1）から対応しており、[LocationManager](https://developer.android.com/reference/android/location/LocationManager)を介して`GPS`経由の端末位置情報を収集することができる。

> 実際には端末全体の設定状況やアプリケーション実行時権限の設定、
> あるいは`GPS`信号の受信状況にも左右される。

Android端末の位置情報の情報源として`GPS`受信機のみを利用する場合、
機能モジュール構成の概略は下図のようになる。

```
        #---------------------------------------------------+
        | User Application                                  |
        +---------------------------------------------------+
             |  A                    |              A
    =========|==|====================|==============|===== API functions
             |  |                    |              |
             |  | [Location]         | [Location]   | [JSON]
      +------|--|--------------------|--------------|---------+
      |      V  |         :          V              |         |
      | +------------+    :    +------------+    +----------+ |
      | | Location   |    :    | Sensor     |--->| Sensor   | |
      | | Tracker    |    :    | Controller |    | Listener | |
      | +------------+    :    +------------+    +----------+ |
      |      |  A         :          :                        |
      |      V  | [Location]         V                        |
      | +------------+    :        [...]                      |
      | | GPS        |    :                                   |
      | | Service    |    :                                   |
      | +------------+    :                 SINETStreamHelper |
      +------|--A---------------------------------------------+
             |  |
    =========|==|======================================== Android System
             |  |
             V  | [Location]
      +----------------+
      | Location       |
      | Manager (GPS)  |
      +----------------+
             |  A
    =========|==|=============================================== Devices
             |  |
             V  | [Raw Data]
         +----------+
         |   GPS    |
         +----------+
```
〈凡例〉
* LocationTracker
  * 位置情報の動作制御など、ユーザインタフェースを担当する。
* GpsService
  * Androidの[LocationManager](https://developer.android.com/reference/android/location/LocationManager)経由で`GPS`受信機の動作制御を依頼する。
  * `GPS`用の[LocationListener](https://developer.android.com/reference/android/location/LocationListener)に設定したコールバックにより、
位置情報[Location](https://developer.android.com/reference/android/location/Location)の非同期通知を受ける。

><em>注意</em><br>
> `LocationTracker`と`SensorController`は独立に動作する。
> 位置情報はユーザアプリケーションを経由して`SensorController`に渡され、
> センサデータと併せてJSON形式に成形される。


#### 2.2.2 FLPサービスを利用

[Google Play開発者サービスの位置情報API](https://developers.google.com/location-context?hl=ja)として
[`Fused Location Provider API (FLP)`](https://developers.google.com/location-context/fused-location-provider?hl=ja)が提供されている。
これを利用することで、
`GPS受信機`だけでなく`携帯電話網の基地局情報`、`Wi-Fiアクセスポイント`、あるいは`Bluetooth`など様々な位置情報源を組み合わせて精度良く位置情報を取得できる可能性がある。

`FLP`はAndroid 12（APIレベル31）以降で対応しており、[LocationManager](https://developer.android.com/reference/android/location/LocationManager)を介して`FLP`経由の端末位置情報を収集することができる。

> 実際には端末全体の設定状況やアプリケーション実行時権限の設定に左右される。

Android端末の位置情報の情報源として`FLP`を利用する場合、
機能モジュール構成の概略は下図のようになる。

```
        #---------------------------------------------------+
        | User Application                                  |
        +---------------------------------------------------+
             |  A                    |              A
    =========|==|====================|==============|===== API functions
             |  |                    |              |
             |  | [Location]         | [Location]   | [JSON]
      +------|--|--------------------|--------------|---------+
      |      V  |         :          V              |         |
      | +------------+    :    +------------+    +----------+ |
      | | Location   |    :    | Sensor     |--->| Sensor   | |
      | | Tracker    |    :    | Controller |    | Listener | |
      | +------------+    :    +------------+    +----------+ |
      |      |  A         :          :                        |
      |      V  | [Location]         V                        |
      | +------------+    :        [...]                      |
      | | FLP        |    :                                   |
      | | Service    |    :                                   |
      | +------------+    :                 SINETStreamHelper |
      +------|--A---------------------------------------------+
             |  |
    =========|==|======================================== Android System
             |  |
             |  |                ..............................
             V  | [Location]     : GoogleAPI                  :
      +----------------+         : +------------------------+ :
      | Location       | <---------| Fused                  | :
      | Manager (FLP)  |      +--->| LocationProviderClient | :
      +----------------+      |  : +------------------------+ :
             |  A             |  :.....A.......A.......A......:
             |  |             |        |       |       |
    =========|==|=============|========|=======|=======|======== Devices
             |  |             |        |       |       |
             V  | [Raw Data]  |        |       |       |
         +----------+         |     +-----+ +----+ +-------+
         |   GPS    |---------+     | NET | | BT | | Wi-Fi |
         +----------+               +-----+ +----+ +-------+
```
〈凡例〉
* LocationTracker
  * 位置情報の動作制御など、ユーザインタフェースを担当する。
* FlpService
  * Androidの[LocationManager](https://developer.android.com/reference/android/location/LocationManager)経由で`Google Play開発者サービス`の
[FusedLocationProviderClient](https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderClient)の動作制御を依頼する。
  * `FLP`用の[LocationCallback](https://developers.google.com/android/reference/com/google/android/gms/location/LocationCallback)に設定したコールバックにより、
位置情報[Location](https://developer.android.com/reference/android/location/Location)の非同期通知を受ける。

><em>注意</em><br>
> `LocationTracker`と`SensorController`は独立に動作する。
> 位置情報はユーザアプリケーションを経由して`SensorController`に渡され、
> センサデータと併せてJSON形式に成形される。


### 2.3 携帯電話の電波受信状況の収集

有効な`SIM（Subscriber Identity Module: 加入者識別情報）`をAndroid端末に装着して携帯電話網に接続している限り、同端末は最寄りの基地局との制御データ通信のために時々刻々と電波状況を監視している。

Androidシステムの[TelephonyManager](https://developer.android.com/reference/android/telephony/TelephonyManager)を介することで、電話機能の制御情報（ここでは電波受信状況）を収集することができる。

電波受信状況の収集に関する機能モジュール構成の概略は下図のようになる。

```
        #---------------------------------------------------+
        | User Application                                  |
        +---------------------------------------------------+
             |  A                    |              A
    =========|==|====================|==============|===== API functions
             |  |                    |              |
             |  | [SignalInfo]       | [SignalInfo] | [JSON]
      +------|--|--------------------|--------------|---------+
      |      V  |         :          V              |         |
      | +------------+    :    +------------+    +----------+ |
      | | Cellular   |    :    | Sensor     |--->| Sensor   | |
      | | Monitor    |    :    | Controller |    | Listener | |
      | +------------+    :    +------------+    +----------+ |
      |      |  A         :          :                        |
      |      V  | [SignalInfo]       V                        |
      | +------------+    :        [...]                      |
      | | Cellular   |    :                                   |
      | | Service    |    :                                   |
      | +------------+    :                 SINETStreamHelper |
      +------|--A---------------------------------------------+
             |  |
    =========|==|======================================== Android System
             |  |
             V  | [SignalInfo]
        +------------+
        | Telephony  |
        | Manager    |
        +------------+
             |  A
    =========|==|=============================================== Devices
             |  |
             V  | [Raw Data]
         +----------+
         |  MODEM   |
         +----------+
```
〈凡例〉
* CellularMonitor
  * 電波受信状況の通知制御など、ユーザインタフェースを担当する。
  * 得られた電波受信状況をユーザアプリケーションにコールバックで通知する。
* CellularService
  * Androidの[TelephonyManager](https://developer.android.com/reference/android/telephony/TelephonyManager
)の動作制御を依頼する。
  * Android11（APIレベル30）以下の場合：[PhoneStateListener](https://developer.android.com/reference/android/telephony/PhoneStateListener)に設定したコールバックにより、
信号強度[SignalStrength](https://developer.android.com/reference/android/telephony/SignalStrength)の非同期通知を受ける。
  * Android12（APIレベル31）以上の場合：[TelephonyCallback](https://developer.android.com/reference/android/telephony/TelephonyCallback)に設定したコールバックにより、
信号強度[SignalStrength](https://developer.android.com/reference/android/telephony/SignalStrength)の非同期通知を受ける。

><em>注意</em><br>
> `CellularMonitor`と`SensorController`は独立に動作する。
> 電波受信状況はユーザアプリケーションを経由して`SensorController`に渡され、
> センサデータと併せてJSON形式に成形される。


## 3. 作業準備

別紙[SINETStream for Android ユーザガイド](android.md)の第3章を参照のこと。

> `SINETStreamHelperライブラリ`の場合、Android動作環境の制約は特にない。


## 4. 作業手順
### 4.1 ビルド環境設定

ユーザアプリケーションにとって、`SINETStreamHelper`は外部ライブラリとして参照するものである。
すなわち「どこに何があるか」というリポジトリ参照先と、「どのバージョンのものを参照するか」という二種類の情報を開発環境に設定する必要がある。

`Android Studio`では、[Gradle Build Tool](https://gradle.org/)によるビルド管理を行なっている。
よって、ユーザアプリケーションが参照する外部ライブラリなどの設定情報は`Gradle`制御ファイルの一つである
[モジュールレベルの`build.gradle`](https://developer.android.com/studio/build#module-level?hl=ja)にて設定する。
[ビルド依存関係の追加](https://developer.android.com/studio/build/dependencies?hl=ja)の記述を参照されたい。

以降では、ユーザのAndroid開発ソースから`SINETStreamHelper`ライブラリを利用するための具体的な手順について記述する。


#### 4.1.1 ライブラリファイルの取得

`SINETStreamHelper`ライブラリは、
ソースおよびバイナリの形式で`GitHub`上で公開される。

国立情報学研究所（NII）が管理するリポジトリ
[sinetstream-android-helper](https://github.com/nii-gakunin-cloud/sinetstream-android-helper/releases)
より、最新バージョンの`libhelper-x.y.z.aar`を手元にダウンロードする。
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
前項で取得した`SINETStreamHelper`ライブラリをそこに格納する。

```console
    % cd app
    % mkdir libs
    % cd libs
    % cp $HOME/Downloads/libhelper-x.y.z.aar .
```


#### 4.1.3 リポジトリ追加

`SINETStreamHelper`ライブラリは、目的のものを手動で手元にダウンロードして、
ローカルライブラリ格納用の`libs`ディレクトリに配置する。
それを参照するよう、
モジュールレベル（この場合`app`）の`build.gradle`に記述する。

```gradle:app/build.gradle
repositories {
    flatDir {
        // For SINETStreamHelper library
        dirs "libs"
    }
}
```


#### 4.1.4 依存関係追加

前項でリポジトリを指定した。そこから参照するライブラリ名とバージョンを、
モジュールレベルの`build.gradle`に[ビルド依存関係を追加する](https://developer.android.com/studio/build/dependencies?hl=ja)。

```gradle:app/build.gradle
dependencies {
    // SINETStreamHelper
    implementation(name: 'sinetstream-android-helper-x.y.z', ext: 'aar')
}
```


### 4.2 マニフェストファイルの記述

`SINETHelperライブラリ`を利用するユーザアプリケーションは、
同ビルド環境の[マニフェストファイル（AndroidManifest.xml）](https://developer.android.com/guide/topics/manifest/manifest-intro?hl=ja)において、
所用の[権限](https://developer.android.com/guide/topics/manifest/manifest-intro?hl=ja#perms)（`uses-permission`句）を宣言する必要がある。
さもないと当該アプリケーション実行時に`権限エラー`が発生する。

> <em>注意</em><br>
> 上記の利用者権限は、ユーザアプリケーションが「潜在的に利用する可能性がある」項目群をビルド時に宣言するものである。
> Android8.0以降、アプリケーション実行時に権限検査が実施されるのみならず、
> 利用者の手動操作により[いつでもこれらの権限を`On/Off`可能](https://developer.android.com/about/versions/oreo/android-8.0-changes#rmp)となる。

以降では、本ライブラリが具備する機能モジュールごとに記述する。


#### 4.2.1 センサ情報取得に関する利用権限追加

Android端末が具備する[センサ](https://developer.android.com/reference/android/hardware/Sensor)種別の大部分は、
その利用に特別な利用権限を求められることはない。

しかし、一部の生体情報など[個人情報と考えられるセンサ種別](https://developers.google.com/fit/android/authorization#data_types_that_need_android_permissions)
に関してはその限りではない。
例えば歩数計アプリケーションなどで以下のセンサ種別

* [step_counter](https://developer.android.com/reference/android/hardware/Sensor#TYPE_STEP_COUNTER)
* [step_detector](https://developer.android.com/reference/android/hardware/Sensor#TYPE_STEP_DETECTOR)

を利用する場合、以下の内容をマニフェストファイルに設定すること。

```xml:AndroidManifest.xml
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
```


#### 4.2.2 端末位置情報の追跡取得に関する利用権限追加

Androidシステムにおいて端末の位置情報はプライバシー保護対象と考えられている。
本ライブラリを利用するアプリケーションは、端末の位置情報にアクセスするため以下の権限をマニフェストファイルに追加で記述する必要がある。

* FOREGROUND_SERVICE
   * Android9以降はバックグラウンドサービスの位置情報取得の頻度に制限がかかるようになった。常にフォアグラウンドモードを利用する。
* ACCESS_COARSE_LOCATION
   * 精度の荒い位置情報を取得する。
* ACCESS_FINE_LOCATION
   * 精度の細かい位置情報を取得する。

```xml:AndroidManifest.xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

<!-- Always include this permission -->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- Include only if your app benefits from precise location access. -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

> <em>注意<em><br>
> `GPS`や`FLP`経由で高精度の位置情報を取得するには、権限`ACCESS_FINE_LOCATION`と
> `ACCESS_COARSE_LOCATION`の両者を同時に指定すること。
> 前者は後者を包含しているように思えるが、前者だけの指定だとビルドエラーとなる。


#### 4.2.3 電話状態の参照に関する利用権限追加

Androidシステムにおいて、電話状態の参照は[危険な権限](https://developer.android.com/guide/topics/permissions/overview?hl=ja#dangerous_permissions)として扱われる。このため以下の権限をマニフェストファイルに追加で記述する必要がある。

* READ_PHONE_STATE
   * 電波受信状況を取得する。

```xml:AndroidManifest.xml
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
```


### 4.3 開発成果物のAndroid端末への導入

開発環境`AndroidStudio`を準備し、ユーザアプリケーションを実装\[1\]\[2\]すると、
`APK`（Android package）と呼ばれるアーカイブファイルが生成される。

APKとは、アプリケーションを構成する「コード、データ、およびリソースファイル」をパッケージとして一つにまとめたものである。
この生成物`APK`ファイルを実行環境（エミュレータや実機）に導入する方法について述べる。

\[1\]:
[アプリの基礎](https://developer.android.com/guide/components/fundamentals)
<br>
\[2\]:
[アプリをビルドして実行する](https://developer.android.com/studio/run)


#### 4.3.1 Androidエミュレータに導入する場合

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


#### 4.3.2 Android実機に導入する場合

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


## 5. アプリケーション実行時の権限の扱い

以下に挙げるようなAndroidシステムが提供する特定の機能

* 一部のセンサ種別の利用
* 端末位置情報の取得
* 電波受信状況の取得

を利用するには、Android端末のハードウェア仕様やAndroid OSに関するシステム要件のみならず、ユーザアプリケーション実行時の適切な[権限管理](https://developer.android.com/guide/topics/permissions/overview?hl=ja)が必要となる。

すなわち、以下のような多段にわたる条件を全て満足する必要がある。

* <em>実行環境</em>
  * Android端末でハードウェア的に対応していること。そもそも動作基盤がなければ対処しようがない。
  * Android OSでシステム的に対応していること。特定の版数以降のOSでないと対応していないことがある。
* [<em>導入時の権限</em>](https://developer.android.com/guide/topics/permissions/overview?hl=ja#install-time)
  * ユーザプログラム開発時のマニフェストファイルで所用の[権限が宣言](https://developer.android.com/training/permissions/declaring?hl=ja)されていること。
* [<em>実行時の権限</em>](https://developer.android.com/guide/topics/permissions/overview?hl=ja#runtime)
  * Android端末全体として所用のシステム設定が有効化されていること。
  * アプリケーション個別に所用の権限が有効化されていること。

> <em>注意</em><br>
> 実行時権限は、ユーザ操作により任意の契機でon/offできるものである。
> また、当該アプリケーションを一定期間起動していないと、システムにより実行時権限が解除されることもある。

例えば、ユーザアプリケーションの実行時にAndroid端末の位置情報を取得するには、システム設定で位置情報を有効化し、さらに当該アプリケーション個別の権限設定で所用の項目を有効化する必要があるという具合である。

動的に権限の付与や解除が可能となる実行時の権限の扱いに関してユーザアプリケーション開発者の負担を軽減するため、本ライブラリでは実行時権限を一元的に扱うインタフェースを用意する。


### 5.1 センサ情報取得

#### 5.1.1 システム要件

ユーザアプリケーションが生体情報のような個人情報に関わるセンサ値を参照するためには、前提条件として以下の全ての項目を満足する必要がある。

|項目|条件|備考|
|----|----|----|
|[step_counter](https://developer.android.com/reference/android/hardware/Sensor#TYPE_STEP_COUNTER)センサ|Android端末に搭載あり？|ハードウェア実装で対応していること|
|[step_detector](https://developer.android.com/reference/android/hardware/Sensor#TYPE_STEP_DETECTOR)センサ|Android端末に搭載あり？|ハードウェア実装で対応していること|
|Android OS|4.4W（APIレベル20）以降？|古すぎるOSでは対応しない|


#### 5.1.2 ユーザアプリケーション設定

特定のセンサ種別の参照に関するシステム要件を満足していたとしても、それだけでは不十分である。
Androidシステム上の[権限管理方針](https://developer.android.com/guide/topics/permissions/overview?hl=ja)により、個々のアプリケーションごとに実行権限の付加状況（この場合は個人情報に関わるセンサ情報を取得できるか）が動的に管理される。
すなわち、いったん利用許可が得られても任意の契機でその権限が解除され得る。

本ライブラリでは、ユーザアプリケーション実行時にセンサ情報の参照権限を検査するAPIを用意している。

```java
import jp.ad.sinet.stream.android.helper.PermissionHandler;

PermissionHandler = new PermissionHandler(...);
PermissionHandler.checkSensorPermissions(); // <--(!)
PermissionHandler.run();

```

上記動作により、状況に応じて以下のようなダイアログがライブラリから表示される。

|項目|条件|備考|
|----|----|----|
|ライブラリダイアログ<br>`Please allow app-level permissions<br>(Activity Recognition)`|未設定時に自動出現|OKを選択|
|システムダイアログ<br>`身体活動データへのアクセスを「XXX」に許可しますか？`|未設定時に自動出現|`許可`を選択|

ここで許可すれば、身体情報に関わるセンサ種別は利用可能なセンサ種別に含まれ、さもなくば除外される。


### 5.2 位置情報取得

#### 5.2.1 システム要件

任意のアプリケーションが`GPS`または`FLP`経由の位置情報を取得するための前提条件として、以下の全ての項目を満足する必要がある。

|項目|条件|備考|
|----|----|----|
|GPSセンサ|Android端末に搭載あり？|ハードウェア実装で対応していること|
|Android OS|`GPS`は全ての版のAndroid、<br>`FLP`はAndroid 12 (APIレベル31）以降|古すぎるOSでは対応しない|
|システム設定<br>`位置情報`|システム全体設定<br>位置情報を利用|設定 -> 位置情報 -> `位置情報を使う`を有効化|
|システム設定<br>`位置情報`|Google Playサービス<br>位置情報の精度|設定 -> 位置情報 -> 詳細設定 -> Google 位置情報の精度 -> `位置情報の精度を改善`を有効化|


#### 5.2.2 ユーザアプリケーション設定

前述した位置情報に関するシステム要件のうち、ハードウェアおよびAndroid OS版数以外のシステム設定は任意の契機で変更可能である。

一方、Androidシステム上の[権限管理方針](https://developer.android.com/guide/topics/permissions/overview?hl=ja)により、個々のアプリケーションごとに実行権限の付加状況（この場合はユーザアプリケーションが位置情報を使えるか）が動的に管理される。
すなわち、いったん利用許可が得られても任意の契機でその権限が解除され得る。

本ライブラリでは、[実行時に位置情報へのアクセスをリクエストする](https://developer.android.com/training/location/permissions?hl=ja#request-location-access-runtime)よう制御するため、ユーザアプリケーション実行時に実行権限を検査するAPIを用意している。

```java
import jp.ad.sinet.stream.android.helper.PermissionHandler;

PermissionHandler = new PermissionHandler(...);
PermissionHandler.checkLocationPermissions(providerName); // <--(!)
PermissionHandler.run();

```

上記動作により、状況に応じて以下のようなダイアログがライブラリから表示される。

|項目|条件|備考|
|----|----|----|
|ライブラリダイアログ<br>`Please allow app-level permissions`<br>`(Location)`|未設定時に自動出現|OKを選択|
|システムダイアログ<br>`このデバイスの位置情報へのアクセスを「XXX」に許可しますか？`|未設定時に自動出現|`アプリの使用時のみ`または`今回のみ`を選択<br>（精度は「おおよそ」ではなく「正確」を選択すること)|
|ライブラリダイアログ<br>`Insufficient location resolution:`<br>`Please turn on "Google Location Accuracy" in "Location Services"`|位置情報の精度不足のときに自動出現|設定 -> 位置情報 -> 詳細設定 -> Google 位置情報の精度 -> `位置情報の精度を改善`を有効化|

以上の検査に全て合格すれば、ユーザアプリケーションからAndroid端末の位置情報を参照できるようになる。


### 5.3 携帯電話網の電波受信状況取得

#### 5.3.1 システム要件

ユーザアプリケーションが携帯電話網の電波受信状況を取得するための前提条件として、以下の全ての項目を満足する必要がある。

|項目|条件|備考|
|----|----|----|
|モデムチップ|Android端末に搭載あり？|ハードウェア実装で対応していること|
|システム設定<br>`ネットワークとインターネット`|機内モードを解除|設定 -> ネットワークとインターネット -> `機内モード`を解除|
|システム設定<br>`ネットワークとインターネット`|有効なSIMの利用|設定 -> ネットワークとインターネット -> SIM -> `SIMを使用`を有効化<br>(SIMの利用者情報やAPNが正しく設定されていること)|


#### 5.3.2 ユーザアプリケーション設定

前項のシステム要件を満足していたとしても、それだけでは不十分である。
Androidシステム上の[権限管理方針](https://developer.android.com/guide/topics/permissions/overview?hl=ja)により、個々のアプリケーションごとに実行権限の付加状況（この場合は電話状態を参照できるか）が動的に管理される。
すなわち、いったん利用許可が得られても任意の契機でその権限が解除され得る。

本ライブラリでは、ユーザアプリケーション実行時に電話状態の参照権限を検査するAPIを用意している。

```java
import jp.ad.sinet.stream.android.helper.PermissionHandler;

PermissionHandler = new PermissionHandler(...);
PermissionHandler.checkCellularPermissions(); // <--(!)
PermissionHandler.run();

```

上記動作により、状況に応じて以下のようなダイアログがライブラリから表示される。

|項目|条件|備考|
|----|----|----|
|ライブラリダイアログ<br>`Please turn off airplane mode`|未設定時に自動出現|OKを選択|
|システム設定<br>`ネットワークとインターネット`|機内モードを解除|設定 -> ネットワークとインターネット -> `機内モード`を解除|
|ライブラリダイアログ<br>`Please allow app-level permissions`<br>`(READ_PHONE_STATE)`|未設定時に自動出現|OKを選択|
|システムダイアログ<br>`電話の発信と管理を「XXX」に許可しますか？`|未設定時に自動出現|`許可`を選択|

以上の検査に全て合格すれば、ユーザアプリケーションから電話状態（電波受信状況）を参照できるようになる。


## 6. まとめ

SINETStreamHelperを用いるアプリケーション開発者が留意すべき項目について、
一通り概説した。

