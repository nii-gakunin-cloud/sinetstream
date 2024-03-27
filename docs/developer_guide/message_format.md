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

[English](https://translate.google.com/translate?hl=en&sl=ja&tl=en&u=https://nii-gakunin-cloud.github.io/sinetstream/docs/developer_guide/message_format.html "google translate")

# メッセージフォーマット

## 互換性

```
 ________     ________     ________
|        |   |        |   |        |
| Writer |   | Broker |   | Reader |
|________|   |________|   |________|
     |__________|  |__________|
```

- 互換性が保てないメッセージフォーマットの変更が行われるとフォーマットバージョンのメジャー番号が上がる。
- 後方互換性を保った変更の場合はマイナー番号が上がる。

## Version 0

ユーザからわたされたメッセージを(value_typeに応じてシリアライズしたあと)
そのままメッセージングシステムにわたす。
つまりSINETStreamレイヤではなにもしない。

### フォーマット

先頭から:

- plaintext
    - ユーザメッセージのバイト列

## Version 1

ユーザからわたされたメッセージを共通鍵暗号で暗号化してメッセージングシステムにわたす。
暗号をつかう設定をしなければversion 0とおなじで素通しになる。

パスワードと暗号パラメータはreaderとwriterで同じ設定を使わなければならない。

#### sinetstream_config.ymlで指定するパスワードと暗号パラメータ

````
<service>:
    crypto:
        data_encryption: true
        password:
            value: <password>
        algorithm: <algorithm>
        key_length: <key_length>
        mode: <mode>
        padding: <padding>
        key_derivation:
            algorithm: <kd_algorithm>
            salt_bytes: <kd_salt_bytes>
            iteration: <kd_iteration>
            prf: <kd_prf>

````

### フォーマット

先頭から:

- salt
    - 鍵導出につかった乱数
    - バイト長: kd_salt_bytes
- IV
    - 暗号モードの初期ベクトル
    - バイト長: 16
- chipertext
    - 暗号化されたユーザメッセージのバイト列
- auth tag
    - 暗号メッセージの検証コード
    - バイト長: 暗号モードによる
        - AES/EAXのとき8
        - AES/GCMのとき16

暗号化しない場合はV0とおなじである。

#### 送信時(暗号化)のながれ(V1)

````
                    ユーザからわたされたメッセージ
                       |
                       | serialize(value_type)
                       V
                    cleartext (バイト列)
                      /|                                  +-- key derivation with
                     / | encrypt with                     |      パスワード(password)
                    /  |    暗号アルゴリズム(algorithm)   |      ソルト(初期化時に乱数生成)
                   /   |    暗号利用モード(mode)          |      鍵長(key_length)
                  /    |    バディング(padding)           |      鍵導出アルゴリズム(kd_algorithm)
   ______________/     |    鍵長(key_length)              |      反復回数(kd_iteration)
  |非暗号化時          |    暗号鍵 <----------------------+      疑似乱数関数(kd_prf)
  |                    |    IV(送信するごとに乱数生成)
  |                    |
  |                    V
  |                 ciphertext (+ auth tag)
  |                    |
  |                    V
  |     salt + IV + ciphertext (+ auth tag)
  |    \___________________________________/
  |        |
  V        V
メッセージングシステムへ
````

### 受信時(復号)のながれ(V1)

````
                    メッセージ ユーザへ
                       A
                       | deserialize(value_type)
                       |
                    cleartext (バイト列)
                       A
                      /|                                  +-- key derivation with
                     / | decrypt with                     |      パスワード(password)
                    /  |    暗号アルゴリズム(algorithm)   |      ソルト(受信メッセージから)
                   /   |    暗号利用モード(mode)          |      鍵長(key_length)
                  /    |    バディング(padding)           |      鍵導出アルゴリズム(kd_algorithm)
   ______________/     |    鍵長(key_length)              |      反復回数(kd_iteration)
  |非暗号時            |    暗号鍵 <----------------------+      疑似乱数関数(kd_prf)
  |                    |    IV(受信メッセージから)
  |                    |
  |                    |
  |     salt + IV + ciphertext (+ auth tag)
  |    \___________________________________/
  |        A
  |        | 分解
  |        |
メッセージングシステムから
````

auth tagはGCM,EAXなど認証付暗号の場合のみ追加される。

### 暗号鍵

設定ファイルで指定されたパスワードから鍵導出アルゴリズムにより暗号鍵が生成される。
鍵導出アルゴリムズをつかうことの利点:

- 任意の鍵長に対応できる。
- ソルトをつけることで辞書攻撃を困難にする。
    - ソルトはWriterオブジェクトを初期化するときに生成することを想定している。
    - Reader側はメッセージにくっついているソルトを用いて暗号鍵を作成する。
      同じWriterからのメッセージは同じソルトがつかわれていると想定できるので、
      ソルト→暗号鍵のキャッシュを使用する。

### IV(Initialization Vector)

暗号利用モードによって要求される初期値で、
平文が同じでも異なる暗号文を生成して推測されにくくするためのものである。
メッセージを送信するごとに乱数生成する。

CTRモードではnonceになる。

暗号アルゴリズムがAESのとき暗号利用モードによらずkey_lengthによらず、
IVのサイズは128ビット(16バイト)である。

### auth tag(認証タグ)

認証付暗号をつかったときの認証タグで、
暗号アルゴリズムがAESのとき128ビット(16バイト)である。


## Version 2

暗号化機能に加え、メッセージに送信時刻のタイムスタンプをつける。
タイムスタンプをつけない設定はできない。

タイムスタンプをつけるのに[Apache Avro][avro]の[Sigle-object Encoding][avros]をつかう。
Avro Sigle-object EncodingはAvro Binary Encodingの前にAvro markerとスキーマのフィンガープリントをつけくわえたものである。

### フォーマット

先頭から:

- salt
    - 鍵導出につかった乱数
    - バイト長: kd_salt_bytes
- IV
    - 暗号モードの初期ベクトル
    - バイト長: 16
- 暗号化されたAvro Single-objectメッセージ
    - Avroメッセージマーカー
        - バイト長: 2
        - `C3 01`
        - 1バイト目の `C3` はLatin1でÃになりAvroっぽい文字ということで選ばれたようだ。
        - 2バイト目の `01` はバージョン番号を意味するようだ。
    - Avroスキーマフィンガープリント
        - バイト長: 8
        - 使うスキーマのCRC-64-AVROを計算したもの。
        - V2での値は `1F 9C 0C 91 EB 33 06 4F` になる。
    - timestamp
        - SINETStream APIが呼ばれた時刻のUNIX時間(単位はマイクロ秒)。
        - Avroではlong型になる。
    - msg
        - ユーザメッセージのバイト列
- auth tag
    - 暗号メッセージの検証コード
    - バイト長: 暗号モードによる
        - AES/EAXのとき8
        - AES/GCMのとき16

暗号化無効時は:

- Avro Single-objectメッセージ
    - Avroメッセージマーカー
    - Avroスキーマフィンガープリント
    - timestamp
    - msg

### Version 2がつかうAvroのスキーマ

````
{
  "namespace": "jp.ad.sinet.stream",
  "type": "record",
  "name": "message",
  "fields": [
    {
      "name": "tstamp",
      "type": {
        "type": "long",
        "logicalType": "timestamp-micros"
      }
    },
    {
      "name": "msg",
      "type": "bytes"
    }
  ]
}
````

### 送信時のながれ(V2)

````
                     ユーザからわたされたメッセージ
                        |
                        | serialize(value_type)
                        V
                     byte-stream     tstamp (unix時刻 in ms)
                        |               |
                        V_______________V
                        |
                        | Avro encode
                        V
                     Avro Single-object encodedメッセージ
                       /|
                      / | encrypt <--- 暗号鍵 <-------------------- パスワード
   __________________/  |       A                 key derivation
  |非暗号時             |       |                           A
  |                     |      IV(乱数)                     |
  |                     V                                  salt(乱数)
  |      salt + IV + ciphertext (+ auth tag)
  |     \___________________________________/
  |                 |
  V                 V
メッセージングシステムへ
````

### 受信時のながれ(V2)

```
                    メッセージ ユーザへ              tstamp (unix時刻 in ms) ユーザへ
                       A                               A
                       | deserialize(value_type)       |
                       |                               |
                    byte-stream                        |
                       A                               |
                       |_______________________________|
                       |
                       | Avro decode
                       |
                    Avro Single-object encodedメッセージ
                       A
                      /| decrypt <--- 暗号鍵 <-------------------- パスワード
                     / |       A                 key derivation
   _________________/  |       |                           A
  |非暗号時            |     IV(受信メッセージから)        |
  |                    |                                  salt(受信メッセージから)
  |     salt + IV + ciphertext (+ auth tag)
  |    \___________________________________/
  |                A
  |                | 分解
  |                |
メッセージングシステムから
```


## Version 2.1

timestmap=0はタイムスタンプが設定されていないことを示す。


## Version 3

メッセージの暗号化につかうパスワードを途中で変更できるように、
鍵バージョンをメッセージにつけくわえる。

### フォーマット

先頭から:

- message marker
    - バイト長: 4
    - `DF 03 00 00`
    - 1バイト目の `DF` はLatin1でßになりSINETStreamっぽい文字ということで選ばれた。
    - 2バイト目の `03` はバージョン番号を意味する。
    - 3～4バイト目の `00` はV2フォーマットとの衝突確率を低減するためのフィラー。
- key version
    - バイト長: 2
    - 鍵バージョン番号(符号無し整数)
        - 0 は暗号化なしを意味する。
    - バイトオーダーはビッグエンディアン。
    - 鍵バージョン番号は頻繁に更新されないと想定して2バイトとした。
- salt
    - 鍵導出につかった乱数
    - バイト長: kd_salt_bytes
        - パスワードから鍵導出したのではなく鍵を直接指定した場合は長さ0である。
- IV
    - 暗号モードの初期ベクトル
    - バイト長: 16
- 暗号化されたAvro Single-objectメッセージ
    - Avroメッセージマーカー
        - バイト長: 2
        - `C3 01`
        - 1バイト目の `C3` はLatin1でÃになりAvroっぽい文字ということで選ばれたようだ。
        - 2バイト目の `01` はバージョン番号を意味するようだ。
    - Avroスキーマフィンガープリント
        - バイト長: 8
        - 使うスキーマのCRC-64-AVROを計算したもの。
        - V2での値は `1F 9C 0C 91 EB 33 06 4F` になる。
    - timestamp
        - SINETStream APIが呼ばれた時刻のUNIX時間(単位はマイクロ秒)。
        - Avroではlong型になる。
    - msg
        - ユーザメッセージのバイト列
- auth tag
    - 暗号メッセージの検証コード
    - バイト長: 暗号モードによる
        - AES/EAXのとき8
        - AES/GCMのとき16

暗号化無効時は:

- message marker
    - `DF 03 00 00`
- key version
    - `00 00`
    - 暗号化なしのときは鍵バージョンは 0 を指定しなければならない。
- Avro Single-objectメッセージ
    - Avroメッセージマーカー
    - Avroスキーマフィンガープリント
    - timestamp
    - msg

> 鍵バージョンを付加するのにAvro Single-object encodingをつかいたいところだが、
> Avroメッセージの入れ子構造を扱えないプロダクトに考慮して
> 単純なバイナリフォーマットにした。

### 送信時のながれ(V3)

````
                               ユーザからわたされたメッセージ
                                  |
                                  | serialize(value_type)
                                  V
                               byte-stream     tstamp (unix時刻 in ms)
                                  |               |
                                  V_______________V
                                  |
                                  | Avro encode
                                  V
                               Avro Single-object encodedメッセージ
                                 /|                       +------------------- key <-------+--- ConfigServer.getKey()
                                / |                       |                                |                   or
                               /  | encrypt <--- 暗号鍵 <-+------------------- password <-+)--- ConfigServer.getPasswd()
              ________________/   |       A                  key derivation               ||
             |非暗号時            |       |                            A                  VV
             |                    |      IV(乱数)                      |                key version
             |                    V                                   salt(乱数)
             |     salt + IV + ciphertext (+ auth tag)
             |    \___________________________________/
             |                    |
             |____________________|
                                  |
                 message          V
         marker + key_version + {   }
        \____________________________/
           |
           V
        メッセージングシステムへ
