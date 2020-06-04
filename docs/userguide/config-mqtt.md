**準備中** (2020-06-04 18:27:50 JST)

<!--
Copyright (C) 2019 National Institute of Informatics

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

[English](config-mqtt.en.md)

SINETStream ユーザガイド

# MQTT固有のパラメータ

* clean_session
    * 再起動、再接続で状態を記憶するかどうか
* protocol
    * MQTTバージョン
    * 指定できる値は `MQTTv31`, `MQTTv311`, `MQTTv5` のどれか
* transport
    * 指定できる値は `tcp`, `websocket` のどちらか
* qos
    * メッセージを送受信する際の QoS
    * 0, 1, 2 が指定でき、それぞれ `Consistency` の `AT_MOST_ONCE`, `AT_LEAST_ONCE`, `EXACTLY_ONCE` に対応する
    * `Consistency` の設定値より、`qos` の設定値が優先される
    * 指定された`qos` の値は `MessageReader`, `MessageWriter` から `getConsistency()` で取得できる
* retain
    * サーバーがこのメッセージを保持するかどうか
* max_inflight_messages_set
    * 以下のパラメータを指定できる
        * inflight
            * ネットワークフローを一度に通過できる QoS > 0のメッセージの最大数
* ws_set_options
    * WebSocket接続のオプションを指定する
    * 以下のパラメータを指定できる
        * path
            * WebSocket のパスを指定する
        * headers
            * 標準の WebSocketヘッダーに追加するヘッダーをマッピングで指定する
* tls_set
    * TLS接続に関するパラメータをマッピングで指定する
    * 以下のパラメータを指定できる
        * ca_certs
            * CA証明書ファイル (PEM) のパス
        * certfile
            * クライアント証明書ファイル (PEM) のパス
        * keyfile
            * クライアント証明書の秘密鍵ファイル (PEM) のパス
        * keyfilePassword (*)
            * クライアント証明書の秘密鍵ファイル (PEM) のパスワード
        * tls_version
            * TLSプロトコルのバージョン
        * ciphers
            * この接続で許可する暗号
        * trustStore (*)
            * トラストストアのパス
        * trustStoreType (*)
            * トラストストアのファイルフォーマット (jks, pkcs12, ...)
        * trustStorePassword (*)
            * トラストストアのパスワード
        * keyStore (*)
            * キーストアのパス
        * keyStoreType (*)
            * キーストアのファイルフォーマット (jks, pkcs12, ...)
        * keyStorePassword (*)
            * キーストアのパスワード
> (*) `trustStore`, `trustStoreType`, `trustStorePassword`, `keyStore`, `keyStoreType`, `keyStorePassword`, `keyfilePassword` は
> Java APIのみで指定できるパラメータである。Python API では指定できない。
* tls_insecure_set
    * 以下のパラメータを指定できる
        * value
            * TLS接続でホスト名の検証を無視するかどうか
* username_pw_set
    * 認証用のユーザ、パスワードをマッピングで指定する
    * 以下のパラメータを指定する
        * username
        * password
* will_set
    * Last Will and Testament (LWT) に関するパラメータをマッピングで指定する
    * クライアントが予期せず切断された場合、ブローカーがLWTに設定されているメッセージを代わりに発行する
    * 以下のパラメータを指定できる
        * topic
        * payload
        * qos
        * retain
* reconnect_delay_set
    * 再接続するまでの待機時間に関するパラメータをマッピングで指定する
    * 以下のパラメータを指定できる
        * max_delay
            * 最大待ち時間 (秒)
        * min_delay
            * 最小待ち時間 (秒)
* connect
    * 接続に関するパラメータをマッピングで指定する
    * 以下のパラメータを指定できる
        * keepalive
            * キープアライブの間隔 (秒) を指定する
        * automatic_reconnect (*)
            * 接続が切れた場合に、自動的に再接続を行うかどうか
        * connection_timeout (*)
            * 接続タイムアウト値 (秒) を指定する
> (*) `automatic_reconnect`, `connection_timeout` は
> Java APIのみで指定できるパラメータである。Python API では指定できない。

## MQTTの設定例

MQTTでブローカーとの接続にTCPではなくWebSocketを使う場合は以下のようにtransportパラメータを設定する。

```
service-mqtt:
  type: mqtt
  brokers: mqtt.example.org
  transport: websockets
```
