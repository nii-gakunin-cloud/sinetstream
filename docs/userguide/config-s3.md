<!--
Copyright (C) 2022 National Institute of Informatics

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

[English](https://translate.google.com/translate?hl=en&sl=ja&tl=en&u=https://nii-gakunin-cloud.github.io/sinetstream/docs/userguide/config-s3.html "google translate")

SINETStream ユーザガイド

# S3固有のパラメータ

## `MessageWriter`, `MessageReader` に共通するパラメータ

* brokers
    * 指定できない
* consistency
    * AT_MOST_ONCE のみ指定できる
* user_data_only (bool)
    * trueを指定するとwriterが送ったデータのみが保存される。
    * falseを指定するとwriterが送ったデータに送信時刻タイムスタンプなどが付け加えられたSINETStream形式で保存される(デフォルト)
    * 注意: データ暗号化(data_encryption)を有効にする場合にはuser_data_onlyはfalseを指定しなければならない。
* s3
    * endpoint_url (必須)
        * S3ストレージのURL
    * bucket (必須)
        * S3バケット名
    * prefix (必須)
        * オブジェクト名の先頭につける文字列
    * suffix (必須)
        * オブジェクト名の最後につける文字列
    * name (必須)
        * オブジェクト名に日付・時刻を埋め込むときの書式
        * day
            * 日単位のフォルダを作成してオブジェクトをまとめる。
            * オブジェクト名: {prefix}/{topic}/{西暦}/{月}/{日}/{topic}-{client-uuid}-{seqno}{suffix}
            * client-uuid は Writer/Reader を作成したときに生成されるUUID version4(ランダム)
            * seqno は0から
        * hour
            * 時単位のフォルダを作成してオブジェクトをまとめる。
            * オブジェクト名: {prefix}/{topic}/{西暦}/{月}/{日}/{時}/{topic}-{client-uuid}-{seqno}{suffix}
        * minute
            * 分単位のフォルダを作成してオブジェクトをまとめる。
            * オブジェクト名: {prefix}/{topic}/{西暦}/{月}/{日}/{時}/{分}/{topic}-{client-uuid}-{seqno}{suffix}
    * aws_access_key_id (必須)
        * S3接続用のアクセスキーID
    * aws_secret_access_key (必須)
        * S3接続用のシークレットアクセスキー

## `MessageWriter` に関するパラメータ

* s3
    * utc_offset
        * 日付・時刻を生成するときに参照する協定世界時との差
        * ±HHMM の形式で指定する。
        * 省略時はローカルタイムゾーンになる。

## `MessageReader` に関するパラメータ

なし

## S3の設定例

AWS S3にテキストを保存する場合の設定例。
保存したデータの利用は外部アプリケーションを想定して
`user_data_only: yes`
を指定している(デフォルト設定の `user_data_only: no` だと読み出しにSINETStreamが必要になるため)。

```
service-s3:
  type: s3
  topic: test-topic
  value_type: text
  user_data_only: yes
  s3:
    endpoint_url: "https://s3.amazonaws.com"
    bucket: test-bucket
    prefix: test-prefix
    name: minute
    suffix: .txt
    aws_access_key_id: "<ACCESS_KEY>"
    aws_secret_access_key: "<SECRET_ACCESS_KEY>"
```

