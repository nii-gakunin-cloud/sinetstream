<!--
Copyright (C) 2020-2021 National Institute of Informatics

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

[English](TUTORIAL-DOCKER-CONTAINER.en.md)

# チュートリアル - DOCKER-CONTAINER

<em>目次</em>
<pre>
1. 概要
2. 動作基盤の整備
2.1 Amazon AWSのEC2をホスト機材とする場合
2.1.1 EC2インスタンスの作成と起動
2.1.2 EC2インスタンスへのDocker Engineの導入
2.2 手元機材をホストとする場合
2.2.1 手元機材の調達
2.2.2 手元機材へのDocker Engineの導入
3. コンテナイメージの操作
3.1 brokerコンテナの導入と起動
3.2 brokerコンテナの停止と再起動
3.3 brokerコンテナの削除と再導入
</pre>


## 1. 概要

本書では、アプリケーション仮想実行環境である`Docker`コンテナ上に`Broker`などバックエンド機能を構築する方法について紹介します。


## 2. 動作基盤の整備

`Docker Engine`はLinuxやmacOS、Windowsなどさまざまな動作環境に対応しています。
まずは適当な機材にこれを導入するところから始めます。
次に、チュートリアル用に用意したコンテナイメージをインターネット経由でダウンロードし、上記`Docker Engine`環境で起動するという作業の流れとなります。

### 2.1 Amazon AWSのEC2をホスト機材とする場合

