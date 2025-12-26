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

# 音声データの送受信

SINETStreamを用いて音声データの送受信を行うスクリプトの実装例を示します。音声データの録音には[python-sounddevice](https://python-sounddevice.readthedocs.io/)を、音声ファイルの保存には[PySoundFile](https://pysoundfile.readthedocs.io/)を利用します。

## 1. 準備

### 1.1. 前提条件

音声データ送受信スクリプトの実行環境からアクセスできるKakfaブローカーが用意されていることを前提条件とします。

### 1.2. ミキサーの設定

Raspberry Piで音声データの送信を行う場合は、以下に示すコマンドを実行してマイクのボリュームを設定してください。

```console
arecord -l                      # デバイス一覧
amixer                          # 現在の状態確認
amixer -D hw:1 sset Mic 100%    # マイクのボリュームを設定する
```

### 1.3. ライブラリのインストール

スクリプトの実行環境を準備します。

以下に示す手順でPythonの仮想環境の作成とライブラリのインストールを行なってください。

```console
python -mvenv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

### 1.4. 設定ファイル

python-sounddevice, PySoundFileでは音声データのやり取りに[NumPy](https://numpy.org/)のndarrayを用いています。そのためSINETStreamの設定ファイル`.sinetstream_config.yml`では`value_type`に`ndarray`を指定してください。設定ファイルの記述例を以下に示します。

```yaml
header:
  #version: 3
config:
  sound:
    type: kafka
    brokers: kafka.example.org
    topic: sound-topic
    consistency: AT_LEAST_ONCE
    value_type: ndarray
```

`brokers`, `topic`の指定を環境に応じた値に変更してスクリプトを実行するディレクトリに保存してください。上記と同じ内容のファイルがこのディレクトリの`example_sinetstream_config.yml`にあります。

## 2. 音声データ送受信スクリプトの実行

### 2.1. 音声データの送信

デフォルトのサウンドデバイスで録音を行い、デフォルトのブローカに送信する場合は以下のようにコマンドを実行してください。

```console
./producer.py
```

SINETStreamの設定ファイル`.sinetstream_config.yml`に複数のサービス設定がある場合はどちらを送信対象のブローカとして利用するのかを指定する必要があります。

```console
./producer.py -s sound
```

また録音するサウンドデバイスを選択する場合は `-d` オプションで対象となるデバイスを指定することができます。

```console
./producer.py -d 1
```

サウンドデバイスを特定するためのIDの値は`--list-device`オプションで確認することができます。

```console
$ ./producer.py --list-device
  0 bcm2835 Headphones: - (hw:0,0), ALSA (0 in, 8 out)
> 1 USB PnP Sound Device: Audio (hw:1,0), ALSA (1 in, 0 out)
  2 sysdefault, ALSA (0 in, 128 out)
< 3 default, ALSA (0 in, 128 out)
  4 dmix, ALSA (0 in, 2 out)
```

### 2.2. 音声データの受信

デフォルトのブローカからデータを受信する場合は以下のようにコマンドを実行してください。音声データの受信を終了するにはキーボードで ctrl-c を押してください。受信したデータを音声ファイル`output.flac`に保存します。

```console
$ ./consumer.py 
^C
recording finished: output.flac
```

出力先となる音声ファイルの名前を変更する場合は`-f`オプションを指定してください。

```console
$ ./consumer.py -f sound-01.flac
^C
recording finished: sound-01.flac
```

出力先となるファイルが既に存在している場合はエラーとなります。上書きする場合は`--force`を指定してください。

```console
$ ./consumer.py             
File exists: 'output.flac'
$ ./consumer.py --force
^C
recording finished: output.flac
```

デフォルトの音声ファイルフォーマットはFLACになっています。他のフォーマットで保存する場合は`--format`オプションを指定してください。

```console
$ ./consumer.py --format wav
^C
recording finished: output.wav
```

SINETStreamの設定ファイル`.sinetstream_config.yml`に複数のサービス設定がある場合は`-s`オプションでどちらのブローカを利用するのか指定する必要があります。

```console
./consumer.py -s sound
```
