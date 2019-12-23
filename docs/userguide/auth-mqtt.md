**準備中** (2019-12-23 18:55:32 JST)

<!--
Copyright (C) 2019 National Institute of Informatics

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

SINETStream ユーザガイド

# MQTTの認証と認可

MQTT で認証の機能を使うには、以下の作業が必要である。

* Mosquitto (MQTT ブローカー) を設定する。
* SINETStream クライアントの設定ファイル `.sinetstream_config.yml` を書く

### Mosquitto (MQTT ブローカー) の認証の設定

#### `mosquitto.conf` を編集してパスワードファイルを指定する

```
password_file /path/to/password
```

#### パスワードファイル `password` に最初のユーザを登録する

```
$ mosquitto_passwd -c /path/to/password <USER1>
Password: <PASSWORD1>
reenter password: <PASSWORD1>
```

補足: パスワードファイルはテキストファイルだがパスワードはハッシュ化されている。

#### ブローカーを再起動する

#### ユーザーの追加方法

パスワードファイルにユーザを追加したあと、以下のコマンドを実行する。

```
$ mosquitto_passwd -b /path/to/password <USER2> <PASSWORD2>
```

Mosquitto プロセスに SIGHUP を送ってパスワードファイルを再読み込みさせる。

```
pkill -HUP mosquitto
```

### MQTT 使用時の SINETStream クライアントの設定

`.sinetstream_config.yml` に設定を書く。

* 以下のポート番号 `1883` はブローカーの設定に合わせる。
* 以下のブローカーホスト `localhost` はブローカーのホスト名に合わせる。
* 以下の `service-foo` は変更してよい。

```
service-foo:
    broker:
        - localhost:1883
    username_pw_set:
        username: <USER1>
        password: <PASSWORD1>
```

## MQTTの認可の設定

### ブローカーの設定ファイル `mosquitto.conf` に認可の設定を追加したあとブローカーを再起動する。

```
acl_file /path/to/aclfile
```

### 認可の設定ファイル `aclfile` にユーザの権限を追加したあとブローカにSIGHUPを送って再読み込みさせる。

aclfileは、あるユーザがどのトピックに読み書きしてよいかを記述する形式が基本である。

ユーザにパブリッシュもサブスクライブも許可する設定:

```
user <USER>
topic readwrite <TOPIC>
```

ユーザにパブリッシュだけ許可する設定:

```
user <USER>
topic write <TOPIC>
```

ユーザにサブスクライブだけ許可する設定:

```
user <USER>
topic read <TOPIC>
```

ユーザ1にパブリッシュだけを許可してユーザ2にサブスクライブだけを許可する設定:

```
user <USER1>
topic write <TOPIC-A>
user <USER2>
topic read <TOPIC-B>
```

トピック名に `+` または `#` を書くとワイルドカードを指定したことになる。

## 参考

* http://www.steves-internet-guide.com/mqtt-username-password-example/
* https://mosquitto.org/man/mosquitto_passwd-1.html
* https://mosquitto.org/man/mosquitto-conf-5.html
* http://www.steves-internet-guide.com/topic-restriction-mosquitto-configuration/

## MQTT設定例

### MQTT ブローカーの設定

#### Mosquitto をダウンロードしてビルドする。

```
$ wget -q https://mosquitto.org/files/source/mosquitto-1.6.2.tar.gz
$ ls mosquitto-1.6.2.tar.gz
$ tar -xzf mosquitto-1.6.2.tar.gz
$ cd mosquitto-1.6.2
$ make
...
$
```

#### 設定ファイル

##### 設定ファイルを編集する

```
$ echo '
password_file ./mosquitto.passwd
acl_file ./mosquitto.acl
' >> mosquitto.conf
$
```

##### ユーザーを登録する

```
$ src/mosquitto_passwd -c mosquitto.passwd alice
Password: alice-secret
Reenter password: alice-secret
$
```

他のユーザーを登録するには:

```
$ src/mosquitto_passwd -b mosquitto.passwd bob bob-secret
$
```

##### ACL ファイルを作成する

```
$ echo '
user alice
topic readwrite #
' > mosquitto.acl
$
```

#### MQTT ブローカーを立ち上げる

```
$src/mosquitto -c mosquitto.conf
1570609944: mosquitto version 1.6.2 starting
1570609944: Config loaded from mosquitto.conf.
1570609944: Opening ipv4 listen socket on port 1883.
1570609944: Opening ipv6 listen socket on port 1883.
```

### 動作確認

SINETStream はすでにインストールされているものとする。

#### `.sinetstream_config.yml` を編集する

```
$ echo '
local-mqtt-authn:
  type: mqtt
  brokers:
    - localhost:1883
  username_pw_set:
    username: alice
    password: alice-secret
  topic: test1
  value_type: text
' >> .sinetstream_config.yml
$
```

#### Readerを実行する

Python:

```
$ python3 ~/repo/sinetstream-x.x.x/python/sample/text/consumer.py -s local-mqtt-authn
# service=local-mqtt-authn
# topics=test1
# group-id=None
```

メッセージを受信すると:

```
topic=test1 value(utf-8)='hogehoge'
```

Java:

```
$ /path/to/sinetstream-x.x.x/java/sample/text/text-producer-x.x.x/bin/text-consumer -s local-mqtt-authn
```

メッセージを受信すると:

```
hogehoge
```

#### Writerを実行する

Python:

```
[koie@vm00 tmp]$ echo "hogehoge" | python3 ~/repo/sinetstream-x.x.x/python/sample/text/producer.py -s local-mqtt-authn
# service=local-mqtt-authn
# topic=test1
$
```

Java:

```
$ echo "hogehoge" | /path/to/sinetstream-x.x.x/java/sample/text/text-producer-x.x.x/bin/text-producer -s local-mqtt-authn
$
```
