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

[日本語](TUTORIAL-docker-container.md)

# TUTORIAL - DOCKER-CONTAINER

<em>Table of contents</em>
<pre>
1. Introduction
2. Preparing a running environment
2.1 Use the Amazon EC2 instance as the Docker host machine
2.1.1 Setup and run the target EC2 instance
2.1.2 Installing Docker Engine on an EC2 instance.
2.2 Use a local PC as the Docker host machine
2.2.1 Procure target machine
2.2.2 Installing Docker Engine on a local PC
3. Container image operations
3.1 Install and run the target container image
3.2 Stop and restart the target container image
3.3 Remove and re-install the target container image
</pre>


## 1. Introduction

In this document, we present how to build a back-end system using
a prefabricated `Docker` container image for this tutorial.
There may be choices for each user that what kind of host machine
is convenient as a running environment.

In the following sections, we present two sample configurations;
cloud computing and local computing, respectively.


## 2. Preparing a running environment

The `Docker Engine` runs on various operating systems such like Linux,
macOS and Windows.
First of all, install the `Docker Engine` for a host computer.
Then download the designated container image prepared for the tutorial.
The container image is available from the NII (National Institute of
Informatics) managed repository.
Finally, run several commands on the `Docker Engine` environment.

### 2.1 Use the Amazon EC2 instance as the `Docker` host machine

