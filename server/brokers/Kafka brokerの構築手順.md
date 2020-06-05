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

[English](https://translate.google.com/translate?hl=en&sl=ja&tl=en&u=https://nii-gakunin-cloud.github.io/sinetstream/server/brokers/Kafka brokerの構築手順.html "google translate")

**準備中** (2020-06-05 18:05:13 JST)

# Kafka brokerの構築手順


```bash
set -o pipefail
```

## Ansibleの設定

ansibleをつかってKafka brokerクラスタ(とzookeeperクラスタ)を構築する。

ansibleのインベントリファイルを作成する。


```bash
cat >inventory.yml <<EOF
all:
    children:
        kafka:
            hosts:
                server1.example.jp:
                #ここにKafkaをうごかすホストを羅列する。行末のコロンを忘れずに
            vars:
                ansible_user: piyo  #実行ユーザは変更する
                ansible_ssh_private_key_file: ~/.ssh/id_rsa
                ansible_python_interpreter: /usr/bin/python3
        zookeeper:
            hosts:
                server1.example.jp:
                #ここにZookeeperをうごかすホストを羅列する。行末のコロンを忘れずに
            vars:
                ansible_user: piyo  #実行ユーザは変更する
                ansible_ssh_private_key_file: ~/.ssh/id_rsa
                ansible_python_interpreter: /usr/bin/python3
EOF
```

ansibleの設定ファイルを作成する。


```bash
cat >ansible.cfg <<EOF
[defaults]
command_warnings = False
inventory = ./inventory.yml
EOF
```

ansibleを通じてzookeeperとkafkaを実行するホストにアクセスできるのを確認する。


```bash
ansible all -m ping
```

    server1.example.jp | SUCCESS => {
        "changed": false,
        "ping": "pong"
    }


Dockerがインストールされているのを確認する。


```bash
ansible all -m command -a "docker version"
```

    server1.example.jp | CHANGED | rc=0 >>
    Client: Docker Engine - Community
     Version:           19.03.5
     API version:       1.40
     Go version:        go1.12.12
     Git commit:        633a0ea
     Built:             Wed Nov 13 07:25:41 2019
     OS/Arch:           linux/amd64
     Experimental:      false
    
    Server: Docker Engine - Community
     Engine:
      Version:          19.03.5
      API version:      1.40 (minimum version 1.12)
      Go version:       go1.12.12
      Git commit:       633a0ea
      Built:            Wed Nov 13 07:24:18 2019
      OS/Arch:          linux/amd64
      Experimental:     false
     containerd:
      Version:          1.2.10
      GitCommit:        b34a5c8af56e510852c35414db4c1f4fa6172339
     runc:
      Version:          1.0.0-rc8+dev
      GitCommit:        3e425f80a8c931f88e6d94a8c831b9d5aa481657
     docker-init:
      Version:          0.18.0
      GitCommit:        fec3683


## ZooKeeperクラスタの構築

zookeeperのdockerイメージ名とポート番号を設定する。
PPORT,LPORT,CPORTはほかのサービスのポート番号とぶつかっているのでなければ変更する必要はない。
zookeeperの仕様によりCPORTは変更できない。


```bash
DOCKER_IMAGE="zookeeper"

ZK_PPORT=12888      # peer
ZK_LPORT=13888      # leader
ZK_CPORT=2181      # client
```

zookeeperを起動するスクリプトを生成する。


```bash
LIST_ZOOKEEPER_HOSTS="$(ansible-inventory --list  | jq  -r '.zookeeper.hosts|.[]')"
list_zookeeper_hosts() {
    echo "$LIST_ZOOKEEPER_HOSTS"
}

print_servers() {
    local MYID="$1"
    local HOST
    local ID=1
    local SERVER
    list_zookeeper_hosts | while read HOST; do
        if [ "$ID" = "$MYID" ]; then
            local ANYADDR="0.0.0.0"
            HOST="$ANYADDR"
        fi
        printf "server.$ID=$HOST:$ZK_PPORT:$ZK_LPORT "
        ID=$((ID + 1))
    done
    printf "\n"
}

print_docker_run() {
    local DIR="$1"
    local ID=1
    list_zookeeper_hosts | while read HOST; do
        #local NAME="sinetstream-zookeeper-$ID"
        local NAME="sinetstream-zookeeper"
        local SERVERS="$(print_servers "$ID")"
        {
            printf "docker run --rm --detach --name '$NAME' --env 'ZOO_MY_ID=$ID' --env 'ZOO_SERVERS=$SERVERS' --publish $ZK_PPORT:$ZK_PPORT --publish $ZK_LPORT:$ZK_LPORT --publish $ZK_CPORT:$ZK_CPORT $DOCKER_IMAGE"
        } > "$DIR/zookeeper-docker_run-${HOST}.sh"
        ID=$((ID + 1))
    done
}

mkdir -p tmp  &&
rm -f tmp/*.sh  &&
print_docker_run tmp  &&
ls -l tmp/*.sh
```

    -rw-r--r-- 1 jovyan users 199 May 12 18:40 tmp/zookeeper-docker_run-server1.example.jp.sh


ansibleをつかってzookeeperサーバーを起動する。


```bash
ansible zookeeper -m script -a 'tmp/zookeeper-docker_run-{{inventory_hostname}}.sh'
```

    server1.example.jp | CHANGED => {
        "changed": true,
        "rc": 0,
        "stderr": "Shared connection to server1.example.jp closed.\r\n",
        "stderr_lines": [
            "Shared connection to server1.example.jp closed."
        ],
        "stdout": "5b920416deb6d59169840a4643fc7ed1e6a0311fd5824f532b19aafbe03858cf\r\n",
        "stdout_lines": [
            "5b920416deb6d59169840a4643fc7ed1e6a0311fd5824f532b19aafbe03858cf"
        ]
    }



```bash
ansible zookeeper -m command -a 'docker ps --filter "name=sinetstream-zookeeper"'
```

    server1.example.jp | CHANGED | rc=0 >>
    CONTAINER ID        IMAGE               COMMAND                  CREATED             STATUS              PORTS                                                                                            NAMES
    5b920416deb6        zookeeper           "/docker-entrypoint.…"   3 seconds ago       Up 1 second         2888/tcp, 0.0.0.0:2181->2181/tcp, 0.0.0.0:12888->12888/tcp, 0.0.0.0:13888->13888/tcp, 3888/tcp   sinetstream-zookeeper


## Kafkaクラスタ

公式のKafka一式をダウンロードする。
手元でダウンロードしてから各ホストにコピーする。


```bash
KAFKA="kafka_2.12-2.4.1"
```


```bash
wget --mirror http://ftp.kddilabs.jp/infosystems/apache/kafka/2.4.1/$KAFKA.tgz
```

    --2020-05-12 18:40:18--  http://ftp.kddilabs.jp/infosystems/apache/kafka/2.4.1/kafka_2.12-2.4.1.tgz
    Resolving ftp.kddilabs.jp (ftp.kddilabs.jp)... 192.26.91.193, 2001:200:601:10:206:5bff:fef0:466c
    Connecting to ftp.kddilabs.jp (ftp.kddilabs.jp)|192.26.91.193|:80... connected.
    HTTP request sent, awaiting response... 304 Not Modified
    File ‘ftp.kddilabs.jp/infosystems/apache/kafka/2.4.1/kafka_2.12-2.4.1.tgz’ not modified on server. Omitting download.
    



```bash
ansible kafka -m command -a "mkdir -p \$PWD/sinetstream-kafka"
```

    server1.example.jp | CHANGED | rc=0 >>
    



```bash
ansible kafka -m copy -a "src=$KAFKA.tgz dest=\$PWD/sinetstream-kafka/"
```

    server1.example.jp | SUCCESS => {
        "changed": false,
        "checksum": "d043d80b62dff190c22d11f4afbe8c59827ba7a5",
        "dest": "/home/piyo/sinetstream-kafka/kafka_2.12-2.4.1.tgz",
        "gid": 1004,
        "group": "piyo",
        "mode": "0664",
        "owner": "piyo",
        "path": "/home/piyo/sinetstream-kafka/kafka_2.12-2.4.1.tgz",
        "size": 62358954,
        "state": "file",
        "uid": 1004
    }


### KafkaブローカーをうごかすCentOSのコンテナを作成

認証方法をどれかひとつ選択する。


```bash
#KAFKA_AUTH=SSL        # SSL/TLS認証（クライアント認証）; 通信にTLSをつかい、認証に証明書をつかう
KAFKA_AUTH=SASL_SSL_SCRAM  # SCRAM認証/TLS; 通信にTLSをつかい、認証にSCRAM(パスワード)をつかう
#KAFKA_AUTH=SASL_SSL_PLAIN  # パスワード認証/TLS; 通信にTLSをつかい、認証に平文パスワードをつかう
#KAFKA_AUTH=PLAINTEXT # 通信は暗号化されず、認証もない ※つかってはいけない
```

truststore/keystoreを保護するためのパスワードを設定する。パスワードは適当に強度の高い文字列を指定する。


```bash
TRUSTSTORE_PASSWORD="trust-pass-00"
KEYSTORE_PASSWORD="key-pass-00"
```

SCAM認証やパスワード認証を使う場合には、ユーザーのリストと各ユーザのパスワードを設定する。
SSL/TLS認証を使う場合はパスワードは設定しなくてよく、ユーザのリストだけを設定する。SSL/TLS認証でのユーザ名は証明書のCommon Nameである。

ユーザ `admin` はkafkaブローカ間の通信につかう特別なユーザなので消してはいけない。
パスワードは十分な強度をもったものに変更すべきである。


```bash
USER_LIST="user01 user02 user03 CN=client0,C=JP"
PASSWORD_admin="admin-pass"
PASSWORD_user01="user01-pass"
PASSWORD_user02="user02-pass"
PASSWORD_user03="user03-pass"
```

認可(ACL)の設定。


```bash
KAFKA_ACL_DEFAULT_TO_ALLOW="false"  # trueに設定するとACLが設定されていないユーザはアクセスが許可される。
ACL_user01="readwrite"
ACL_user02="write"
ACL_user03="read"
ACL_CN_client0_C_JP="readwrite"  # 英数字以外は _ に置き換えて
```
Kafkaブローカのポート番号を設定する。ほかのサービスとぶつかっていなければ変更しなくてよい。

```bash
KAFKA_PORT_SSL=9093    
KAFKA_PORT_SASL_SSL=9093
```

認証方法の詳細なパラメータを設定する。


```bash
SCRAM_MECHANISM="SCRAM-SHA-256"
```

Kafkaブローカを動かすコンテナを作る。


```bash
ansible kafka -m command -a "docker run \
    --detach \
    --interactive \
    --net host \
    --name sinetstream-kafka \
    --volume \$PWD/sinetstream-kafka:/sinetstream-kafka \
    centos:7"
```

    server1.example.jp | CHANGED | rc=0 >>
    642b24558f7629a0a071f65bac082644fd61fc40dac92d912b08a75a097f7268



```bash
ansible kafka -m command -a "docker exec sinetstream-kafka true"
```

    server1.example.jp | CHANGED | rc=0 >>
    


コンテナにKafkaの実行に必要なソフトウェアをインストールする。


```bash
ansible kafka -m command -a "docker exec sinetstream-kafka yum update -y"
```

    server1.example.jp | CHANGED | rc=0 >>
    Loaded plugins: fastestmirror, ovl
    Determining fastest mirrors
     * base: ty1.mirror.newmediaexpress.com
     * extras: ty1.mirror.newmediaexpress.com
     * updates: ty1.mirror.newmediaexpress.com
    Resolving Dependencies
    --> Running transaction check
    ---> Package acl.x86_64 0:2.2.51-14.el7 will be updated
    ---> Package acl.x86_64 0:2.2.51-15.el7 will be an update
    ---> Package bash.x86_64 0:4.2.46-33.el7 will be updated
    ---> Package bash.x86_64 0:4.2.46-34.el7 will be an update
    ---> Package bind-license.noarch 32:9.11.4-9.P2.el7 will be updated
    ---> Package bind-license.noarch 32:9.11.4-16.P2.el7_8.2 will be an update
    ---> Package binutils.x86_64 0:2.27-41.base.el7 will be updated
    ---> Package binutils.x86_64 0:2.27-43.base.el7 will be an update
    ---> Package ca-certificates.noarch 0:2018.2.22-70.0.el7_5 will be updated
    ---> Package ca-certificates.noarch 0:2019.2.32-76.el7_7 will be an update
    ---> Package centos-release.x86_64 0:7-7.1908.0.el7.centos will be updated
    ---> Package centos-release.x86_64 0:7-8.2003.0.el7.centos will be an update
    ---> Package cryptsetup-libs.x86_64 0:2.0.3-5.el7 will be updated
    ---> Package cryptsetup-libs.x86_64 0:2.0.3-6.el7 will be an update
    ---> Package curl.x86_64 0:7.29.0-54.el7 will be updated
    ---> Package curl.x86_64 0:7.29.0-57.el7 will be an update
    ---> Package device-mapper.x86_64 7:1.02.158-2.el7 will be updated
    ---> Package device-mapper.x86_64 7:1.02.164-7.el7_8.1 will be an update
    ---> Package device-mapper-libs.x86_64 7:1.02.158-2.el7 will be updated
    ---> Package device-mapper-libs.x86_64 7:1.02.164-7.el7_8.1 will be an update
    ---> Package dracut.x86_64 0:033-564.el7 will be updated
    ---> Package dracut.x86_64 0:033-568.el7 will be an update
    ---> Package elfutils-default-yama-scope.noarch 0:0.176-2.el7 will be updated
    ---> Package elfutils-default-yama-scope.noarch 0:0.176-4.el7 will be an update
    ---> Package elfutils-libelf.x86_64 0:0.176-2.el7 will be updated
    ---> Package elfutils-libelf.x86_64 0:0.176-4.el7 will be an update
    ---> Package elfutils-libs.x86_64 0:0.176-2.el7 will be updated
    ---> Package elfutils-libs.x86_64 0:0.176-4.el7 will be an update
    ---> Package expat.x86_64 0:2.1.0-10.el7_3 will be updated
    ---> Package expat.x86_64 0:2.1.0-11.el7 will be an update
    ---> Package file-libs.x86_64 0:5.11-35.el7 will be updated
    ---> Package file-libs.x86_64 0:5.11-36.el7 will be an update
    ---> Package glibc.x86_64 0:2.17-292.el7 will be updated
    ---> Package glibc.x86_64 0:2.17-307.el7.1 will be an update
    ---> Package glibc-common.x86_64 0:2.17-292.el7 will be updated
    ---> Package glibc-common.x86_64 0:2.17-307.el7.1 will be an update
    ---> Package hostname.x86_64 0:3.13-3.el7 will be updated
    ---> Package hostname.x86_64 0:3.13-3.el7_7.1 will be an update
    ---> Package kmod.x86_64 0:20-25.el7 will be updated
    ---> Package kmod.x86_64 0:20-28.el7 will be an update
    ---> Package kmod-libs.x86_64 0:20-25.el7 will be updated
    ---> Package kmod-libs.x86_64 0:20-28.el7 will be an update
    ---> Package kpartx.x86_64 0:0.4.9-127.el7 will be updated
    ---> Package kpartx.x86_64 0:0.4.9-131.el7 will be an update
    ---> Package krb5-libs.x86_64 0:1.15.1-37.el7_7.2 will be updated
    ---> Package krb5-libs.x86_64 0:1.15.1-46.el7 will be an update
    ---> Package libacl.x86_64 0:2.2.51-14.el7 will be updated
    ---> Package libacl.x86_64 0:2.2.51-15.el7 will be an update
    ---> Package libblkid.x86_64 0:2.23.2-61.el7 will be updated
    ---> Package libblkid.x86_64 0:2.23.2-63.el7 will be an update
    ---> Package libcap.x86_64 0:2.22-10.el7 will be updated
    ---> Package libcap.x86_64 0:2.22-11.el7 will be an update
    ---> Package libcom_err.x86_64 0:1.42.9-16.el7 will be updated
    ---> Package libcom_err.x86_64 0:1.42.9-17.el7 will be an update
    ---> Package libcurl.x86_64 0:7.29.0-54.el7 will be updated
    ---> Package libcurl.x86_64 0:7.29.0-57.el7 will be an update
    ---> Package libffi.x86_64 0:3.0.13-18.el7 will be updated
    ---> Package libffi.x86_64 0:3.0.13-19.el7 will be an update
    ---> Package libmount.x86_64 0:2.23.2-61.el7 will be updated
    ---> Package libmount.x86_64 0:2.23.2-63.el7 will be an update
    ---> Package libselinux.x86_64 0:2.5-14.1.el7 will be updated
    ---> Package libselinux.x86_64 0:2.5-15.el7 will be an update
    ---> Package libsmartcols.x86_64 0:2.23.2-61.el7 will be updated
    ---> Package libsmartcols.x86_64 0:2.23.2-63.el7 will be an update
    ---> Package libuuid.x86_64 0:2.23.2-61.el7 will be updated
    ---> Package libuuid.x86_64 0:2.23.2-63.el7 will be an update
    ---> Package libxml2.x86_64 0:2.9.1-6.el7_2.3 will be updated
    ---> Package libxml2.x86_64 0:2.9.1-6.el7.4 will be an update
    ---> Package libxml2-python.x86_64 0:2.9.1-6.el7_2.3 will be updated
    ---> Package libxml2-python.x86_64 0:2.9.1-6.el7.4 will be an update
    ---> Package nss.x86_64 0:3.44.0-4.el7 will be updated
    ---> Package nss.x86_64 0:3.44.0-7.el7_7 will be an update
    ---> Package nss-softokn.x86_64 0:3.44.0-5.el7 will be updated
    ---> Package nss-softokn.x86_64 0:3.44.0-8.el7_7 will be an update
    ---> Package nss-softokn-freebl.x86_64 0:3.44.0-5.el7 will be updated
    ---> Package nss-softokn-freebl.x86_64 0:3.44.0-8.el7_7 will be an update
    ---> Package nss-sysinit.x86_64 0:3.44.0-4.el7 will be updated
    ---> Package nss-sysinit.x86_64 0:3.44.0-7.el7_7 will be an update
    ---> Package nss-tools.x86_64 0:3.44.0-4.el7 will be updated
    ---> Package nss-tools.x86_64 0:3.44.0-7.el7_7 will be an update
    ---> Package nss-util.x86_64 0:3.44.0-3.el7 will be updated
    ---> Package nss-util.x86_64 0:3.44.0-4.el7_7 will be an update
    ---> Package pam.x86_64 0:1.1.8-22.el7 will be updated
    ---> Package pam.x86_64 0:1.1.8-23.el7 will be an update
    ---> Package passwd.x86_64 0:0.79-5.el7 will be updated
    ---> Package passwd.x86_64 0:0.79-6.el7 will be an update
    ---> Package procps-ng.x86_64 0:3.3.10-26.el7 will be updated
    ---> Package procps-ng.x86_64 0:3.3.10-27.el7 will be an update
    ---> Package python.x86_64 0:2.7.5-86.el7 will be updated
    ---> Package python.x86_64 0:2.7.5-88.el7 will be an update
    ---> Package python-libs.x86_64 0:2.7.5-86.el7 will be updated
    ---> Package python-libs.x86_64 0:2.7.5-88.el7 will be an update
    ---> Package python-urlgrabber.noarch 0:3.10-9.el7 will be updated
    ---> Package python-urlgrabber.noarch 0:3.10-10.el7 will be an update
    ---> Package rpm.x86_64 0:4.11.3-40.el7 will be updated
    ---> Package rpm.x86_64 0:4.11.3-43.el7 will be an update
    ---> Package rpm-build-libs.x86_64 0:4.11.3-40.el7 will be updated
    ---> Package rpm-build-libs.x86_64 0:4.11.3-43.el7 will be an update
    ---> Package rpm-libs.x86_64 0:4.11.3-40.el7 will be updated
    ---> Package rpm-libs.x86_64 0:4.11.3-43.el7 will be an update
    ---> Package rpm-python.x86_64 0:4.11.3-40.el7 will be updated
    ---> Package rpm-python.x86_64 0:4.11.3-43.el7 will be an update
    ---> Package sed.x86_64 0:4.2.2-5.el7 will be updated
    ---> Package sed.x86_64 0:4.2.2-6.el7 will be an update
    ---> Package setup.noarch 0:2.8.71-10.el7 will be updated
    ---> Package setup.noarch 0:2.8.71-11.el7 will be an update
    ---> Package shared-mime-info.x86_64 0:1.8-4.el7 will be updated
    ---> Package shared-mime-info.x86_64 0:1.8-5.el7 will be an update
    ---> Package sqlite.x86_64 0:3.7.17-8.el7 will be updated
    ---> Package sqlite.x86_64 0:3.7.17-8.el7_7.1 will be an update
    ---> Package systemd.x86_64 0:219-67.el7_7.1 will be updated
    ---> Package systemd.x86_64 0:219-73.el7_8.5 will be an update
    ---> Package systemd-libs.x86_64 0:219-67.el7_7.1 will be updated
    ---> Package systemd-libs.x86_64 0:219-73.el7_8.5 will be an update
    ---> Package tzdata.noarch 0:2019b-1.el7 will be updated
    ---> Package tzdata.noarch 0:2020a-1.el7 will be an update
    ---> Package util-linux.x86_64 0:2.23.2-61.el7 will be updated
    ---> Package util-linux.x86_64 0:2.23.2-63.el7 will be an update
    ---> Package yum.noarch 0:3.4.3-163.el7.centos will be updated
    ---> Package yum.noarch 0:3.4.3-167.el7.centos will be an update
    ---> Package yum-plugin-fastestmirror.noarch 0:1.1.31-52.el7 will be updated
    ---> Package yum-plugin-fastestmirror.noarch 0:1.1.31-53.el7 will be an update
    ---> Package yum-plugin-ovl.noarch 0:1.1.31-52.el7 will be updated
    ---> Package yum-plugin-ovl.noarch 0:1.1.31-53.el7 will be an update
    ---> Package yum-utils.noarch 0:1.1.31-52.el7 will be updated
    ---> Package yum-utils.noarch 0:1.1.31-53.el7 will be an update
    --> Finished Dependency Resolution
    
    Dependencies Resolved
    
    ================================================================================
     Package                      Arch    Version                    Repository
                                                                               Size
    ================================================================================
    Updating:
     acl                          x86_64  2.2.51-15.el7              base      81 k
     bash                         x86_64  4.2.46-34.el7              base     1.0 M
     bind-license                 noarch  32:9.11.4-16.P2.el7_8.2    updates   89 k
     binutils                     x86_64  2.27-43.base.el7           base     5.9 M
     ca-certificates              noarch  2019.2.32-76.el7_7         base     399 k
     centos-release               x86_64  7-8.2003.0.el7.centos      base      26 k
     cryptsetup-libs              x86_64  2.0.3-6.el7                base     339 k
     curl                         x86_64  7.29.0-57.el7              base     270 k
     device-mapper                x86_64  7:1.02.164-7.el7_8.1       updates  295 k
     device-mapper-libs           x86_64  7:1.02.164-7.el7_8.1       updates  324 k
     dracut                       x86_64  033-568.el7                base     329 k
     elfutils-default-yama-scope  noarch  0.176-4.el7                base      33 k
     elfutils-libelf              x86_64  0.176-4.el7                base     195 k
     elfutils-libs                x86_64  0.176-4.el7                base     291 k
     expat                        x86_64  2.1.0-11.el7               base      81 k
     file-libs                    x86_64  5.11-36.el7                base     340 k
     glibc                        x86_64  2.17-307.el7.1             base     3.6 M
     glibc-common                 x86_64  2.17-307.el7.1             base      11 M
     hostname                     x86_64  3.13-3.el7_7.1             base      17 k
     kmod                         x86_64  20-28.el7                  base     123 k
     kmod-libs                    x86_64  20-28.el7                  base      51 k
     kpartx                       x86_64  0.4.9-131.el7              base      80 k
     krb5-libs                    x86_64  1.15.1-46.el7              base     809 k
     libacl                       x86_64  2.2.51-15.el7              base      27 k
     libblkid                     x86_64  2.23.2-63.el7              base     182 k
     libcap                       x86_64  2.22-11.el7                base      47 k
     libcom_err                   x86_64  1.42.9-17.el7              base      42 k
     libcurl                      x86_64  7.29.0-57.el7              base     223 k
     libffi                       x86_64  3.0.13-19.el7              base      30 k
     libmount                     x86_64  2.23.2-63.el7              base     184 k
     libselinux                   x86_64  2.5-15.el7                 base     162 k
     libsmartcols                 x86_64  2.23.2-63.el7              base     142 k
     libuuid                      x86_64  2.23.2-63.el7              base      83 k
     libxml2                      x86_64  2.9.1-6.el7.4              base     668 k
     libxml2-python               x86_64  2.9.1-6.el7.4              base     247 k
     nss                          x86_64  3.44.0-7.el7_7             base     854 k
     nss-softokn                  x86_64  3.44.0-8.el7_7             base     330 k
     nss-softokn-freebl           x86_64  3.44.0-8.el7_7             base     224 k
     nss-sysinit                  x86_64  3.44.0-7.el7_7             base      65 k
     nss-tools                    x86_64  3.44.0-7.el7_7             base     528 k
     nss-util                     x86_64  3.44.0-4.el7_7             base      79 k
     pam                          x86_64  1.1.8-23.el7               base     721 k
     passwd                       x86_64  0.79-6.el7                 base     106 k
     procps-ng                    x86_64  3.3.10-27.el7              base     291 k
     python                       x86_64  2.7.5-88.el7               base      96 k
     python-libs                  x86_64  2.7.5-88.el7               base     5.6 M
     python-urlgrabber            noarch  3.10-10.el7                base     108 k
     rpm                          x86_64  4.11.3-43.el7              base     1.2 M
     rpm-build-libs               x86_64  4.11.3-43.el7              base     107 k
     rpm-libs                     x86_64  4.11.3-43.el7              base     278 k
     rpm-python                   x86_64  4.11.3-43.el7              base      84 k
     sed                          x86_64  4.2.2-6.el7                base     231 k
     setup                        noarch  2.8.71-11.el7              base     166 k
     shared-mime-info             x86_64  1.8-5.el7                  base     312 k
     sqlite                       x86_64  3.7.17-8.el7_7.1           base     394 k
     systemd                      x86_64  219-73.el7_8.5             updates  5.1 M
     systemd-libs                 x86_64  219-73.el7_8.5             updates  416 k
     tzdata                       noarch  2020a-1.el7                updates  495 k
     util-linux                   x86_64  2.23.2-63.el7              base     2.0 M
     yum                          noarch  3.4.3-167.el7.centos       base     1.2 M
     yum-plugin-fastestmirror     noarch  1.1.31-53.el7              base      34 k
     yum-plugin-ovl               noarch  1.1.31-53.el7              base      28 k
     yum-utils                    noarch  1.1.31-53.el7              base     122 k
    
    Transaction Summary
    ================================================================================
    Upgrade  63 Packages
    
    Total download size: 49 M
    Downloading packages:
    Delta RPMs disabled because /usr/bin/applydeltarpm not installed.
    Public key for acl-2.2.51-15.el7.x86_64.rpm is not installed
    Public key for bind-license-9.11.4-16.P2.el7_8.2.noarch.rpm is not installed
    --------------------------------------------------------------------------------
    Total                                               52 MB/s |  49 MB  00:00     
    Retrieving key from file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7
    Running transaction check
    Running transaction test
    Transaction test succeeded
    Running transaction
      Updating   : centos-release-7-8.2003.0.el7.centos.x86_64                1/126 
      Updating   : tzdata-2020a-1.el7.noarch                                  2/126 
      Updating   : bash-4.2.46-34.el7.x86_64                                  3/126 
      Updating   : nss-softokn-freebl-3.44.0-8.el7_7.x86_64                   4/126 
      Updating   : glibc-common-2.17-307.el7.1.x86_64                         5/126 
      Updating   : glibc-2.17-307.el7.1.x86_64                                6/126 
      Updating   : libselinux-2.5-15.el7.x86_64                               7/126 
      Updating   : nss-util-3.44.0-4.el7_7.x86_64                             8/126 
      Updating   : libacl-2.2.51-15.el7.x86_64                                9/126 
      Updating   : libcap-2.22-11.el7.x86_64                                 10/126 
      Updating   : elfutils-libelf-0.176-4.el7.x86_64                        11/126 
      Updating   : sed-4.2.2-6.el7.x86_64                                    12/126 
    install-info: No such file or directory for /usr/share/info/sed.info.gz
      Updating   : pam-1.1.8-23.el7.x86_64                                   13/126 
      Updating   : libuuid-2.23.2-63.el7.x86_64                              14/126 
      Updating   : libblkid-2.23.2-63.el7.x86_64                             15/126 
      Updating   : libmount-2.23.2-63.el7.x86_64                             16/126 
      Updating   : sqlite-3.7.17-8.el7_7.1.x86_64                            17/126 
      Updating   : nss-softokn-3.44.0-8.el7_7.x86_64                         18/126 
      Updating   : nss-sysinit-3.44.0-7.el7_7.x86_64                         19/126 
      Updating   : nss-3.44.0-7.el7_7.x86_64                                 20/126 
      Updating   : file-libs-5.11-36.el7.x86_64                              21/126 
      Updating   : libxml2-2.9.1-6.el7.4.x86_64                              22/126 
      Updating   : libcom_err-1.42.9-17.el7.x86_64                           23/126 
      Updating   : krb5-libs-1.15.1-46.el7.x86_64                            24/126 
      Updating   : libcurl-7.29.0-57.el7.x86_64                              25/126 
      Updating   : curl-7.29.0-57.el7.x86_64                                 26/126 
      Updating   : rpm-4.11.3-43.el7.x86_64                                  27/126 
      Updating   : rpm-libs-4.11.3-43.el7.x86_64                             28/126 
      Updating   : rpm-build-libs-4.11.3-43.el7.x86_64                       29/126 
      Updating   : acl-2.2.51-15.el7.x86_64                                  30/126 
      Updating   : kmod-libs-20-28.el7.x86_64                                31/126 
      Updating   : libsmartcols-2.23.2-63.el7.x86_64                         32/126 
      Updating   : binutils-2.27-43.base.el7.x86_64                          33/126 
    install-info: No such file or directory for /usr/share/info/as.info.gz
    install-info: No such file or directory for /usr/share/info/binutils.info.gz
    install-info: No such file or directory for /usr/share/info/gprof.info.gz
    install-info: No such file or directory for /usr/share/info/ld.info.gz
    install-info: No such file or directory for /usr/share/info/standards.info.gz
      Updating   : util-linux-2.23.2-63.el7.x86_64                           34/126 
      Updating   : procps-ng-3.3.10-27.el7.x86_64                            35/126 
      Updating   : kpartx-0.4.9-131.el7.x86_64                               36/126 
      Updating   : 7:device-mapper-1.02.164-7.el7_8.1.x86_64                 37/126 
      Updating   : 7:device-mapper-libs-1.02.164-7.el7_8.1.x86_64            38/126 
      Updating   : cryptsetup-libs-2.0.3-6.el7.x86_64                        39/126 
      Updating   : dracut-033-568.el7.x86_64                                 40/126 
      Updating   : kmod-20-28.el7.x86_64                                     41/126 
      Updating   : systemd-libs-219-73.el7_8.5.x86_64                        42/126 
      Updating   : elfutils-libs-0.176-4.el7.x86_64                          43/126 
      Updating   : systemd-219-73.el7_8.5.x86_64                             44/126 
    Failed to get D-Bus connection: Operation not permitted
      Updating   : elfutils-default-yama-scope-0.176-4.el7.noarch            45/126 
      Updating   : expat-2.1.0-11.el7.x86_64                                 46/126 
      Updating   : libffi-3.0.13-19.el7.x86_64                               47/126 
      Updating   : python-libs-2.7.5-88.el7.x86_64                           48/126 
      Updating   : python-2.7.5-88.el7.x86_64                                49/126 
      Updating   : libxml2-python-2.9.1-6.el7.4.x86_64                       50/126 
      Updating   : python-urlgrabber-3.10-10.el7.noarch                      51/126 
      Updating   : rpm-python-4.11.3-43.el7.x86_64                           52/126 
      Updating   : yum-plugin-fastestmirror-1.1.31-53.el7.noarch             53/126 
      Updating   : yum-3.4.3-167.el7.centos.noarch                           54/126 
      Updating   : yum-utils-1.1.31-53.el7.noarch                            55/126 
      Updating   : yum-plugin-ovl-1.1.31-53.el7.noarch                       56/126 
      Updating   : shared-mime-info-1.8-5.el7.x86_64                         57/126 
      Updating   : nss-tools-3.44.0-7.el7_7.x86_64                           58/126 
      Updating   : passwd-0.79-6.el7.x86_64                                  59/126 
      Updating   : hostname-3.13-3.el7_7.1.x86_64                            60/126 
      Updating   : ca-certificates-2019.2.32-76.el7_7.noarch                 61/126 
      Updating   : setup-2.8.71-11.el7.noarch                                62/126 
    warning: /etc/shadow created as /etc/shadow.rpmnew
      Updating   : 32:bind-license-9.11.4-16.P2.el7_8.2.noarch               63/126 
      Cleanup    : yum-utils-1.1.31-52.el7.noarch                            64/126 
      Cleanup    : yum-plugin-ovl-1.1.31-52.el7.noarch                       65/126 
      Cleanup    : yum-plugin-fastestmirror-1.1.31-52.el7.noarch             66/126 
      Cleanup    : yum-3.4.3-163.el7.centos.noarch                           67/126 
      Cleanup    : python-urlgrabber-3.10-9.el7.noarch                       68/126 
      Cleanup    : setup-2.8.71-10.el7.noarch                                69/126 
      Cleanup    : ca-certificates-2018.2.22-70.0.el7_5.noarch               70/126 
      Cleanup    : 32:bind-license-9.11.4-9.P2.el7.noarch                    71/126 
      Cleanup    : rpm-python-4.11.3-40.el7.x86_64                           72/126 
      Cleanup    : rpm-build-libs-4.11.3-40.el7.x86_64                       73/126 
      Cleanup    : rpm-libs-4.11.3-40.el7.x86_64                             74/126 
      Cleanup    : rpm-4.11.3-40.el7.x86_64                                  75/126 
      Cleanup    : curl-7.29.0-54.el7.x86_64                                 76/126 
      Cleanup    : nss-tools-3.44.0-4.el7.x86_64                             77/126 
      Cleanup    : libxml2-python-2.9.1-6.el7_2.3.x86_64                     78/126 
      Cleanup    : passwd-0.79-5.el7.x86_64                                  79/126 
      Cleanup    : python-2.7.5-86.el7.x86_64                                80/126 
      Cleanup    : python-libs-2.7.5-86.el7.x86_64                           81/126 
      Cleanup    : shared-mime-info-1.8-4.el7.x86_64                         82/126 
      Cleanup    : procps-ng-3.3.10-26.el7.x86_64                            83/126 
      Cleanup    : util-linux-2.23.2-61.el7.x86_64                           84/126 
      Cleanup    : cryptsetup-libs-2.0.3-5.el7.x86_64                        85/126 
      Cleanup    : systemd-libs-219-67.el7_7.1.x86_64                        86/126 
      Cleanup    : kpartx-0.4.9-127.el7.x86_64                               87/126 
      Cleanup    : kmod-20-25.el7.x86_64                                     88/126 
      Cleanup    : dracut-033-564.el7.x86_64                                 89/126 
      Cleanup    : elfutils-libs-0.176-2.el7.x86_64                          90/126 
      Cleanup    : elfutils-default-yama-scope-0.176-2.el7.noarch            91/126 
      Cleanup    : 7:device-mapper-libs-1.02.158-2.el7.x86_64                92/126 
      Cleanup    : 7:device-mapper-1.02.158-2.el7.x86_64                     93/126 
      Cleanup    : systemd-219-67.el7_7.1.x86_64                             94/126 
      Cleanup    : libcurl-7.29.0-54.el7.x86_64                              95/126 
      Cleanup    : nss-sysinit-3.44.0-4.el7.x86_64                           96/126 
      Cleanup    : nss-3.44.0-4.el7.x86_64                                   97/126 
      Cleanup    : nss-softokn-3.44.0-5.el7.x86_64                           98/126 
      Cleanup    : krb5-libs-1.15.1-37.el7_7.2.x86_64                        99/126 
      Cleanup    : libmount-2.23.2-61.el7.x86_64                            100/126 
      Cleanup    : libblkid-2.23.2-61.el7.x86_64                            101/126 
      Cleanup    : sed-4.2.2-5.el7.x86_64                                   102/126 
      Cleanup    : pam-1.1.8-22.el7.x86_64                                  103/126 
      Cleanup    : acl-2.2.51-14.el7.x86_64                                 104/126 
      Cleanup    : binutils-2.27-41.base.el7.x86_64                         105/126 
      Cleanup    : libacl-2.2.51-14.el7.x86_64                              106/126 
      Cleanup    : libuuid-2.23.2-61.el7.x86_64                             107/126 
      Cleanup    : libcom_err-1.42.9-16.el7.x86_64                          108/126 
      Cleanup    : sqlite-3.7.17-8.el7.x86_64                               109/126 
      Cleanup    : libcap-2.22-10.el7.x86_64                                110/126 
      Cleanup    : elfutils-libelf-0.176-2.el7.x86_64                       111/126 
      Cleanup    : kmod-libs-20-25.el7.x86_64                               112/126 
      Cleanup    : libsmartcols-2.23.2-61.el7.x86_64                        113/126 
      Cleanup    : libxml2-2.9.1-6.el7_2.3.x86_64                           114/126 
      Cleanup    : expat-2.1.0-10.el7_3.x86_64                              115/126 
      Cleanup    : libffi-3.0.13-18.el7.x86_64                              116/126 
      Cleanup    : file-libs-5.11-35.el7.x86_64                             117/126 
      Cleanup    : hostname-3.13-3.el7.x86_64                               118/126 
      Cleanup    : centos-release-7-7.1908.0.el7.centos.x86_64              119/126 
      Cleanup    : libselinux-2.5-14.1.el7.x86_64                           120/126 
      Cleanup    : glibc-common-2.17-292.el7.x86_64                         121/126 
      Cleanup    : bash-4.2.46-33.el7.x86_64                                122/126 
      Cleanup    : nss-util-3.44.0-3.el7.x86_64                             123/126 
      Cleanup    : nss-softokn-freebl-3.44.0-5.el7.x86_64                   124/126 
      Cleanup    : glibc-2.17-292.el7.x86_64                                125/126 
      Cleanup    : tzdata-2019b-1.el7.noarch                                126/126 
      Verifying  : acl-2.2.51-15.el7.x86_64                                   1/126 
      Verifying  : libacl-2.2.51-15.el7.x86_64                                2/126 
      Verifying  : kpartx-0.4.9-131.el7.x86_64                                3/126 
      Verifying  : centos-release-7-8.2003.0.el7.centos.x86_64                4/126 
      Verifying  : libcap-2.22-11.el7.x86_64                                  5/126 
      Verifying  : nss-3.44.0-7.el7_7.x86_64                                  6/126 
      Verifying  : python-2.7.5-88.el7.x86_64                                 7/126 
      Verifying  : libxml2-python-2.9.1-6.el7.4.x86_64                        8/126 
      Verifying  : libmount-2.23.2-63.el7.x86_64                              9/126 
      Verifying  : sqlite-3.7.17-8.el7_7.1.x86_64                            10/126 
      Verifying  : util-linux-2.23.2-63.el7.x86_64                           11/126 
      Verifying  : ca-certificates-2019.2.32-76.el7_7.noarch                 12/126 
      Verifying  : sed-4.2.2-6.el7.x86_64                                    13/126 
      Verifying  : 32:bind-license-9.11.4-16.P2.el7_8.2.noarch               14/126 
      Verifying  : kmod-libs-20-28.el7.x86_64                                15/126 
      Verifying  : elfutils-libs-0.176-4.el7.x86_64                          16/126 
      Verifying  : yum-utils-1.1.31-53.el7.noarch                            17/126 
      Verifying  : libsmartcols-2.23.2-63.el7.x86_64                         18/126 
      Verifying  : systemd-219-73.el7_8.5.x86_64                             19/126 
      Verifying  : setup-2.8.71-11.el7.noarch                                20/126 
      Verifying  : nss-tools-3.44.0-7.el7_7.x86_64                           21/126 
      Verifying  : pam-1.1.8-23.el7.x86_64                                   22/126 
      Verifying  : python-urlgrabber-3.10-10.el7.noarch                      23/126 
      Verifying  : elfutils-default-yama-scope-0.176-4.el7.noarch            24/126 
      Verifying  : cryptsetup-libs-2.0.3-6.el7.x86_64                        25/126 
      Verifying  : yum-plugin-ovl-1.1.31-53.el7.noarch                       26/126 
      Verifying  : yum-3.4.3-167.el7.centos.noarch                           27/126 
      Verifying  : python-libs-2.7.5-88.el7.x86_64                           28/126 
      Verifying  : nss-sysinit-3.44.0-7.el7_7.x86_64                         29/126 
      Verifying  : file-libs-5.11-36.el7.x86_64                              30/126 
      Verifying  : elfutils-libelf-0.176-4.el7.x86_64                        31/126 
      Verifying  : 7:device-mapper-1.02.164-7.el7_8.1.x86_64                 32/126 
      Verifying  : libxml2-2.9.1-6.el7.4.x86_64                              33/126 
      Verifying  : tzdata-2020a-1.el7.noarch                                 34/126 
      Verifying  : binutils-2.27-43.base.el7.x86_64                          35/126 
      Verifying  : rpm-python-4.11.3-43.el7.x86_64                           36/126 
      Verifying  : bash-4.2.46-34.el7.x86_64                                 37/126 
      Verifying  : nss-softokn-freebl-3.44.0-8.el7_7.x86_64                  38/126 
      Verifying  : nss-softokn-3.44.0-8.el7_7.x86_64                         39/126 
      Verifying  : libselinux-2.5-15.el7.x86_64                              40/126 
      Verifying  : libcom_err-1.42.9-17.el7.x86_64                           41/126 
      Verifying  : passwd-0.79-6.el7.x86_64                                  42/126 
      Verifying  : libcurl-7.29.0-57.el7.x86_64                              43/126 
      Verifying  : procps-ng-3.3.10-27.el7.x86_64                            44/126 
      Verifying  : glibc-common-2.17-307.el7.1.x86_64                        45/126 
      Verifying  : kmod-20-28.el7.x86_64                                     46/126 
      Verifying  : libblkid-2.23.2-63.el7.x86_64                             47/126 
      Verifying  : glibc-2.17-307.el7.1.x86_64                               48/126 
      Verifying  : hostname-3.13-3.el7_7.1.x86_64                            49/126 
      Verifying  : rpm-4.11.3-43.el7.x86_64                                  50/126 
      Verifying  : systemd-libs-219-73.el7_8.5.x86_64                        51/126 
      Verifying  : rpm-libs-4.11.3-43.el7.x86_64                             52/126 
      Verifying  : yum-plugin-fastestmirror-1.1.31-53.el7.noarch             53/126 
      Verifying  : krb5-libs-1.15.1-46.el7.x86_64                            54/126 
      Verifying  : curl-7.29.0-57.el7.x86_64                                 55/126 
      Verifying  : rpm-build-libs-4.11.3-43.el7.x86_64                       56/126 
      Verifying  : 7:device-mapper-libs-1.02.164-7.el7_8.1.x86_64            57/126 
      Verifying  : shared-mime-info-1.8-5.el7.x86_64                         58/126 
      Verifying  : dracut-033-568.el7.x86_64                                 59/126 
      Verifying  : libuuid-2.23.2-63.el7.x86_64                              60/126 
      Verifying  : expat-2.1.0-11.el7.x86_64                                 61/126 
      Verifying  : nss-util-3.44.0-4.el7_7.x86_64                            62/126 
      Verifying  : libffi-3.0.13-19.el7.x86_64                               63/126 
      Verifying  : yum-3.4.3-163.el7.centos.noarch                           64/126 
      Verifying  : acl-2.2.51-14.el7.x86_64                                  65/126 
      Verifying  : python-libs-2.7.5-86.el7.x86_64                           66/126 
      Verifying  : file-libs-5.11-35.el7.x86_64                              67/126 
      Verifying  : glibc-common-2.17-292.el7.x86_64                          68/126 
      Verifying  : kmod-libs-20-25.el7.x86_64                                69/126 
      Verifying  : rpm-libs-4.11.3-40.el7.x86_64                             70/126 
      Verifying  : 7:device-mapper-1.02.158-2.el7.x86_64                     71/126 
      Verifying  : systemd-219-67.el7_7.1.x86_64                             72/126 
      Verifying  : libffi-3.0.13-18.el7.x86_64                               73/126 
      Verifying  : sqlite-3.7.17-8.el7.x86_64                                74/126 
      Verifying  : elfutils-default-yama-scope-0.176-2.el7.noarch            75/126 
      Verifying  : nss-util-3.44.0-3.el7.x86_64                              76/126 
      Verifying  : libuuid-2.23.2-61.el7.x86_64                              77/126 
      Verifying  : libmount-2.23.2-61.el7.x86_64                             78/126 
      Verifying  : passwd-0.79-5.el7.x86_64                                  79/126 
      Verifying  : nss-sysinit-3.44.0-4.el7.x86_64                           80/126 
      Verifying  : 32:bind-license-9.11.4-9.P2.el7.noarch                    81/126 
      Verifying  : ca-certificates-2018.2.22-70.0.el7_5.noarch               82/126 
      Verifying  : nss-3.44.0-4.el7.x86_64                                   83/126 
      Verifying  : setup-2.8.71-10.el7.noarch                                84/126 
      Verifying  : yum-utils-1.1.31-52.el7.noarch                            85/126 
      Verifying  : elfutils-libs-0.176-2.el7.x86_64                          86/126 
      Verifying  : shared-mime-info-1.8-4.el7.x86_64                         87/126 
      Verifying  : libsmartcols-2.23.2-61.el7.x86_64                         88/126 
      Verifying  : libselinux-2.5-14.1.el7.x86_64                            89/126 
      Verifying  : rpm-build-libs-4.11.3-40.el7.x86_64                       90/126 
      Verifying  : nss-tools-3.44.0-4.el7.x86_64                             91/126 
      Verifying  : yum-plugin-ovl-1.1.31-52.el7.noarch                       92/126 
      Verifying  : nss-softokn-freebl-3.44.0-5.el7.x86_64                    93/126 
      Verifying  : centos-release-7-7.1908.0.el7.centos.x86_64               94/126 
      Verifying  : python-urlgrabber-3.10-9.el7.noarch                       95/126 
      Verifying  : cryptsetup-libs-2.0.3-5.el7.x86_64                        96/126 
      Verifying  : libxml2-2.9.1-6.el7_2.3.x86_64                            97/126 
      Verifying  : yum-plugin-fastestmirror-1.1.31-52.el7.noarch             98/126 
      Verifying  : libblkid-2.23.2-61.el7.x86_64                             99/126 
      Verifying  : systemd-libs-219-67.el7_7.1.x86_64                       100/126 
      Verifying  : hostname-3.13-3.el7.x86_64                               101/126 
      Verifying  : pam-1.1.8-22.el7.x86_64                                  102/126 
      Verifying  : rpm-4.11.3-40.el7.x86_64                                 103/126 
      Verifying  : binutils-2.27-41.base.el7.x86_64                         104/126 
      Verifying  : expat-2.1.0-10.el7_3.x86_64                              105/126 
      Verifying  : bash-4.2.46-33.el7.x86_64                                106/126 
      Verifying  : util-linux-2.23.2-61.el7.x86_64                          107/126 
      Verifying  : krb5-libs-1.15.1-37.el7_7.2.x86_64                       108/126 
      Verifying  : dracut-033-564.el7.x86_64                                109/126 
      Verifying  : rpm-python-4.11.3-40.el7.x86_64                          110/126 
      Verifying  : python-2.7.5-86.el7.x86_64                               111/126 
      Verifying  : libxml2-python-2.9.1-6.el7_2.3.x86_64                    112/126 
      Verifying  : libacl-2.2.51-14.el7.x86_64                              113/126 
      Verifying  : kpartx-0.4.9-127.el7.x86_64                              114/126 
      Verifying  : libcap-2.22-10.el7.x86_64                                115/126 
      Verifying  : libcom_err-1.42.9-16.el7.x86_64                          116/126 
      Verifying  : 7:device-mapper-libs-1.02.158-2.el7.x86_64               117/126 
      Verifying  : libcurl-7.29.0-54.el7.x86_64                             118/126 
      Verifying  : nss-softokn-3.44.0-5.el7.x86_64                          119/126 
      Verifying  : curl-7.29.0-54.el7.x86_64                                120/126 
      Verifying  : sed-4.2.2-5.el7.x86_64                                   121/126 
      Verifying  : glibc-2.17-292.el7.x86_64                                122/126 
      Verifying  : kmod-20-25.el7.x86_64                                    123/126 
      Verifying  : elfutils-libelf-0.176-2.el7.x86_64                       124/126 
      Verifying  : tzdata-2019b-1.el7.noarch                                125/126 
      Verifying  : procps-ng-3.3.10-26.el7.x86_64                           126/126 
    
    Updated:
      acl.x86_64 0:2.2.51-15.el7                                                    
      bash.x86_64 0:4.2.46-34.el7                                                   
      bind-license.noarch 32:9.11.4-16.P2.el7_8.2                                   
      binutils.x86_64 0:2.27-43.base.el7                                            
      ca-certificates.noarch 0:2019.2.32-76.el7_7                                   
      centos-release.x86_64 0:7-8.2003.0.el7.centos                                 
      cryptsetup-libs.x86_64 0:2.0.3-6.el7                                          
      curl.x86_64 0:7.29.0-57.el7                                                   
      device-mapper.x86_64 7:1.02.164-7.el7_8.1                                     
      device-mapper-libs.x86_64 7:1.02.164-7.el7_8.1                                
      dracut.x86_64 0:033-568.el7                                                   
      elfutils-default-yama-scope.noarch 0:0.176-4.el7                              
      elfutils-libelf.x86_64 0:0.176-4.el7                                          
      elfutils-libs.x86_64 0:0.176-4.el7                                            
      expat.x86_64 0:2.1.0-11.el7                                                   
      file-libs.x86_64 0:5.11-36.el7                                                
      glibc.x86_64 0:2.17-307.el7.1                                                 
      glibc-common.x86_64 0:2.17-307.el7.1                                          
      hostname.x86_64 0:3.13-3.el7_7.1                                              
      kmod.x86_64 0:20-28.el7                                                       
      kmod-libs.x86_64 0:20-28.el7                                                  
      kpartx.x86_64 0:0.4.9-131.el7                                                 
      krb5-libs.x86_64 0:1.15.1-46.el7                                              
      libacl.x86_64 0:2.2.51-15.el7                                                 
      libblkid.x86_64 0:2.23.2-63.el7                                               
      libcap.x86_64 0:2.22-11.el7                                                   
      libcom_err.x86_64 0:1.42.9-17.el7                                             
      libcurl.x86_64 0:7.29.0-57.el7                                                
      libffi.x86_64 0:3.0.13-19.el7                                                 
      libmount.x86_64 0:2.23.2-63.el7                                               
      libselinux.x86_64 0:2.5-15.el7                                                
      libsmartcols.x86_64 0:2.23.2-63.el7                                           
      libuuid.x86_64 0:2.23.2-63.el7                                                
      libxml2.x86_64 0:2.9.1-6.el7.4                                                
      libxml2-python.x86_64 0:2.9.1-6.el7.4                                         
      nss.x86_64 0:3.44.0-7.el7_7                                                   
      nss-softokn.x86_64 0:3.44.0-8.el7_7                                           
      nss-softokn-freebl.x86_64 0:3.44.0-8.el7_7                                    
      nss-sysinit.x86_64 0:3.44.0-7.el7_7                                           
      nss-tools.x86_64 0:3.44.0-7.el7_7                                             
      nss-util.x86_64 0:3.44.0-4.el7_7                                              
      pam.x86_64 0:1.1.8-23.el7                                                     
      passwd.x86_64 0:0.79-6.el7                                                    
      procps-ng.x86_64 0:3.3.10-27.el7                                              
      python.x86_64 0:2.7.5-88.el7                                                  
      python-libs.x86_64 0:2.7.5-88.el7                                             
      python-urlgrabber.noarch 0:3.10-10.el7                                        
      rpm.x86_64 0:4.11.3-43.el7                                                    
      rpm-build-libs.x86_64 0:4.11.3-43.el7                                         
      rpm-libs.x86_64 0:4.11.3-43.el7                                               
      rpm-python.x86_64 0:4.11.3-43.el7                                             
      sed.x86_64 0:4.2.2-6.el7                                                      
      setup.noarch 0:2.8.71-11.el7                                                  
      shared-mime-info.x86_64 0:1.8-5.el7                                           
      sqlite.x86_64 0:3.7.17-8.el7_7.1                                              
      systemd.x86_64 0:219-73.el7_8.5                                               
      systemd-libs.x86_64 0:219-73.el7_8.5                                          
      tzdata.noarch 0:2020a-1.el7                                                   
      util-linux.x86_64 0:2.23.2-63.el7                                             
      yum.noarch 0:3.4.3-167.el7.centos                                             
      yum-plugin-fastestmirror.noarch 0:1.1.31-53.el7                               
      yum-plugin-ovl.noarch 0:1.1.31-53.el7                                         
      yum-utils.noarch 0:1.1.31-53.el7                                              
    
    Complete!warning: /var/cache/yum/x86_64/7/base/packages/acl-2.2.51-15.el7.x86_64.rpm: Header V3 RSA/SHA256 Signature, key ID f4a80eb5: NOKEY
    Importing GPG key 0xF4A80EB5:
     Userid     : "CentOS-7 Key (CentOS 7 Official Signing Key) <security@centos.org>"
     Fingerprint: 6341 ab27 53d7 8a78 a7c2 7bb1 24c6 a8a7 f4a8 0eb5
     Package    : centos-release-7-7.1908.0.el7.centos.x86_64 (@CentOS)
     From       : /etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7



```bash
ansible kafka -m command -a "docker exec sinetstream-kafka yum install -y java-1.8.0-openjdk openssl"
```

    server1.example.jp | CHANGED | rc=0 >>
    Loaded plugins: fastestmirror, ovl
    Loading mirror speeds from cached hostfile
     * base: ty1.mirror.newmediaexpress.com
     * extras: ty1.mirror.newmediaexpress.com
     * updates: ty1.mirror.newmediaexpress.com
    Resolving Dependencies
    --> Running transaction check
    ---> Package java-1.8.0-openjdk.x86_64 1:1.8.0.252.b09-2.el7_8 will be installed
    --> Processing Dependency: java-1.8.0-openjdk-headless(x86-64) = 1:1.8.0.252.b09-2.el7_8 for package: 1:java-1.8.0-openjdk-1.8.0.252.b09-2.el7_8.x86_64
    --> Processing Dependency: xorg-x11-fonts-Type1 for package: 1:java-1.8.0-openjdk-1.8.0.252.b09-2.el7_8.x86_64
    --> Processing Dependency: libpng15.so.15(PNG15_0)(64bit) for package: 1:java-1.8.0-openjdk-1.8.0.252.b09-2.el7_8.x86_64
    --> Processing Dependency: libjvm.so(SUNWprivate_1.1)(64bit) for package: 1:java-1.8.0-openjdk-1.8.0.252.b09-2.el7_8.x86_64
    --> Processing Dependency: libjpeg.so.62(LIBJPEG_6.2)(64bit) for package: 1:java-1.8.0-openjdk-1.8.0.252.b09-2.el7_8.x86_64
    --> Processing Dependency: libjava.so(SUNWprivate_1.1)(64bit) for package: 1:java-1.8.0-openjdk-1.8.0.252.b09-2.el7_8.x86_64
    --> Processing Dependency: libasound.so.2(ALSA_0.9.0rc4)(64bit) for package: 1:java-1.8.0-openjdk-1.8.0.252.b09-2.el7_8.x86_64
    --> Processing Dependency: libasound.so.2(ALSA_0.9)(64bit) for package: 1:java-1.8.0-openjdk-1.8.0.252.b09-2.el7_8.x86_64
    --> Processing Dependency: libXcomposite(x86-64) for package: 1:java-1.8.0-openjdk-1.8.0.252.b09-2.el7_8.x86_64
    --> Processing Dependency: gtk2(x86-64) for package: 1:java-1.8.0-openjdk-1.8.0.252.b09-2.el7_8.x86_64
    --> Processing Dependency: fontconfig(x86-64) for package: 1:java-1.8.0-openjdk-1.8.0.252.b09-2.el7_8.x86_64
    --> Processing Dependency: libpng15.so.15()(64bit) for package: 1:java-1.8.0-openjdk-1.8.0.252.b09-2.el7_8.x86_64
    --> Processing Dependency: libjvm.so()(64bit) for package: 1:java-1.8.0-openjdk-1.8.0.252.b09-2.el7_8.x86_64
    --> Processing Dependency: libjpeg.so.62()(64bit) for package: 1:java-1.8.0-openjdk-1.8.0.252.b09-2.el7_8.x86_64
    --> Processing Dependency: libjava.so()(64bit) for package: 1:java-1.8.0-openjdk-1.8.0.252.b09-2.el7_8.x86_64
    --> Processing Dependency: libgif.so.4()(64bit) for package: 1:java-1.8.0-openjdk-1.8.0.252.b09-2.el7_8.x86_64
    --> Processing Dependency: libasound.so.2()(64bit) for package: 1:java-1.8.0-openjdk-1.8.0.252.b09-2.el7_8.x86_64
    --> Processing Dependency: libXtst.so.6()(64bit) for package: 1:java-1.8.0-openjdk-1.8.0.252.b09-2.el7_8.x86_64
    --> Processing Dependency: libXrender.so.1()(64bit) for package: 1:java-1.8.0-openjdk-1.8.0.252.b09-2.el7_8.x86_64
    --> Processing Dependency: libXi.so.6()(64bit) for package: 1:java-1.8.0-openjdk-1.8.0.252.b09-2.el7_8.x86_64
    --> Processing Dependency: libXext.so.6()(64bit) for package: 1:java-1.8.0-openjdk-1.8.0.252.b09-2.el7_8.x86_64
    --> Processing Dependency: libX11.so.6()(64bit) for package: 1:java-1.8.0-openjdk-1.8.0.252.b09-2.el7_8.x86_64
    ---> Package openssl.x86_64 1:1.0.2k-19.el7 will be installed
    --> Processing Dependency: make for package: 1:openssl-1.0.2k-19.el7.x86_64
    --> Running transaction check
    ---> Package alsa-lib.x86_64 0:1.1.8-1.el7 will be installed
    ---> Package fontconfig.x86_64 0:2.13.0-4.3.el7 will be installed
    --> Processing Dependency: freetype >= 2.8-7 for package: fontconfig-2.13.0-4.3.el7.x86_64
    --> Processing Dependency: freetype for package: fontconfig-2.13.0-4.3.el7.x86_64
    --> Processing Dependency: fontpackages-filesystem for package: fontconfig-2.13.0-4.3.el7.x86_64
    --> Processing Dependency: dejavu-sans-fonts for package: fontconfig-2.13.0-4.3.el7.x86_64
    --> Processing Dependency: libfreetype.so.6()(64bit) for package: fontconfig-2.13.0-4.3.el7.x86_64
    ---> Package giflib.x86_64 0:4.1.6-9.el7 will be installed
    --> Processing Dependency: libSM.so.6()(64bit) for package: giflib-4.1.6-9.el7.x86_64
    --> Processing Dependency: libICE.so.6()(64bit) for package: giflib-4.1.6-9.el7.x86_64
    ---> Package gtk2.x86_64 0:2.24.31-1.el7 will be installed
    --> Processing Dependency: pango >= 1.20.0-1 for package: gtk2-2.24.31-1.el7.x86_64
    --> Processing Dependency: libtiff >= 3.6.1 for package: gtk2-2.24.31-1.el7.x86_64
    --> Processing Dependency: libXrandr >= 1.2.99.4-2 for package: gtk2-2.24.31-1.el7.x86_64
    --> Processing Dependency: atk >= 1.29.4-2 for package: gtk2-2.24.31-1.el7.x86_64
    --> Processing Dependency: hicolor-icon-theme for package: gtk2-2.24.31-1.el7.x86_64
    --> Processing Dependency: gtk-update-icon-cache for package: gtk2-2.24.31-1.el7.x86_64
    --> Processing Dependency: libpangoft2-1.0.so.0()(64bit) for package: gtk2-2.24.31-1.el7.x86_64
    --> Processing Dependency: libpangocairo-1.0.so.0()(64bit) for package: gtk2-2.24.31-1.el7.x86_64
    --> Processing Dependency: libpango-1.0.so.0()(64bit) for package: gtk2-2.24.31-1.el7.x86_64
    --> Processing Dependency: libgdk_pixbuf-2.0.so.0()(64bit) for package: gtk2-2.24.31-1.el7.x86_64
    --> Processing Dependency: libcups.so.2()(64bit) for package: gtk2-2.24.31-1.el7.x86_64
    --> Processing Dependency: libcairo.so.2()(64bit) for package: gtk2-2.24.31-1.el7.x86_64
    --> Processing Dependency: libatk-1.0.so.0()(64bit) for package: gtk2-2.24.31-1.el7.x86_64
    --> Processing Dependency: libXrandr.so.2()(64bit) for package: gtk2-2.24.31-1.el7.x86_64
    --> Processing Dependency: libXinerama.so.1()(64bit) for package: gtk2-2.24.31-1.el7.x86_64
    --> Processing Dependency: libXfixes.so.3()(64bit) for package: gtk2-2.24.31-1.el7.x86_64
    --> Processing Dependency: libXdamage.so.1()(64bit) for package: gtk2-2.24.31-1.el7.x86_64
    --> Processing Dependency: libXcursor.so.1()(64bit) for package: gtk2-2.24.31-1.el7.x86_64
    ---> Package java-1.8.0-openjdk-headless.x86_64 1:1.8.0.252.b09-2.el7_8 will be installed
    --> Processing Dependency: tzdata-java >= 2015d for package: 1:java-1.8.0-openjdk-headless-1.8.0.252.b09-2.el7_8.x86_64
    --> Processing Dependency: copy-jdk-configs >= 3.3 for package: 1:java-1.8.0-openjdk-headless-1.8.0.252.b09-2.el7_8.x86_64
    --> Processing Dependency: pcsc-lite-libs(x86-64) for package: 1:java-1.8.0-openjdk-headless-1.8.0.252.b09-2.el7_8.x86_64
    --> Processing Dependency: lksctp-tools(x86-64) for package: 1:java-1.8.0-openjdk-headless-1.8.0.252.b09-2.el7_8.x86_64
    --> Processing Dependency: jpackage-utils for package: 1:java-1.8.0-openjdk-headless-1.8.0.252.b09-2.el7_8.x86_64
    ---> Package libX11.x86_64 0:1.6.7-2.el7 will be installed
    --> Processing Dependency: libX11-common >= 1.6.7-2.el7 for package: libX11-1.6.7-2.el7.x86_64
    --> Processing Dependency: libxcb.so.1()(64bit) for package: libX11-1.6.7-2.el7.x86_64
    ---> Package libXcomposite.x86_64 0:0.4.4-4.1.el7 will be installed
    ---> Package libXext.x86_64 0:1.3.3-3.el7 will be installed
    ---> Package libXi.x86_64 0:1.7.9-1.el7 will be installed
    ---> Package libXrender.x86_64 0:0.9.10-1.el7 will be installed
    ---> Package libXtst.x86_64 0:1.2.3-1.el7 will be installed
    ---> Package libjpeg-turbo.x86_64 0:1.2.90-8.el7 will be installed
    ---> Package libpng.x86_64 2:1.5.13-7.el7_2 will be installed
    ---> Package make.x86_64 1:3.82-24.el7 will be installed
    ---> Package xorg-x11-fonts-Type1.noarch 0:7.5-9.el7 will be installed
    --> Processing Dependency: ttmkfdir for package: xorg-x11-fonts-Type1-7.5-9.el7.noarch
    --> Processing Dependency: ttmkfdir for package: xorg-x11-fonts-Type1-7.5-9.el7.noarch
    --> Processing Dependency: mkfontdir for package: xorg-x11-fonts-Type1-7.5-9.el7.noarch
    --> Processing Dependency: mkfontdir for package: xorg-x11-fonts-Type1-7.5-9.el7.noarch
    --> Running transaction check
    ---> Package atk.x86_64 0:2.28.1-2.el7 will be installed
    ---> Package cairo.x86_64 0:1.15.12-4.el7 will be installed
    --> Processing Dependency: libpixman-1.so.0()(64bit) for package: cairo-1.15.12-4.el7.x86_64
    --> Processing Dependency: libGL.so.1()(64bit) for package: cairo-1.15.12-4.el7.x86_64
    --> Processing Dependency: libEGL.so.1()(64bit) for package: cairo-1.15.12-4.el7.x86_64
    ---> Package copy-jdk-configs.noarch 0:3.3-10.el7_5 will be installed
    ---> Package cups-libs.x86_64 1:1.6.3-43.el7 will be installed
    --> Processing Dependency: libavahi-common.so.3()(64bit) for package: 1:cups-libs-1.6.3-43.el7.x86_64
    --> Processing Dependency: libavahi-client.so.3()(64bit) for package: 1:cups-libs-1.6.3-43.el7.x86_64
    ---> Package dejavu-sans-fonts.noarch 0:2.33-6.el7 will be installed
    --> Processing Dependency: dejavu-fonts-common = 2.33-6.el7 for package: dejavu-sans-fonts-2.33-6.el7.noarch
    ---> Package fontpackages-filesystem.noarch 0:1.44-8.el7 will be installed
    ---> Package freetype.x86_64 0:2.8-14.el7 will be installed
    ---> Package gdk-pixbuf2.x86_64 0:2.36.12-3.el7 will be installed
    --> Processing Dependency: libjasper.so.1()(64bit) for package: gdk-pixbuf2-2.36.12-3.el7.x86_64
    ---> Package gtk-update-icon-cache.x86_64 0:3.22.30-5.el7 will be installed
    ---> Package hicolor-icon-theme.noarch 0:0.12-7.el7 will be installed
    ---> Package javapackages-tools.noarch 0:3.4.1-11.el7 will be installed
    --> Processing Dependency: python-javapackages = 3.4.1-11.el7 for package: javapackages-tools-3.4.1-11.el7.noarch
    --> Processing Dependency: libxslt for package: javapackages-tools-3.4.1-11.el7.noarch
    ---> Package libICE.x86_64 0:1.0.9-9.el7 will be installed
    ---> Package libSM.x86_64 0:1.2.2-2.el7 will be installed
    ---> Package libX11-common.noarch 0:1.6.7-2.el7 will be installed
    ---> Package libXcursor.x86_64 0:1.1.15-1.el7 will be installed
    ---> Package libXdamage.x86_64 0:1.1.4-4.1.el7 will be installed
    ---> Package libXfixes.x86_64 0:5.0.3-1.el7 will be installed
    ---> Package libXinerama.x86_64 0:1.1.3-2.1.el7 will be installed
    ---> Package libXrandr.x86_64 0:1.5.1-2.el7 will be installed
    ---> Package libtiff.x86_64 0:4.0.3-32.el7 will be installed
    --> Processing Dependency: libjbig.so.2.0()(64bit) for package: libtiff-4.0.3-32.el7.x86_64
    ---> Package libxcb.x86_64 0:1.13-1.el7 will be installed
    --> Processing Dependency: libXau.so.6()(64bit) for package: libxcb-1.13-1.el7.x86_64
    ---> Package lksctp-tools.x86_64 0:1.0.17-2.el7 will be installed
    ---> Package pango.x86_64 0:1.42.4-4.el7_7 will be installed
    --> Processing Dependency: libthai(x86-64) >= 0.1.9 for package: pango-1.42.4-4.el7_7.x86_64
    --> Processing Dependency: libXft(x86-64) >= 2.0.0 for package: pango-1.42.4-4.el7_7.x86_64
    --> Processing Dependency: harfbuzz(x86-64) >= 1.4.2 for package: pango-1.42.4-4.el7_7.x86_64
    --> Processing Dependency: fribidi(x86-64) >= 1.0 for package: pango-1.42.4-4.el7_7.x86_64
    --> Processing Dependency: libthai.so.0(LIBTHAI_0.1)(64bit) for package: pango-1.42.4-4.el7_7.x86_64
    --> Processing Dependency: libthai.so.0()(64bit) for package: pango-1.42.4-4.el7_7.x86_64
    --> Processing Dependency: libharfbuzz.so.0()(64bit) for package: pango-1.42.4-4.el7_7.x86_64
    --> Processing Dependency: libfribidi.so.0()(64bit) for package: pango-1.42.4-4.el7_7.x86_64
    --> Processing Dependency: libXft.so.2()(64bit) for package: pango-1.42.4-4.el7_7.x86_64
    ---> Package pcsc-lite-libs.x86_64 0:1.8.8-8.el7 will be installed
    ---> Package ttmkfdir.x86_64 0:3.0.9-42.el7 will be installed
    ---> Package tzdata-java.noarch 0:2020a-1.el7 will be installed
    ---> Package xorg-x11-font-utils.x86_64 1:7.5-21.el7 will be installed
    --> Processing Dependency: libfontenc.so.1()(64bit) for package: 1:xorg-x11-font-utils-7.5-21.el7.x86_64
    --> Running transaction check
    ---> Package avahi-libs.x86_64 0:0.6.31-20.el7 will be installed
    ---> Package dejavu-fonts-common.noarch 0:2.33-6.el7 will be installed
    ---> Package fribidi.x86_64 0:1.0.2-1.el7_7.1 will be installed
    ---> Package harfbuzz.x86_64 0:1.7.5-2.el7 will be installed
    --> Processing Dependency: libgraphite2.so.3()(64bit) for package: harfbuzz-1.7.5-2.el7.x86_64
    ---> Package jasper-libs.x86_64 0:1.900.1-33.el7 will be installed
    ---> Package jbigkit-libs.x86_64 0:2.0-11.el7 will be installed
    ---> Package libXau.x86_64 0:1.0.8-2.1.el7 will be installed
    ---> Package libXft.x86_64 0:2.3.2-2.el7 will be installed
    ---> Package libfontenc.x86_64 0:1.1.3-3.el7 will be installed
    ---> Package libglvnd-egl.x86_64 1:1.0.1-0.8.git5baa1e5.el7 will be installed
    --> Processing Dependency: libglvnd(x86-64) = 1:1.0.1-0.8.git5baa1e5.el7 for package: 1:libglvnd-egl-1.0.1-0.8.git5baa1e5.el7.x86_64
    --> Processing Dependency: mesa-libEGL(x86-64) >= 13.0.4-1 for package: 1:libglvnd-egl-1.0.1-0.8.git5baa1e5.el7.x86_64
    --> Processing Dependency: libGLdispatch.so.0()(64bit) for package: 1:libglvnd-egl-1.0.1-0.8.git5baa1e5.el7.x86_64
    ---> Package libglvnd-glx.x86_64 1:1.0.1-0.8.git5baa1e5.el7 will be installed
    --> Processing Dependency: mesa-libGL(x86-64) >= 13.0.4-1 for package: 1:libglvnd-glx-1.0.1-0.8.git5baa1e5.el7.x86_64
    ---> Package libthai.x86_64 0:0.1.14-9.el7 will be installed
    ---> Package libxslt.x86_64 0:1.1.28-5.el7 will be installed
    ---> Package pixman.x86_64 0:0.34.0-1.el7 will be installed
    ---> Package python-javapackages.noarch 0:3.4.1-11.el7 will be installed
    --> Processing Dependency: python-lxml for package: python-javapackages-3.4.1-11.el7.noarch
    --> Running transaction check
    ---> Package graphite2.x86_64 0:1.3.10-1.el7_3 will be installed
    ---> Package libglvnd.x86_64 1:1.0.1-0.8.git5baa1e5.el7 will be installed
    ---> Package mesa-libEGL.x86_64 0:18.3.4-7.el7 will be installed
    --> Processing Dependency: mesa-libgbm = 18.3.4-7.el7 for package: mesa-libEGL-18.3.4-7.el7.x86_64
    --> Processing Dependency: libxshmfence.so.1()(64bit) for package: mesa-libEGL-18.3.4-7.el7.x86_64
    --> Processing Dependency: libwayland-server.so.0()(64bit) for package: mesa-libEGL-18.3.4-7.el7.x86_64
    --> Processing Dependency: libwayland-client.so.0()(64bit) for package: mesa-libEGL-18.3.4-7.el7.x86_64
    --> Processing Dependency: libglapi.so.0()(64bit) for package: mesa-libEGL-18.3.4-7.el7.x86_64
    --> Processing Dependency: libgbm.so.1()(64bit) for package: mesa-libEGL-18.3.4-7.el7.x86_64
    --> Processing Dependency: libdrm.so.2()(64bit) for package: mesa-libEGL-18.3.4-7.el7.x86_64
    ---> Package mesa-libGL.x86_64 0:18.3.4-7.el7 will be installed
    --> Processing Dependency: libXxf86vm.so.1()(64bit) for package: mesa-libGL-18.3.4-7.el7.x86_64
    ---> Package python-lxml.x86_64 0:3.2.1-4.el7 will be installed
    --> Running transaction check
    ---> Package libXxf86vm.x86_64 0:1.1.4-1.el7 will be installed
    ---> Package libdrm.x86_64 0:2.4.97-2.el7 will be installed
    --> Processing Dependency: libpciaccess.so.0()(64bit) for package: libdrm-2.4.97-2.el7.x86_64
    ---> Package libwayland-client.x86_64 0:1.15.0-1.el7 will be installed
    ---> Package libwayland-server.x86_64 0:1.15.0-1.el7 will be installed
    ---> Package libxshmfence.x86_64 0:1.2-1.el7 will be installed
    ---> Package mesa-libgbm.x86_64 0:18.3.4-7.el7 will be installed
    ---> Package mesa-libglapi.x86_64 0:18.3.4-7.el7 will be installed
    --> Running transaction check
    ---> Package libpciaccess.x86_64 0:0.14-1.el7 will be installed
    --> Processing Dependency: hwdata for package: libpciaccess-0.14-1.el7.x86_64
    --> Running transaction check
    ---> Package hwdata.x86_64 0:0.252-9.5.el7 will be installed
    --> Finished Dependency Resolution
    
    Dependencies Resolved
    
    ================================================================================
     Package                     Arch   Version                       Repository
                                                                               Size
    ================================================================================
    Installing:
     java-1.8.0-openjdk          x86_64 1:1.8.0.252.b09-2.el7_8       updates 295 k
     openssl                     x86_64 1:1.0.2k-19.el7               base    493 k
    Installing for dependencies:
     alsa-lib                    x86_64 1.1.8-1.el7                   base    425 k
     atk                         x86_64 2.28.1-2.el7                  base    263 k
     avahi-libs                  x86_64 0.6.31-20.el7                 base     62 k
     cairo                       x86_64 1.15.12-4.el7                 base    741 k
     copy-jdk-configs            noarch 3.3-10.el7_5                  base     21 k
     cups-libs                   x86_64 1:1.6.3-43.el7                base    358 k
     dejavu-fonts-common         noarch 2.33-6.el7                    base     64 k
     dejavu-sans-fonts           noarch 2.33-6.el7                    base    1.4 M
     fontconfig                  x86_64 2.13.0-4.3.el7                base    254 k
     fontpackages-filesystem     noarch 1.44-8.el7                    base    9.9 k
     freetype                    x86_64 2.8-14.el7                    base    380 k
     fribidi                     x86_64 1.0.2-1.el7_7.1               base     79 k
     gdk-pixbuf2                 x86_64 2.36.12-3.el7                 base    570 k
     giflib                      x86_64 4.1.6-9.el7                   base     40 k
     graphite2                   x86_64 1.3.10-1.el7_3                base    115 k
     gtk-update-icon-cache       x86_64 3.22.30-5.el7                 base     27 k
     gtk2                        x86_64 2.24.31-1.el7                 base    3.4 M
     harfbuzz                    x86_64 1.7.5-2.el7                   base    267 k
     hicolor-icon-theme          noarch 0.12-7.el7                    base     42 k
     hwdata                      x86_64 0.252-9.5.el7                 base    2.4 M
     jasper-libs                 x86_64 1.900.1-33.el7                base    150 k
     java-1.8.0-openjdk-headless x86_64 1:1.8.0.252.b09-2.el7_8       updates  32 M
     javapackages-tools          noarch 3.4.1-11.el7                  base     73 k
     jbigkit-libs                x86_64 2.0-11.el7                    base     46 k
     libICE                      x86_64 1.0.9-9.el7                   base     66 k
     libSM                       x86_64 1.2.2-2.el7                   base     39 k
     libX11                      x86_64 1.6.7-2.el7                   base    607 k
     libX11-common               noarch 1.6.7-2.el7                   base    164 k
     libXau                      x86_64 1.0.8-2.1.el7                 base     29 k
     libXcomposite               x86_64 0.4.4-4.1.el7                 base     22 k
     libXcursor                  x86_64 1.1.15-1.el7                  base     30 k
     libXdamage                  x86_64 1.1.4-4.1.el7                 base     20 k
     libXext                     x86_64 1.3.3-3.el7                   base     39 k
     libXfixes                   x86_64 5.0.3-1.el7                   base     18 k
     libXft                      x86_64 2.3.2-2.el7                   base     58 k
     libXi                       x86_64 1.7.9-1.el7                   base     40 k
     libXinerama                 x86_64 1.1.3-2.1.el7                 base     14 k
     libXrandr                   x86_64 1.5.1-2.el7                   base     27 k
     libXrender                  x86_64 0.9.10-1.el7                  base     26 k
     libXtst                     x86_64 1.2.3-1.el7                   base     20 k
     libXxf86vm                  x86_64 1.1.4-1.el7                   base     18 k
     libdrm                      x86_64 2.4.97-2.el7                  base    151 k
     libfontenc                  x86_64 1.1.3-3.el7                   base     31 k
     libglvnd                    x86_64 1:1.0.1-0.8.git5baa1e5.el7    base     89 k
     libglvnd-egl                x86_64 1:1.0.1-0.8.git5baa1e5.el7    base     44 k
     libglvnd-glx                x86_64 1:1.0.1-0.8.git5baa1e5.el7    base    125 k
     libjpeg-turbo               x86_64 1.2.90-8.el7                  base    135 k
     libpciaccess                x86_64 0.14-1.el7                    base     26 k
     libpng                      x86_64 2:1.5.13-7.el7_2              base    213 k
     libthai                     x86_64 0.1.14-9.el7                  base    187 k
     libtiff                     x86_64 4.0.3-32.el7                  base    171 k
     libwayland-client           x86_64 1.15.0-1.el7                  base     33 k
     libwayland-server           x86_64 1.15.0-1.el7                  base     39 k
     libxcb                      x86_64 1.13-1.el7                    base    214 k
     libxshmfence                x86_64 1.2-1.el7                     base    7.2 k
     libxslt                     x86_64 1.1.28-5.el7                  base    242 k
     lksctp-tools                x86_64 1.0.17-2.el7                  base     88 k
     make                        x86_64 1:3.82-24.el7                 base    421 k
     mesa-libEGL                 x86_64 18.3.4-7.el7                  base    109 k
     mesa-libGL                  x86_64 18.3.4-7.el7                  base    165 k
     mesa-libgbm                 x86_64 18.3.4-7.el7                  base     39 k
     mesa-libglapi               x86_64 18.3.4-7.el7                  base     45 k
     pango                       x86_64 1.42.4-4.el7_7                base    280 k
     pcsc-lite-libs              x86_64 1.8.8-8.el7                   base     34 k
     pixman                      x86_64 0.34.0-1.el7                  base    248 k
     python-javapackages         noarch 3.4.1-11.el7                  base     31 k
     python-lxml                 x86_64 3.2.1-4.el7                   base    758 k
     ttmkfdir                    x86_64 3.0.9-42.el7                  base     48 k
     tzdata-java                 noarch 2020a-1.el7                   updates 188 k
     xorg-x11-font-utils         x86_64 1:7.5-21.el7                  base    104 k
     xorg-x11-fonts-Type1        noarch 7.5-9.el7                     base    521 k
    
    Transaction Summary
    ================================================================================
    Install  2 Packages (+71 Dependent packages)
    
    Total download size: 50 M
    Installed size: 169 M
    Downloading packages:
    --------------------------------------------------------------------------------
    Total                                               60 MB/s |  50 MB  00:00     
    Running transaction check
    Running transaction test
    Transaction test succeeded
    Running transaction
      Installing : libjpeg-turbo-1.2.90-8.el7.x86_64                           1/73 
      Installing : 2:libpng-1.5.13-7.el7_2.x86_64                              2/73 
      Installing : freetype-2.8-14.el7.x86_64                                  3/73 
      Installing : mesa-libglapi-18.3.4-7.el7.x86_64                           4/73 
      Installing : libxshmfence-1.2-1.el7.x86_64                               5/73 
      Installing : libxslt-1.1.28-5.el7.x86_64                                 6/73 
      Installing : 1:libglvnd-1.0.1-0.8.git5baa1e5.el7.x86_64                  7/73 
      Installing : fontpackages-filesystem-1.44-8.el7.noarch                   8/73 
      Installing : libICE-1.0.9-9.el7.x86_64                                   9/73 
      Installing : libwayland-server-1.15.0-1.el7.x86_64                      10/73 
      Installing : libSM-1.2.2-2.el7.x86_64                                   11/73 
      Installing : dejavu-fonts-common-2.33-6.el7.noarch                      12/73 
      Installing : dejavu-sans-fonts-2.33-6.el7.noarch                        13/73 
      Installing : fontconfig-2.13.0-4.3.el7.x86_64                           14/73 
      Installing : python-lxml-3.2.1-4.el7.x86_64                             15/73 
      Installing : python-javapackages-3.4.1-11.el7.noarch                    16/73 
      Installing : javapackages-tools-3.4.1-11.el7.noarch                     17/73 
      Installing : ttmkfdir-3.0.9-42.el7.x86_64                               18/73 
      Installing : jasper-libs-1.900.1-33.el7.x86_64                          19/73 
      Installing : pixman-0.34.0-1.el7.x86_64                                 20/73 
      Installing : avahi-libs-0.6.31-20.el7.x86_64                            21/73 
      Installing : 1:cups-libs-1.6.3-43.el7.x86_64                            22/73 
      Installing : libfontenc-1.1.3-3.el7.x86_64                              23/73 
      Installing : 1:xorg-x11-font-utils-7.5-21.el7.x86_64                    24/73 
      Installing : xorg-x11-fonts-Type1-7.5-9.el7.noarch                      25/73 
      Installing : atk-2.28.1-2.el7.x86_64                                    26/73 
      Installing : libthai-0.1.14-9.el7.x86_64                                27/73 
      Installing : graphite2-1.3.10-1.el7_3.x86_64                            28/73 
      Installing : harfbuzz-1.7.5-2.el7.x86_64                                29/73 
      Installing : libXau-1.0.8-2.1.el7.x86_64                                30/73 
      Installing : libxcb-1.13-1.el7.x86_64                                   31/73 
      Installing : jbigkit-libs-2.0-11.el7.x86_64                             32/73 
      Installing : libtiff-4.0.3-32.el7.x86_64                                33/73 
      Installing : pcsc-lite-libs-1.8.8-8.el7.x86_64                          34/73 
      Installing : hwdata-0.252-9.5.el7.x86_64                                35/73 
      Installing : libpciaccess-0.14-1.el7.x86_64                             36/73 
      Installing : libdrm-2.4.97-2.el7.x86_64                                 37/73 
      Installing : mesa-libgbm-18.3.4-7.el7.x86_64                            38/73 
      Installing : lksctp-tools-1.0.17-2.el7.x86_64                           39/73 
      Installing : 1:make-3.82-24.el7.x86_64                                  40/73 
      Installing : libX11-common-1.6.7-2.el7.noarch                           41/73 
      Installing : libX11-1.6.7-2.el7.x86_64                                  42/73 
      Installing : libXext-1.3.3-3.el7.x86_64                                 43/73 
      Installing : libXrender-0.9.10-1.el7.x86_64                             44/73 
      Installing : libXfixes-5.0.3-1.el7.x86_64                               45/73 
      Installing : libXi-1.7.9-1.el7.x86_64                                   46/73 
      Installing : libXdamage-1.1.4-4.1.el7.x86_64                            47/73 
      Installing : gdk-pixbuf2-2.36.12-3.el7.x86_64                           48/73 
      Installing : libXcomposite-0.4.4-4.1.el7.x86_64                         49/73 
      Installing : gtk-update-icon-cache-3.22.30-5.el7.x86_64                 50/73 
      Installing : libXtst-1.2.3-1.el7.x86_64                                 51/73 
      Installing : libXcursor-1.1.15-1.el7.x86_64                             52/73 
      Installing : libXft-2.3.2-2.el7.x86_64                                  53/73 
      Installing : libXrandr-1.5.1-2.el7.x86_64                               54/73 
      Installing : libXinerama-1.1.3-2.1.el7.x86_64                           55/73 
      Installing : libXxf86vm-1.1.4-1.el7.x86_64                              56/73 
      Installing : mesa-libGL-18.3.4-7.el7.x86_64                             57/73 
      Installing : 1:libglvnd-glx-1.0.1-0.8.git5baa1e5.el7.x86_64             58/73 
      Installing : giflib-4.1.6-9.el7.x86_64                                  59/73 
      Installing : fribidi-1.0.2-1.el7_7.1.x86_64                             60/73 
      Installing : copy-jdk-configs-3.3-10.el7_5.noarch                       61/73 
      Installing : libwayland-client-1.15.0-1.el7.x86_64                      62/73 
      Installing : 1:libglvnd-egl-1.0.1-0.8.git5baa1e5.el7.x86_64             63/73 
      Installing : mesa-libEGL-18.3.4-7.el7.x86_64                            64/73 
      Installing : cairo-1.15.12-4.el7.x86_64                                 65/73 
      Installing : pango-1.42.4-4.el7_7.x86_64                                66/73 
      Installing : alsa-lib-1.1.8-1.el7.x86_64                                67/73 
      Installing : hicolor-icon-theme-0.12-7.el7.noarch                       68/73 
      Installing : gtk2-2.24.31-1.el7.x86_64                                  69/73 
      Installing : tzdata-java-2020a-1.el7.noarch                             70/73 
      Installing : 1:java-1.8.0-openjdk-headless-1.8.0.252.b09-2.el7_8.x86_   71/73 
      Installing : 1:java-1.8.0-openjdk-1.8.0.252.b09-2.el7_8.x86_64          72/73 
      Installing : 1:openssl-1.0.2k-19.el7.x86_64                             73/73 
      Verifying  : libXext-1.3.3-3.el7.x86_64                                  1/73 
      Verifying  : mesa-libEGL-18.3.4-7.el7.x86_64                             2/73 
      Verifying  : libXi-1.7.9-1.el7.x86_64                                    3/73 
      Verifying  : fontconfig-2.13.0-4.3.el7.x86_64                            4/73 
      Verifying  : giflib-4.1.6-9.el7.x86_64                                   5/73 
      Verifying  : libpciaccess-0.14-1.el7.x86_64                              6/73 
      Verifying  : libXinerama-1.1.3-2.1.el7.x86_64                            7/73 
      Verifying  : libXrender-0.9.10-1.el7.x86_64                              8/73 
      Verifying  : 1:cups-libs-1.6.3-43.el7.x86_64                             9/73 
      Verifying  : libXxf86vm-1.1.4-1.el7.x86_64                              10/73 
      Verifying  : libwayland-server-1.15.0-1.el7.x86_64                      11/73 
      Verifying  : libXcursor-1.1.15-1.el7.x86_64                             12/73 
      Verifying  : tzdata-java-2020a-1.el7.noarch                             13/73 
      Verifying  : 2:libpng-1.5.13-7.el7_2.x86_64                             14/73 
      Verifying  : freetype-2.8-14.el7.x86_64                                 15/73 
      Verifying  : libICE-1.0.9-9.el7.x86_64                                  16/73 
      Verifying  : dejavu-fonts-common-2.33-6.el7.noarch                      17/73 
      Verifying  : fontpackages-filesystem-1.44-8.el7.noarch                  18/73 
      Verifying  : ttmkfdir-3.0.9-42.el7.x86_64                               19/73 
      Verifying  : hicolor-icon-theme-0.12-7.el7.noarch                       20/73 
      Verifying  : alsa-lib-1.1.8-1.el7.x86_64                                21/73 
      Verifying  : libwayland-client-1.15.0-1.el7.x86_64                      22/73 
      Verifying  : gdk-pixbuf2-2.36.12-3.el7.x86_64                           23/73 
      Verifying  : pango-1.42.4-4.el7_7.x86_64                                24/73 
      Verifying  : gtk2-2.24.31-1.el7.x86_64                                  25/73 
      Verifying  : copy-jdk-configs-3.3-10.el7_5.noarch                       26/73 
      Verifying  : python-javapackages-3.4.1-11.el7.noarch                    27/73 
      Verifying  : mesa-libgbm-18.3.4-7.el7.x86_64                            28/73 
      Verifying  : 1:java-1.8.0-openjdk-headless-1.8.0.252.b09-2.el7_8.x86_   29/73 
      Verifying  : libXcomposite-0.4.4-4.1.el7.x86_64                         30/73 
      Verifying  : fribidi-1.0.2-1.el7_7.1.x86_64                             31/73 
      Verifying  : libXtst-1.2.3-1.el7.x86_64                                 32/73 
      Verifying  : libX11-1.6.7-2.el7.x86_64                                  33/73 
      Verifying  : libX11-common-1.6.7-2.el7.noarch                           34/73 
      Verifying  : 1:java-1.8.0-openjdk-1.8.0.252.b09-2.el7_8.x86_64          35/73 
      Verifying  : 1:make-3.82-24.el7.x86_64                                  36/73 
      Verifying  : libdrm-2.4.97-2.el7.x86_64                                 37/73 
      Verifying  : mesa-libGL-18.3.4-7.el7.x86_64                             38/73 
      Verifying  : lksctp-tools-1.0.17-2.el7.x86_64                           39/73 
      Verifying  : gtk-update-icon-cache-3.22.30-5.el7.x86_64                 40/73 
      Verifying  : 1:libglvnd-1.0.1-0.8.git5baa1e5.el7.x86_64                 41/73 
      Verifying  : libjpeg-turbo-1.2.90-8.el7.x86_64                          42/73 
      Verifying  : libxcb-1.13-1.el7.x86_64                                   43/73 
      Verifying  : xorg-x11-fonts-Type1-7.5-9.el7.noarch                      44/73 
      Verifying  : hwdata-0.252-9.5.el7.x86_64                                45/73 
      Verifying  : harfbuzz-1.7.5-2.el7.x86_64                                46/73 
      Verifying  : libXft-2.3.2-2.el7.x86_64                                  47/73 
      Verifying  : libxslt-1.1.28-5.el7.x86_64                                48/73 
      Verifying  : 1:openssl-1.0.2k-19.el7.x86_64                             49/73 
      Verifying  : 1:libglvnd-glx-1.0.1-0.8.git5baa1e5.el7.x86_64             50/73 
      Verifying  : dejavu-sans-fonts-2.33-6.el7.noarch                        51/73 
      Verifying  : libXrandr-1.5.1-2.el7.x86_64                               52/73 
      Verifying  : pcsc-lite-libs-1.8.8-8.el7.x86_64                          53/73 
      Verifying  : javapackages-tools-3.4.1-11.el7.noarch                     54/73 
      Verifying  : jbigkit-libs-2.0-11.el7.x86_64                             55/73 
      Verifying  : cairo-1.15.12-4.el7.x86_64                                 56/73 
      Verifying  : mesa-libglapi-18.3.4-7.el7.x86_64                          57/73 
      Verifying  : libxshmfence-1.2-1.el7.x86_64                              58/73 
      Verifying  : libXau-1.0.8-2.1.el7.x86_64                                59/73 
      Verifying  : libtiff-4.0.3-32.el7.x86_64                                60/73 
      Verifying  : libSM-1.2.2-2.el7.x86_64                                   61/73 
      Verifying  : jasper-libs-1.900.1-33.el7.x86_64                          62/73 
      Verifying  : graphite2-1.3.10-1.el7_3.x86_64                            63/73 
      Verifying  : 1:xorg-x11-font-utils-7.5-21.el7.x86_64                    64/73 
      Verifying  : python-lxml-3.2.1-4.el7.x86_64                             65/73 
      Verifying  : libthai-0.1.14-9.el7.x86_64                                66/73 
      Verifying  : libXdamage-1.1.4-4.1.el7.x86_64                            67/73 
      Verifying  : libXfixes-5.0.3-1.el7.x86_64                               68/73 
      Verifying  : atk-2.28.1-2.el7.x86_64                                    69/73 
      Verifying  : libfontenc-1.1.3-3.el7.x86_64                              70/73 
      Verifying  : avahi-libs-0.6.31-20.el7.x86_64                            71/73 
      Verifying  : 1:libglvnd-egl-1.0.1-0.8.git5baa1e5.el7.x86_64             72/73 
      Verifying  : pixman-0.34.0-1.el7.x86_64                                 73/73 
    
    Installed:
      java-1.8.0-openjdk.x86_64 1:1.8.0.252.b09-2.el7_8                             
      openssl.x86_64 1:1.0.2k-19.el7                                                
    
    Dependency Installed:
      alsa-lib.x86_64 0:1.1.8-1.el7                                                 
      atk.x86_64 0:2.28.1-2.el7                                                     
      avahi-libs.x86_64 0:0.6.31-20.el7                                             
      cairo.x86_64 0:1.15.12-4.el7                                                  
      copy-jdk-configs.noarch 0:3.3-10.el7_5                                        
      cups-libs.x86_64 1:1.6.3-43.el7                                               
      dejavu-fonts-common.noarch 0:2.33-6.el7                                       
      dejavu-sans-fonts.noarch 0:2.33-6.el7                                         
      fontconfig.x86_64 0:2.13.0-4.3.el7                                            
      fontpackages-filesystem.noarch 0:1.44-8.el7                                   
      freetype.x86_64 0:2.8-14.el7                                                  
      fribidi.x86_64 0:1.0.2-1.el7_7.1                                              
      gdk-pixbuf2.x86_64 0:2.36.12-3.el7                                            
      giflib.x86_64 0:4.1.6-9.el7                                                   
      graphite2.x86_64 0:1.3.10-1.el7_3                                             
      gtk-update-icon-cache.x86_64 0:3.22.30-5.el7                                  
      gtk2.x86_64 0:2.24.31-1.el7                                                   
      harfbuzz.x86_64 0:1.7.5-2.el7                                                 
      hicolor-icon-theme.noarch 0:0.12-7.el7                                        
      hwdata.x86_64 0:0.252-9.5.el7                                                 
      jasper-libs.x86_64 0:1.900.1-33.el7                                           
      java-1.8.0-openjdk-headless.x86_64 1:1.8.0.252.b09-2.el7_8                    
      javapackages-tools.noarch 0:3.4.1-11.el7                                      
      jbigkit-libs.x86_64 0:2.0-11.el7                                              
      libICE.x86_64 0:1.0.9-9.el7                                                   
      libSM.x86_64 0:1.2.2-2.el7                                                    
      libX11.x86_64 0:1.6.7-2.el7                                                   
      libX11-common.noarch 0:1.6.7-2.el7                                            
      libXau.x86_64 0:1.0.8-2.1.el7                                                 
      libXcomposite.x86_64 0:0.4.4-4.1.el7                                          
      libXcursor.x86_64 0:1.1.15-1.el7                                              
      libXdamage.x86_64 0:1.1.4-4.1.el7                                             
      libXext.x86_64 0:1.3.3-3.el7                                                  
      libXfixes.x86_64 0:5.0.3-1.el7                                                
      libXft.x86_64 0:2.3.2-2.el7                                                   
      libXi.x86_64 0:1.7.9-1.el7                                                    
      libXinerama.x86_64 0:1.1.3-2.1.el7                                            
      libXrandr.x86_64 0:1.5.1-2.el7                                                
      libXrender.x86_64 0:0.9.10-1.el7                                              
      libXtst.x86_64 0:1.2.3-1.el7                                                  
      libXxf86vm.x86_64 0:1.1.4-1.el7                                               
      libdrm.x86_64 0:2.4.97-2.el7                                                  
      libfontenc.x86_64 0:1.1.3-3.el7                                               
      libglvnd.x86_64 1:1.0.1-0.8.git5baa1e5.el7                                    
      libglvnd-egl.x86_64 1:1.0.1-0.8.git5baa1e5.el7                                
      libglvnd-glx.x86_64 1:1.0.1-0.8.git5baa1e5.el7                                
      libjpeg-turbo.x86_64 0:1.2.90-8.el7                                           
      libpciaccess.x86_64 0:0.14-1.el7                                              
      libpng.x86_64 2:1.5.13-7.el7_2                                                
      libthai.x86_64 0:0.1.14-9.el7                                                 
      libtiff.x86_64 0:4.0.3-32.el7                                                 
      libwayland-client.x86_64 0:1.15.0-1.el7                                       
      libwayland-server.x86_64 0:1.15.0-1.el7                                       
      libxcb.x86_64 0:1.13-1.el7                                                    
      libxshmfence.x86_64 0:1.2-1.el7                                               
      libxslt.x86_64 0:1.1.28-5.el7                                                 
      lksctp-tools.x86_64 0:1.0.17-2.el7                                            
      make.x86_64 1:3.82-24.el7                                                     
      mesa-libEGL.x86_64 0:18.3.4-7.el7                                             
      mesa-libGL.x86_64 0:18.3.4-7.el7                                              
      mesa-libgbm.x86_64 0:18.3.4-7.el7                                             
      mesa-libglapi.x86_64 0:18.3.4-7.el7                                           
      pango.x86_64 0:1.42.4-4.el7_7                                                 
      pcsc-lite-libs.x86_64 0:1.8.8-8.el7                                           
      pixman.x86_64 0:0.34.0-1.el7                                                  
      python-javapackages.noarch 0:3.4.1-11.el7                                     
      python-lxml.x86_64 0:3.2.1-4.el7                                              
      ttmkfdir.x86_64 0:3.0.9-42.el7                                                
      tzdata-java.noarch 0:2020a-1.el7                                              
      xorg-x11-font-utils.x86_64 1:7.5-21.el7                                       
      xorg-x11-fonts-Type1.noarch 0:7.5-9.el7                                       
    
    Complete!



```bash
ansible kafka -m command -a "docker exec sinetstream-kafka tar xf /sinetstream-kafka/$KAFKA.tgz" &&
ansible kafka -m command -a "docker exec sinetstream-kafka ln -s /$KAFKA /kafka"
```

    server1.example.jp | CHANGED | rc=0 >>
    
    server1.example.jp | CHANGED | rc=0 >>
    


### Kafkaブローカの設定

kafkaブローカの設定ファイルを生成する。


```bash
LIST_KAFKA_HOSTS="$(ansible-inventory --list  | jq  -r '.kafka.hosts|.[]')"
list_kafka_hosts() {
    echo "$LIST_KAFKA_HOSTS"
}

print_server_properties() {
    local HOST="$1"
    local ID="$2"
    
    echo "broker.id=${ID}"
    
    local ZKHOST
    printf "zookeeper.connect="
    list_zookeeper_hosts | sed "s/\$/:${ZK_CPORT}/" | paste -s -d,

    printf "listeners="
    {
        case "$KAFKA_AUTH" in
        PLAINTEXT) echo "PLAINTEXT://:${KAFKA_PORT_PLAINTEXT}" ;;
        SSL)       echo "SSL://:${KAFKA_PORT_SSL}" ;;
        SASL_SSL*) echo "SASL_SSL://:${KAFKA_PORT_SASL_SSL}"
                   echo "SSL://:$((KAFKA_PORT_SASL_SSL+1))" ;;
        esac
    } | paste -s -d,
    
    printf "advertised.listeners="
    {
        case "$KAFKA_AUTH" in
        PLAINTEXT) echo "PLAINTEXT://${HOST}:${KAFKA_PORT_PLAINTEXT}" ;;
        SSL)       echo "SSL://${HOST}:${KAFKA_PORT_SSL}" ;;
        SASL_SSL*) echo "SASL_SSL://${HOST}:${KAFKA_PORT_SASL_SSL}"
                   echo "SSL://${HOST}:$((KAFKA_PORT_SASL_SSL+1))" ;; # for inter-broker
        esac
    } | paste -s -d,
    

    # CA証明書の設定
    echo "ssl.truststore.location=/sinetstream-kafka/truststore.p12"
    echo "ssl.truststore.password=${TRUSTSTORE_PASSWORD}"
    echo "ssl.truststore.type=pkcs12"
    # サーバー秘密鍵の設定
    echo "ssl.keystore.location=/sinetstream-kafka/keystore.p12"
    echo "ssl.keystore.password=${KEYSTORE_PASSWORD}"
    echo "ssl.keystore.type=pkcs12"
        
    case "$KAFKA_AUTH" in
    SSL)
        # SSL/TLS認証（クライアント認証）
        echo "ssl.client.auth=required"
        echo "security.inter.broker.protocol=SSL"
        ;;
    SASL_SSL_SCRAM)
        # SCRAM認証/TLS
        echo "ssl.client.auth=required"
        echo "security.inter.broker.protocol=SSL"
        echo "sasl.enabled.mechanisms=${SCRAM_MECHANISM}"
        #echo "sasl.mechanism.inter.broker.protocol=${SCRAM_MECHANISM}"
        local scram_mechanism="$(echo "${SCRAM_MECHANISM}" | tr '[A-Z]' '[a-z]')"
        echo "listener.name.sasl_ssl.${scram_mechanism}.sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required \\"
        echo "    username=admin password=${PASSWORD_admin};"
        ;;
    SASL_SSL_PLAIN)
        # パスワード認証/TLS
        echo "ssl.client.auth=required"
        echo "security.inter.broker.protocol=SSL"
        echo "sasl.enabled.mechanisms=PLAIN"
        #echo "sasl.mechanism.inter.broker.protocol=PLAIN"
        echo "listener.name.sasl_ssl.plain.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required \\"
        echo "    username=admin password=${PASSWORD_admin} \\"
        local USER PASSWORD
        for USER in ${USER_LIST}; do
            eval PASSWORD=\$PASSWORD_${USER}
            echo "    user_${USER}=\"${PASSWORD}\" \\"
        done
        echo "    ;"
        ;;
    esac
    
    # 認可
    echo "authorizer.class.name=kafka.security.auth.SimpleAclAuthorizer"  # ZooKeeperに記録されているACL設定による認可
    echo "allow.everyone.if.no.acl.found=${KAFKA_ACL_DEFAULT_TO_ALLOW}"
    echo "super.users=User:admin"  # adminには特権を与える
}

ID=1
tar x -f $KAFKA.tgz --to-stdout $KAFKA/config/server.properties >server.properties  &&
mkdir -p tmp  &&
rm -f tmp/*.properties  &&
list_kafka_hosts | while read HOST; do
    {
        cat server.properties
        print_server_properties "$HOST" "$ID"
    } >"tmp/server-${HOST}.properties"
    ID=$((ID + 1))
done
```


```bash
ls -l tmp/server-*.properties
```

    -rw-r--r-- 1 jovyan users 7644 May 12 18:44 tmp/server-server1.example.jp.properties


kafkaブローカの設定ファイルを各ホストにコピーする。


```bash
ansible kafka -m copy -a "src=tmp/server-{{inventory_hostname}}.properties dest=\$PWD/sinetstream-kafka/server.properties"
```

    server1.example.jp | CHANGED => {
        "changed": true,
        "checksum": "8db15549d6e82be4c5c3a93360f97de6cfbf0573",
        "dest": "/home/piyo/sinetstream-kafka/server.properties",
        "gid": 1004,
        "group": "piyo",
        "md5sum": "1f3d845ad03be8bfef632b72d54336ab",
        "mode": "0664",
        "owner": "piyo",
        "size": 7644,
        "src": "/home/piyo/.ansible/tmp/ansible-tmp-1589276689.879836-22666949452525/source",
        "state": "file",
        "uid": 1004
    }


### SSL/TLSのための証明書を設定

opensslをつかってPEM形式の証明書をkafkaブローカが扱えるPKCS#12(p12)形式に変換する。

CA証明書・サーバ秘密鍵・サーバ証明書をkafkaブローカの動かすコンテナ内にコピーする。

自己署名CA証明書の場合はCA秘密鍵もコピーする。


```bash
CA_CERT_PATH=./cacert.pem
CA_KEY_PATH=NONE  
CA_KEY_PATH=./cakey.pem  # CA証明書が自己署名の場合はCA秘密鍵も指定する

BROKER_CERT_PATH=./broker.crt
BROKER_KEY_PATH=./broker.key

# 以下、変更しなくてよい
CA_CERT_FILE=$(basename "${CA_CERT_PATH}")
BROKER_CERT_FILE=$(basename "${BROKER_CERT_PATH}")
BROKER_KEY_FILE=$(basename "${BROKER_KEY_PATH}")
if [ "x$CA_KEY_PATH" != "xNONE" ]; then
    CA_KEY_FILE=$(basename "${CA_KEY_PATH}")
else
    CA_KEY_FILE=""
fi
```


```bash
ansible kafka -m copy -a "src=${CA_CERT_PATH} dest=\$PWD/sinetstream-kafka/${CA_CERT_FILE}" &&
ansible kafka -m copy -a "src=${BROKER_CERT_PATH} dest=\$PWD/sinetstream-kafka/${BROKER_CERT_FILE}" &&
ansible kafka -m copy -a "src=${BROKER_KEY_PATH} dest=\$PWD/sinetstream-kafka/${BROKER_KEY_FILE}" &&
if [ -n "${CA_KEY_FILE}" ]; then
    ansible kafka -m copy -a "src=${CA_KEY_PATH} dest=\$PWD/sinetstream-kafka/${CA_KEY_FILE}"
fi
```

    server1.example.jp | SUCCESS => {
        "changed": false,
        "checksum": "43513e67aa1278fdd15ad23304971edc3f6dda52",
        "dest": "/home/piyo/sinetstream-kafka/cacert.pem",
        "gid": 1004,
        "group": "piyo",
        "mode": "0664",
        "owner": "piyo",
        "path": "/home/piyo/sinetstream-kafka/cacert.pem",
        "size": 4349,
        "state": "file",
        "uid": 1004
    }
    server1.example.jp | SUCCESS => {
        "changed": false,
        "checksum": "d92b90e240f0bf59677367354ffe2ce6e5f5c8c6",
        "dest": "/home/piyo/sinetstream-kafka/broker.crt",
        "gid": 1004,
        "group": "piyo",
        "mode": "0664",
        "owner": "piyo",
        "path": "/home/piyo/sinetstream-kafka/broker.crt",
        "size": 4389,
        "state": "file",
        "uid": 1004
    }
    server1.example.jp | SUCCESS => {
        "changed": false,
        "checksum": "cf6364f56c6ec29b2acdb40e3ede96fe77821585",
        "dest": "/home/piyo/sinetstream-kafka/broker.key",
        "gid": 1004,
        "group": "piyo",
        "mode": "0664",
        "owner": "piyo",
        "path": "/home/piyo/sinetstream-kafka/broker.key",
        "size": 1708,
        "state": "file",
        "uid": 1004
    }
    server1.example.jp | SUCCESS => {
        "changed": false,
        "checksum": "3fd725769a1ce97b7087a17af1bf1fc51a102b81",
        "dest": "/home/piyo/sinetstream-kafka/cakey.pem",
        "gid": 1004,
        "group": "piyo",
        "mode": "0664",
        "owner": "piyo",
        "path": "/home/piyo/sinetstream-kafka/cakey.pem",
        "size": 1708,
        "state": "file",
        "uid": 1004
    }


CA証明書を変換してtruststoreに登録する。


```bash
ansible kafka -m command -a "docker exec sinetstream-kafka \
  openssl pkcs12 -export \
    -in sinetstream-kafka/${CA_CERT_FILE} \
    ${CA_KEY_FILE:+-inkey sinetstream-kafka/${CA_KEY_FILE}} \
    -name private-ca \
    -CAfile sinetstream-kafka/${CA_CERT_FILE}\
    -caname private-ca \
    -out sinetstream-kafka/truststore.p12 \
    -passout pass:${TRUSTSTORE_PASSWORD}" &&
ansible kafka -m command -a "docker exec sinetstream-kafka \
  openssl pkcs12 -in sinetstream-kafka/truststore.p12 -passin pass:${TRUSTSTORE_PASSWORD} -info -noout"
```

    server1.example.jp | CHANGED | rc=0 >>
    
    server1.example.jp | CHANGED | rc=0 >>
    MAC Iteration 2048
    MAC verified OK
    PKCS7 Encrypted data: pbeWithSHA1And40BitRC2-CBC, Iteration 2048
    Certificate bag
    PKCS7 Data
    Shrouded Keybag: pbeWithSHA1And3-KeyTripleDES-CBC, Iteration 2048


サーバ秘密鍵・サーバ証明書・CA証明書を変換してkeystoreに登録する。


```bash
ansible kafka -m command -a "docker exec sinetstream-kafka \
  openssl pkcs12 -export \
    -in sinetstream-kafka/${BROKER_CERT_FILE} \
    -inkey sinetstream-kafka/${BROKER_KEY_FILE} \
    -name broker \
    -CAfile sinetstream-kafka/${CA_CERT_FILE} \
    -caname private-ca \
    -out sinetstream-kafka/keystore.p12 \
    -passout pass:${KEYSTORE_PASSWORD}" &&
ansible kafka -m command -a "docker exec sinetstream-kafka \
  openssl pkcs12 -in sinetstream-kafka/keystore.p12 -passin pass:${KEYSTORE_PASSWORD} -info -noout"
```

    server1.example.jp | CHANGED | rc=0 >>
    
    server1.example.jp | CHANGED | rc=0 >>
    MAC Iteration 2048
    MAC verified OK
    PKCS7 Encrypted data: pbeWithSHA1And40BitRC2-CBC, Iteration 2048
    Certificate bag
    PKCS7 Data
    Shrouded Keybag: pbeWithSHA1And3-KeyTripleDES-CBC, Iteration 2048


### SCRAM認証の設定

パスワードをzookeeperに保存する。


```bash
if [ "x$KAFKA_AUTH" = "xSASL_SSL_SCRAM" ]; then
    ZK1="$(list_zookeeper_hosts | head -1)"
    KAFKA1="$(list_kafka_hosts | head -1)"
    for USER in admin ${USER_LIST}; do
        eval PASSWORD=\$PASSWORD_${USER}
        ansible kafka --limit="${KAFKA1}" -m command -a "docker exec sinetstream-kafka \
            /kafka/bin/kafka-configs.sh --zookeeper ${ZK1}:${ZK_CPORT} --alter \
                --entity-type users \
                --entity-name ${USER} \
                --add-config 'SCRAM-SHA-256=[iterations=8192,password=${PASSWORD}]'"
    done &&
    ansible kafka -m command -a "docker exec sinetstream-kafka \
            /kafka/bin/kafka-configs.sh --zookeeper ${ZK1}:${ZK_CPORT} --describe --entity-type users"
fi
```

    server1.example.jp | CHANGED | rc=0 >>
    Completed Updating config for entity: user-principal 'admin'.
    server1.example.jp | CHANGED | rc=0 >>
    Completed Updating config for entity: user-principal 'user01'.
    server1.example.jp | CHANGED | rc=0 >>
    Completed Updating config for entity: user-principal 'user02'.
    server1.example.jp | CHANGED | rc=0 >>
    Completed Updating config for entity: user-principal 'user03'.
    server1.example.jp | CHANGED | rc=0 >>
    Completed Updating config for entity: user-principal 'CN=client0,C=JP'.
    server1.example.jp | CHANGED | rc=0 >>
    Configs for user-principal 'admin' are SCRAM-SHA-256=salt=bW5hOGN4MWhqdDFnb2x6M3JzZjZyNmkzdA==,stored_key=ABCz61QlROA189AQ08lwSJfccwrPHGfIsGbjJo0ytBQ=,server_key=VYAOL6tjsvIi/dBl9eLMFFo6eKRiuQPSYGbCaEFrj4w=,iterations=8192
    Configs for user-principal 'user03' are SCRAM-SHA-256=salt=aWp1a2tmb3FsMzBxbjMxZ2lyOTV6dWltaw==,stored_key=GXmoPMFH43u6FrFAXGbE8vno8LutImQsdob86BptD/E=,server_key=/dtVne4q1dDvJgeDXryufJqaPCwxdyj8dnaDrWxKpME=,iterations=8192
    Configs for user-principal 'user02' are SCRAM-SHA-256=salt=MWRhZnh3bHllb2E1ZGhhNGpneGszbGY1Zno=,stored_key=SfI9vcJYIkGvVdKuMyyElLZSGOivJIZDyC8jqVvDxr0=,server_key=mcEbH92Q5PVzEzM0gk3HAhtRwMedaGygc+Zqm4zXSLY=,iterations=8192
    Configs for user-principal 'CN=client0,C=JP' are SCRAM-SHA-256=salt=MWV4b2o4MnFseWxqeHJxeWN0dWllc3Z1b2s=,stored_key=MfKFvHiIHr21OZN7xCqvu0tJxkCgFaVdg2eZ8jRNibc=,server_key=EtSsd7UknoF+DgjMOb894IzfN3YoawbNSomSyGRwVhU=,iterations=8192
    Configs for user-principal 'user01' are SCRAM-SHA-256=salt=bmZ0Y25xeHFuNTlqcGhtbWYzOTF3cGE1OA==,stored_key=DAqDLQTHYohZ8wamY0kXww8cvUfM4LqTwG01K/viiHs=,server_key=PiY3omsrnG6xJRrvpLPGEvGiYUp+74Pdgpqj8Uo4CRE=,iterations=8192


### 認可(ACL)の設定

ブローカがつかっているサーバ証明書のCommon Nameを設定する。ブローカ間通信の認可で必要となる。


```bash
ADMIN_USER="CN=server1.example.jp,C=JP"
```


```bash
ZK1="$(list_zookeeper_hosts | head -1)"
KAFKA1="$(list_kafka_hosts | head -1)"

ansible kafka --limit="${KAFKA1}" -m command -a "docker exec sinetstream-kafka \
    /kafka/bin/kafka-acls.sh --authorizer-properties zookeeper.connect=${ZK1}:${ZK_CPORT} \
    --add --allow-principal User:${ADMIN_USER} --cluster --operation All"  &&

for USER in ${USER_LIST}; do
    USER1=$(echo "$USER" | sed 's/[^[:alnum:]]/_/g')  # サニタイズ
    eval ACL=\$ACL_${USER1}
    case "${ACL}" in
    *write*)
        ansible kafka --limit="${KAFKA1}" -m command -a "docker exec sinetstream-kafka \
            /kafka/bin/kafka-acls.sh --authorizer-properties zookeeper.connect=${ZK1}:${ZK_CPORT} \
            --add --allow-principal User:${USER} \
            --producer --topic '*'"
            ;;
    esac
    case "${ACL}" in
    *read*)
        ansible kafka --limit="${KAFKA1}" -m command -a "docker exec sinetstream-kafka \
            /kafka/bin/kafka-acls.sh --authorizer-properties zookeeper.connect=${ZK1}:${ZK_CPORT} \
            --add --allow-principal User:${USER} \
            --consumer --topic '*' --group '*'"
        ;;
    esac
done 
```

    server1.example.jp | CHANGED | rc=0 >>
    Adding ACLs for resource `ResourcePattern(resourceType=CLUSTER, name=kafka-cluster, patternType=LITERAL)`: 
     	(principal=User:CN=server1.example.jp,C=JP, host=*, operation=ALL, permissionType=ALLOW) 
    
    Current ACLs for resource `Cluster:LITERAL:kafka-cluster`: 
     	User:CN=server1.example.jp,C=JP has Allow permission for operations: All from hosts: * 
    server1.example.jp | CHANGED | rc=0 >>
    Adding ACLs for resource `ResourcePattern(resourceType=TOPIC, name=*, patternType=LITERAL)`: 
     	(principal=User:user01, host=*, operation=DESCRIBE, permissionType=ALLOW)
    	(principal=User:user01, host=*, operation=WRITE, permissionType=ALLOW)
    	(principal=User:user01, host=*, operation=CREATE, permissionType=ALLOW) 
    
    Current ACLs for resource `Topic:LITERAL:*`: 
     	User:user01 has Allow permission for operations: Describe from hosts: *
    	User:user01 has Allow permission for operations: Write from hosts: *
    	User:user01 has Allow permission for operations: Create from hosts: * 
    server1.example.jp | CHANGED | rc=0 >>
    Adding ACLs for resource `ResourcePattern(resourceType=TOPIC, name=*, patternType=LITERAL)`: 
     	(principal=User:user01, host=*, operation=READ, permissionType=ALLOW)
    	(principal=User:user01, host=*, operation=DESCRIBE, permissionType=ALLOW) 
    
    Adding ACLs for resource `ResourcePattern(resourceType=GROUP, name=*, patternType=LITERAL)`: 
     	(principal=User:user01, host=*, operation=READ, permissionType=ALLOW) 
    
    Current ACLs for resource `Topic:LITERAL:*`: 
     	User:user01 has Allow permission for operations: Describe from hosts: *
    	User:user01 has Allow permission for operations: Write from hosts: *
    	User:user01 has Allow permission for operations: Create from hosts: *
    	User:user01 has Allow permission for operations: Read from hosts: * 
    
    Current ACLs for resource `Group:LITERAL:*`: 
     	User:user01 has Allow permission for operations: Read from hosts: * 
    server1.example.jp | CHANGED | rc=0 >>
    Adding ACLs for resource `ResourcePattern(resourceType=TOPIC, name=*, patternType=LITERAL)`: 
     	(principal=User:user02, host=*, operation=CREATE, permissionType=ALLOW)
    	(principal=User:user02, host=*, operation=DESCRIBE, permissionType=ALLOW)
    	(principal=User:user02, host=*, operation=WRITE, permissionType=ALLOW) 
    
    Current ACLs for resource `Topic:LITERAL:*`: 
     	User:user01 has Allow permission for operations: Write from hosts: *
    	User:user01 has Allow permission for operations: Create from hosts: *
    	User:user02 has Allow permission for operations: Write from hosts: *
    	User:user01 has Allow permission for operations: Describe from hosts: *
    	User:user01 has Allow permission for operations: Read from hosts: *
    	User:user02 has Allow permission for operations: Describe from hosts: *
    	User:user02 has Allow permission for operations: Create from hosts: * 
    server1.example.jp | CHANGED | rc=0 >>
    Adding ACLs for resource `ResourcePattern(resourceType=TOPIC, name=*, patternType=LITERAL)`: 
     	(principal=User:user03, host=*, operation=DESCRIBE, permissionType=ALLOW)
    	(principal=User:user03, host=*, operation=READ, permissionType=ALLOW) 
    
    Adding ACLs for resource `ResourcePattern(resourceType=GROUP, name=*, patternType=LITERAL)`: 
     	(principal=User:user03, host=*, operation=READ, permissionType=ALLOW) 
    
    Current ACLs for resource `Topic:LITERAL:*`: 
     	User:user01 has Allow permission for operations: Write from hosts: *
    	User:user01 has Allow permission for operations: Create from hosts: *
    	User:user03 has Allow permission for operations: Read from hosts: *
    	User:user02 has Allow permission for operations: Write from hosts: *
    	User:user01 has Allow permission for operations: Describe from hosts: *
    	User:user01 has Allow permission for operations: Read from hosts: *
    	User:user02 has Allow permission for operations: Describe from hosts: *
    	User:user02 has Allow permission for operations: Create from hosts: *
    	User:user03 has Allow permission for operations: Describe from hosts: * 
    
    Current ACLs for resource `Group:LITERAL:*`: 
     	User:user03 has Allow permission for operations: Read from hosts: *
    	User:user01 has Allow permission for operations: Read from hosts: * 
    server1.example.jp | CHANGED | rc=0 >>
    Adding ACLs for resource `ResourcePattern(resourceType=TOPIC, name=*, patternType=LITERAL)`: 
     	(principal=User:CN=client0,C=JP, host=*, operation=DESCRIBE, permissionType=ALLOW)
    	(principal=User:CN=client0,C=JP, host=*, operation=WRITE, permissionType=ALLOW)
    	(principal=User:CN=client0,C=JP, host=*, operation=CREATE, permissionType=ALLOW) 
    
    Current ACLs for resource `Topic:LITERAL:*`: 
     	User:user01 has Allow permission for operations: Write from hosts: *
    	User:user01 has Allow permission for operations: Create from hosts: *
    	User:CN=client0,C=JP has Allow permission for operations: Describe from hosts: *
    	User:CN=client0,C=JP has Allow permission for operations: Create from hosts: *
    	User:CN=client0,C=JP has Allow permission for operations: Write from hosts: *
    	User:user03 has Allow permission for operations: Read from hosts: *
    	User:user02 has Allow permission for operations: Write from hosts: *
    	User:user01 has Allow permission for operations: Describe from hosts: *
    	User:user01 has Allow permission for operations: Read from hosts: *
    	User:user02 has Allow permission for operations: Describe from hosts: *
    	User:user02 has Allow permission for operations: Create from hosts: *
    	User:user03 has Allow permission for operations: Describe from hosts: * 
    server1.example.jp | CHANGED | rc=0 >>
    Adding ACLs for resource `ResourcePattern(resourceType=TOPIC, name=*, patternType=LITERAL)`: 
     	(principal=User:CN=client0,C=JP, host=*, operation=READ, permissionType=ALLOW)
    	(principal=User:CN=client0,C=JP, host=*, operation=DESCRIBE, permissionType=ALLOW) 
    
    Adding ACLs for resource `ResourcePattern(resourceType=GROUP, name=*, patternType=LITERAL)`: 
     	(principal=User:CN=client0,C=JP, host=*, operation=READ, permissionType=ALLOW) 
    
    Current ACLs for resource `Topic:LITERAL:*`: 
     	User:CN=client0,C=JP has Allow permission for operations: Read from hosts: *
    	User:user01 has Allow permission for operations: Write from hosts: *
    	User:user01 has Allow permission for operations: Create from hosts: *
    	User:CN=client0,C=JP has Allow permission for operations: Describe from hosts: *
    	User:CN=client0,C=JP has Allow permission for operations: Create from hosts: *
    	User:CN=client0,C=JP has Allow permission for operations: Write from hosts: *
    	User:user03 has Allow permission for operations: Read from hosts: *
    	User:user02 has Allow permission for operations: Write from hosts: *
    	User:user01 has Allow permission for operations: Describe from hosts: *
    	User:user01 has Allow permission for operations: Read from hosts: *
    	User:user02 has Allow permission for operations: Describe from hosts: *
    	User:user02 has Allow permission for operations: Create from hosts: *
    	User:user03 has Allow permission for operations: Describe from hosts: * 
    
    Current ACLs for resource `Group:LITERAL:*`: 
     	User:user03 has Allow permission for operations: Read from hosts: *
    	User:CN=client0,C=JP has Allow permission for operations: Read from hosts: *
    	User:user01 has Allow permission for operations: Read from hosts: * 



```bash
ansible kafka --limit="${KAFKA1}" -m command -a "docker exec sinetstream-kafka \
        /kafka/bin/kafka-acls.sh --authorizer-properties zookeeper.connect=${ZK1}:${ZK_CPORT} \
        --list"
```

    server1.example.jp | CHANGED | rc=0 >>
    Current ACLs for resource `Group:LITERAL:*`: 
     	User:user03 has Allow permission for operations: Read from hosts: *
    	User:CN=client0,C=JP has Allow permission for operations: Read from hosts: *
    	User:user01 has Allow permission for operations: Read from hosts: * 
    
    Current ACLs for resource `Topic:LITERAL:*`: 
     	User:CN=client0,C=JP has Allow permission for operations: Read from hosts: *
    	User:user01 has Allow permission for operations: Write from hosts: *
    	User:user01 has Allow permission for operations: Create from hosts: *
    	User:CN=client0,C=JP has Allow permission for operations: Describe from hosts: *
    	User:CN=client0,C=JP has Allow permission for operations: Create from hosts: *
    	User:CN=client0,C=JP has Allow permission for operations: Write from hosts: *
    	User:user03 has Allow permission for operations: Read from hosts: *
    	User:user02 has Allow permission for operations: Write from hosts: *
    	User:user01 has Allow permission for operations: Describe from hosts: *
    	User:user01 has Allow permission for operations: Read from hosts: *
    	User:user02 has Allow permission for operations: Describe from hosts: *
    	User:user02 has Allow permission for operations: Create from hosts: *
    	User:user03 has Allow permission for operations: Describe from hosts: * 
    
    Current ACLs for resource `Cluster:LITERAL:kafka-cluster`: 
     	User:CN=server1.example.jp,C=JP has Allow permission for operations: All from hosts: * 


### Kafkaブローカー起動


```bash
ansible kafka -m command -a "docker exec --detach sinetstream-kafka \
   /kafka/bin/kafka-server-start.sh /sinetstream-kafka/server.properties"
```

    server1.example.jp | CHANGED | rc=0 >>
    

