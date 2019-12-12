**準備中** (2019-12-12 16:54:49 JST)

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

SINETStream ユーザガイド

# Kafka固有のパラメータ

## `MessageWriter` に関するパラメータ

* acks
    * 要求が完了したと見なすのに必要な ack の数
* compression_type
    * データの圧縮タイプ
    * 有効な値は `none`, `gzip`, `snappy`, `lz4`, `zstd`
* retries
    * ゼロより大きい値を設定すると、一時的なエラーで送信が失敗したレコードを再送信する
* batch_size
    * 送信処理のバッチサイズ
* linger_ms
    * 送信処理をまとめておこなうための遅延時間 (ミリ秒)
* buffer_memory
    * バッファリングするため使用するメモリの合計バイト数
* max_block_ms
    * `send()`, `partitionsFor()` がブロックする時間 (ミリ秒)
* max_request_size
    * リクエストの最大サイズ
* max_in_flight_requests_per_connection
    * ackの応答なしでリクエストする最大数
* delivery_timeout_ms
    * `send()` が呼び出されてから結果を報告するまでの時間の上限 (ミリ秒)
* enable_idempotence
    * プロデューサーからの書き込みをべき等となるようにするかどうか
* transaction_timeout_ms
    * トランザクションコーディネータがプロデューサからの更新を待機してからの最大待ち時間 (ミリ秒)
* transactional_id
    * トランザクションID
* key_serializer
    * メッセージキーのシリアライザ

<!--
* partitioner_class
-->

## `MessageReader` に関するパラメータ

* group_id
    * コンシューマグループの名前
* fetch_min_bytes
    * フェッチリクエストに対してサーバーが返す必要があるデータの最小量
* fetch_max_wait_ms
    *  サーバが応答を返す際に `fetch_min_bytes` の条件を満たすのを待つ最大時間 (ミリ秒)
* fetch_max_bytes
    * フェッチリクエストに対してサーバーが返すデータの最大量
* max_partition_fetch_bytes
    * サーバーが返すパーティションごとのデータの最大量
* enable_auto_commit
    *  オフセットをバックグラウンドで定期的にコミットするかどうか
* auto_commit_interval_ms
    * 自動オフセットコミットの時間間隔 (ミリ秒)
* check_crcs
    *  消費したレコードのCRC32を自動的にチェックするかどうか
* partition_assignment_strategy
    * コンシューマグループのパーティション割り当て戦略のクラス名のリスト
* max_poll_records
    * `poll()` で返される最大のレコード数
* max_poll_interval_ms
    * `poll()` の最大遅延時間 (ミリ秒)
* session_timeout_ms
    * コンシューマーの障害を検出するために使用されるタイムアウト (ミリ秒)
* heartbeat_interval_ms
    * コンシューマコーディネーター間のハートビートの期待値 (ミリ秒)
* allow_auto_create_topics
    * トピックの自動作成を行うかどうか
* auto_offset_reset
    * Kafkaに初期オフセットがない場合の振舞いを指定する
    * `latest`, `earliest`, `none` のいずれかの値を指定する
* default_api_timeout_ms
    * コンシューマAPIのデフォルトのタイムアウト値 (ミリ秒)
* group_instance_id
    * コンシューマインスタンスのID
* isolation_level
    * アイソレーションレベルを指定する
    * `read_committed`, `read_uncommitted` のいずれかの値を指定する
* key_deserializer
    * メッセージキーのデシリアライザ

## `MessageWriter`, `MessageReader` に共通するパラメータ

* request_timeout_ms
    * クライアントからのリクエストのタイムアウト値 (ミリ秒)
* retry_backoff_ms
    * エラー時に再試行するときのバックオフまでの時間 (ミリ秒)
* reconnect_backoff_ms
    * 特定のホストへの再接続を試行する前に待機する時間 (ミリ秒)
* reconnect_backoff_max_ms
    * 接続に繰り返し失敗したブローカーに再接続するときに待機する最大時間 (ミリ秒)
* receive_buffer_bytes
    * TCP受信バッファーサイズ
* send_buffer_bytes
    * TCP送信バッファーサイズ
* metadata_max_age_ms
    * メタデータの更新を強制する時間 (ミリ秒)
* security_protocol
    * ブローカーとの通信に使用されるプロトコル
* connections_max_idle_ms
    * 指定された時間後にアイドル接続を閉じる (ミリ秒)
* exclude_internal_topics
    * 内部トピックをコンシューマーに公開するかどうか
* client_dns_lookup
    * クライアントがDNSルックアップする方法を指定する
* ssl_check_hostname
    * 証明書がホスト名と一致することを検証するかどうか
* ssl_cafile
    * CA証明書ファイルのパス
* ssl_certfile
    * クライアント証明書ファイルのパス
* ssl_keyfile
    *  クライアントの秘密鍵ファイルのパス
* ssl_password
    *  証明書ファイルをロードするときに使用するパスワード
* ssl_ciphers
    * SSL接続に使用可能な暗号を指定する
* ssl_truststore_location
    * トラストストアファイルのパス
* ssl_truststore_password
    * トラストストアファイルのパスワード
* ssl_truststore_type
    * トラストストアファイルのファイルフォーマット
* ssl_keystore_location
    * キーストアファイルのパス
* ssl_keystore_password
    * キーストアファイルのパスワード
* ssl_keystore_type
    * キーストアファイルのファイルフォーマット

<!--
* interceptor_classes
* ssl_key_password
* ssl_enabled_protocols
* ssl_protocol
* ssl_provider
* ssl_cipher_suites
-->

## Kafka の設定例

Kafkaの機能をつかってレコード・バッチをgzipで圧縮する場合は以下のようにcompression_typeパラメータを設定する。
(compression_typeを指定するのはWriter側だけでよい)

```
service-kafka:
  type: kafka
  brokers:
    - kafka0.example.org:9092
  compression_type: gzip
```
