<!--
Copyright (C) 2021 National Institute of Informatics

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

# perftool.py

- 概要
- 前提条件
- 書式
- オプション
- 出力
- 実行例

## 概要

本ツールは、[SINETStream Writer/Reader API](https://www.sinetstream.net/docs/userguide/api-python.html)によるデータの送信/受信を実行し、その際に取得されたメトリクス情報を出力する。
メトリクス情報は、[SINETStream API Metricsクラス](https://www.sinetstream.net/docs/userguide/api-python.html)のプロパティのうちraw(メッセージングシステム固有の統計情報)を除くものを標準出力に出力する。
メトリクス情報の計測期間は、Writerがオプション等で指定されたサンプリング数のデータ送信を開始/終了する期間とし、Readerも同期間とする。
そのため、データ通信に問題がない場合もReaderの受信数はWriterの送信数より少なくなることがある。
送信データのペイロード型はバイト列を使用する。


## 前提条件

- Python3.6
- SINETStream v1.5.0(Python)
- [SINETStream API 設定ファイル](https://github.com/nii-gakunin-cloud/sinetstream/blob/main/docs/userguide/config.md) (データ送受信先の接続情報等が記述されているもの)

#### 設定ファイル記述例
```
service-perftool-kafka:
    type: kafka
    brokers: "broker:9092"
    topic: topic-perftool-kafka

service-perftool-mqtt:
    type: mqtt
    brokers: "broker:1883"
    topic: topic-perftool-mqtt

```

<div style="page-break-after:always"></div>
## 書式

perftool.py -s|--service *SERVICE_NAME* [-f|--format *FORMAT*] [-c|--output-count *OUTPUT_COUNT*] [-n|--num-samples *NUM_SAMPLES*] [-p|--payload-size *PAYLOAD_SIZE*] [-a|--async-api]

## オプション

- `-s`, `--service` *SERVICE_NAME*
    * SINETStream API で使用するサービス名
- `-f`, `--format` *FORMAT*
    * 出力形式。jsonかtsvが指定可能。デフォルト:json
- `-c`, `--output-count` *OUTPUT_COUNT*
    * *OUTPUT_COUNT*回メトリクス情報を計測し出力する。デフォルト:1
- `-n`, `--num-samples` *NUM_SAMPLES*
    * 一回のメトリクス情報を出力するまでに*NUM_SAMPLES*個のデータ送信を試行する。デフォルト:300
- `-p`, `--payload-size` *PAYLOAD_SIZE*
    * データ送信試行時に*PAYLOAD_SIZE*バイトのペイロード長のデータを使用する。デフォルト:1024
- `-a`, `--async-api`
    * 指定した場合はデータの送信/受信に非同期APIを使用する。指定しなかった場合は同期APIを使用する

<div style="page-break-after:always"></div>
## 出力
#### JSON形式
`-c`, `--output-count`オプションで指定した回数この形式で出力する
ここでは人間が見やすいよう整形してコメントも追記しているが、実際は改行無しで
詰めた形式で出力される。
実際の出力は実行例を参照。

```
{
    # writerのメトリクス情報
     "writer": {
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
出力は、1行目がヘッダ、以降は `-c`, `--output-count`オプションで指定した回数分のデータ行となる。
ここではヘッダの一覧を示す。
実際の出力は実行例を参照。

```
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
$ python3 perftool.py -s service-perftool-kafka
{"writer": {"start_time": 1616677087.4501278, "start_time_ms": 1616677087450.128, "end_time": 1616677088.464487, "end_time_ms": 1616677088464.487, "time": 1.0143592357635498, "time_ms": 1014.3592357635498, "msg_count_total": 300, "msg_count_rate": 295.7532099307773, "msg_bytes_total": 313200, "msg_bytes_rate": 308766.3511677315, "msg_size_min": 1044, "msg_size_max": 1044, "msg_size_avg": 1044.0, "error_count_total": 0, "error_count_rate": 0.0}, "reader": {"start_time": 1616677087.4501393, "start_time_ms": 1616677087450.1394, "end_time": 1616677088.463871, "end_time_ms": 1616677088463.871, "time": 1.0137317180633545, "time_ms": 1013.7317180633545, "msg_count_total": 234, "msg_count_rate": 230.83030335386613, "msg_bytes_total": 244296, "msg_bytes_rate": 240986.83670143623, "msg_size_min": 1044, "msg_size_max": 1044, "msg_size_avg": 1044.0, "error_count_total": 0, "error_count_rate": 0.0}}
```

```
$ python3 perftool.py -s service-perftool-kafka  -f tsv -c 3 -n 1000 -p 1024 -a
writer_start_time       writer_start_time_ms    writer_end_time writer_end_time_ms      writer_time     writer_time_ms  writer_msg_count_total  writer_msg_count_rate   writer_msg_bytes_total  writer_msg_bytes_rate   writer_msg_size_min     writer_msg_size_max     writer_msg_size_avg     writer_error_count_total        writer_error_count_rate reader_start_time       reader_start_time_ms    reader_end_time reader_end_time_ms      reader_time     reader_time_ms  reader_msg_count_total  reader_msg_count_rate   reader_msg_bytes_total  reader_msg_bytes_rate   reader_msg_size_min     reader_msg_size_max     reader_msg_size_avg     reader_error_count_total        reader_error_count_rate
1616591229.4149387      1616591229414.9387      1616591230.9502277      1616591230950.2278      1.5352890491485596      1535.2890491485596      1000    651.3431464612999       1044000 680002.2449055971       1044    1044    1044.0  0       0.0     1616591229.296469       1616591229296.469       1616591230.940047       1616591230940.047       1.643578052520752       1643.578052520752       976     593.8263768508657       1018944 619954.7374323038       1044    1044    1044.0  0       0.0
1616591231.1887398      1616591231188.7397      1616591232.7698293      1616591232769.8293      1.5810894966125488      1581.0894966125488      1000    632.4752660380574       1044000 660304.1777437319       1044    1044    1044.0  0       0.0     1616591231.0742073      1616591231074.2073      1616591232.7586951      1616591232758.695       1.6844878196716309      1684.4878196716309      967     574.0617347939649       1009548 599320.4511248994       1044    1044    1044.0  0       0.0
1616591233.01446        1616591233014.4602      1616591234.5790718      1616591234579.0718      1.5646116733551025      1564.6116733551025      1000    639.1362259592711       1044000 667258.219901479        1044    1044    1044.0  0       0.0     1616591232.9014885      1616591232901.4885      1616591234.5769625      1616591234576.9624      1.675473928451538       1675.473928451538       994     593.2649760289903       1037736 619368.6349742659       1044    1044    1044.0  0       0.0
```