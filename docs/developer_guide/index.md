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

[English](https://translate.google.com/translate?hl=en&sl=ja&tl=en&u=https://nii-gakunin-cloud.github.io/sinetstream/docs/developer_guide/index.html "google translate")

# 開発者ガイド

## プラグイン開発ガイド

SINETStreamではプラグインを追加することにより、既存の機能を拡張することができます。
SINETStream v1.1以降では以下に示す２つのタイプのプラグインに対応しています。

* 新たなメッセージングシステムをサポートするためのプラグイン
* 新たなメッセージタイプをサポートするためのプラグイン

それぞれのプラグインを開発する手順を以下に示すリンク先に記します。
SINETStreamは開発言語として Python と Java に対応しているので、
開発手順はそれぞれの言語ごとに用意しています。

* 新たなメッセージングシステムをサポートするためのプラグイン
    * [Python](plugin_broker_python.md)
    * [Java](plugin_broker_java.md)
* 新たなメッセージタイプをサポートするためのプラグイン
    * [Python](plugin_value_type_python.md)
    * [Java](plugin_value_type_java.md)
* 新たな圧縮タイプをサポートするためのプラグイン
    * [Python](plugin_compression_python.md)
    * [Java](plugin_compression_java.md)

## 外部仕様

* [メッセージフォーマット](message_format.md)
