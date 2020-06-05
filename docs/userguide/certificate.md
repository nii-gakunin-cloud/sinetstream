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

[English](certificate.en.md)

**準備中** (2020-06-05 18:05:13 JST)

# プライベート認証局で証明書を作成する

## 概要

SINETStreamでSSL/TLS接続を行うために必要となる証明書をプライベート認証局にて作成する。

この文書のおもな記述の流れを以下に示す。

1. 設定手順の前提条件について
1. プライベート認証局の構築手順
1. サーバ証明書の作成
1. クライアント証明書の作成

## 前提条件

設定手順の記述を簡潔にするために、ここでは以下の前提条件をおく。

* プライベート認証局の実行環境はCentOS 7とする
* プライベート認証局で作成する証明書の形式はPEMとする

設定例を示す場合のホスト名などの値を以下に示す。
> 実際に設定を行う際は、以下の値に対応する箇所を環境に合わせて適宜変更すること。

* プライベート認証局
    * OpenSSLの設定ファイル
        * /etc/pki/tls/openssl.cnf
    * CAに関連するファイルを配置するディレクトリ
        * /etc/pki/CA
* 証明書
    * CA証明書
        * 証明書ファイルのパス
            * /etc/pki/CA/cacert.pem
        * 秘密鍵のパス
            * /etc/pki/CA/private/cakey.pem
        * サブジェクト
            * /C=JP/ST=Example_State/O=Example_Organization/CN=private-ca
        * 有効期限（日）
            * 3650
    * ブローカーのサーバ証明書
        * 証明書ファイルのパス
            * /etc/pki/CA/certs/broker.crt
        * 秘密鍵のパス
            * /etc/pki/CA/private/broker.key
        * サブジェクト
            * /C=JP/CN=broker.example.org
    * クライアント証明書
        * 証明書ファイルのパス
            * /etc/pki/CA/certs/client0.crt
        * 秘密鍵のパス
            * /etc/pki/CA/private/client0.key
        * サブジェクト
            * /C=JP/CN=client0

## プライベート認証局を構築する

プライベート認証局を構築する。 ここで示す手順は CentOS 7 で実行することを前提としている。

> プライベート認証局はブローカーを実行している環境に構築する必要はない。

具体的な手順を以下に示す。

`openssl` パッケージをインストールする。
> インストール済であればスキップしてよい。
```bash
$ sudo yum -y install openssl
```

証明書や秘密鍵などを格納するディレクトリを作成する。

```bash
$ sudo mkdir -p /etc/pki/CA/certs /etc/pki/CA/crl /etc/pki/CA/newcerts /etc/pki/CA/private
```

OpenSSLの設定ファイルを変更し、プライベート認証局のために必要となる設定を行う。以下のパラメータを変更する。

* unique_subject
    * CA証明書のロールオーバーを簡単にするために `no` を指定する
* copy_extensions
    * 証明書のリクエストが SAN(subjectAltName)をコピーできるようにするために`copy`を指定する

`openssl.cnf`の `[ CA_default ]`セクションを以下のように書き換える。

> デフォルトの `openssl.cnf` では `copy_extensions`, `unique_subject`ともにコメントアウトされているので、
> 行頭のコメント記号 `#` を削除すればよい。

```
[ CA_default ]
(中略)
unique_subject  = no                    # Set to 'no' to allow creation of
                                        # several ctificates with same subject.
(中略)
# Extension copying option: use with caution.
copy_extensions = copy
```

プライベート認証局が署名した証明書を記録するためのファイル `index.txt` を作成する。

```bash
$ sudo touch /etc/pki/CA/index.txt
```

CA証明書のCSRと秘密鍵を作成する。

```bash
$ sudo openssl req -new -keyout /etc/pki/CA/private/cakey.pem \
       -out /etc/pki/CA/careq.pem -nodes \
       -subj /C=JP/ST=Example_State/O=Example_Organization/CN=private-ca
```

自己署名によるCA証明書を作成する。

```bash
$ sudo openssl ca -batch -in /etc/pki/CA/careq.pem -selfsign -extensions v3_ca \
       -keyfile /etc/pki/CA/private/cakey.pem -days 3650 -create_serial \
       -out /etc/pki/CA/cacert.pem
```

作成したCA証明書`/etc/pki/CA/cacert.pem`の内容を確認する。

```bash
$ openssl x509 -in /etc/pki/CA/cacert.pem -noout -text
```

以下のような出力内容が表示される。

