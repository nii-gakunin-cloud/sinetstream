<!--
Copyright (C) 2021 National Institute of Informatics

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

[English](https://translate.google.com/translate?hl=en&sl=ja&tl=en&u=https://nii-gakunin-cloud.github.io/sinetstream/docs/userguide/libhelper_api/location_tracker.html "google translate")

SINETStreamHelper ユーザガイド（端末位置情報の追跡部）

**目次**
<pre>
1. モジュール構成
2. API概要
3. インタフェース LocationTrackerListener
3.1 メソッド概要
3.2 メソッド詳細
3.2.1 onLocationSettingsChecked
3.2.2 onLocationEngaged
3.2.3 onLocationDisengaged
3.2.4 onLocationDataReceived
3.2.5 onError
4. クラスLocationTracker
4.1 コンストラクタ概要
4.2 メソッド概要
4.3 コンストラクタ詳細
4.4 メソッド詳細
4.4.1 start
4.4.2 stop
4.4.3 bindLocationService
4.4.4 unbindLocationService

付録
A.1 ライフサイクル
</pre>


# 1. モジュール構成

`SINETStreamHelper`ライブラリにおける「Android端末位置の追跡取得」機能のモジュール構成を以下に示す。

```
      #-------------------------------------------------------+
      |                User Application                       |
      +-------------------------------------------------------+
                         |                             A
    =====================|=============================|== API functions
                         |                             |
      +------------------|-----------------------------|------+
      |                  V                             | [Location]
      | +----------------------------------+     +----------+ |
      | | LocationTracker                  |---> | Location | |
      | |       +----------+  +----------+ |     | Tracker  | |
      | |       |Permission|  |Settings  | |     | Listener | |
      | |       |Handler   |  |Handler   | |     +----------+ |
      | |       +----------+  +----------+ |                  |
      | +----------------------------------+                  |
      |      |  A                   |  A                      |
      |      V  | [Location]        V  | [Location]           |
      | +----------------+     +----------------+             |
      | | GpsService     |     | FlpService     |             |
      | +----------------+     +----------------+             |
      |      |  A                   |  A                      |
      |      |  |                   |  |    SINETStreamHelper |
      |      |  |                   |  |    (Location Handler)|
      +------|--|-------------------|--|----------------------+
             |  |                   |  |
    =========|==|===================|==|================= Android System
             |  |                   V  |
             |  |                ..............................
             V  | [Location]     : Google Play Service        :
        +------------+           : +------------------------+ :
        | Location   |        +--->| Fused                  | :
        | Manager    |        |  : | LocationProviderClient | :
        +------------+        |  : +------------------------+ :
             |  A             |  :     A       A       A      :
             |  |             |  :.....|.......|.......|......:
    =========|==|=============|========|=======|=======|======== Devices
             |  |             |        |       |       |
             V  | [Raw Data]  |        |       |       | [Raw Data]
         +----------+         |     +-----+ +----+ +-------+
         |   GPS    |---------+     | NET | | BT | |  ...  |
         +----------+               +-----+ +----+ +-------+
```
〈凡例〉
* LocationTracker
    * Android端末位置情報の追跡取得機能のフロントエンドとして、制御用のAPI関数一式を提供する。

* Permission Handler
    * Androidシステムで端末の位置情報取得のためには、ユーザアプリケーション実行時に所用のアクセス権限を取得する必要がある。
    * `SINETStreamHelper`ライブラリがユーザアプリケーションの代理として権限取得処理を操作する。

* Settings Handler
    * システム設定情報の現在の値（特に位置情報取得に関する箇所）を参照する。

* GpsService
    * Android端末位置情報の追跡取得機能のバックエンドとして、常駐型サービスとして動作する。本書では対となるFlpServiceと合わせ`位置情報サービス`と呼称する。
    * Android端末の具備するGPSデバイスを利用して測位するための制御を担当する。
    * 測位精度を確保するためにフォアグラウンドで動作し、動作中であることを示す通知アイコンを表示する。
    * FlpServiceとは排他動作となる。

* FlpService
    * Android端末位置情報の追跡取得機能のバックエンドとして、常駐型サービスとして動作する。本書では対となるGpsServiceと合わせ`位置情報サービス`と呼称する。
    * Google Play開発者サービスと連携して測位するための制御を担当する。
    * 測位精度を確保するためにフォアグラウンドで動作し、動作中であることを示す通知アイコンを表示する。
    * GpsServiceとは排他動作となる。

* LocationTrackerListener
    * `SINETStreamHelper`ライブラリからの端末位置情報自動更新に関するコールバックインタフェースを定義する。
    * これらの実体は利用者側アプリケーションで実装すること。


# 2. API概要

* パッケージ
    * jp.ad.sinet.stream.android.helper

* 公開インタフェース
    * LocationTrackerListener

* 公開クラス
    * LocationTracker


# 3. インタフェース LocationTrackerListener

## 3.1 メソッド概要
* onError
    * エラー検出時に呼ばれる。
* onLocationSettingsChecked
    * 位置情報の利用可能性検査が完了した場合に呼ばれる。
* onLocationDisengaged
    * メソッド`LocationTracker.unbindLocationService()`により、
本ライブラリ内部の`位置情報サービス`と切断した場合に呼ばれる。
* onLocationEngaged
    * メソッド`LocationTracker.bindLocationService()`により、
本ライブラリ内部の`位置情報サービス`と結合した場合に呼ばれる。
* onLocationDataReceived
    * 新しい位置情報が得られた場合に呼ばれる。


## 3.2 メソッド詳細
### 3.2.1 onLocationSettingsChecked

```java
void onLocationSettingsChecked(boolean isReady)
```

* 説明：
    * 位置情報の利用可能性検査が完了した場合に呼ばれる。

* 引数:
    * isReady - 真であれば、次のメソッド`LocationTracker.bindLocationService()`を発行可能。さもなくば、同メソッドがエラーで失敗する。


### 3.2.2 onLocationEngaged

```java
void onLocationEngaged(@NonNull
                       java.lang.String info)
```

* 説明：
    * メソッド`LocationTracker.bindLocationService()`により、
本ライブラリ内部の`位置情報サービス`と結合した場合に呼ばれる。
    * この時点から、クライアントは端末の位置情報の通知を受けられる。

* 引数:
    * info - システムからの通知メッセージ（なければ空文字）


### 3.2.3 onLocationDisengaged

```java
void onLocationDisengaged(@NonNull
                          java.lang.String info)
```

* 説明：
    * メソッド`LocationTracker.unbindLocationService()`により、
本ライブラリ内部の`位置情報サービス`と切断した場合に呼ばれる。
    * クライアントの終了前にこの通知を待つこと。

* 引数:
    * info - システムからの通知メッセージ（なければ空文字）


### 3.2.4 onLocationDataReceived

```java
void onLocationDataReceived(@NonNull
                            android.location.Location location)
```

* 説明：
    * 新しい位置情報を得た場合に呼ばれる。

* 引数:
    * location - Androidシステムから通知された位置情報
    > Android開発者文書にて定義された
    > [Location](https://developer.android.com/reference/android/location/Location)
    > オブジェクト


### 3.2.5 onError

```java
void onError(@NonNull
             java.lang.String errmsg)
```

* 説明：
    * 何らかのエラー条件を満たしたときに呼ばれる。
* 引数:
    * errmsg - エラー内容メッセージ


# 4. クラスLocationTracker

* 本クラスは、SINETStreamHelperライブラリのフロントエンドとして端末位置情報の自動更新を受けるためのAPI関数一式を提供する。
* LocationTrackerは本ライブラリ内部の
[サービス](https://developer.android.com/guide/components/bound-services)
要素`GpsService`または`FlpService`と結合し、同サービスと協調動作する。
* 位置情報サービスとはAndroidのメッセージング機構で通信するため、以下の
メソッドは非同期要求として扱うこと。
    * start
    * stop
    * bindLocationService
    * unbindLocationService
* 本クラスの利用者は、呼び出し側の
[Activity](https://developer.android.com/guide/components/activities/intro-activities)
において`LocationTrackerListener`を実装し、処理結果やエラーの非同期通知を受けられるようにしなければならない。


## 4.1 コンストラクタ概要
* LocationTracker
    * LocationTrackerのインスタンスを生成する。


## 4.2 メソッド概要
* start
    * システム設定状況に問題なければ、`位置情報サービス`を起動する。
* stop
    * `位置情報サービス`が実行中であれば、これを終了する。
* bindLocationService
    * `位置情報サービス`と結合し、センサー処理を開始する。
* unbindLocationService
    * `位置情報サービス`と切断し、センサー処理を終了する。


## 4.3 コンストラクタ詳細

```java
public LocationTracker(@NonNull
                       androidx.appcompat.app.AppCompatActivity activity,
                       @NonNull
                       final String locationProviderName,
                       int clientId)
```

* 説明：
    * LocationTrackerのインスタンスを生成する。
* 引数:
    * activity - `LocationTrackerListener`を実装した
[基本アクティビティ](https://developer.android.com/reference/androidx/appcompat/app/AppCompatActivity)
    * locationProviderName - 端末位置を追跡する情報源となる位置情報プロバイダ種別（`gps`または`fused`）
    * clientId - `位置情報サービス`に結合したクライアント同士を峻別する識別子
* 注意：
    * 本コンストラクタは、端末位置の取得に関するシステム設定状況や実行時権限の確認のために
[ActivityResultLauncher](https://developer.android.com/reference/androidx/activity/result/ActivityResultLauncher)により別アクティビティを本ライブラリから呼び出している\[1\]。
    * 上記`ActivityResultLauncher`の利用上の制限のため、本コンストラクタは利用者側`Activity`の`onCreate()`内で呼び出す必要がある。さもないと実行時にIllegalStateExceptionが発生する\[2\]。
* 例外:
    * java.lang.RuntimeException - 付与のコンテクストが所用のリスナーを実装していない
    * java.lang.IllegalStateException - 呼び出し側Activityの内部状態が`onCreate`でない
* 参考:
    * \[1\] [アクティビティの結果を取得する](https://developer.android.com/training/basics/intents/result?hl=ja)
    * \[2\] [Activity バージョン 1.2.0-beta01: 動作の変更](https://developer.android.com/jetpack/androidx/releases/activity?hl=ja#1.2.0-beta01)


## 4.4 メソッド詳細
### 4.4.1 start

```java
public void start()
```

* 説明：
    * `位置情報サービス`を利用するにあたり、現状のシステム設定内容の妥当性に関して一連の検査を実施する。
    * 処理に成功した場合は、コンストラクタで指定された位置情報プロバイダに対応する「本ライブラリ内部サービス（`GpsService`または`FlpService`）」が自動的に起動される。
    * Androidシステムから位置情報の自動更新が受けられるか否か
`LocationTrackerListener.onLocationSettingsChecked`にて通知される。
    * 処理途中で何らかのエラーを検出した場合は
`LocationTrackerListener.onError`で通知される。

* 参考：
    * [サービスを開始する](https://developer.android.com/guide/components/services#StartingAService)


### 4.4.2 stop

```java
public void stop()
```

* 説明：
    * `位置情報サービス`が実行中であれば、これを停止する。
    * 処理途中で何らかのエラーを検出した場合は
`LocationTrackerListener.onError`で通知される。

* 参考：
    * [サービスを停止する](https://developer.android.com/guide/components/services#Stopping)


### 4.4.3 bindLocationService

```java
public void bindLocationService()
```

* 説明：
    * `位置情報サービス`に結合し、センサー制御を開始する。
    * 本メソッドは非同期要求であるため、利用者は操作完了を待つ必要がある。
    * 処理に成功した場合は`LocationTrackerListener.onLocationEngaged`で通知される。
さもなくば`LocationTrackerListener.onError`で通知される。

* 参考：
    * [Bound services overview](https://developer.android.com/guide/components/bound-services)


### 4.4.4 unbindLocationService

```java
public void unbindLocationService()
```

* 説明：
    * `位置情報サービス`と切断し、センサー制御を終了する。
    * 本メソッドは非同期要求であるため、利用者は操作完了を待つ必要がある。
    * 処理に成功した場合は`LocationTrackerListener.onLocationEngaged`で通知される。
さもなくば`LocationTrackerListener.onError`で通知される。

* 参考：
    * [Bound services overview](https://developer.android.com/guide/components/bound-services)


# 付録
## A.1 ライフサイクル

```
            ( constructor )
                  |
    +-----------> |
    |             |
    |             V
    |           start()
    |             |
    |             |-----> onLocationSettingsChecked(true)
    |    +------> |
    |    |        V
    |    |   bindLocationService()
    |    |        |
    |    |        |-----> onLocationEngaged()
    |    |        |
    |    |        |-----> onLocationDataReceived()
    //   //       //           :
    |    |        |-----> onLocationDataReceived()
    |    |        V
    |    |   unbindLocationService()
    |    |        |
    |    |        |-----> onLocationDisengaged()
    |    +-------<>
    |             |
    |             V
    |           stop()
    |             |
    +------------<>
                  |
                  V
```

