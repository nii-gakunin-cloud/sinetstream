<!--
Copyright (C) 2023 National Institute of Informatics

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

[English](https://translate.google.com/translate?hl=en&sl=ja&tl=en&u=https://nii-gakunin-cloud.github.io/sinetstream/docs/userguide/compat.html "google translate")

# 互換性

## メッセージ互換性

SINETStreamがブローカに送信するメッセージフォーマットには世代があり、現在はバージョン3(V3)が使われている。

* V0: アプリケーションのデータを無加工で送受信
* V1: メッセージ暗号化
* V2: タイムスタンプを付加
* V3: メッセージフォーマット番号とメッセージ暗号鍵のバージョン番号を付加

メッセージのフォーマットバージョンと対応するSINETStreamのリリースの関係は以下のとおりである。

| フォーマットバージョン | 対応するSINETStreamリリース | 暗号化 | タイムスタンプ | 暗号鍵変更 |
|:--- |:--- |:--:|:--:|:--:|
| Version 0 | 〜0.9.5 | No | No | No |
| Version 1 | 0.9.7〜1.0 | Yes | No | No |
| Version 2 | 1.1〜1.8 | Yes | Yes | No |
| Version 3 | 1.9〜 | Yes | Yes | Yes |

基本的にReaderとWriterは同じメッセージフォーマットを使用しなければならず、
異なっていた場合はメッセージを受信したReaderが例外を発生するかデータ化けを起こす。

| Writer \ Reader | V0    | V1    | V2  | V3     |
| --------------- | ---   | ---   | --- | ---    |
| **V0**          | Yes   | No *1 | No  | No     |
| **V1**          | No *1 | Yes   | No  | No     |
| **V2**          | No    | No    | Yes | Yes *2 |
| **V3**          | No    | No    | No  | Yes    |

- *1 V1側が暗号化を無効にすれば通信可能
- *2 V3 ReaderがV2ダウングレードを許可していれば通信可能

<!---
SINETStream 1.7以降ではデータ無加工を意味する `user_data_only: true` パラメータを指定できるため、
V0としてふるまう。
--->

メッセージフォーマットの技術的詳細は
[メッセージフォーマット](../developer_guide/message_format.md)
を参照のこと。


<!---
## API互換性
--->
