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

[English](https://translate.google.com/translate?hl=en&sl=ja&tl=en&u=https://nii-gakunin-cloud.github.io/sinetstream/docs/developer_guide/plugin_compression_java.html "google translate")

# プラグイン開発ガイド(compression / Java)

* 新たな圧縮アルゴリズムをSINETStream (Java)で扱えるようにするためのプラグインを開発する手順について説明します。

## 1. はじめに

SINETStream では設定ファイルあるいはコンストラクタのパラメータで指定する`compression.algorithm`の値に応じて、
メッセージの圧縮/展開を行います。

SINETStream v1.7 以降では以下の `compression.algorithm` をサポートしています。

* `gzip`
    * [apache commons-compress](https://commons.apache.org/proper/commons-compress/examples.html#zip)から
      [java.util.zip](https://docs.oracle.com/javase/jp/8/docs/api/index.html)をつかいます。
* `zstd`
    * [apache commons-compress](https://commons.apache.org/proper/commons-compress/examples.html#Zstandard)から
      [Zstd-jni](https://github.com/luben/zstd-jni)をつかいます。

<!---
* 'lz4'
    * [lz4](https://lz4.github.io/lz4/)をつかいます。
--->

`gzip`, `zstd`はSINETStream本体に組み込みの `compression.algorithm` です。
<!---
`lz4`は追加プラグインとして提供している`compression.algorithm`です。
--->

新たなプラグインを実装することで、上記に示した`compression.algorithm`以外の圧縮アルゴリズムで
SINETStreamのメッセージを圧縮/展開できるようになります。

### 1.1 対象者

このドキュメントが対象としている読者を以下に示します。

* SINETStreamで新たな圧縮アルゴリズムを利用できるようにしたい開発者
### 1.2 前提知識

このドキュメントの説明は、以下の知識を有している読者を前提としています。

* Java 8
* [ServiceLoader](https://docs.oracle.com/javase/jp/8/docs/api/java/util/ServiceLoader.html)の利用方法
* SINETStream の Java APIの利用方法、設定ファイルの記述方法

## 2. プラグインの実装方法

### 2.1 概要

SINETStreamのプラグインを作成するためには以下の作業が必要となります。

* プロバイダ構成ファイルの作成
* サービスプロバイダの実装

それぞれの作業項目の詳細について以下に記します。

### 2.2 プロバイダ構成ファイルの作成

プロバイダ構成ファイルにサービスプロバイダを登録することで、
ServiceLoaderがプラグインを見つけることができるようになります。

構成ファイルはリソースディレクトリの`META-INF/services/`に配置します。
ファイル名はサービスプロバイダの完全修飾クラス名にする必要があります。
SINETStreamに`compression.algorithm`を追加するためのサービスプロバイダの場合、以下のファイル名となります。

* `jp.ad.sinet.stream.spi.CompressionProvider`

構成ファイルには、サービスプロバイダの実装クラス名を完全修飾名で１クラス1行で記述します。

例えば`lz4`の`compression.algorithm`を追加するクラス`jp.ad.sinet.stream.api.compression.Lz4CompressionProvider`を追加する場合、
以下の内容を構成ファイル`META-INF/services/jp.ad.sinet.stream.spi.CompressionProvider`に記します。

```
jp.ad.sinet.stream.api.compression.Lz4CompressionProvider
```

### 2.3 サービスプロバイダの実装

`compression.algorithm`を追加するサービスプロバイダを作成するには、
以下に示すインターフェースを実装したクラスが必要となります。

* `jp.ad.sinet.stream.spi.CompressionProvider`
    * サービスプロバイダインタフェース
* `jp.ad.sinet.stream.api.Compression`
    * `compression`に対応したシリアライザ、デシリアライザを得るためのインタフェース

`CompressionProvider` のメソッドを以下に示します。

* `String getName()`
    * `compression.algorithm`のタイプを表す名前を返す
* `Compression getCompression()`
    * プラグインの`compression.algorithm`に対応した圧縮/展開を得るインターフェースを返す

`Compression` のメソッドを以下に示します。

* `Compressor getCompressor()`
    * シリアライザを返す
* `Decompressor getDecompressor()`
    * デシリアライザを返す
* `String getName()`
    * `compression.algorithm`のタイプを表す名前を返す

## 3. プラグインの実装例

プラグイン実装の具体的な手順を示すために実装例を示します。

ここではSINETStreamのメッセージを[LZ4](https://lz4.github.io/lz4/)で圧縮・展開するための`compression`プラグインを実装します。
LZ4の実装には [apache commons-compress](https://commons.apache.org/proper/commons-compress/examples.html#LZ4) を使います。

### 3.1 ファイル構成

以下のファイルを作成します。

* src/main/java/ssplugin/
    * Lz4Provider.java
    * Lz4Compression.java
    * Lz4Compressor.java
    * Lz4Decompressor.java
* src/main/resources/META-INF/services/jp.ad.sinet.stream.api.compression.CompressionProvider
* build.gradle
* settings.gradle

### 3.2 実装クラス

プラグインとして実装するクラスについて説明します。

#### 3.2.1 Lz4Providerクラス

プラグインのプロバイダインタフェース`CompressionProvider`を実装したクラスになります。

このプラグインの`Compression`実装となる`Lz4Compression`オブジェクトを返す`getCompression()`とタイプ名を返す`getName()`の実装を行います。

```java
import jp.ad.sinet.stream.api.Compression;
import jp.ad.sinet.stream.spi.CompressionProvider;
public class Lz4Provider implements CompressionProvider {
    private static final Compression compression = new Lz4Compression();

    @Override
    public Compression getCompression() {
        return compression;
    }

    @Override
    public String getName() {
        return compression.getName();
    }
}
```

#### 3.2.2 Lz4Compressionクラス

このプラグインの`Compression`実装になります。

```java
import jp.ad.sinet.stream.api.Compression;
import jp.ad.sinet.stream.api.Compressor;
import jp.ad.sinet.stream.api.Decompressor;

class Lz4Compression implements Compression {
    @Override
    public String getName() {
        return "lz4";
    }

    @Override
    public Compressor getCompressor(Integer level, Map<String, Object> parameters) {
        return new Lz4Compressor(level, parameters);
    }

    @Override
    public Decompressor getDecompressor(Map<String, Object> parameters) {
        return new Lz4Decompressor(parameters);
    }
}
```

`getName()`はcompressorのタイプ名を返します。
`getCompressor()`, `getDecompressor()`はそれぞれ圧縮、展開を返します。

#### 3.2.3 Lz4Compressorクラス

`LZ4`の圧縮の実装になります。

```java
import jp.ad.sinet.stream.api.Compressor;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream;

class Lz4Compressor implements Compressor {
    Integer level;

    public Lz4Compressor(Integer level, Map<String, Object> parameters) {
	this.level = level;
    }

    @Override
    public byte[] compress(byte[] data) {
        ByteArrayOutputStream compOut = new ByteArrayOutputStream();
	OutputStream compIn = new FramedLZ4CompressorOutputStream(compOut);
	compIn.write(data);
	compIn.flush();
	return compOut.toByteArray();
    }
 }
```

#### 3.2.4 Lz4Decompressorクラス

`LZ4`の展開の実装になります。

```java
import jp.ad.sinet.stream.api.Decompressor;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream;

class Lz4Decompressor implements Decompressor {
    public Lz4Decompressor(Map<String, Object> parameters) {
    }

    @Override
    public byte[] decompress(byte[] data) {
	int bufsz = 1000;
	ByteArrayInputStream decompIn = new ByteArrayInputStream(bytes);
	ByteArrayOutputStream data = new ByteArrayOutputStream(bufsz);
	InputStream decompOut = new FramedLZ4CompressorInputStream(decompIn);
	byte[] buf = new byte[bufsz];
	int n = 0;
	while ((n = decompOut.read(buf)) != -1) {
	    data.write(buf, 0, n);
	}
	return data.toByteArray();
    }
}
```

### 3.3 プロバイダ構成ファイルの作成

リソースディレクトリの`META-INF/services/`に構成ファイル`jp.ad.sinet.stream.spi.CompressionProvider`を以下の内容で作成します。

```
ssplugin.Lz4CompressionProvider
```

### 3.4 jarファイルの作成

プラグインのjarファイルを作成する手順を以下に示します。

1. [Gradle](https://gradle.org)をインストールする
    * 参考: [インストール手順](https://gradle.org/install/)
2. gradle を実行して jar ファイルを作成する
```bash
$ gradle jar
```
3. `build/libs/`にjarファイルが作成されたことを確認する
```bash
$ ls build/libs/
SINETStream-compression-lz4-compression-1.0.0.jar
```

### 3.5 ソースコード
プラグインの実装例となるファイルへのリンクを以下に示します。

* src/main/java/ssplugin/
    * [Lz4CompressionProvider.java](https://github.com/nii-gakunin-cloud/sinetstream/blob/main/java/plugin-comp-lz4/src/main/java/jp/ad/sinet/stream/api/compression/Lz4CompressionProvider.java)
    * [Lz4Compressor.java](https://github.com/nii-gakunin-cloud/sinetstream/blob/main/java/plugin-comp-lz4/src/main/java/jp/ad/sinet/stream/api/compression/Lz4Compressor.java)
    * [Lz4Decompressor.java](https://github.com/nii-gakunin-cloud/sinetstream/blob/main/java/plugin-comp-lz4/src/main/java/jp/ad/sinet/stream/api/compression/Lz4Decompressor.java)
* [src/main/resources/META-INF/services/jp.ad.sinet.stream.spi.CompressionProvider](https://github.com/nii-gakunin-cloud/sinetstream/blob/main/java/plugin-comp-lz4/src/main/resources/META-INF/services/jp.ad.sinet.stream.spi.CompressionProvider)
* [build.gradle](https://github.com/nii-gakunin-cloud/sinetstream/blob/main/java/plugin-comp-lz4/build.gradle)
* [settings.gradle](https://github.com/nii-gakunin-cloud/sinetstream/blob/main/java/plugin-comp-lz4/settings.gradle-sample)
