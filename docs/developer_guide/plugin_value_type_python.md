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

[English](https://translate.google.com/translate?hl=en&sl=ja&tl=en&u=https://nii-gakunin-cloud.github.io/sinetstream/docs/developer_guide/plugin_value_type_python.html "google translate")

# プラグイン開発ガイド(message type/ Python)

* 新たなメッセージタイプをSINETStream (Python)で扱えるようにするためのプラグインを開発する手順について説明します。

## 1. はじめに

SINETStream では設定ファイルあるいはコンストラクタのパラメータで指定する`value_type`の値に応じて、
メッセージのシリアライズ、デシリアライズを行います。

SINETStream v1.1 以降では以下の `value_type` をサポートしています。

* `byte_array`
    * `bytes`型のメッセージに対応するタイプ
    * シリアライザ、デシリアライザは入力をそのまま出力する
* `text`
    * `str`型のメッセージに対応するタイプ
    * シリアライザは文字列を `str.encode()` にてバイト列に変換する
    * デシリアライザはバイト列を `bytes.decode()`にて文字列に変換する
* `image`
    * [OpenCV](https://pypi.org/project/opencv-python/)の画像型のメッセージに対応するタイプ
    * シリアライザはOpenCVの画像(ndarray)をPNGファイルフォーマットのバイト列に変換する
    * デシリアライザは画像ファイルフォーマットのバイト列をOpenCVの画像オブジェクト(ndarray)に変換する

`byte_array`, `text`はSINETStream本体に組み込みの `value_type` です。
`image`は追加プラグインとして提供している`value_type`です。

新たなプラグインを実装することで、上記に示した`value_type`以外のタイプをSINETStreamのメッセージとして扱えるようになります。

### 1.1 対象者

このドキュメントが対象としている読者を以下に示します。

* SINETStreamで新たなメッセージタイプを利用できるようにしたい開発者

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

`value_type`のプラグインではメッセージのタイプに応じたシリアライザ、デシリアライザをプロパティとして提供する必要があります。
具体的には以下のプロパティの定義が必要となります。

* `serializer`
    * メッセージのシリアライザ
* `deserializer`
    * メッセージのデシリアライザ

プラグインが上記のプロパティを実装することを確認するために、
抽象基底クラス `sinetstream.spi.PluginValueType`を利用することができます。
`PluginValueType`では上記のプロパティが抽象プロパティとして定義されています。

### 2.3 パッケージメタデータの作成

[setuptools](http://setuptools.readthedocs.io/)のエントリポイントにクラスを登録することで、SINETStreamがプラグインを見つけることができるようになります。
これは登録されたエントリポイントをsetuptoolsが検出する機能を利用して実現しています。
setuptoolsはPythonの配布パッケージのビルドなどを行うためのツールです。

登録されているエントリポイントからSINETStreamで必要となるクラスを探し出すことができるようにするためには、
エントリポイントのグループと名前を適切に設定する必要があります。
`value_type`プラグインでは`sinetstream.value_type`をグループに指定します。
また名前には `value_type`として追加するタイプ名を指定します。

例えば`value_type`に`image`を追加プラグインの場合`setup.cfg` に以下の記述を行います。

```
[options.entry_points]
sinetstream.value_type =
    image = sinetstreamplugin.valuetype.image:ImageValueType
```

エントリポイントの詳細については
[setuptools documentation - Entry Points](https://setuptools.readthedocs.io/en/latest/pkg_resources.html#entry-points)
を参照してください。


## 3. プラグインの実装例

プラグイン実装の具体的な手順を示すために実装例を示します。

ここでは dict型のオブジェクトをSINETStreamのメッセージとして扱えるようにするための`value_type`プラグインを実装します。

### 3.1 ファイル構成

以下のファイルを作成します。

* src/ssplugin/map_yaml.py
    * `map_yaml`タイププラグインの実装
* setup.py
    * パッケージングを行う際のコマンドラインインタフェース
* setup.cfg
    * `setup.py`の設定ファイル

### 3.2 プラグイン実装

プラグインの実装を行うモジュールファイル`map_yaml.py`について説明します。

まずクラス定義を行います。

```python
class MapYamlValueType(PluginValueType):
```

ここでは抽象基底クラス`PluginValueType`を継承したクラスを定義します。
プラグインクラスの実装において`PluginValueType`を継承することは必須ではありません。
しかし開発環境によっては抽象基底クラスを継承することにより、
プラグイン実装に必要となるメソッドに関する情報などの支援を受けられる場合があります。

次にシリアライザ、デシリアライザの処理を実装するメソッドを定義します。

```python
    def _map_to_bytes(self, params):
        return safe_dump(params, encoding='utf-8')

    def _map_from_bytes(self, data):
        return safe_load(data)
```

ここでは `dict`型のオブジェクトを Yamlのバイト列に変換するシリアライザと、
その逆向きの処理を行うデシリアライザを定義しています。

次にプラグインで実装する必要のあるプロパティを定義します。

```python
    @property
    def serializer(self):
        return self._map_to_bytes

    @property
    def deserializer(self):
        return self._map_from_bytes
```

先ほど定義したシリアライザ`_map_to_bytes`、デシリアライザ`_map_from_bytes`を返すプロパティを定義しています。

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
name = sinetstream-type-map-yaml
version = 1.0.0

[options]
package_dir=
    =src
packages = find_namespace:
zip_safe = False
namespace_packages =
  ssplugin
install_requires =
  sinetstream>=1.1.0
  pyyaml
python_requires = >= 3.6

[options.packages.find]
where = src

[options.entry_points]
sinetstream.value_type =
    map_yaml = ssplugin.map_yaml:MapYamlValueType
```

プラグインに直接関わる設定は `options.entry_points`セクションです。
`sinetstream.value_type`が`value_type`プラグインに対応するグループになります。
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
dist/sinetstream_type_map_yaml-1.0.0-py3-none-any.whl
```

### 3.4 利用例

作成したプラグインを利用して dict型オブジェクトを送信する例を以下に示します。

```python
msg = {
    'message': 'message 001',
    'value': 17,
}

with MessageWriter(service='service-1', value_type='map_yaml') as writer:
    writer.publish(msg)
```

`MessageWriter`のコンストラクタのパラメータ`value_type`の値に、
新たに作成した`value_type`プラグインのタイプ名`map_yaml`を指定しています。
そのため、`writer.publish()`の引数に直接dict型変数を渡すことができます。

### 3.5 ソースコード
ここまで記した実装例のファイルへのリンクを以下に示します。
* [src/ssplugin/map_yaml.py](https://github.com/nii-gakunin-cloud/sinetstream/blob/main/docs/developer_guide/sample/value_type/python/src/ssplugin/map_yaml.py)
* [setup.py](https://github.com/nii-gakunin-cloud/sinetstream/blob/main/docs/developer_guide/sample/value_type/python/setup.py)
* [setup.cfg](https://github.com/nii-gakunin-cloud/sinetstream/blob/main/docs/developer_guide/sample/value_type/python/setup.cfg)
