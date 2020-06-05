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

[English](https://translate.google.com/translate?hl=en&sl=ja&tl=en&u=https://nii-gakunin-cloud.github.io/sinetstream/docs/developer_guide/plugin_value_type_java.html "google translate")

**準備中** (2020-06-05 19:08:22 JST)

# プラグイン開発ガイド(message type/ Java)

* 新たなメッセージタイプをSINETStream (Java)で扱えるようにするためのプラグインを開発する手順について説明します。

## 1. はじめに

SINETStream では設定ファイルあるいはコンストラクタのパラメータで指定する
`value_type`の値に応じて、メッセージのシリアライズ、デシリアライズを行います。

SINETStream v1.1 以降では以下の `value_type` をサポートしています。

* `byte_array`
    * `byte[]`型のメッセージに対応するタイプ
    * シリアライザ、デシリアライザは入力をそのまま出力する
* `text`
    * `String`型のメッセージに対応するタイプ
    * シリアライザは文字列に対して `.getBytes(StandardCharsets.UTF_8)` にてバイト列に変換する
    * デシリアライザはバイト列を `new String(bytes, StandardCharsets.UTF_8)`にて文字列に変換する
* `image`
    * 画像(`java.awt.image.BufferedImage`)のメッセージに対応するタイプ
    * シリアライザは`BufferedImage`をPNGファイルフォーマットのバイト列に変換する
    * デシリアライザは画像ファイルフォーマットのバイト列を`BufferedImage`の画像オブジェクトに変換する
    
`byte_array`, `text`はSINETStream本体に組み込みの `value_type` です。
`image`は追加プラグインとして提供している`value_type`です。

新たなプラグインを実装することで、上記に示した`value_type`以外のタイプを
SINETStreamのメッセージとして扱えるようになります。

### 1.1 対象者

このドキュメントが対象としている読者を以下に示します。

* SINETStreamで新たなメッセージタイプを利用できるようにしたい開発者

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
SINETStreamに`value_type`を追加するためのサービスプロバイダの場合、以下のファイル名となります。

* `jp.ad.sinet.stream.spi.ValueTypeProvider`
    
構成ファイルには、サービスプロバイダの実装クラス名を完全修飾名で１クラス1行で記述します。

例えば`image`の`value_type`を追加するクラス`jp.ad.sinet.stream.api.valuetype.ImageValueTypeProvider`を追加する場合、
以下の内容を構成ファイル`META-INF/services/jp.ad.sinet.stream.spi.ValueTypeProvider`に記します。

```
jp.ad.sinet.stream.api.valuetype.ImageValueTypeProvider
```

### 2.3 サービスプロバイダの実装

`value_type`を追加するサービスプロバイダを作成するには、
以下に示すインターフェースを実装したクラスが必要となります。

* `jp.ad.sinet.stream.spi.ValueTypeProvider`
    * サービスプロバイダインタフェース
* `jp.ad.sinet.stream.api.ValueType`
    * `value_type`に対応したシリアライザ、デシリアライザを得るためのインタフェース
    
`ValueTypeProvider` のメソッドを以下に示します。
    
* `String getName()`
    * `value_type`のタイプを表す名前を返す
* `ValueType getValueType()`
    * プラグインの`value_type`に対応したシリアライザ、デシリアライザを得るインターフェースを返す
    
`ValueType` のメソッドを以下に示します。
    
* `Serializer getSerializer()`    
    * シリアライザを返す
* `Deserializer getDeserializer()`    
    * デシリアライザを返す
* `String getName()`    
    * `value_type`のタイプを表す名前を返す
    

## 3. プラグインの実装例

プラグイン実装の具体的な手順を示すために実装例を示します。

ここでは `java.util.Map`のオブジェクトをSINETStreamのメッセージとして扱えるようにするための
`value_type`プラグインを実装します。

### 3.1 ファイル構成

以下のファイルを作成します。

* src/main/java/ssplugin/
    * MapTypeProvider.java
    * MapYamlType.java
    * MapYamlSerializer.java
    * MapYamlDeserializer.java
* src/main/resources/META-INF/services/jp.ad.sinet.stream.api.valuetype.ValueTypeProvider
* build.gradle
* settings.gradle

### 3.2 実装クラス

プラグインとして実装するクラスについて説明します。

#### 3.2.1 MapTypeProviderクラス

プラグインのプロバイダインタフェース`ValueTypeProvider`を実装したクラスになります。

このプラグインの`ValueType`実装となる`MapYamlType`オブジェクトを返す`getValueType()`と
タイプ名を返す`getName()`の実装を行います。

```java
public class MapTypeProvider implements ValueTypeProvider {
    private static final ValueType valueType = new MapYamlType();

    @Override
    public ValueType getValueType() {
        return valueType;
    }

    @Override
    public String getName() {
        return valueType.getName();
    }
}
```

#### 3.2.2 MapYamlTypeクラス

このプラグインの`ValueType`実装になります。

```java
class MapYamlType implements ValueType {

    private static final Serializer serializer = new MapYamlSerializer();

    private static final Deserializer deserializer = new MapYamlDeserializer();

    @Override
    public String getName() {
        return "map_yaml";
    }

    @Override
    public Serializer getSerializer() {
        return serializer;
    }

    @Override
    public Deserializer getDeserializer() {
        return deserializer;
    }
}
```

`getName()`はvalue_typeのタイプ名を返します。
`getSerializer()`, `getDeserializer()`はそれぞれシリアライザ、デシリアライザを返します。

#### 3.2.3 MapYamlSerializerクラス

`Map`のシリアライザの実装になります。`Map`をYAMLフォーマットのバイト列に変換する処理を行っています。

```java
class MapYamlSerializer implements Serializer<Map> {
     @Override
     public byte[] serialize(Map map) {
         Yaml yaml = new Yaml();
         return yaml.dump(map).getBytes(StandardCharsets.UTF_8);
     }
 }
```

#### 3.2.4 MapYamlDeserializerクラス

`Map`のデシリアライザの実装になります。YAMLフォーマットのバイト列をMapに変換する処理を行っています。

```java
class MapYamlDeserializer implements Deserializer<Map> {
    @Override
    public Map deserialize(byte[] aByte) {
        Yaml yaml = new Yaml();
        return yaml.load(new String(aByte, StandardCharsets.UTF_8));
    }
}
```

### 3.3 プロバイダ構成ファイルの作成

リソースディレクトリの`META-INF/services/`に構成ファイル`jp.ad.sinet.stream.spi.ValueTypeProvider`を以下の内容で作成します。

```
ssplugin.MapTypeProvider
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
SINETStream-type-map-yaml-1.0.0.jar
```

### 3.5 ソースコード
プラグインの実装例となるファイルへのリンクを以下に示します。

* src/main/java/ssplugin/
    * [MapTypeProvider.java](https://github.com/nii-gakunin-cloud/sinetstream/blob/master/docs/developer_guide/sample/value_type/java/src/main/java/ssplugin/MapTypeProvider.java)
    * [MapYamlType.java](https://github.com/nii-gakunin-cloud/sinetstream/blob/master/docs/developer_guide/sample/value_type/java/src/main/java/ssplugin/MapYamlType.java)
    * [MapYamlSerializer.java](https://github.com/nii-gakunin-cloud/sinetstream/blob/master/docs/developer_guide/sample/value_type/java/src/main/java/ssplugin/MapYamlSerializer.java)
    * [MapYamlDeserializer.java](https://github.com/nii-gakunin-cloud/sinetstream/blob/master/docs/developer_guide/sample/value_type/java/src/main/java/ssplugin/MapYamlDeserializer.java)
* [src/main/resources/META-INF/services/jp.ad.sinet.stream.spi.ValueTypeProvider](https://github.com/nii-gakunin-cloud/sinetstream/blob/master/docs/developer_guide/sample/value_type/java/src/main/resources/META-INF/services/jp.ad.sinet.stream.spi.ValueTypeProvider)
* [build.gradle](https://github.com/nii-gakunin-cloud/sinetstream/blob/master/docs/developer_guide/sample/value_type/java/build.gradle)
* [settings.gradle](https://github.com/nii-gakunin-cloud/sinetstream/blob/master/docs/developer_guide/sample/value_type/java/settings.gradle)