```
Certificate:
    Data:
        Version: 3 (0x2)
        Serial Number:
            ef:f1:65:60:87:be:24:2d
    Signature Algorithm: sha256WithRSAEncryption
        Issuer: C=JP, ST=Example_State, O=Example_Organization, CN=private-ca
        Validity
            Not Before: Feb 01 00:00:00 2020 GMT
            Not After : Jan 31 00:00:00 2023 GMT
        Subject: C=JP, ST=Default_Organization, O=Default_Organization, CN=private-ca
(中略)
        X509v3 extensions:
            X509v3 Subject Key Identifier:
                73:16:96:F1:00:79:31:FE:16:FD:73:C7:E7:C9:99:02:7D:0C:50:68
            X509v3 Authority Key Identifier:
                keyid:73:16:96:F1:00:79:31:FE:16:FD:73:C7:E7:C9:99:02:7D:0C:50:68

            X509v3 Basic Constraints:
                CA:TRUE
(略)
```

## サーバ証明書の秘密鍵、証明書を作成する

サーバ証明書を作成する。

はじめに、証明書のSAN(subjectAltName)にサーバのホスト名を追加するための設定を `openssl.cnf` に行う。以下に示す内容を `openssl.cnf`に追記する。
> `DNS = `の後のホスト名を指定する箇所は実際の環境に合わせて変更すること。

```
(略)
[ req ]
(中略)
req_extensions = v3_req
(中略)
[ v3_req ]
subjectAltName = @alt_names
(中略)
[ alt_names ]
DNS = broker.example.org
```

サーバ証明書のCSRと秘密鍵を作成する。`-keyout`に秘密鍵の出力ファイル名、`-out`にCSRの出力ファイル名、
`-subj`に証明書のサブジェクトを指定する。

```bash
$ sudo openssl req -new -keyout /etc/pki/CA/private/broker.key \
       -out /etc/pki/CA/broker.csr -nodes -subj /C=JP/CN=broker.example.org
```

CA証明書で署名をおこない、サーバ証明書を作成する。`-keyfile`, `-cert`にCA証明書の秘密鍵と証明書のファイル名を、
`-in`にサーバ証明書のCSRを、`-out`に証明書の出力ファイル名を指定する。

```bash
$ sudo openssl ca -batch -keyfile /etc/pki/CA/private/cakey.pem \
      -cert /etc/pki/CA/cacert.pem -in /etc/pki/CA/broker.csr \
      -out /etc/pki/CA/certs/broker.crt -policy policy_anything
```

作成したサーバ証明書`/etc/pki/CA/certs/broker.crt`の内容を確認する。

```bash
$ openssl x509 -in /etc/pki/CA/certs/broker.crt -noout -text
```

以下のような出力内容が表示される。

```
Certificate:
    Data:
        Version: 3 (0x2)
        Serial Number:
            ef:f1:65:60:87:be:24:2e
    Signature Algorithm: sha256WithRSAEncryption
        Issuer: C=JP, ST=Example_State, O=Example_Organization, CN=private-ca
        Validity
            Not Before: Feb 01 00:00:00 2020 GMT
            Not After : Jan 31 00:00:00 2023 GMT
        Subject: C=JP, CN=broker
(中略)
        X509v3 extensions:
            X509v3 Basic Constraints:
                CA:FALSE
(中略)
            X509v3 Subject Alternative Name:
                DNS:broker.example.org
(後略)
```

#### クライアント認証のための秘密鍵、証明書を作成する

クライアント証明書のCSRと秘密鍵を作成する。`-keyout`に秘密鍵の出力ファイル名、`-out`にCSRの出力ファイル名、
`-subj`に証明書のサブジェクトを指定する。

```bash
$ sudo openssl req -new -keyout /etc/pki/CA/private/client0.key \
       -out /etc/pki/CA/client0.csr -nodes -subj /C=JP/CN=client0
```

CA証明書で署名をおこない、クライアント証明書を作成する。`-keyfile`, `-cert`にCA証明書の秘密鍵と証明書のファイル名を、
`-in`に証明書のCSRを、`-out`に証明書の出力ファイル名を指定する。

```bash
$ sudo openssl ca -batch -keyfile /etc/pki/CA/private/cakey.pem \
      -cert /etc/pki/CA/cacert.pem -in /etc/pki/CA/client0.csr \
      -out /etc/pki/CA/certs/client0.crt -policy policy_anything
```

作成したクライアント証明書`/etc/pki/CA/certs/client0.crt`の内容を確認する。

```bash
$ openssl x509 -in /etc/pki/CA/certs/client0.crt -noout -text
```

以下のような出力内容が表示される。

```
Certificate:
    Data:
        Version: 3 (0x2)
        Serial Number:
            ef:f1:65:60:87:be:24:2f
    Signature Algorithm: sha256WithRSAEncryption
        Issuer: C=JP, ST=Example_State, O=Example_Organization, CN=private-ca
        Validity
            Not Before: Feb 01 00:00:00 2020 GMT
            Not After : Jan 31 00:00:00 2023 GMT
        Subject: C=JP, CN=client0
(後略)
```
