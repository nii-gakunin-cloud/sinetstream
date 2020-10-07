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
        - 送信完了時刻または受信時刻 (ms)
        - メッセージサイズ (B)
        - メッセージに埋め込まれたメッセージシーケンス番号
        - メッセージに埋め込まれた送信時刻 (ms)
- CSVを解析して統計値を得る:
    - `stat-perf.sh perftest-*.csv` を実行すると平均スループットなどがCSVの形式で得られる。
    - このCSVファイルのフォーマットは以下のとおり:
        - file: 入力CSVファイル名
        - snd: (測定パラメータ)writeマシン名
        - rcv: (測定パラメータ)readerマシン名
        - qsys: (測定パラメータ)キューイング・システム名
        - qos: (測定パラメータ)QoSレベル
        - lib: (測定パラメータ)クライアント・ライブラリ
        - crypt: (測定パラメータ)暗号化
        - msz: (測定パラメータ)送受信メッセージ・サイズ
        - nmsg: (測定パラメータ)送受信メッセージ数
        - role: (測定パラメータ) 送信側(pub) or 受信側(sub)
        - n: (測定結果)総送受信メッセージ数
        - total: (測定結果)総送受信メッセージ量
        - time: (測定結果)総送受信時間
        - imin: (測定結果)最短メッセージ送受信間隔
        - iavg: (測定結果)平均メッセージ送受信間隔
        - imax: (測定結果)最大メッセージ送受信間隔
        - size: (測定結果)平均送受信メッセージ・サイズ
        - bw: (測定結果)平均送受信スループット
        - iops: (測定結果)平均送受信頻度
        - dmin: (測定結果)最小遅延時間
        - davg: (測定結果)平均遅延時間
        - dmax: (測定結果)最大遅延時間
        - idev: (測定結果)メッセージ送受信間隔の標準偏差
        - ddev: (測定結果)遅延時間の標準偏差
        - drop: (測定結果)欠損メッセージ数 = nmsg - n
        - drop: (測定結果)欠損メッセージ割合 = (nmsg - n)/nmsg*100
        - dup: (測定結果)重複メッセージ数
        - note: (測定結果)エラー情報(タイムアウトなど)
- データを表示する:
    - CSVファイル一般:
        - `view-csv.sh`: CSVを表にして表示する。
        - `slice-csv.sh`: CSVの列を抜き出す。
        - `pick-csv.sh`: CSVから条件の違いによる結果の比較をCSVにする(つまり一言では言いあらわせない)。
    - テスト結果(`perftest-*.csv`)用:
        - `test.sh': テストを実行する。結果ファイルが作成される。
        - `stat-perf.sh`: 統計ファイルを作成する。
        - `plot-seq.sh`: メッセージ・シーケンス番号の時間変化をみる。
        - `plot-delay.sh`: メッセージの遅延時間の時間変化を表示する。
        - `plot-delay-hist.sh`: メッセージ遅延時間のヒストグラムを表示する。
    - 統計ファイル(`stat-perf.sh` の出力)用:
        - `check-ooo.sh`:  `*-sub.csv`をみてシーケンス番号が連続しているか確認する。
        - `check-stat.sh`: 統計ファイルをみて測定がうまくいっているか確認する。
        - `plot-hist.sh`: 論文用のグラフを作成する。
        - `concat-hist.sh`: 論文用グラフ(PNG/PDFファイル)を1つにまとめる。(一覧性を高める目的)

- そのほかのスクリプト:
    - `perf-net.sh`: pingとiperfを実行する。
    - `stat-iperf.sh`: (perf-net.shを実行して出力された)iperfの結果jsonファイルを平均スループットを計算する。
    - `plot-iperf.sh`: `test.sh`を実行して作成されたiperfの結果ファイルからテスト期間全体のスループット変化をグラフにする。
    - `runtest.sh`: `test.sh`の実行と後処理。
    - `transpose.sh`: 転置行列にする。

## 測定ポイント

- 送信側
    - send: write()を呼んだ時刻
    - comp: 送信が完了した時刻
- 受信側
    - recv: read()が戻ってきた時刻
    - send: 送信側がwrite()を呼んだ時刻(これはメッセージ内に埋め込まれたタイムスタンプにより得る)

//http://wiki.c2.com/?MessageSequenceChart
````
        __writer__              ___reader__
       {          }            {           }
        app     ss    broker    ss      app
        |       |       |       |       |
    send|------>|       |       |       |
        |<------|------>|------>|------>|recv
    comp|<------|<------|       |       |
        |       |  ack  |       |       |
        |       |       |       |       |
````

//http://wiki.c2.com/?UseCaseMap
````
     writer     lib    broker   lib    reader
     .......  ....... ....... ....... .......
     :     :  :     : :     : :     : : read:
     :write:  :  send :  send :   ______@   :
     :  @_______|_______|_______@|  : :     :
     :     :  : |_  : : |_  : :  |______|   :
     :  |_________| : :   | : :     : : return
     :  return:     : :   | : :     : :     :
     :     :  :     : :   | : :     : :     :
     :  |_________________| : :     : :     :
     :complete:     : :     : :     : :     :
     ......:  :.....: :.....: :.....: :.....:
````
