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

[English](https://translate.google.com/translate?hl=en&sl=ja&tl=en&u=https://nii-gakunin-cloud.github.io/sinetstream/docs/developer_guide/plugin_compression_python.html "google translate")

# プラグイン開発ガイド(compression / Python)

* 新たな圧縮アルゴリズムをSINETStream (Python)で扱えるようにするためのプラグインを開発する手順について説明します。

## 1. はじめに

SINETStream では設定ファイルあるいはコンストラクタのパラメータで指定する`compression.algorithm`の値に応じて、
メッセージの圧縮/展開を行います。

SINETStream v1.7 以降では以下の `compression.algorithm` をサポートしています。

* `gzip`
    * [zlib](https://zlib.net/)をつかいます。
* `zstd`
    * [zstandard](https://facebook.github.io/zstd/)をつかいます。

<!--
* 'lz4'
    * [lz4](https://lz4.github.io/lz4/)をつかいます。
-->

`gzip`, `zstd`はSINETStream本体に組み込みの `compression.algorithm` です。
<!--
`lz4`は追加プラグインとして提供している`compression.algorithm`です。
-->

新たなプラグインを実装することで、上記に示した`compression.algorithm`以外の圧縮アルゴリズムで
SINETStreamのメッセージを圧縮/展開できるようになります。

### 1.1 対象者

このドキュメントが対象としている読者を以下に示します。

* SINETStreamで新たな圧縮アルゴリズムを利用できるようにしたい開発者

### 1.2 前提知識

このドキュメントの説明は、以下の知識を有している読者を前提としています。

* Python 3
* [setuptools](https://setuptools.readthedocs.io/en/latest/)による配布パッケージの作成手順
* SINETStream の Python APIの利用方法、設定ファイルの記述方法

## 2. プラグインの実装方法

### 2.1 概要

SINETStreamのプラグインを作成するためには以下の作業が必要となります。

* プラグインに定められているメソッドを実装したクラスの作成
* パッケージメタデータの作成

それぞれの作業項目の詳細について以下に記します。

### 2.2 プラグインに定められているプロパティを実装したクラスの作成

`compression`のプラグインでは圧縮アルゴリズム応じた圧縮/展開関数をプロパティとして提供する必要があります。
具体的には以下のプロパティの定義が必要となります。

* `compressor`
    * 圧縮処理の関数を返す
* `decompressor`
    * 展開処理の関数を返す

プラグインが上記のプロパティを実装することを確認するために、
抽象基底クラス `sinetstream.spi.PluginCompression`を利用することができます。
`PluginCompression`では上記のプロパティが抽象プロパティとして定義されています。

### 2.3 パッケージメタデータの作成

[setuptools](http://setuptools.readthedocs.io/)のエントリポイントにクラスを登録することで、SINETStreamがプラグインを見つけることができるようになります。
これは登録されたエントリポイントをsetuptoolsが検出する機能を利用して実現しています。
setuptoolsはPythonの配布パッケージのビルドなどを行うためのツールです。

登録されているエントリポイントからSINETStreamで必要となるクラスを探し出すことができるようにするためには、
エントリポイントのグループと名前を適切に設定する必要があります。
`compression`プラグインでは`sinetstream.compression`をグループに指定します。
また名前には `compression`として追加する圧縮アルゴリズム名を指定します。

例えば`compression`に`lz4`を追加するプラグインの場合`setup.cfg` に以下の記述を行います。

```
[options.entry_points]
sinetstream.compression =
    lz4 = sinetstreamplugin.compression.lz4:LZ4Compression
```

エントリポイントの詳細については
[setuptools documentation - Entry Points](https://setuptools.readthedocs.io/en/latest/pkg_resources.html#entry-points)
を参照してください。


## 3. プラグインの実装例

プラグイン実装の具体的な手順を示すために実装例を示します。

ここではSINETStreamのメッセージを[LZ4](https://pypi.org/project/lz4/)で圧縮・展開するための`compression`プラグインを実装します。

### 3.1 ファイル構成

以下のファイルを作成します。

* src/sinetstreamplugin/compression/lz4.py
    * `lz4`タイププラグインの実装
* setup.py
    * パッケージングを行う際のコマンドラインインタフェース
* setup.cfg
    * `setup.py`の設定ファイル

### 3.2 プラグイン実装

プラグインの実装を行うモジュールファイル`lz4.py`について説明します。

まずクラス定義を行います。

```python
class LZ4Compression(PluginCompression):
    def __init__(self, level=None, params={}):
        self._level = level
        self._params = params
```

ここでは抽象基底クラス`PluginCompression`を継承したクラスを定義します。
プラグインクラスの実装において`PluginValueType`を継承することは必須ではありません。
しかし開発環境によっては抽象基底クラスを継承することにより、
プラグイン実装に必要となるメソッドに関する情報などの支援を受けられる場合があります。

次に圧縮・展開処理を実装するメソッドを定義します。
い
```python
    def compress(self, data, params):
        return lz4.frame.compress(data, **params)

    def decompress(self, data):
        return lz4.frame.decompress(data)
```

次にプラグインで実装する必要のあるプロパティを定義します。

```python
    @property
    def compressor(self):
        params = copy.deepcopy(self._params)
        if self._level is not None and "compression_level" not in params:
            params["compression_level"] = self._level
        return lambda data: self.compress(data, params)

    @property
    def decompressor(self):
        return lambda data: self.decompress(data)
```

先ほど定義した圧縮処理`compress`、展開処理`decompress`を返すプロパティを定義しています。
圧縮処理では圧縮レベルを指定するためにlambdaをつかっています。

### 3.3 パッケージング

#### 3.3.1 `setup.py`, `setup.cfg`の作成

パッケージングを行う際のコマンドラインインタフェースとなる `setup.py` とその設定ファイル `setup.cfg` を作成します。

まず `setup.py` を作成します。設定については全て`setup.cfg`で行うので `setup.py`は必要最小限なものとします。

```python
from setuptools import setup
setup()
```

次に `setup.cfg` を作成します。

```
[metadata]
name = sinetstream-comp-lz4
version = 1.7.0

[options]
package_dir=
    =src
packages = find_namespace:
zip_safe = False
namespace_packages =
  sinetstreamplugin
install_requires =
  sinetstream>=1.7.0
  lz4
python_requires = >= 3.7

[options.packages.find]
where = src

[options.entry_points]
sinetstream.compression =
    lz4 = sinetstreamplugin.compression.lz4:LZ4Compression
```

プラグインに直接関わる設定は `options.entry_points`セクションです。
`sinetstream.compression`が`compression`プラグインに対応するグループになります。
グループに対して (`value_type`のタイプ名)=(パッケージ名:クラス名) を指定しています。

#### 3.3.2 パッケージの作成

wheelパッケージを作成します。

```bash
$ python setup.py bdist_wheel
running bdist_wheel
running build
running build_py
(中略)
$ ls dist/
dist/sinetstream_comp_lz4-1.7.0-py3-none-any.whl
```

### 3.4 利用例

作成したプラグインを利用してデータ圧縮する例を以下に示します。

```python
with MessageWriter(service='service-1', data_compression=True, compression={'algorithm': 'lz4'}) as writer:
    writer.publish('test')
```

`MessageWriter`のコンストラクタのパラメータ`compression.algorithm`の値に
新たに作成した`compression`プラグインの圧縮アルゴリズム名`lz4`を指定しています。

### 3.5 ソースコード
ここまで記した実装例のファイルへのリンクを以下に示します。
* [src/src/sinetstreamplugin/compression/lz4.py](https://github.com/nii-gakunin-cloud/sinetstream/blob/main/python/plugins/compression/lz4/src/src/sinetstreamplugin/compression/lz4.py)
* [setup.py](https://github.com/nii-gakunin-cloud/sinetstream/blob/main/python/plugins/compression/lz4/setup.py)
* [setup.cfg](https://github.com/nii-gakunin-cloud/sinetstream/blob/main/python/plugins/compression/lz4/setup.cfg)