````

### 受信時のながれ(V3)

````
                               メッセージ ユーザへ              tstamp (unix時刻 in ms) ユーザへ
                                  A                               A
                                  | deserialize(value_type)       |
                                  |                               |
                               byte-stream                        |
                                  A                               |
                                  |_______________________________|
                                  |
                                  | Avro decode
                                  |
                               Avro Single-object encodedメッセージ
                                  A
                                  |                           ConfigServer.getKey(key_version)
                                  |                            |            when cache-miss
                                  |                           _V_____
                                  |                       +--|key|ver|<--- key version
                                  |                       |  |   |   |    (受信メッセージから)
                                  |                       |  |___|___|
                                  |                       |   cache
                                  |                       |                   ConfigServer.getPasswd(key_version)
                                  |                       |                      |            when cache-miss
                                 /|                       |                   ___V________
                                / | decrypt <--- 暗号鍵 <-+------------------|password|ver|<--- key version
              _________________/  |       A                 key derivation   |        |   |    (受信メッセージから)
             |非暗号時            |       |                           A      |________|___|
             |                    |     IV(受信メッセージから)        |       cache
             |                    V                                   |
             |     salt + IV + ciphertext (+ auth tag)               salt(受信メッセージから)
             |    \___________________________________/
             |                    A
             |                    | 分解
             |____________________|
                                  |
         message                  |
         marker + key_version + {   }
        \____________________________/
           A
           | 分解
           |
        メッセージングシステムから
````

<!--- link definitions --->
[avro]:http://avro.apache.org/
[avros]:http://avro.apache.org/docs/current/spec.html#single_object_encoding