As a cloud computing sample, we choose a commercial cloud service
[Amazon EC2](https://aws.amazon.com/ec2/)
(and
[Amazon Linux 2](https://aws.amazon.com/amazon-linux-2/)
as its operating system),
which is widely used as a virtual server platform.

We run the
[Docker container](https://www.docker.com/resources/what-container)
image prepared for this tutorial. This container image includes
the back-end system along with `Broker` functionality.

```
    [Android]-----(Cellular)-----(INTERNET)-----[Amazon AWS]
```

In this case, a client Android device connects to the Amazon EC2
instance via cellular network and the Internet.


#### 2.1.1 Setup and run the target EC2 instance

Once you setup and run an EC2 instance by AWS console operations,
you will get a global IP address and a SSH authentication key.
Login to the EC2 instance via SSH session.

```console
[localUser@localPC]$ ssh ec2-user@aws-ipaddress

       __|  __|_  )
       _|  (     /   Amazon Linux 2 AMI
      ___|\___|___|

https://aws.amazon.com/amazon-linux-2/
```

> Beware memory shortage of the EC2 instance!
> -------------------------------------------
> When we went through testing, we choose `t2.micro` (2MB memory)
> as the minimum configuration of the EC2 instance type.
> But it did not work well.
>
> There is no problem if we simply put a `MQTT Broker` to let it
> "redistribute the received message". However, when we built
> additional back-end system which works behind the `MQTT Broker`,
> we suffered from system instability due to memory shortage.
> After all, we choose larger `t3.large` (8MB memory) and rebuild
> the EC2 instance which now works properly.


#### 2.1.2 Installing `Docker Engine` on an EC2 instance.

As for the installation of `Docker Engine` on an Amazon EC2 instance,
see also the relevant Amazon AWS document
[Docker basics for Amazon ECS](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/docker-basics.html).
Read through until the section `Create a Docker image` in the document.

Followings are operational images on your EC2 instance.
First of all, update the whole system.
```console
[ec2-user@ip-172-29-2-12 ~]$ sudo yum update
[sudo] password for ec2-user:
Loaded plugins: extras_suggestions, langpacks, priorities, update-motd
amzn2-core                                               | 3.7 kB     00:00
No packages marked for update
```

Along with the AWS document, install required packages.
```console
[ec2-user@ip-172-29-2-12 ~]$ sudo amazon-linux-extras install docker
Installing docker
[...]
Complete!
```

Start the `Docker` service.
```console
[ec2-user@ip-172-29-2-12 ~]$ sudo service docker start
Redirecting to /bin/systemctl start docker.service
```

Logout from the EC2 instance.
```console
[ec2-user@ip-172-29-2-12 ~]$ exit
logout
Connection to 172.29.2.12 closed.
Killed by signal 1.
```

Login to the EC2 instance again, and check the installed `Docker`
information.
```console
[localUser@localPC]$ ssh ec2-user@aws-ipaddress
...
[ec2-user@ip-172-30-2-88 ~]$ sudo docker info
...
```

Now that it is just after the fresh `Docker Engine` installation,
there is no container image yet.
```console
[ec2-user@ip-172-29-2-12 ~]$ sudo docker images
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
```

And of course, there is no running container image yet.
```console
[ec2-user@ip-172-29-2-12 ~]$ sudo docker ps -a
CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS               NAMES
```

### 2.2 Use a local PC as the `Docker` host machine
#### 2.2.1 Procure target machine

If your local desktop or notebook PC (macOS/Windows/Linux) satisfies
the system requirements for the `Docker Engine`, and is connected
to the Internet, you can use the PC as the `Docker` host machine.

```
    [Android]-----[Wi-Fi_AP]-----(LAN)-----[Local PC]
```
In this case, connect your Android device and the local PC via local
area network.


#### 2.2.2 Installing `Docker Engine` on a local PC

Here are guidance documents for installing the latest `Docker Engine`
on representative platforms.

* macOS
    - [Install Docker Desktop on Mac](https://docs.docker.com/docker-for-mac/install/)
* Microsoft Windows 10
    - [Install Docker Desktop on Windows](https://docs.docker.com/docker-for-windows/install/)
* CentOS(x86_64)
    - [Get Docker Engine - Community for CentOS](https://docs.docker.com/install/linux/docker-ce/centos/)

For other operating systems, see links in the official `Docker` site
[Supported platforms](https://docs.docker.com/install/#supported-platforms).


## 3. Container image operations
### 3.1 Install and run the target container image

We prepared a container image, named as `broker`, for this tutorial.
Install this container image by the following command.

> If this is the initial installation, it will take several minutes
> for downloading from the container repository over the Internet.
> There are two separate commands; `docker pull` for downloading the
> container image, and `docker start` for starting it.
> However, using `docker run` command is convenient, as it executes
> the above two commands together.

```console
[ec2-user@ip-172-29-2-12 ~]$ sudo docker run -d --name broker -p 1883:1883 -p 80:80 harbor.vcloud.nii.ac.jp/sinetstream/android-tutorial:latest
Unable to find image 'harbor.vcloud.nii.ac.jp/sinetstream/android-tutorial:latest' locally
latest: Pulling from sinetstream/android-tutorial
[...]
Status: Downloaded newer image for harbor.vcloud.nii.ac.jp/sinetstream/android-tutorial:latest
b1020bcf10fa4d20971db247c12e7a9d3b4803ea0ee4dd11d14ea6bd1bc95c3a
```

> In the `docker run` command above, we specify the container name
> `broker` by `--name` option, and TCP port number 1883 (mqtt) and
> 80 (http) by `-p` option.
>
> If you need to change the listen port number, change value of `-p`
> option. For example, to change the MQTT broker listen port to `11883`,
> specify as `-p 11883:1883`.
> For details of `-p` option usage, see the `Docker` document
> [Docker run reference](https://docs.docker.com/engine/reference/run/#expose-incoming-ports).


Check how the installed `broker` container image looks like.
```console
[ec2-user@ip-172-29-2-12 ~]$ sudo docker images
REPOSITORY                                             TAG                 IMAGE ID            CREATED             SIZE
harbor.vcloud.nii.ac.jp/sinetstream/android-tutorial   latest              1b697be85b10        2 months ago        1.22GB
```

Check if the STATUS of the `Docker` process is `UP`.
```console
[ec2-user@ip-172-29-2-12 ~]$ sudo docker ps -a
CONTAINER ID        IMAGE                                                         COMMAND                  CREATED             STATUS              PORTS                                        NAMES
b1020bcf10fa        harbor.vcloud.nii.ac.jp/sinetstream/android-tutorial:latest   "/usr/local/bin/supe…"   4 minutes ago       Up 4 minutes        0.0.0.0:80->80/tcp, 0.0.0.0:1883->1883/tcp   broker
```

Check if specified TCP ports are listening on the `broker` container image.
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

### 3.2 Stop and restart the target container image

Run the following command to stop the `broker` container image.
It may take some time for the command completion.
```console
[ec2-user@ip-172-29-2-12 ~]$ sudo docker stop broker
broker
```

Check if `Docker` process state (STATUS value) is now `Exited`.
```console
[ec2-user@ip-172-29-2-12 ~]$ sudo docker ps -a
CONTAINER ID        IMAGE                                                         COMMAND                  CREATED             STATUS                        PORTS               NAMES
b1020bcf10fa        harbor.vcloud.nii.ac.jp/sinetstream/android-tutorial:latest   "/usr/local/bin/supe…"   10 hours ago        Exited (137) 38 seconds ago                       broker
```

You can restart the `broker` container image by the following command.
```console
[ec2-user@ip-172-29-2-12 ~]$ sudo docker start broker
broker
```

### 3.3 Remove and re-install the target container image

If the `broker` container image is no longer necessary, remove it
from the `Docker` by the following command.
```console
[ec2-user@ip-172-29-2-12 ~]$ sudo docker rm broker
broker
```

Note that the container image is left untouched on the host machine.
```console
[ec2-user@ip-172-29-2-12 ~]$ sudo docker images
REPOSITORY                                             TAG                 IMAGE ID            CREATED             SIZE
harbor.vcloud.nii.ac.jp/sinetstream/android-tutorial   latest              1b697be85b10        2 months ago        1.22GB
```

If you run the identical `docker run` command again, the `broker`
container image will restart without downloading from the repository.
```console
[ec2-user@ip-172-29-2-12 ~]$ sudo docker run -d --name broker -p 1883:1883 -p 80:80   harbor.vcloud.nii.ac.jp/sinetstream/android-tutorial:latest
[sudo] password for ec2-user:
5f183700ea81ffe2118fe5117f306bd9897ac27d28d5dce124ad391993db7782
```

