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
--->

# 性能測定

## ビルド方法

クライアント・マシン上で以下のことを行う:

- まずSINETStream Javaビルドする。
- このディレクトリ(`sinetstream/java/sample/perf`)で `make` を実行する。

## 実行方法

- KafkaブローカとMQTTブローカを用意して立ち上げる。
- `~/.sinetstream_config.yml` をクライアント・マシン上に作成する。
    - サンプル: `sinetstream/java/sample/perf/dot.sinetstream_config.yml`
- `~/.ssh/config` を編集して、テストスクリプトを実行するマシンからクライアント・マシンにSSHログインできるようにする。
  ```
  Host *
    ServerAliveInterval 10
    Compression yes
  Host aws2
    Hostname 172.30.2.43
    User centos
  Host raspi
    Hostname localhost
    User pi
    Port 20022
  ```
- テストスクリプト `test.sh` を編集する:
    - SERVICE を環境にあわせて設定する
    - BROKER を環境にあわせて設定する
    - HOST を環境にあわせて設定する
- テストスクリプトを実行する: `script -c ./test.sh`
- クライアント・マシン上に作成されたテスト結果(`perftest-*.csv`)を集める。
    - このCSVファイルのフォーマットは以下のとおり:
        - 送信時刻または受信時刻 (ms)
        - メッセージサイズ (B)
- CSVを解析して統計値を得る:
    - `stat-perf.sh perftest-*.csv` を実行すると平均スループットなどが得られる。
- `test.sh` の代りに `test-tls.sh` を実行するとTLSありでの測定、`test-crypto.sh` を実行すると暗号化ありでの測定がおこなわれる。

## 測定内容

### 一次データ

````
      Producer    Broker      Consumer
      |           |           |
 write|---------->|           |
 write|---------->|---------->|read
 write|---------->|---------->|read
      |           |---------->|read
      |           |           |
      V           V           V
````

- Producer:
    - writer.write()の呼び出しが戻ってきた時刻を記録(単位はミリ秒)
- Consumer:
    - reader.read()の呼び出しが戻ってきた時刻を記録(単位はミリ秒)

この時刻がCSVファイルに出力される。出力CSVのフォーマット:

- 時刻 (ms)
- メッセージサイズ (B)

### stat-perf.shの出力

ProducerまたはConsumerが出力したCSVファイルを読み込んで
統計処理した値をC出力する(1CSVファイルを読んでCSVファイル1行を出力)。
出力CSVのフォーマット:

- 入力ファイル名
- メッセージ数
- 総バイト量
- 時間(1つめのメッセージから最後のメッセージまでの時間) (ms)
- メッセージ間隔の最小値 (ms)
- メッセージ間隔の平均値 (ms)
- メッセージ間隔の最大値 (ms)
- 平均メッセージサイズ(= 総バイト量÷メッセージ数)
- 平均スループット(= 総バイト量÷時間) (MiB/s)
- 平均I/Oスループット(= メッセージ数÷時間)
