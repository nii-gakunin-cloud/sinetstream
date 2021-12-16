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

[English](https://translate.google.com/translate?hl=en&sl=ja&tl=en&u=https://nii-gakunin-cloud.github.io/sinetstream/docs/userguide/libhelper_api/sensor_controller.html "google translate")

SINETStreamHelper ユーザガイド（センサー制御部）

**目次**
<pre>
1. モジュール構成
2. API概要
3. インタフェース SensorListener
3.1 メソッド概要
3.2 メソッド詳細
3.2.1 onSensorTypesReceived
3.2.2 onSensorEngaged
3.2.3 onSensorDisengaged
3.2.4 onSensorDataReceived
3.2.5 onError
4. クラスSensorController
4.1 コンストラクタ概要
4.2 メソッド概要
4.3 コンストラクタ詳細
4.4 メソッド詳細
4.4.1 bindSensorService
4.4.2 unbindSensorService
4.4.3 getAvailableSensorTypes
4.4.4 enableSensors
4.4.5 disableSensors
4.4.6 setIntervalTimer
4.4.7 setLocation
4.4.8 resetLocation
4.4.9 setUserData

付録
A.1 ライフサイクル
</pre>


# 1. モジュール構成

`SINETStreamHelper`ライブラリにおける「センサー読み取り値の収集」機能のモジュール構成を以下に示す。

```
        #---------------------------------------+
        | User Application                      |
        +---------------------------------------+
             |                      A
    =========|======================|=============== API functions
             |                      |
      +------|----------------------|-------------+
      |      V                      | [JSON]      |
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
    * `SINETStreamHelper`ライブラリのフロントエンドとして、
センサー制御用のAPI関数一式を提供する。
* SensorService
    * `SINETStreamHelper`ライブラリのバックエンドとして、
Androidの`SensorManager`経由でセンサーデバイスを制御する。
* SensorListener
    * `SINETStreamHelper`ライブラリからの非同期通知を受けるため、
コールバックインタフェースを定義する。


# 2. API概要

* パッケージ
    * jp.ad.sinet.stream.android.helper

* 公開インタフェース
    * SensorListener

* 公開クラス
    * SensorController


# 3. インタフェース SensorListener

## 3.1 メソッド概要
* onError
    * エラー検出時に呼ばれる。
* onSensorDataReceived
    * ライブラリ内部に一時蓄積したセンサーデータの出力時に呼ばれる。
* onSensorDisengaged
    * メソッド`SensorController.unbindSensorService()`により
`SensorService`と切断した場合に呼ばれる。
* onSensorEngaged
    * メソッド`SensorController.bindSensorService()`により
`SensorService`と結合した場合に呼ばれる。
* onSensorTypesReceived
    * メソッド`SensorController.getAvailableSensorTypes()`により
`SensorService`が「センサー種別、センサー種別名称」の組を返却した場合に呼ばれる。


## 3.2 メソッド詳細
### 3.2.1 onSensorTypesReceived

```java
void onSensorTypesReceived(@NonNull
                           java.util.ArrayList<java.lang.Integer> sensorTypes,
                           @NonNull
                           java.util.ArrayList<java.lang.String> sensorTypeNames)
```

* 説明：
    * メソッド`SensorController.getAvailableSensorTypes()`により
`SensorService`が「センサー種別、センサー種別名称」の組を返却した場合に呼ばれる。

    * 返却された配列リストの各要素は、以下のようにして得られる。

```java
    for (int i = 0; i < sensorTypes.size(i); i++) {
        int sensorType = sensorTypes.get(i);
        String sensorTypeName = sensorTypeNames.get(i);
        ...
    }
```

* 引数:
    * sensorTypes - 利用可能なセンサー種別の配列リスト
    > Android開発者文書
    > [Sensor](https://developer.android.com/reference/android/hardware/Sensor)
    > にて`TYPE_ACCELEROMETER`などと定義されている値である
    * sensorTypeNames - 利用可能なセンサー種別名称（例：accelerometer）
の配列リスト


### 3.2.2 onSensorEngaged

```java
void onSensorEngaged(@NonNull
                     java.lang.String info)
```

* 説明：
    * メソッド`SensorController.bindSensorService()`により
`SensorService`と結合した場合に呼ばれる。
    * この時点から、クライアントはデバイス上のセンサーを利用可能になる。

* 引数:
    * info - システムからの通知メッセージ（なければ空文字）


### 3.2.3 onSensorDisengaged

```java
void onSensorDisengaged(@NonNull
                        java.lang.String info)
```

* 説明：
    * メソッド`SensorController.unbindSensorService()`により
`SensorService`と切断した場合に呼ばれる。
    * クライアントの終了前にこの通知を待つこと。

* 引数:
    * info - システムからの通知メッセージ（なければ空文字）


### 3.2.4 onSensorDataReceived

```java
void onSensorDataReceived(@NonNull
                          java.lang.String jsonData)
```

* 説明：
    * ライブラリ内部に一時蓄積したセンサーデータの出力時に呼ばれる。
    * JSON形式に整形されており、出力例は以下のようになる。
    > ここでは見やすさのために`PrettyPrint`展開して表示するが、実際は
    > コンパクトに詰め込んだ形式で出力する。

```json
     {
         "device":{
             "sysinfo":{
                 "android":"8.0.0",
                 "manufacturer":"Google",
                 "model":"Android SDK built for x86"
             },
             "userinfo":{},
             "location":{}
         },
         "sensors":[
             {
                 "type":"light",
                 "name":"Goldfish Light sensor",
                 "timestamp":"20210224T184244.120+0900",
                 "value":9894.7001953125
             }
         ]
     }
```

* 引数:
    * jsonData - JSON形式データ


### 3.2.5 onError

```java
void onError(@NonNull
             java.lang.String errmsg)
```

* 説明：
    * 何らかのエラー条件を満たしたときに呼ばれる。
* 引数:
    * errmsg - エラー内容メッセージ


# 4. クラスSensorController

* 本クラスは、SINETStreamHelperライブラリのフロントエンドとしてセンサー
制御用のAPI関数一式を提供する。
* SensorControllerは内部の
[サービス](https://developer.android.com/guide/components/bound-services)
要素`SenseorService`と結合し、同サービスと協調動作する。
* `SensorService`とはAndroidのメッセージング機構で通信するため、以下の
メソッドは非同期要求として扱うこと。
    * bindSensorService
    * unbindSensorService
    * enableSensors
    * disableSensors
* 本クラスの利用者は、呼び出し側の
[Activity](https://developer.android.com/guide/components/activities/intro-activities)
において`SensorListener`を実装し、処理結果やエラーの非同期通知を受けられるようにしなければならない。


## 4.1 コンストラクタ概要
* SensorController
    * SinetControllerのインスタンスを生成する。


## 4.2 メソッド概要
* bindSensorService
    * `SensorService`と結合し、センサー処理を開始する。
* disableSensors
    * `SensorService`に対し、指定センサー種別群の無効化を要求する。
* enableSensors
    * `SensorService`に対し、指定センサー種別群の有効化を要求する。
* getAvailableSensorTypes
    * `SensorService`に対し、デバイス上で利用可能なセンサー情報を
要求する。
* setIntervalTimer
    * `SensorService`に対し、`SensorListener.onSensorDataReceived`で
センサー読取値が通知される際の`最小時間間隔`を指定する。
* setLocation
    * `SensorService`に対し、地理的な位置情報（緯度、軽度）を内部で保管するよう要求する。
* resetLocation
    * `SensorService`に対し、地理的な位置情報（緯度、軽度）を初期化するよう要求する。
* setUserData
    * `SensorService`に対し、利用者の情報を内部で保管するよう要求する。
* unbindSensorService
    * `SensorService`と切断し、センサー処理を終了する。


## 4.3 コンストラクタ詳細

```java
public SensorController(@NonNull
                        android.content.Context context,
                        int clientId)
```

* 説明：
    * SensorControllerのインスタンスを生成する。
* 引数:
    * context - `SensorListener`を実装したアプリケーション
[コンテクスト](https://developer.android.com/reference/android/content/Context)
、通常は呼出側のActivityそのもの。
    * clientId - `SensorService`に結合したクライアント同士を峻別する識別子

* 例外:
    * java.lang.RuntimeException - 付与のコンテクストが所用のリスナーを実装していない


## 4.4 メソッド詳細
### 4.4.1 bindSensorService

```java
public void bindSensorService()
```

* 説明：
    * `SensorService`に結合し、センサー制御を開始する。
    * 本メソッドは非同期要求であるため、利用者は操作完了を待つ必要がある。
    * 処理に成功した場合は`SensorListener.onSensorEngaged`で通知される。
さもなくば`SensorListener.onError`で通知される。

* 参考：
    * [Bound services overview](https://developer.android.com/guide/components/bound-services)

### 4.4.2 unbindSensorService

```java
public void unbindSensorService()
```

* 説明：
    * `SensorService`と切断し、センサー制御を終了する。
    * 本メソッドは非同期要求であるため、利用者は操作完了を待つ必要がある。
    * 処理に成功した場合は`SensorListener.onSensorEngaged`で通知される。
さもなくば`SensorListener.onError`で通知される。

* 参考：
    * [Bound services overview](https://developer.android.com/guide/components/bound-services)


### 4.4.3 getAvailableSensorTypes

```java
public void getAvailableSensorTypes()
```

* 説明：
    * デバイス上で利用可能なセンサー情報を`SensorService`に要求する。
    * 本メソッドは非同期要求であるため、利用者は操作完了を待つ必要がある。
    * 処理に成功した場合は`SensorListener.onSensorEngaged`で通知される。
さもなくば`SensorListener.onError`で通知される。

* 注意：
    * どのセンサー種別が利用可能かは動作環境に依存する。
    * デバイスによってはベンダー独自のセンサーを具備するものさえあり、
それらの読取値がどのような形式で得られるかわからない。
    * 曖昧さを避けるため、Android開発文書でセンサー種別と値が定義された
ものだけを扱う。

* 参考：
    * [Sensor Values](https://developer.android.com/reference/android/hardware/SensorEvent#values)


### 4.4.4 enableSensors

```java
public void enableSensors(@NonNull
                          java.util.ArrayList<java.lang.Integer> sensorTypes)
```

* 説明：
    * 指定したセンサー群を有効化するよう`SensorService`に要求する。
    * 本メソッドは非同期要求であるが、利用者は操作完了を待つ
<em>必要はない</em>。
    * 指定したセンサー群の有効化に成功すると、それらの読取値は
`SensorListener.onSensorDataReceived`で周期的（または事象検出時）に通知される。
さもなくば`SensorListener.onError`で通知される。

* 注意：
    * デバイスで扱えるセンサー情報は`SensorService`起動時に取得され、内部情報として管理される。
    クライアントが指定したセンサー種別が不明の場合は単に無視される。

* 引数：
    * sensorTypes - センサー種別の配列リスト


### 4.4.5 disableSensors

```java
public void disableSensors(@NonNull
                           java.util.ArrayList<java.lang.Integer> sensorTypes)
```

* 説明：
    * 指定したセンサー群を無効化するよう`SensorService`に要求する。
    * 本メソッドは非同期要求であるが、利用者は操作完了を待つ
<em>必要はない</em>。
    * 指定したセンサー群が無効化に成功すると、以降はそれらの読取値の
通知が停止する。さもなくば`SensorListener.onError`で通知される。

* 注意：
    * デバイスで扱えるセンサー情報は`SensorService`起動時に取得され、内部情報として管理される。
クライアントが指定したセンサー種別が不明の場合は単に無視される。

* 引数：
    * sensorTypes - センサー種別の配列リスト


### 4.4.6 setIntervalTimer

```java
public void setIntervalTimer(long seconds)
```

* 説明：
    * `SensorListener.onSensorDataReceived`による通知の最小時間間隔
を設定するよう`SensorService`に要求する。
    * 本メソッドの利用は任意である。既定値は10（秒）が使われる。

* 引数：
    * seconds - 通知の時間間隔（0 < seconds <= Long.MAX_VALUE）
    > Long.MAX_VALUE = 0x7fffffffffffffffL (2<sup>63</sup>-1)


### 4.4.7 setLocation

```java
public void setLocation(double latitude, double longitude)
```

* 説明：
    * デバイスの地理的な位置情報（緯度、経度）を指定の値で設定するよう
`SensorService`に要求する。
    * ここで指定された値の組は`SensorListener.onSensorDataReceived`で
通知されるJSONデータに組み込まれる。
を`SensorService`に指定する。
    * 本メソッドの利用は任意である。省略時は位置情報が空要素となる。

* 引数：
    * latitude - デバイスの緯度（-90.0 <= latitude <= 90.0）
    * longitude - デバイスの経度（-180.0 <= longitude <= 180.0）


### 4.4.8 resetLocation

```java
public void resetLocation()
```

* 説明：
    * デバイスの地理的な位置情報（緯度、経度）を初期化（非指定）するよう
`SensorService`に要求する。
    * 本メソッドの典型的な利用状況は、ユーザアプリケーション実行中に発生したシステム設定の変化（位置情報の設定ON/OFF）に動的に対応する場合であろう。


### 4.4.9 setUserData

```java
public void setUserData(@Nullable
                        java.lang.String publisher,
                        @Nullable
                        java.lang.String note)
```

* 説明：
    * ユーザ情報を指定の値で設定するよう`SensorService`に要求する。
    * ここで指定された値の組は`SensorListener.onSensorDataReceived`で
通知されるJSONデータに組み込まれる。
を`SensorService`に指定する。
    * 本メソッドの利用は任意である。省略時は空文字が使われる。

* 引数：
    * publisher - ユーザ情報
    * note - 備考


# 付録
## A.1 ライフサイクル

```
           ( constructor )
                  |
                  | <---- setIntervalTimer()
                  | <---- setLocation()
                  | <---- setUserData()
    +-----------> |
    |             V
    |        bindSensorService()
    |             |
    |             |-----> onSensorEngaged()
    |             V
    |        getAvailableSensorTypes()
    |             |
    |             |-----> onSensorTypesReceived()
    |             |
    |    +------> |
    |    |        V
    |    |   enableSensors()
    |    |        |
    |    |        |-----> onSensorDataReceived()
    //   //       //           :
    |    |        |-----> onSensorDataReceived()
    |    |        V
    |    |   disableSensors()
    |    |        |
    |    +--------<>
    |             |
    |             V
    |        unbindSensorService()
    |             |
    |             |-----> onSensorDisengaged()
    |             |
    +-------------<>
                  |
                  V
```

