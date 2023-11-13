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

SINETStreamを用いて音声データの送受信を行う。音声データの録音には[python-sounddevice](https://python-sounddevice.readthedocs.io/)を、
音声ファイルの保存には[PySoundFile](https://pysoundfile.readthedocs.io/)を利用する。

## 準備

利用するライブラリなどをインストールする。

```console
python -mvenv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

Raspberry Piで録音を行う場合は、必要に応じてマイクのボリュームを設定する。

```console
arecord -l                      # デバイス一覧
amixer                          # 現在の状態確認
amixer -D hw:1 sset Mic 100%    # マイクのボリュームを設定する
```

## 設定ファイル

python-sounddevice, PySoundFileでは音声データのやり取りに[NumPy](https://numpy.org/)のndarrayを用いている。
そこで`value_type`には`ndarray`を指定し、設定ファイルは以下のようになる。

```yaml
header:
  version: 2
config:
  sound:
    type: kafka
    brokers: kafka.example.org
    topic: sound-topic
    consistency: AT_LEAST_ONCE
    value_type: ndarray
```

## 音声データの送受信を行うスクリプト

* [producer.py](./producer.py)
  * オーディオ入力の信号をブローカに送信する
* [consumer.py](./consumer.py)
  * ブローカに送信された音声データをファイルに保存する
