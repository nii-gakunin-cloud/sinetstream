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

[English](https://translate.google.com/translate?hl=en&sl=ja&tl=en&u=https://nii-gakunin-cloud.github.io/sinetstream/docs/userguide/config-android.html "google translate")

# SINETStream ユーザガイド

## Android版のSINETStream設定ファイル

## 概要

Java版やPython版とは異なり、Android版のSINETStreamライブラリは
足回りのメッセージングシステムとして(現状では)
[Paho MQTT Android](https://www.eclipse.org/paho/index.php?page=clients/android/index.php)
のみを利用する。
ここでの設定内容はPahoの[MqttConnectOptions](https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttConnectOptions.html)に反映される。オプション扱いの設定項目が省略された場合、MqttConnectOptions組み込みの値が使われる。

## サービスによる記述内容のブロック化

* SINETStreamの設定ファイルの記述内容はサービス単位のブロックとして扱う。
* ファイル中に複数のサービス内容を記述できるため、それらを識別するための
検索鍵として`service`が使われる。

```
   sinetstream_config.yml
   +------------------------+
   |  +------------------+  |
   |  | service #1       |  |
   |  |   ...            |  |
   |  +------------------+  |
   |      ...               |
   |  +------------------+  |
   |  | service #n       | <---- target service
   |  |   ...            |  |
   |  +------------------+  |
   +------------------------+
```

## 設定項目一覧
### 基本的なパラメータ

|大分類|中分類|小分類|型|値域|必須|備考|
|:-----|:-----|:-----|:-|:---|:---|:---|
|service|||String|任意|o|サービス識別子|
||type||String|{"mqtt"}|o|現状では`"mqtt"`のみ|
||brokers||String|hostport1[,hostport2[, ...]]|o|複数要素の場合はコンマで連結する|


### APIのパラメータ

|大分類|中分類|小分類|型|値域|必須|備考|
|:-----|:-----|:-----|:-|:---|:---|:---|
||topics||String|topic1[,topic2[, ...]]|o|複数要素の場合はコンマで連結する|
||client_id||String|Any|x|省略時は本ライブラリ内部で自動生成する|
||consistency||String|{"AT_MOST_ONCE","AT_LEAST_ONCE","EXACTLY_ONCE"}|x|省略時は"AT_LEAST_ONCE"|


### SSL/TLSに関するパラメータ
#### 形式1

|大分類|中分類|小分類|型|値域|必須|備考|
|:-----|:-----|:-----|:-|:---|:---|:---|
||tls||Boolean|{true,false}|x|省略時はfalse|

#### 形式2

|大分類|中分類|小分類|型|値域|必須|備考|
|:-----|:-----|:-----|:-|:---|:---|:---|
||tls|ca_certs|String|Any|x|自己署名サーバ証明書（xxx.crt）のファイル名|
||tls|certfile|String|Any|x|クライアント証明書（xxx.pfx）のファイル名|
||tls|keyfilePassword|String|Any|x|クライアント証明書（xxx.pfx）のパスワード|
||tls|check_hostname|Boolean|{true,false}|x|省略時はtrue|


### MQTT固有のパラメータ
#### 基本

|大分類|中分類|小分類|型|値域|必須|備考|
|:-----|:-----|:-----|:-|:---|:---|:---|
||clean_session||Boolean|{true,false}|x|省略時はtrue|
||protocol||String|{"MQTTv31","MQTTv311","DEFAULT"}|x|省略時は"DEFAULT"|
||transport||String|{"tcp","websockets"}|x|省略時は"tcp"|
||qos||Integer|{0,1,2}|x|省略時は1|
||retain||Boolean|{true,false}|x|省略時はtrue|
||max_inflight_messages_set|inflight|Integer|正整数|x|省略時は0|
||reconnect_delay_set|max_delay|Integer|正整数|x|省略時は0|
||connect|keepalive|Integer|正整数|x|省略時は0|
||connect|automatic_reconnect|Boolean|{true,false}|x|省略時はtrue|
||connect|connection_timeout|Integer|正整数|x|省略時は0|

* 項目`protocol`での`"DEFAULT"`指定時は、
[まずMQTTv311を試し、次にMQTTv31を試す](https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttConnectOptions.html#MQTT_VERSION_DEFAULT)という振る舞いとなる。
* 項目`qos`に関しては、上記共通部の「APIのパラメータ」の項目`consistency`より
こちらが優先される。


#### MQTTのユーザ認証に関するパラメータ

|大分類|中分類|小分類|型|値域|必須|備考|
|:-----|:-----|:-----|:-|:---|:---|:---|
||username_pw_set|username|String|Any|x||
||username_pw_set|password|String|Any|x||

* ユーザ認証パラメータは項目`username`と`password`の両方を同時に設定、
または両方同時に省略のいずれかとすること。


#### MQTTのLWT (Last Will and Testament) に関するパラメータ

|大分類|中分類|小分類|型|値域|必須|備考|
|:-----|:-----|:-----|:-|:---|:---|:---|
||will_set|topic|String|Any|o||
||will_set|payload|String|Any|o||
||will_set|qos|Integer|正整数|o||
||will_set|retain|Boolean|{true,false}|o||

* 親項目`will_set`なしだと子項目（たとえば`topic`）が基本パラメータの
ものと重複してしまう。
* 曖昧さを防ぐため、上記４つの項目のいずれも必須とする。


#### MQTTのSSL/TLSパラメータ

|大分類|中分類|小分類|型|値域|必須|備考|
|:-----|:-----|:-----|:-|:---|:---|:---|
||tls_set|ca_certs|String|Any|x|自己署名サーバ証明書（xxx.crt）のファイル名|
||tls_set|certfile|String|Any|x|クライアント証明書（xxx.pfx）のファイル名|
||tls_set|keyfilePassword|String|Any|x|クライアント証明書（xxx.pfx）のパスワード|
||tls_insecure_set|value|Boolean|{true,false}|x|省略時はtrue|

* 上記共通部の「SSL/TLSに関するパラメータ」よりこちらが優先される。