商用クラウドサービスの一つとして広く使われている仮想サーバ基盤
[Amazon EC2](https://aws.amazon.com/jp/ec2/?nc1=h_ls)
（OSに
[Amazon Linux 2](https://aws.amazon.com/jp/amazon-linux-2/)
を採用）を利用します。この上に
[Dockerコンテナ](https://www.docker.com/resources/what-container)
を導入して`Broker`などバックエンド機能を載せるという構成です。

```
    [Android]-----(Celluar)-----(INTERNET)-----[Amazon AWS]
```
この場合、Android端末上のクライアントと`Amazon AWS`の`EC2`とはインターネット経由で接続します。

#### 2.1.1 EC2インスタンスの作成と起動

AWSコンソール上の操作により適当なEC2インスタンスを作成して起動すると、グローバルIPアドレスやSSHログイン認証鍵が払い出されます。
当該EC2インスタンスにSSHログインしてください。

```console
[localUser@localPC]$ ssh ec2-user@aws-ipaddress

       __|  __|_  )
       _|  (     /   Amazon Linux 2 AMI
      ___|\___|___|

https://aws.amazon.com/amazon-linux-2/
```

> EC2インスタンスのメモリ不足に注意！
> -----------------------------------
> 筆者が動作試験をした際、当初はEC2インスタンスタイプとして最小
> 構成の`t2.micro`（メモリ2GB）を選択しました。
> 単純に`MQTT`ブローカを置いて「受信メッセージを再配信する」には
> 問題ないのです。しかし「同ブローカからさらに`Kafka`ブローカを
> 介してセンサー情報を可視化するバックエンドシステム」を構築しよ
> うとして動作不安定（グラフ表示されない）事象が発生しました。
> Kafkaブローカがメモリ不足で異常終了し、supervisorがこれを自動
> 再起動するという状態を繰り返すことが原因でした。
> 結局、より容量の大きな`t3.large`（メモリ8GB）でEC2インスタンス
> を作り直したことで本事象を解消しました。

#### 2.1.2 EC2インスタンスへの`Docker Engine`の導入

Amazon EC2への`Docker Engine`導入方法に関してはAWSから当該文書
[Amazon ECSにおけるDockerの基本](https://docs.aws.amazon.com/ja_jp/AmazonECS/latest/developerguide/docker-basics.html)
が用意されているので、こちらを適宜参照してください。
文中で`Docker イメージの作成`の手前までが該当します。

まずはシステムを最新化します。
```console
[ec2-user@ip-172-29-2-12 ~]$ sudo yum update
[sudo] password for ec2-user:
Loaded plugins: extras_suggestions, langpacks, priorities, update-motd
amzn2-core                                               | 3.7 kB     00:00
No packages marked for update
```

AWSの案内に従い、所用のパッケージを導入します。
```console
[ec2-user@ip-172-29-2-12 ~]$ sudo amazon-linux-extras install docker
Installing docker
[...]
Complete!
```

`Docker`サービスを開始します。
```console
[ec2-user@ip-172-29-2-12 ~]$ sudo service docker start
Redirecting to /bin/systemctl start docker.service
```

一旦ログアウトしてください。
```console
[ec2-user@ip-172-29-2-12 ~]$ exit
logout
Connection to 172.29.2.12 closed.
Killed by signal 1.
```

AWSに再ログインし、導入された`Docker`情報を確認してみます。
```console
[localUser@localPC]$ ssh ec2-user@aws-ipaddress
...
[ec2-user@ip-172-30-2-88 ~]$ sudo docker info
...
```

`Docker Engine`導入直後なので、`Dockerイメージ`は空です。
```console
[ec2-user@ip-172-29-2-12 ~]$ sudo docker images
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
```

コンテナ上のプロセスもありません。
```console
[ec2-user@ip-172-29-2-12 ~]$ sudo docker ps -a
CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS               NAMES
```

### 2.2 手元機材をホストとする場合
#### 2.2.1 手元機材の調達

お手元の適当なPC機材（macOS/Windows/Linux）が`Docker Engine`の動作条件を満足し、
かつインターネットに接続可能であるなら、それをホスト機材として活用できます。
```
    [Android]-----[WiFi_AP]-----(LAN)-----[Local PC]
```
この場合、Android端末上のクライアントと当該機材とはLANで接続します。


#### 2.2.2 手元機材への`Docker Engine`の導入

代表的なプラットフォームにおける`Docker Engine`導入方法を示します。

* macOS
    - [Install Docker Desktop on Mac](https://docs.docker.com/docker-for-mac/install/)
* Microsoft Windows 10
    - [Install Docker Desktop on Windows](https://docs.docker.com/docker-for-windows/install/)
* CentOS(x86_64)
    - [Get Docker Engine - Community for CentOS](https://docs.docker.com/install/linux/docker-ce/centos/)

その他のOSについてはDocker公式サイトの
[Supported platforms](https://docs.docker.com/install/#supported-platforms) 
に記載されているリンク先などを参照してください。


## 3. コンテナイメージの操作
### 3.1 `broker`コンテナの導入と起動

このチュートリアル用のコンテナイメージを用意してあります。

以下のコマンドにより導入してください。
手元になければリポジトリからダウンロードするため数分かかります。
イメージを取得すだけなら`docker pull`、
コンテナ起動は`docker start`とそれぞれ単独コマンドがあるのですが、
`docker run`によりこの2つの操作をまとめて実施することになります。

```console
[ec2-user@ip-172-29-2-12 ~]$ sudo docker run -d --name broker -p 1883:1883 -p 80:80 harbor.vcloud.nii.ac.jp/sinetstream/android-tutorial:latest
Unable to find image 'harbor.vcloud.nii.ac.jp/sinetstream/android-tutorial:latest' locally
latest: Pulling from sinetstream/android-tutorial
[...]
Status: Downloaded newer image for harbor.vcloud.nii.ac.jp/sinetstream/android-tutorial:latest
b1020bcf10fa4d20971db247c12e7a9d3b4803ea0ee4dd11d14ea6bd1bc95c3a
```
> 上記`docker run`コマンドの引数で、コンテナ名称を`broker`、TCPポート
> 1883(mqtt)と80(http)の2つを開くように指定しています。
>
> サービスの待ち受けポート番号を変更する場合は`-p`オプションの値を
> 変更してください。例えばMQTTブローカーのポート番号を`11883`に
> 変更する場合は`-p 11883:1883`と指定してください。
> `-p`引数の詳細については
> [Docker run reference](https://docs.docker.com/engine/reference/run/#expose-incoming-ports)を参照してください。

導入した`broker`コンテナイメージを見てみます。
```console
[ec2-user@ip-172-29-2-12 ~]$ sudo docker images
REPOSITORY                                             TAG                 IMAGE ID            CREATED             SIZE
harbor.vcloud.nii.ac.jp/sinetstream/android-tutorial   latest              1b697be85b10        2 months ago        1.22GB
```

`Docker`プロセスを参照して、状態(STATUS)が`UP`であれば成功です。
```console
[ec2-user@ip-172-29-2-12 ~]$ sudo docker ps -a
CONTAINER ID        IMAGE                                                         COMMAND                  CREATED             STATUS              PORTS                                        NAMES
b1020bcf10fa        harbor.vcloud.nii.ac.jp/sinetstream/android-tutorial:latest   "/usr/local/bin/supe…"   4 minutes ago       Up 4 minutes        0.0.0.0:80->80/tcp, 0.0.0.0:1883->1883/tcp   broker
```

`broker`コンテナ上で所用のTCPポートを開いていることを確認します。
```console
[ec2-user@ip-172-29-2-12 ~]$ sudo docker exec -t broker ss -an | grep LISTEN
u_str  LISTEN     0      128    /var/run/supervisor/supervisor.sock.1 30266                 * 0
tcp    LISTEN     0      128       *:80                    *:*    <--(!)
tcp    LISTEN     0      50        *:45521                 *:*
tcp    LISTEN     0      50        *:8083                  *:*
tcp    LISTEN     0      100       *:1883                  *:*    <--(!)
tcp    LISTEN     0      50        *:34147                 *:*
tcp    LISTEN     0      50        *:9092                  *:*
tcp    LISTEN     0      50        *:2181                  *:*
tcp    LISTEN     0      50        *:36551                 *:*
tcp    LISTEN     0      100    [::]:1883               [::]:*
tcp    LISTEN     0      128    [::]:9000               [::]:*
```


### 3.2 `broker`コンテナの停止と再起動

以下のコマンドにより`broker`コンテナを停止します。
コマンド完了までしばらく時間がかかるかもしれません。
```console
[ec2-user@ip-172-29-2-12 ~]$ sudo docker stop broker
broker
```

`Docker`プロセスを参照して、状態(STATUS)が`Exited`であれば成功です。
```console
[ec2-user@ip-172-29-2-12 ~]$ sudo docker ps -a
CONTAINER ID        IMAGE                                                         COMMAND                  CREATED             STATUS                        PORTS               NAMES
b1020bcf10fa        harbor.vcloud.nii.ac.jp/sinetstream/android-tutorial:latest   "/usr/local/bin/supe…"   10 hours ago        Exited (137) 38 seconds ago                       broker
```

また、以下のコマンドにより`broker`コンテナを再起動できます。
```console
[ec2-user@ip-172-29-2-12 ~]$ sudo docker start broker
broker
```

### 3.3 `broker`コンテナの削除と再導入

不要になった`broker`コンテナは、以下のコマンドにより削除してください。
```console
[ec2-user@ip-172-29-2-12 ~]$ sudo docker rm broker
broker
```

なお、手元のホスト環境にはコンテナイメージは残ります。
```console
[ec2-user@ip-172-29-2-12 ~]$ sudo docker images
REPOSITORY                                             TAG                 IMAGE ID            CREATED             SIZE
harbor.vcloud.nii.ac.jp/sinetstream/android-tutorial   latest              1b697be85b10        2 months ago        1.22GB
```

初期導入時と同じ`docker run`コマンドを実行しても、
リポジトリからの取得はスキップされ、そのまま`broker`コンテナが再起動されます。
```console
[ec2-user@ip-172-29-2-12 ~]$ sudo docker run -d --name broker -p 1883:1883 -p 80:80   harbor.vcloud.nii.ac.jp/sinetstream/android-tutorial:latest
[sudo] password for ec2-user:
5f183700ea81ffe2118fe5117f306bd9897ac27d28d5dce124ad391993db7782
```

