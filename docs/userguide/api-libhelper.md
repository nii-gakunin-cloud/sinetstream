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

[English](api-android.en.md)

SINETStream ユーザガイド：SINETStreamHelperライブラリ

**目次**
<pre>
1. 概要
2. SINETStreamHelperライブラリの位置づけ
3. システム設定および実行時権限の扱い
4. SINETStreamHelperライブラリのAPI詳細
4.1 センサー読み取り値の収集機能
4.2 Android端末位置の追跡取得機能
</pre>


# 1. 概要

SINETStreamHelperライブラリ（以降、本ライブラリと記述）は、Android端末の具備する各種センサーデバイスの読み取り値をJSON形式に整形して出力するものである。

このJSONデータ形式は制御ヘッダ部分およびセンサー値のボディ部分から構成され、ヘッダにはAndroid端末の地理的な位置情報（緯度経度）を格納する箇所がある。
この位置情報の利用は任意であり、設定する場合は以下の2通りの使い方が可能である。

* 本ライブラリの初期化時に利用者が手動で固定値を設定する。
* 本ライブラリが用意する「位置情報の自動更新機能」で測位値の通知を受け、利用者がJSONヘッダ情報に反映する。

以降では、本ライブラリが提供する公開API関数群およびインタフェース群に関して述べる。


# 2. SINETStreamHelperライブラリの位置づけ

ユーザアプリケーションが本ライブラリを用いてセンサー情報の収集や端末位置情報の更新を受ける場合、概略以下のような4階層構成となる。
図中、二重線（=）で区切られた区間のうち、上から2層目が本ライブラリが位置する箇所である。ここで左側に示したセンサー情報を扱う部分（`Sensor Handler`）と、右側に示した端末位置情報を扱う部分（`Location Handler`）とに大別されており、両者は独立の機能として個別に制御される。
階層を跨がる上下の矢印は、下向きが操作用コマンド関数の作用方向、上向きが非同期通知を受けるためのコールバック関数の作用方向を示す。

```
        #--------------------------------------+
        |           User Application           |
        +--------------------------------------+
             |   A                  |   A
    =========|===|==================|===|================= API functions
             |   |                  |   |
      +------|-- |------------------|---|---------------------+
      |      V   | [JSON]           V   | [Location]          |
      | +----------------+    +------------------+            |
      | | Sensor Handler |    | Location Handler |            |
      | +----------------+    +------------------+            |
      |      |   A                  |   A                     |
      |      |   |                  |   |   SINETStreamHelper |
      +------|---|------------------|---|---------------------+
             |   |                  |   |
    =========|===|==================|===|================ Android System
            [ ... ]                [ ... ]
    =========|===|==================|===|======================= Devices
             |   |                  |   |
             V   | [Raw Data]       V   |  [Raw Data]
         +----------+           +----------+
         | Sensor   |+          | Location |+
         | Device   ||+         | Source   ||+
         +----------+||         +----------+||
          +----------+|          +----------+|
           +----------+           +----------+
```
〈凡例〉
* Sensor Handler
    * Android端末の具備するセンサーデバイスの読み取り値を収集する機能モジュール
* Location Handler
    * GPSなどの情報源経由でAndroid端末の位置情報を追跡取得する機能モジュール


# 3. システム設定および実行時権限の扱い

Android端末の具備するセンサーデバイス、あるいは位置情報の参照など、
実行時に動的にシステム設定状況を確認したり、必要に応じてダイアログを表示して利用者に妥当な設定を促す必要がある\[1\]。
利用者が本ライブラリを利用しやすくするため、システム設定や実行時権限の扱いに関する一連の処理は本ライブラリ内部で対処する。

<em>参考：</em>
\[1\]: [アプリの権限をリクエストする](https://developer.android.com/training/permissions/requesting?hl=ja)


# 4. SINETStreamHelperライブラリのAPI詳細

## 4.1 センサー読み取り値の収集機能

センサ制御クラスおよびインタフェース：
[SensorController, SensorListener](libhelper_api/sensor_controller.md)

## 4.2 Android端末位置の追跡取得機能

端末位置追跡クラスおよびインタフェース：
[LocationTracker, LocationTrackerListener](libhelper_api/location_tracker.md)

