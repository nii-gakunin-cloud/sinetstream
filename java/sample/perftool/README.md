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

# perftool

- 概要
- 前提条件
- 書式
- オプション
- 出力
- 実行例

## 概要

本ツールは、[SINETStream Writer/Reader API](https://www.sinetstream.net/docs/userguide/api-java.html)によるデータの送信/受信を実行し、その際に取得されたメトリクス情報を出力する。
メトリクス情報は、[SINETStream API Metricsクラス](https://www.sinetstream.net/docs/userguide/api-java.html)のプロパティのうちraw(メッセージングシステム固有の統計情報)を除くものを標準出力に出力する。
メトリクス情報の計測期間は、Writerがオプション等で指定されたサンプリング数のデータ送信を開始/終了する期間とし、Readerも同期間とする。
そのため、データ通信に問題がない場合もReaderの受信数はWriterの送信数より少なくなることがある。
送信データのペイロード型はバイト列を使用する。


## 前提条件

- Java11
- SINETStream v1.7.0(Java)
- [SINETStream API 設定ファイル](https://github.com/nii-gakunin-cloud/sinetstream/blob/main/docs/userguide/config.md) (データ送受信先の接続情報等が記述されているもの)

#### 設定ファイル記述例
```
service-perftool-kafka:
    type: kafka
    brokers: "broker:9092"

service-perftool-mqtt:
    type: mqtt
    brokers: "broker:1883"

```
topicは指定しても無視される。本ツール内で送受信に使用するtopicは`sinetstream-perftool-*`になる。(`*`は各reader/writerペアの*THREAD_NUMBER*)

## ビルド

次のコマンドを実行する。

```
$ ./gradlew build
```

` build/distributions/perftool-*` にビルドしたコマンドなどを
zip, tar でアーカイブしたファイルが作成されている。


## インストール

ビルドされたアーカイブファイルをインストール先のディレクトリに展開する。
展開したディレクトリの `bin/perftool` がサンプルプログラムを実行するスクリプトになっている。


<div style="page-break-after:always"></div>

## 書式

perftool -s|--service *SERVICE_NAME* [-f|--format *FORMAT*] [-c|--output-count *OUTPUT_COUNT*] [-n|--num-samples *NUM_SAMPLES*] [-p|--payload-size *PAYLOAD_SIZE*] [-a|--async-api] [-t|--num-threads *NUM_THREADS*] 

## オプション

- `-s`, `--service` *SERVICE_NAME*
    * SINETStream API で使用するサービス名
- `--config` *CONFIG*
    * SINETStream API で使用するコンフィグ名。設定されていない場合はコンフィグサーバを使用しない
- `-f`, `--format` *FORMAT*
    * 出力形式。jsonかtsvが指定可能。デフォルト:json
- `-c`, `--output-count` *OUTPUT_COUNT*
    * *OUTPUT_COUNT*回メトリクス情報を計測し出力する。デフォルト:1
- `-n`, `--num-samples` *NUM_SAMPLES*
    * 一回のメトリクス情報を出力するまでにwriterが*NUM_SAMPLES*個のデータ送信を試行する。デフォルト:300
- `-p`, `--payload-size` *PAYLOAD_SIZE*
    * データ送信試行時に*PAYLOAD_SIZE*バイトのペイロード長のデータを使用する。デフォルト:1024
- `-a`, `--async-api`
    * 指定した場合はデータの送信/受信に非同期APIを使用する。指定しなかった場合は同期APIを使用する
- `-t`, `--num-threads` *NUM_THREADS*
    * *NUM_THREADS*個のreader/writerのペアを並列実行する。各ペアは*OUTPUT_COUNT*回メトリクス情報を計測する。デフォルト:1


<div style="page-break-after:always"></div>

## 出力
#### JSON形式
`-c`, `--output-count`オプションで指定した回数と`-t`, `--num-threads`オプションで指定したスレッド数分をこの形式で出力する(output-count * num-threads が全出力数となる)。各スレッドの出力は`thread_number`で判別する。
ここでは人間が見やすいよう整形してコメントも追記しているが、実際は改行無しで詰めた形式で出力される。
実際の出力は実行例を参照。

```
{
    # writerのメトリクス情報
     "writer": {
         "thread_number":0,
         "start_time": 1616591251.9623938,
         "start_time_ms": 1616591251962.3938,
         "end_time": 1616591255.0322828,
         "end_time_ms": 1616591255032.2827,
         "time": 3.0698890686035156,
         "time_ms": 3069.8890686035156,
         "msg_count_total": 1000,
         "msg_count_rate": 325.7446694824375,
         "msg_bytes_total": 1044000,
         "msg_bytes_rate": 340077.43493966473,
         "msg_size_min": 1044,
         "msg_size_max": 1044,
         "msg_size_avg": 1044.0,
         "error_count_total": 0,
         "error_count_rate": 0.0
     },
    # readerのメトリクス情報
     "reader": {
         "thread_number":0,
         "start_time": 1616591251.962402,
         "start_time_ms": 1616591251962.402,
         "end_time": 1616591255.0317662,
         "end_time_ms": 1616591255031.766,
         "time": 3.069364070892334,
         "time_ms": 3069.364070892334,
         "msg_count_total": 1000,
         "msg_count_rate": 325.8003863025859,
         "msg_bytes_total": 1044000,
         "msg_bytes_rate": 340135.60329989967,
         "msg_size_min": 1044,
         "msg_size_max": 1044,
         "msg_size_avg": 1044.0,
         "error_count_total": 0,
         "error_count_rate": 0.0}
     }
}

```

<div style="page-break-after:always"></div>

#### TSV形式
writerとreaderのメトリクス情報を一行で出力する。writerのメトリクス情報ののヘッダはプレフィクスとして"`writer_`"を付与している。readerのプレフィクスは"`reader_`"としている。
出力は、1行目がヘッダ、以降は `-c`, `--output-count`オプションで指定した回数と`-t`, `--num-threads`オプションで指定したスレッド数分のデータ行となる(output-count * num-threads が全データ行数となる)。各スレッドの出力は`thread_number`で判別する。
ここではヘッダの一覧を示す。
実際の出力は実行例を参照。

```
    thread_number
    writer_start_time
    writer_start_time_ms
    writer_end_time
    writer_end_time_ms
    writer_time
    writer_time_ms
    writer_msg_count_total
    writer_msg_count_rate
    writer_msg_bytes_total
    writer_msg_bytes_rate
    writer_msg_size_min
    writer_msg_size_max
    writer_msg_size_avg
    writer_error_count_total
    writer_error_count_rate
    reader_start_time
    reader_start_time_ms
    reader_end_time
    reader_end_time_ms
    reader_time
    reader_time_ms
    reader_msg_count_total
    reader_msg_count_rate
    reader_msg_bytes_total
    reader_msg_bytes_rate
    reader_msg_size_min
    reader_msg_size_max
    reader_msg_size_avg
    reader_error_count_total
    reader_error_count_rate

```

<div style="page-break-after:always"></div>

## 実行例

```
$ ./perftool-1.6.0/bin/perftool -s service-perftool-mqtt
{"writer":{"thread_number":0,"start_time":1.648491955007E9,"start_time_ms":1648491955007,"end_time":1.648491955657E9,"end_time_ms":1648491955657,"time":0.65,"time_ms":650,"msg_count_total":300,"msg_count_rate":461.53846153846155,"msg_bytes_total":313200,"msg_bytes_rate":481846.1538461538,"msg_size_min":1044,"msg_size_max":1044,"msg_size_avg":1044.0,"error_count_total":0,"error_count_rate":0.0},"reader":{"thread_number":0,"start_time":1.648491955007E9,"start_time_ms":1648491955007,"end_time":1.648491955659E9,"end_time_ms":1648491955659,"time":0.652,"time_ms":652,"msg_count_total":300,"msg_count_rate":460.12269938650303,"msg_bytes_total":313200,"msg_bytes_rate":480368.0981595092,"msg_size_min":1044,"msg_size_max":1044,"msg_size_avg":1044.0,"error_count_total":0,"error_count_rate":0.0}}
```

```
$ ./perftool-1.6.0/bin/perftool -s service-perftool-mqtt -f tsv -c 3 -n 3 -p 1024 -a -t 2
thread_number   writer_start_time       writer_start_time_ms    writer_end_time writer_end_time_ms      writer_time     writer_time_ms  writer_msg_count_total  writer_msg_count_rate   writer_msg_bytes_total  writer_msg_bytes_rate   writer_msg_size_min     writer_msg_size_max     writer_msg_size_avg     writer_error_count_total        writer_error_count_rate reader_start_time       reader_start_time_ms    reader_end_time reader_end_time_ms      reader_time     reader_time_ms  reader_msg_count_total  reader_msg_count_rate   reader_msg_bytes_total  reader_msg_bytes_rate   reader_msg_size_min     reader_msg_size_max     reader_msg_size_avg     reader_error_count_total        reader_error_count_rate
0       1.649312268022E9        1649312268022   1.649312268048E9        1649312268048   0.026   26      3       115.38461538461539      3132    120461.53846153847      1044    1044.0  1044    0       0.0     1.649312267411E9        1649312267411   1.649312268049E9        1649312268049   0.638   638     1       1.567398119122257       1044    1636.3636363636363      1044    1044.0  1044    0       0.0
0       1.64931226874E9 1649312268740   1.649312268746E9        1649312268746   0.006   6       3       500.0   3132    522000.0        1044    1044.0  1044    0       0.0     1.649312268424E9        1649312268424   1.649312268749E9        1649312268749   0.325   325     3       9.23076923076923        3132    9636.923076923076       1044    1044.0  1044    0       0.0
0       1.6493122694E9  1649312269400   1.649312269411E9        1649312269411   0.011   11      3       272.72727272727275      3132    284727.27272727276      1044    1044.0  1044    0       0.0     1.649312269072E9        1649312269072   1.649312269409E9        1649312269409   0.337   337     2       5.9347181008902075      2088    6195.845697329377       1044    1044.0  1044    0       0.0
1       1.649312268021E9        1649312268021   1.649312268066E9        1649312268066   0.045   45      3       66.66666666666667       3132    69600.0 1044    1044.0  1044    0       0.0     1.649312267403E9        1649312267403   1.649312268066E9        1649312268066   0.663   663     1       1.5082956259426847      1044    1574.6606334841629      1044    1044.0  1044    0       0.0
1       1.649312268746E9        1649312268746   1.649312268753E9        1649312268753   0.007   7       3       428.57142857142856      3132    447428.5714285714       1044    1044.0  1044    0       0.0     1.649312268434E9        1649312268434   1.649312268754E9        1649312268754   0.32    320     2       6.25    2088    6525.0  1044    1044.0  1044    0       0.0
1       1.6493122694E9  1649312269400   1.649312269408E9        1649312269408   0.008   8       3       375.0   3132    391500.0        1044    1044.0  1044    0       0.0     1.649312269086E9        1649312269086   1.64931226941E9 1649312269410   0.324   324     3       9.25925925925926        3132    9666.666666666666       1044    1044.0  1044    0       0.0
```
