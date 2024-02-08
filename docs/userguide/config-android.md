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

[English](config-android.en.md)

# SINETStream ユーザガイド

## Android版のSINETStream設定ファイル

## 概要

Java版やPython版とは異なり、Android版のSINETStreamライブラリは足回りのメッセージングシステムとして(現状では)
[Paho MQTT Android](https://www.eclipse.org/paho/index.php?page=clients/android/index.php)
のみを利用する。
ここでの設定内容はPahoの[MqttConnectOptions](https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttConnectOptions.html)に反映される。
オプション扱いの設定項目が省略された場合、MqttConnectOptions組み込みの値が使われる。

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

## 設定サーバとの連携

[SINETStream 1.6版](https://www.sinetstream.net/docs/news/20211223-release_v16.html)より、SINETStreamの設定情報を一元管理する`設定サーバ`を導入した。

従来の「Android端末ごとにSINETStream設定ファイルを用意する」運用の他に、
「システム管理者が`設定サーバ`に登録した内容をREST-API経由でAndroid端末に取り込む」運用も可能とする。
前者の運用方法ではAndroid版のSINETStreamライブラリは単にYAML形式のローカルファイルを読み込むだけである。後者ではAndroid版のSINETStreamライブラリが`設定サーバ`からJSONデータをダウンロードしてメモリ上でのみ扱われる。

`設定サーバ`からダウンロードされるJSONデータ構造の概略を以下に示す。
このうち下図右端に示す部分が従来のSINETStream設定ファイル相当（JSON形式で表現）となる。
`設定サーバ`は、複数の情報ブロック（基本情報としてのSINETStream設定、SSL/TLS証明書のような添付情報、公開鍵のような秘匿情報）を一括して管理できる。
基本情報以外の付加情報がある場合、それぞれオプション要素（`attachments`、`secrets`）としてJSONデータに含まれるとともに、基本情報部分も拡張項目が設定される。
SINETStream設定における記法のバージョン識別のため、下図中央に示すヘッダ情報が含まれる。ただしJSONデータに添付情報や秘匿情報を含まず、かつ従来の`SINETStream 1.6版`に準じた記法に閉じる場合はヘッダ情報が省略される。

```
    JSON data                 config part        sinetstream_config
   +---------------+     - - +----------+        part
   | name          |    /    | (header) |
   +---------------+ - -     +----------+ - - - +----------------+
   | config        |         | config   |       | +------------+ |
   +---------------+ - - - - +----------+ - -   | | service #1 | |
   | (attachments) |                         \  | |   ...      | |
   +---------------+                          \ | +------------+ |
   | (secrets)     |                            +----------------+
   +---------------+
```

## 設定項目一覧
### 基本的なパラメータ

|大分類|中分類|小分類|型|値域|必須|備考|
|:-----|:-----|:-----|:-|:---|:---|:---|
|service|||String|任意|o|サービス識別子|
||type||String|{"mqtt"}|o|現状では`"mqtt"`のみ|
||brokers||String|hostport1[,hostport2[, ...]]|o|複数要素の場合はコンマで連結する|

* 項目`brokers`に複数要素を指定した場合、候補順に接続試行を繰り返す。
  * 接続に成功した場合、あるいは全て接続失敗した場合に制御が戻る。


### APIのパラメータ

|大分類|中分類|小分類|型|値域|必須|備考|
|:-----|:-----|:-----|:-|:---|:---|:---|
||topics||String|topic1[,topic2[, ...]]|o|複数要素の場合はコンマで連結する|
||client_id||String|Any||省略時はMQTTライブラリ内部で自動生成する|
||consistency||String|{"AT_MOST_ONCE","AT_LEAST_ONCE","EXACTLY_ONCE"}||省略時は"AT_LEAST_ONCE"|

* 項目`client_id`は`無効`とする。

  > ユーザ指定の`client_id`を用いると、PahoのMQTTライブラリ内部でエラーが発生する[不具合](https://github.com/eclipse/paho.mqtt.android/issues/238)がある。<br>
  > 本現象を回避するため、SINETStream設定で`client_id`が指定されても無視する運用とする。


### SSL/TLSに関するパラメータ
#### 形式1: SSL/TLS証明書の使用方法が限定的

|大分類|中分類|小分類|型|値域|必須|備考|
|:-----|:-----|:-----|:-|:---|:---|:---|
||tls||Boolean|{true,false}||省略時はfalse|

* 子要素なしで「tls: true」指定の場合、以下に示す限定的な使用形態と看做す。
  * クライアント証明書の提示をサーバ側から要求されない。
  * 公的な認証局（CA）から払い出されたサーバ証明書が使われる。

    > 上記以外の使用条件だとSSL/TLS証明書の設定が不十分のため接続に失敗する。


#### 形式2: Androidのキーチェインに格納されたSSL/TLS証明書を使う

|大分類|中分類|小分類|型|値域|必須|備考|
|:-----|:-----|:-----|:-|:---|:---|:---|
||tls|protocol|String|{TLSv1.2,TLSv1.3}||省略時は"TLSv1.2"|
||tls|client_certs|Boolean|{true,false}||省略時はfalse|
||tls|server_certs|Boolean|{true,false}||省略時はfalse|
||tls|check_hostname|Boolean|{true,false}||省略時はtrue|

* SSL/TLS関連の証明書は、事前にAndroidシステム秘匿領域「[キーチェイン](https://developer.android.com/reference/android/security/KeyChain)」に格納されたものを参照する運用とする。
  * クライアント証明書を使う場合、項目`client_certs`をtrueとする。
  * 自己署名サーバ証明書（いわゆるオレオレサーバ証明書）を使う場合、項目`server_certs`をtrueとする。

    > 対象のSSL/TLS証明書は以下のシステム設定階層を辿って参照できる。<br>
    >
    > [クライアント証明書]<br>
    > Settings -> Security & location -> Encryption & credentials -> User credentials
    >
    > [自己署名サーバ証明書]<br>
    > Settings -> Security & location -> Encryption & credentials -> Trusted credentials -> USER

* Androidの`キーチェイン`を利用する場合、項目`keyfilePassword`は無視される。
  * クライアント証明書の登録時は対となるパスワード入力をシステムから要求される。証明書の登録処理に成功すると`エイリアス`が払い出される。
  * クライアント証明書の参照時はこの`エイリアス`を指定する必要がある。

* 項目`protocol`での`TLSv1.3`はAndroid 10 (APIレベル29）以降の対応となる。

  > [Default configuration for different Android versions](https://developer.android.com/reference/javax/net/ssl/SSLSocket.html#default-configuration-for-different-android-versions)


#### 形式3: 設定サーバからダウンロードしたSSL/TLS証明書を使う

|大分類|中分類|小分類|型|値域|必須|備考|
|:-----|:-----|:-----|:-|:---|:---|:---|
||tls|protocol|String|{TLSv1.2,TLSv1.3}||省略時は"TLSv1.2"|
||tls|keyfilePassword|String|Any||クライアント証明書（xxx.pfx）のパスワード|
||tls|check_hostname|Boolean|{true,false}||省略時はtrue|

* SSL/TLS関連の証明書は、設定サーバから取得したものを参照する。
  * クライアント証明書は添付情報（`attachments`配列のうち、`target`の値が`*.tls.certfile_data`に対応する要素の値）として入手できる。

    > 本要素の値はBase64エンコード文字列が埋め込まれた形となっているが、その実体はPFX形式のバイナリファイル（バイト列）が入っているはずである。

  * 自己署名サーバ証明書（いわゆるオレオレサーバ証明書）は添付情報（`attachments`配列のうち、`target`の値が`*.tls.ca_certs_data`に対応する要素の値）として入手できる。

    > 本要素の値はBase64エンコード文字列が埋め込まれた形となっているが、その実体はPEM形式のASCIIファイルが入っているはずである。

* クライアント証明書が添付情報に含まれる場合、項目`keyfilePassword`の指定が必須となる。
  * 逆に`keyfilePassword`が設定されてクライアント証明書が添付情報に含まれない場合は、このパスワードは無視される。

* 設定サーバを利用する場合、項目`client_certs`および`server_certs`は無視される。

* 項目`protocol`での`TLSv1.3`はAndroid 10 (APIレベル29）以降の対応となる。

  > [Default configuration for different Android versions](https://developer.android.com/reference/javax/net/ssl/SSLSocket.html#default-configuration-for-different-android-versions)


### MQTT固有のパラメータ
#### 基本

|大分類|中分類|小分類|型|値域|必須|備考|
|:-----|:-----|:-----|:-|:---|:---|:---|
||clean_session||Boolean|{true,false}||省略時はtrue|
||protocol \[1\]||String|{"MQTTv31","MQTTv311","DEFAULT"}||省略時は"DEFAULT"|
||transport||String|{"tcp","websocket"}||省略時は"tcp"|
||qos||Integer|{0,1,2}||省略時は1|
||retain||Boolean|{true,false}||省略時はtrue|
||max_inflight_messages_set|inflight|Integer|正整数||省略時は10|
||reconnect_delay_set|max_delay|Integer|正整数||省略時は128000|
||connect|keepalive|Integer|正整数||省略時は60|
||connect|automatic_reconnect|Boolean|{true,false}||省略時はfalse|
||connect|connection_timeout|Integer|正整数||省略時は30|
||mqtt_debug \[2\]||Boolean|{true,false}||Default: false|

* 項目`protocol`での`"DEFAULT"`指定時は、
[まずMQTTv311を試し、次にMQTTv31を試す](https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttConnectOptions.html#MQTT_VERSION_DEFAULT)という振る舞いとなる。
* 項目`qos`に関しては、上記共通部の「APIのパラメータ」の項目`consistency`より
こちらが優先される。

\[1\]: 項目`protocol`の別名として`mqtt_version`を使っても良い。
両者を同時に指定した場合、`protocol`を採用する。

\[2\]: 開発者向け。MqttAndroidClientのデバッグトレースの有無を設定する。


#### MQTTのユーザ認証に関するパラメータ

|大分類|中分類|小分類|型|値域|必須|備考|
|:-----|:-----|:-----|:-|:---|:---|:---|
||username_pw_set|username|String|Any|||
||username_pw_set|password|String|Any|||

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

* 項目`payload`はバイト列として`Paho MQTT Android`ライブラリに渡される。
  * SINETStream設定ファイル上はBase64エンコード文字列としてペイロードを指定すること。


#### MQTTのSSL/TLSパラメータ

<!-- OBSOLETED
|大分類|中分類|小分類|型|値域|必須|備考|
|:-----|:-----|:-----|:-|:---|:---|:---|
||tls_set|ca_certs|String|Any||自己署名サーバ証明書（xxx.crt）のファイル名|
||tls_set|certfile|String|Any||クライアント証明書（xxx.pfx）のファイル名|
||tls_set|keyfilePassword|String|Any||クライアント証明書（xxx.pfx）のパスワード|
||tls_insecure_set|value|Boolean|{true,false}||省略時はtrue|
-->

* 本カテゴリーは`無効`とする。
  * 上記共通部の「SSL/TLSに関するパラメータ」を使うこと。

