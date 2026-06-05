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

[English](TUTORIAL-android-step2-webbrowser.en.md)

# チュートリアル - センサーデータ可視化用のウェブインタフェース

<em>目次</em>
<pre>
1. 概要
2. サーバへの接続
3. 主画面
3.1 画面構成
3.2 グラフ表示領域の削除
4. 設定画面
4.1 画面構成
4.2 センサー種別一覧
</pre>


## 1. 概要

チュートリアルSTEP2用のバックエンド側システムは、
センサーデータ可視化用のウェブインタフェースを用意しています。
手元のウェブブラウザから指定のURLに接続することで、
センサー種別ごとの読取値が時系列でグラフ表示される様子を観測できます。


## 2. サーバへの接続

バックエンド側のシステムが稼働中の状態で、
観測用PCのウェブブラウザから以下のURLに接続してください。
もちろんお手元のスマートフォンやタブレットからの接続でも構いません。

```
    http://<server_address>/
```

上記URLの`server_address`要素は、
IPアドレスまたはFQDN (Fully Qualified Domain Name) 形式で記述します。
「バックエンドシステム用のコンテナイメージが稼動する」ホスト機材のアドレスを指定してください。
コンテナ起動時の設定でウェブサーバのポート番号を標準の80番から変更した場合、
この`server_address`要素を`<address>:<port>`形式で指定してください。

URLが正しいはずなのにウェブサーバに接続できない場合、
一般的なTCP/IP通信レベルの接続失敗が考えられます。
経路制御やファイアウォールの設定などを見直してください。
`server_address`をFQDNで指定した場合、DNSが正常動作するかも見てください。


## 3. 主画面
### 3.1 画面構成

主画面はヘッダ部とボディ部に分かれています。
ヘッダ部にはタイトルと設定ボタン（歯車アイコン）が配置されています。
ボディ部はセンサー種別ごとの時系列グラフ表示領域（カード）が縦に並ぶ構成となっています。

![グラフ表示画面](images/step2/graph_main_window.png)

＜凡例＞
1. タイトル
2. 設定ボタン（歯車アイコン）
3. センサー種別ごとのグラフ表示カード
4. グラフ表示カードの閉じるボタン（×）

Android端末側からは周期的にセンサーデータが送られ、
バックエンド側に逐次蓄積されます。
センサーデータが追加され続けている限りグラフ表示領域は自動更新され続けます。

グラフ表示画面では、既定値として以下のセンサー種別の表示領域が用意されています。
* 照度計（light）
* 地磁気計（magnetic_field）
* 重力計（gravity）

Android端末側でのセンサー実装状況あるいは所望のセンサー種別に対応するため、
利用者が表示対象のセンサー種別を変更可能です。
設定ボタン（歯車アイコン）から設定画面を開いて変更してください。

画面構成はウェブブラウザのローカルストレージ領域に保存されるので、
いったんブラウザを終了してサーバに再接続すると画面構成が再現されます。


### 3.2 グラフ表示領域の削除

各グラフ表示カードの右上にある閉じるボタン（×）を押下すると、
当該センサー種別のグラフ表示領域を個別に削除できます。


## 4. 設定画面
### 4.1 画面構成

主画面ヘッダ領域の右端にある設定ボタン（歯車アイコン）を押下すると、
以下のような設定画面がポップアップ表示されます。

![設定画面](images/step2/graph_add_window.png)

＜凡例＞
1. 更新間隔（秒）：グラフの自動更新間隔を秒単位で指定する（既定値：3秒）
2. 最大表示範囲（分）：グラフに表示する時間幅を分単位で指定する（既定値：60分）
3. センサー種別一覧：チェックボックスにより表示対象のセンサー種別を選択する
4. `全選択`ボタン：すべてのセンサー種別を選択する
5. `全クリア`ボタン：すべてのセンサー種別の選択を解除する
6. `決定`ボタン：編集内容を確定してこの画面を終了する
7. `閉じる`ボタン：編集内容を破棄してこの画面を終了する

表示したいセンサー種別のチェックボックスをオンにし、
「決定」ボタンを押下して設定内容を確定してください。
「閉じる」ボタンの押下により、いつでも編集作業をキャンセルできます。

### 4.2 センサー種別一覧

本システムで対応しているセンサー種別の一覧を以下に示します。

* 表の1列目`Type`と2列目`Symbol`がAndroid開発者向け文書
[Sensor](https://developer.android.com/reference/android/hardware/Sensor)
中で定数として定義されている値です。

* 表の3列目`TypeName`に注目ください。
この値（`accelerometer`など）が設定画面のセンサー種別一覧に表示される名称です。

* なお、Android端末のハードウェアやOSバージョンにより、
利用可能なセンサー種別は異なり得ます。

|Type|Symbol|TypeName|
|---|---|---|
|1|Sensor.TYPE_ACCELEROMETER|accelerometer|
|2|Sensor.TYPE_MAGNETIC_FIELD|magnetic_field|
|4|Sensor.TYPE_GYROSCOPE|gyroscope|
|5|Sensor.TYPE_LIGHT|light|
|6|Sensor.TYPE_PRESSURE|pressure|
|8|Sensor.TYPE_PROXIMITY|proximity|
|9|Sensor.TYPE_GRAVITY|gravity|
|10|Sensor.TYPE_LINEAR_ACCELERATION|linear_acceleration|
|11|Sensor.TYPE_ROTATION_VECTOR|rotation_vector|
|14|Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED|magnetic_field_uncalibrated|
|15|Sensor.TYPE_GAME_ROTATION_VECTOR|game_rotation_vector|
|16|Sensor.TYPE_GYROSCOPE_UNCALIBRATED|gyroscope_uncalibrated|
|17|Sensor.TYPE_SIGNIFICANT_MOTION|significant_motion|
|18|Sensor.TYPE_STEP_DETECTOR|step_detector|
|19|Sensor.TYPE_STEP_COUNTER|step_counter|
|20|Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR|geomagnetic_rotation_vector|
|35|Sensor.TYPE_ACCELEROMETER_UNCALIBRATED|accelerometer_uncalibrated|

