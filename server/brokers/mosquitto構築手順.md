**準備中** (2020-06-05 14:26:44 JST)

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

[English](https://translate.google.com/translate?hl=en&sl=ja&tl=en&u=https://nii-gakunin-cloud.github.io/sinetstream/server/brokers/mosquitto構築手順.html "google translate")

# mosquitto(MQTT broker)の構築手順

`server` 上でdocker composeをつかってdockerコンテナでCentOS 7を立ち上げ、その中でmosquitto(MQTT broker)を起動する。

moqsuittoを実行するノード(`server`)と実行ユーザ(`user`)を決めて変数に設定する。


```bash
server=server1.example.jp
user=piyo
```

## 下準備

`server`でdockerとdocker-composeが動くのを確認する。


```bash
ssh ${user}@${server} 'docker info'
```

    Client:
     Debug Mode: false
    
    Server:
     Containers: 7
      Running: 3
      Paused: 0
      Stopped: 4
     Images: 27
     Server Version: 19.03.5
     Storage Driver: overlay2
      Backing Filesystem: xfs
      Supports d_type: true
      Native Overlay Diff: true
     Logging Driver: json-file
     Cgroup Driver: cgroupfs
     Plugins:
      Volume: local
      Network: bridge host ipvlan macvlan null overlay
      Log: awslogs fluentd gcplogs gelf journald json-file local logentries splunk syslog
     Swarm: inactive
     Runtimes: runc
     Default Runtime: runc
     Init Binary: docker-init
     containerd version: b34a5c8af56e510852c35414db4c1f4fa6172339
     runc version: 3e425f80a8c931f88e6d94a8c831b9d5aa481657
     init version: fec3683
     Security Options:
      seccomp
       Profile: default
     Kernel Version: 3.10.0-1062.9.1.el7.x86_64
     Operating System: CentOS Linux 7 (Core)
     OSType: linux
     Architecture: x86_64
     CPUs: 4
     Total Memory: 7.795GiB
     Name: server1.example.jp
     ID: BJXN:IKAF:66EJ:FL7M:YHS2:E2ET:DCLY:GVYR:COC3:5AAY:T2E4:S522
     Docker Root Dir: /var/lib/docker
     Debug Mode: false
     Registry: https://index.docker.io/v1/
     Labels:
     Experimental: false
     Insecure Registries:
      192.168.2.1:5000
      192.168.2.1:5001
      127.0.0.0/8
     Live Restore Enabled: false
    



```bash
ssh ${user}@${server} 'docker-compose version'
```

    docker-compose version 1.23.2, build 1110ad01
    docker-py version: 3.6.0
    CPython version: 3.6.7
    OpenSSL version: OpenSSL 1.1.0f  25 May 2017


## 設定ファイル

設定ファイルを用意するための一時ディレクトリを作成する。


```bash
work_dir=$(mktemp -d)
echo "${work_dir}"
mkdir ${work_dir}/config
ls -l ${work_dir}
```

    /tmp/tmp.UWsZ9aGSMC
    total 0
    drwxr-xr-x 2 jovyan users 6 Mar 27 17:34 config


docker-composeの設定ファイルを用意する。


```bash
cat > ${work_dir}/docker-compose.yml <<EOF
version: '3.7'
services:
  mosquitto:
    image: eclipse-mosquitto:1.6
    hostname: ${server}
    ports:
      - "8883:8883"
    volumes:
      - ./config:/mosquitto/config
      - ./pki:/etc/pki
      - /mosquitto/data
      - /mosquitto/log
EOF
cat ${work_dir}/docker-compose.yml
```

    version: '3.7'
    services:
      mosquitto:
        image: eclipse-mosquitto:1.6
        hostname: server1.example.jp
        ports:
          - "8883:8883"
        volumes:
          - ./config:/mosquitto/config
          - ./pki:/etc/pki
          - /mosquitto/data
          - /mosquitto/log


mosquittoの設定ファイルを用意する。


```bash
cat >${work_dir}/config/mosquitto.conf <<EOF
per_listener_settings true
listener 8883

persistence true
persistence_location /mosquitto/data/
log_dest file /mosquitto/log/mosquitto.log

EOF
cat ${work_dir}/config/mosquitto.conf
```

    per_listener_settings true
    listener 8883
    
    persistence true
    persistence_location /mosquitto/data/
    log_dest file /mosquitto/log/mosquitto.log
    


## 認証の設定

TLSで通信路を暗号化したうえでのパスワード認証の設定をおこなう。

### パスワードの設定

認証のためのユーザとパスワードを書いたファイルを用意する。
フォーマットは行単位でユーザ名とパスワードをコロンで区切る。

この段階ではパスワードは平文だがが後の手順でハッシュする。


```bash
cat >${work_dir}/config/mosquitto.passwd <<EOF
user01:user01-pass
user02:user02-pass
user03:user03-pass
EOF
cat ${work_dir}/config/mosquitto.passwd
```

    user01:user01-pass
    user02:user02-pass
    user03:user03-pass



```bash
cat >>${work_dir}/config/mosquitto.conf <<EOF
password_file /mosquitto/config/mosquitto.passwd

EOF
cat ${work_dir}/config/mosquitto.conf
```

    persistence true
    persistence_location /mosquitto/data/
    log_dest file /mosquitto/log/mosquitto.log
    per_listener_settings true
    listener 8883
    
    password_file /mosquitto/config/mosquitto.passwd
    


### TLSの設定

TLSで通信を暗号化するための秘密鍵と証明書を設定する。

CA証明書とmosquitto用のサーバ秘密鍵とサーバ証明書は事前に作成しておく
ここではファイル名を
* CA証明書: `./cacert.pem`
* サーバ秘密鍵: `./broker.key`
* サーバ証明書: `./broker.crt`

とする。

> 本来ならmosquittoを動かすコンテナのなかでサーバの秘密鍵とCSRを作成して、
> CSRをCAに渡して証明書を受け取るの安全だが、
> 簡単のため別の場所で秘密鍵と証明書を作成する手順となっている。


```bash
cat >>${work_dir}/config/mosquitto.conf <<EOF
cafile /etc/pki/CA/cacert.pem
certfile /etc/pki/CA/certs/broker.crt
keyfile /etc/pki/CA/private/broker.key
require_certificate false

EOF
cat ${work_dir}/config/mosquitto.conf
```

    persistence true
    persistence_location /mosquitto/data/
    log_dest file /mosquitto/log/mosquitto.log
    per_listener_settings true
    listener 8883
    
    password_file /mosquitto/config/mosquitto.passwd
    
    cafile /etc/pki/CA/cacert.pem
    certfile /etc/pki/CA/certs/broker.crt
    keyfile /etc/pki/CA/private/broker.key
    require_certificate false
    


サーバに秘密鍵と証明書をコピーする。


```bash
ssh ${user}@${server} 'mkdir -p mosquitto/pki/CA mosquitto/pki/CA/certs mosquitto/pki/CA/private'
scp ./cacert.pem ${user}@${server}:mosquitto/pki/CA/cacert.pem
scp ./broker.crt ${user}@${server}:mosquitto/pki/CA/certs/broker.crt
scp ./broker.key ${user}@${server}:mosquitto/pki/CA/private/broker.key
```

    cacert.pem                                    100% 4349     3.7MB/s   00:00    
    broker.crt                                    100% 4389     4.6MB/s   00:00    
    broker.key                                    100% 1704     1.4MB/s   00:00    


## 認可の設定

認可のためのACLファイルを用意する。
フォーマットは2行単位で

`user` ユーザ名<br>
`topic` `read`または`write`または`readwrite` `#`<br>

`topic`のあとに`read`を書くとそのユーザはメッセージを読めるけど書かけない、
`write`だと書けるけど読めない、
`readwrite`だと読み書きできる指定になる。

最後の`#`は任意のトピック名の意味する特殊文字(マルチレベル・ワイルドカード)である。


```bash
cat >${work_dir}/config/mosquitto.acl <<EOF
user user01
topic write #

user user02
topic read #

user user03
topic readwrite #

EOF
cat ${work_dir}/config/mosquitto.acl
```

    user user01
    topic write #
    
    user user02
    topic read #
    
    user user03
    topic readwrite #
    



```bash
cat >>${work_dir}/config/mosquitto.conf <<EOF
acl_file /mosquitto/config/mosquitto.acl

EOF
cat ${work_dir}/config/mosquitto.conf
```

    persistence true
    persistence_location /mosquitto/data/
    log_dest file /mosquitto/log/mosquitto.log
    per_listener_settings true
    listener 8883
    
    password_file /mosquitto/config/mosquitto.passwd
    
    cafile /etc/pki/CA/cacert.pem
    certfile /etc/pki/CA/certs/broker.crt
    keyfile /etc/pki/CA/private/broker.key
    require_certificate false
    
    acl_file /mosquitto/config/mosquitto.acl
    


## mosquittoの起動

`server` 上にmosquittoがつかうディレクトリを用意する。


```bash
ssh ${user}@${server} 'mkdir -p mosquitto && ls -ld mosquitto'
```

    drwxrwxr-x 4 piyo piyo 57 Mar 27 17:10 mosquitto


一時ディレクトリに用意した設定ファイルを`server`上にコピーする。


```bash
scp -r ${work_dir}/docker-compose.yml ${work_dir}/config ${user}@${server}:mosquitto/
ssh ${user}@${server} 'ls -laR mosquitto/'
```

    docker-compose.yml                            100%  253   245.2KB/s   00:00    
    mosquitto.conf                                100%  366   365.4KB/s   00:00    
    mosquitto.passwd                              100%   57    13.3KB/s   00:00    
    mosquitto.acl                                 100%   84   128.6KB/s   00:00    
    mosquitto/:
    total 8
    drwxrwxr-x  4 piyo piyo   57 Mar 27 17:10 .
    drwxr-xr-x 33 piyo piyo 4096 Mar 27 17:30 ..
    drwxr-xr-x  2 piyo piyo   73 Mar 27 17:26 config
    -rw-r--r--  1 piyo piyo  253 Mar 27 17:35 docker-compose.yml
    drwxrwxr-x  3 piyo piyo   16 Mar 27 17:09 pki
    
    mosquitto/config:
    total 12
    drwxr-xr-x 2 piyo piyo  73 Mar 27 17:26 .
    drwxrwxr-x 4 piyo piyo  57 Mar 27 17:10 ..
    -rw-r--r-- 1 piyo piyo  84 Mar 27 17:35 mosquitto.acl
    -rw-r--r-- 1 piyo piyo 366 Mar 27 17:35 mosquitto.conf
    -rw-r--r-- 1 piyo piyo  57 Mar 27 17:35 mosquitto.passwd
    
    mosquitto/pki:
    total 0
    drwxrwxr-x 3 piyo piyo 16 Mar 27 17:09 .
    drwxrwxr-x 4 piyo piyo 57 Mar 27 17:10 ..
    drwxrwxr-x 4 piyo piyo 52 Mar 27 17:30 CA
    
    mosquitto/pki/CA:
    total 8
    drwxrwxr-x 4 piyo piyo   52 Mar 27 17:30 .
    drwxrwxr-x 3 piyo piyo   16 Mar 27 17:09 ..
    -rw-r--r-- 1 piyo piyo 4349 Mar 27 17:35 cacert.pem
    drwxrwxr-x 2 piyo piyo   24 Mar 27 17:30 certs
    drwxrwxr-x 2 piyo piyo   24 Mar 27 17:30 private
    
    mosquitto/pki/CA/certs:
    total 8
    drwxrwxr-x 2 piyo piyo   24 Mar 27 17:30 .
    drwxrwxr-x 4 piyo piyo   52 Mar 27 17:30 ..
    -rw-r--r-- 1 piyo piyo 4389 Mar 27 17:35 broker.crt
    
    mosquitto/pki/CA/private:
    total 4
    drwxrwxr-x 2 piyo piyo   24 Mar 27 17:30 .
    drwxrwxr-x 4 piyo piyo   52 Mar 27 17:30 ..
    -rw-r--r-- 1 piyo piyo 1704 Mar 27 17:35 broker.key


`server`にコピーしたパスワードをハッシュする。


```bash
ssh ${user}@${server} 'cd mosquitto && docker run -v $PWD/config:/mosquitto/config eclipse-mosquitto:1.6 mosquitto_passwd -U /mosquitto/config/mosquitto.passwd'
ssh ${user}@${server} 'cat mosquitto/config/mosquitto.passwd'
```

    user01:$6$Z5MQud/y0Ume9EPH$A8bplGBWKeYFpbDS91/NNGbX80aCaKAEDr7wOhdswPYvOsaiGh446+6IF5fag7EjV8GzirudJsQOV3ubA4sh1Q==
    user02:$6$HSfbzGThVLJgc8jl$gCmkqg0+Q4qyVzwZCRf3Ynsjxc/MGtjHvMT6vY4CRVOLphp1my7jLLc9VZpVo1ga1TknYj3t26Pzcg+zsQpuFw==
    user03:$6$Dw4TN0L41wXi2bH2$MQvotJU1bEjanrhpQ9PHkB+T3ZYp9wrI4rxE2mHYVvrbaZJfCCJASCZ24CdWVHJcf9cXvEG/7Wd8/j32HlIAgg==


mosquittoを起動する。


```bash
ssh ${user}@${server} 'cd mosquitto && docker-compose up -d'
ssh ${user}@${server} 'cd mosquitto && docker-compose ps'
```

## ユーザ追加

mosquitto起動後にユーザを追加するには `mosquitto_passwd` コマンドでユーザをパスワードファイルに追加した後、
mosquittoを再起動して新しいパスワードファイルを反映する必要がある。
ここではコンテナごと再起動している。


```bash
ssh ${user}@${server} 'cd mosquitto && docker run -v $PWD/config:/mosquitto/config eclipse-mosquitto:1.6 mosquitto_passwd -b /mosquitto/config/mosquitto.passwd user04 user04-pass'
ssh ${user}@${server} 'cd mosquitto && cat >> config/mosquitto.acl && cat config/mosquitto.acl' << EOF
user user04
topic readwrite +

EOF
ssh ${user}@${server} 'cd mosquitto && docker-compose restart'
```
