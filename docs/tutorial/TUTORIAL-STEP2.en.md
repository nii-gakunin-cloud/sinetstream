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

[日本語](TUTORIAL-STEP2.md)

# Tutorial - STEP2

## 1. Overview

The components used in this tutorial are shown in the figure below.

![configuration STEP2](images/tutorial-020.png)

The role of those components shown in [Quick Start Guide](index.en.md#preface) are as follows.

* `Writer` sends messages to `Broker` via `SINETStream`
* `Reader` receives messages from `Broker` via `SINETStream`
* `Broker` aggregates and delivers messages from `Writer` to `Reader`, and separates the endpoints from each other.

In the STEP1 of the tutorial, `Writer`, `Reader`, and `Broker` were executed on the same machine.
In the STEP2, in contrast, `Writer`, `Reader`, and `Broker` are executed on each different machines.

### Prerequisite

1. Docker Engine must be installed on the machines where you run the tutorial.
    - You can find information about installing Docker Engine in [Links to Docker Engine installation instructions](index.en.md#links-to-docker-engine-installation-instructions).
2. Make the TCP/1883 and TCP/9092 ports available on the Broker machine.
    - These ports will be used by the broker for listening TCP.
3. Enable access to TCP/1883 and TCP/9092 ports on the Broker machine from the `Writer` and `Reader` machines.
    - Please set your firewall policy so that these communications are not blocked.

### Notational conventions

There will be six environments: the host environment and the container environment for each of `Writer`, `Reader`, and `Broker`.
We use the following conventions for distinguishing them.

| Role | Environment | Hostname | Username | IP address |
|---|---|---|---|---|
| Broker | host environment | host-broker | user00 | `192.168.1.XXX` |
| Reader | host environment | host-reader | user00 | - |
| Writer | host environment | host-writer | user00 | - |
| Broker | container environment | broker | user01 | - |
| Reader | container environment | reader | user01 | - |
| Writer | container environment | writer | user01 | - |

Please change the values of the host environment according to your actual machines.
In the command execution examples below, the execution environment is indicated by the hostname and the username of the command prompt.
For example, execution of a command on the `Writer`'s container environment is shown as follows.

```console
[user01@writer]$ ls
```

In the above example, the username is `user01` and the hostname is `writer` in the `Writer`'s container environment.
The `Writer`'s host environment would be shown as follows.

```console
[user00@host-writer]$ ls
```

## 2. Prepare the execution environment

Prepare three environments for `Broker`, `Reader`, and `Writer`, in this order.
Open the terminal for three machines and perform the operations as follows.

### 2.1. Prepare Broker

#### 2.1.1. Prepare the backend system

Run the backend messaging systems (Kafka and MQTT) used by SINETStream in a Docker container.

Execute the following command in the `Broker`'s host environment.

```console
[user00@host-broker]$ docker run -d --name broker --hostname broker \
                      -p 1883:1883 -p 9092:9092 sinetstream/tutorial:1.0.0
```

Show the status to confirm that the container has started successfully.

```console
[user00@host-broker]$ docker ps -l
CONTAINER ID        IMAGE                        COMMAND                  CREATED              STATUS              PORTS                                            NAMES
xxxxxxxxxxxx        sinetstream/tutorial:1.0.0   "/usr/local/bin/supe…"   About a minute ago   Up About a minute   0.0.0.0:1883->1883/tcp, 0.0.0.0:9092->9092/tcp   broker
```

If `STATUS` is shown as `Up`, the container has started successfully.

In the started container, the broker of the messaging systems, i.e., Kafka and MQTT (Mosquitto), are running.
The processes running in the container can be checked as follows.

```console
[user00@host-broker]$ docker exec -t broker ps ax
  PID TTY      STAT   TIME COMMAND
    1 ?        Ss     0:00 /usr/bin/python3 /usr/local/bin/supervisord -n -c /et
    9 ?        Sl     0:05 java -Xmx1G -Xms1G -server -XX:+UseG1GC -XX:MaxGCPaus
   10 ?        S      0:00 /usr/sbin/mosquitto -c /etc/mosquitto/mosquitto.conf
   12 ?        Sl     0:01 java -Xmx512M -Xms512M -server -XX:+UseG1GC -XX:MaxGC
  822 pts/0    Rs+    0:00 ps ax
```

The role of each process shown in the above example is as follows.

* PID 1
    - `init` process
    - Manages the services running in this container
* PID 9
    - The Kafka broker
* PID 10
    - The MQTT (Mosquitto) broker
* PID 12
    - ZooKeeper
    - The service used by Kafka to store settings, configuration information, etc.
* PID 822
    - The ps command executed to show this process list.

### 2.2. Prepare Reader

Prepare the `Reader`'s environment in a Docker container.

#### 2.2.1. Start the Reader's container environment

Execute the following command in the `Reader`'s host environment.

```console
[user00@host-reader]$ docker run -d --name reader --hostname reader -e ENABLE_BROKER=false \
                      --add-host=broker:192.168.1.xxx sinetstream/tutorial:1.0.0
```

> Specify the IP address of the `Broker` in your environment in place of `192.168.1.XXX`.

Show the status to confirm that the container has started successfully.

```console
[user00@host-reader]$ docker ps -l
CONTAINER ID        IMAGE                        COMMAND                  CREATED             STATUS              PORTS                NAMES
xxxxxxxxxxxx        sinetstream/tutorial:1.0.0   "/usr/local/bin/supe…"   About a minute ago  Up About a minute   1883/tcp, 9092/tcp   reader
```

If `STATUS` is shown as `Up`, the container has started successfully.

Here we run the same container image as the `Broker`'s one, but the `-e ENABLE_BROKER = false` option prevents `Broker` from being started.
Show the status to confirm it.

```console
[user00@host-reader]$ docker exec -t reader ps ax
  PID TTY      STAT   TIME COMMAND
    1 ?        Ss     0:00 /usr/bin/python3 /usr/local/bin/supervisord -n -c /et
   30 pts/0    Rs+    0:00 ps ax
```

Unlike the status of the `Broker`'s container, here the Kafka broker, the MQTT broker and ZooKeeper are not running.

The `--add-host` option specified when starting the `Reader`'s container is for put the `Broker`'s IP address into the /etc/hosts file in the `Reader`'s container.
This option is required because the server name must be resolved for the Kafka broker to work.
Show the /etc/hosts file in the `Reader`'s container to confirm that the `Broker`'s IP address is registered.

```console
[user00@host-reader]$ docker exec -t reader cat /etc/hosts
127.0.0.1       localhost
::1     localhost ip6-localhost ip6-loopback
fe00::0 ip6-localnet
ff00::0 ip6-mcastprefix
ff02::1 ip6-allnodes
ff02::2 ip6-allrouters
192.168.1.XXX   broker
172.17.0.3      reader
```

> The `Broker`'s IP address specified when starting the container is shown in place of `192.168.1.XXX`.
> The Reader's IP address will differ depending on your environment.

#### 2.2.2. Install SINETStream

Install the Python3 library of SINETStream on the container environment.
First, log in to the container environment from the `Reader`'s host environment.

```console
[user00@host-reader]$ docker exec -it -u user01 reader bash
```

Next, install the library of SINETStream on the container environment.
Please execute the following command.

```console
[user01@reader]$ pip3 install --user sinetstream-kafka sinetstream-mqtt
Collecting sinetstream-kafka
(omit)
Successfully installed avro-python3-1.10.0 kafka-python-2.0.2 paho-mqtt-1.5.1 promise-2.3 pycryptodome-3.9.9 pyyaml-3.13 sinetstream-1.4.0 sinetstream-kafka-1.4.0 sinetstream-mqtt-1.4.0 six-1.15.0 
```

Finally, if the message `Successfully installed ...` is shown, the library has been successfully installed.
To show the list of installed Python 3 libraries, use the following command.

```console
[user01@reader]$ pip3 list
Package           Version
----------------- --------
avro-python3      1.10.0
kafka-python      2.0.2
paho-mqtt         1.5.1
pip               20.2.4
promise           2.3
pycryptodome      3.9.9
PyYAML            3.13
setuptools        50.3.2
sinetstream       1.4.0
sinetstream-kafka 1.4.0
sinetstream-mqtt  1.4.0
six               1.15.0
supervisor        4.2.1
```

> The `Version`s of other libraries than SINETStream may differ from the above example.

#### 2.2.3. Prepare Reader program and configuration file

Below is the procedure.

1. Create a directory for `Reader`
2. Prepare the SINETStream configuration file
3. Prepare the `Reader` program

> In this section, the command is executed in the container environment. We use the same container environment as in section `2.2.2`.

Create a directory and change to that directory.

```console
[user01@reader]$ mkdir -p ~/sinetstream/reader
[user01@reader]$ cd ~/sinetstream/reader
```

Prepare the SINETStream configuration file. Download the configuration file prepared for this tutorial from GitHub.

```console
[user01@reader]$ ss_url=https://raw.githubusercontent.com/nii-gakunin-cloud/sinetstream/main
[user01@reader]$ curl -O ${ss_url}/docs/tutorial/.sinetstream_config.yml
```

Download the sample program of Reader that uses the SINETStream Python3 API from GitHub. Grant execute permission to the program.

```console
[user01@reader]$ curl -O ${ss_url}/python/sample/text/consumer.py
[user01@reader]$ chmod a+x consumer.py
```

Verify that the above procedure has been performed correctly.
Make sure that the directories and files are the same as in the example below.

```console
[user01@reader]$ pwd
/home/user01/sinetstream/reader
[user01@reader]$ ls -a
.  ..  .sinetstream_config.yml  consumer.py
```

### 2.3. Prepare Writer

Prepare `Writer` in the container environment.
Below is the procedure.

#### 2.3.1. Start the Writer's container

Execute the following command in the `Writer`'s host environment.

```console
[user00@host-writer]$ docker run -d --name writer --hostname writer -e ENABLE_BROKER=false \
                      --add-host=broker:192.168.1.xxx sinetstream/tutorial:1.0.0
```

> Specify the `Broker`'s IP address in place of `192.168.1.XXX`.

Show the status to confirm that the container has started successfully.

```console
[user00@host-writer]$ docker ps -l
CONTAINER ID        IMAGE                        COMMAND                  CREATED             STATUS              PORTS                NAMES
xxxxxxxxxxxx        sinetstream/tutorial:1.0.0   "/usr/local/bin/supe…"   About a minute ago  Up About a minute   1883/tcp, 9092/tcp   writer
```

If `STATUS` is shown as `Up`, the container has started successfully.

Since you specified the `-e ENABLE_BROKER=false` option, `Broker` will not run in the container.
Show the status to confirm it.

```console
[user00@host-writer]$ docker exec -t writer ps ax
  PID TTY      STAT   TIME COMMAND
    1 ?        Ss     0:00 /usr/bin/python3 /usr/local/bin/supervisord -n -c /et
   31 pts/0    Rs+    0:00 ps ax
```

Also, since you specified the `--add-host` option, the `Broker`'s IP address is put into the /etc/hosts file in the Writer's container.
Show the /etc/hosts file to confirm it.

```console
[user00@host-writer]$ docker exec -t writer cat /etc/hosts
127.0.0.1       localhost
::1     localhost ip6-localhost ip6-loopback
fe00::0 ip6-localnet
ff00::0 ip6-mcastprefix
ff02::1 ip6-allnodes
ff02::2 ip6-allrouters
192.168.1.XXX   broker
172.17.0.4      writer
```

> The `Broker`'s IP address specified when starting the container is shown in place of `192.168.1.XXX`.
> The `Writer`'s IP address will differ depending on your environment.

#### 2.3.2. Install SINETStream

Install the Python3 library of SINETStream on the container environment.
First, log in to the container environment from the Reader's host environment.

```console
[user00@host-writer]$ docker exec -it -u user01 writer bash
```

Next, install the library of SINETStream on the container environment.
Please execute the following command.

```console
[user01@writer]$ pip3 install --user sinetstream-kafka sinetstream-mqtt
Collecting sinetstream-kafka
(omit)
Successfully installed avro-python3-1.10.0 kafka-python-2.0.2 paho-mqtt-1.5.1 promise-2.3 pycryptodome-3.9.9 pyyaml-3.13 sinetstream-1.4.0 sinetstream-kafka-1.4.0 sinetstream-mqtt-1.4.0 six-1.15.0 
```

Finally, if the message `Successfully installed ...` is shown, the library has been successfully installed.
To show the list of installed Python 3 libraries, use the following command.

```console
[user01@writer]$ pip3 list
Package           Version
----------------- --------
avro-python3      1.10.0
kafka-python      2.0.2
paho-mqtt         1.5.1
pip               20.2.4
promise           2.3
pycryptodome      3.9.9
PyYAML            3.13
setuptools        50.3.2
sinetstream       1.4.0
sinetstream-kafka 1.4.0
sinetstream-mqtt  1.4.0
six               1.15.0
supervisor        4.2.1
```

> The `Version`s of other libraries than SINETStream may differ from the above example.

#### 2.3.3. Prepare Writer program and configuration file

Below is the procedure.

1. Create a directory for `Writer`
2. Prepare the SINETStream configuration file
3. Prepare the `Writer` program

> In this section, the command is executed in the container environment.
> We use the same container environment as in section `2.3.2`.

Create a directory and change to that directory.

```console
[user01@writer]$ mkdir -p ~/sinetstream/writer
[user01@writer]$ cd ~/sinetstream/writer
```

Prepare SINETStream configuration file.
Download the configuration file prepared for this tutorial from GitHub.

```console
[user01@writer]$ ss_url=https://raw.githubusercontent.com/nii-gakunin-cloud/sinetstream/main
[user01@writer]$ curl -O ${ss_url}/docs/tutorial/.sinetstream_config.yml
```

Download the sample program of Writer that uses the SINETStream Python3 API from GitHub.
Grant execute permission to the program.

```console
[user01@writer]$ curl -O ${ss_url}/python/sample/text/producer.py
[user01@writer]$ chmod a+x producer.py
```

Verify that the above procedure has been performed correctly.
Make sure that the directories and files are the same as in the example below.

```console
[user01@writer]$ pwd
/home/user01/sinetstream/writer
[user01@writer]$ ls -a
.  ..  .sinetstream_config.yml  producer.py
```

## 3. Run Reader and Writer

Run `Reader` and `Writer` to confirm that messages can be sent and received via SINETStream.

SINETStream supports [Kafka](https://kafka.apache.org/) and [MQTT(Mosquitto)](https://mosquitto.org/) as backend messaging systems.
First, make sure that you can send and receive messages to/from the Kafka broker.
After that, confirm that you can send and receive messages to/from the MQTT broker by changing the settings (without changing the program).

### 3.1. To send and receive messages to/from Kafka brokers

Thereafter, you will execute the `Reader` and `Writer` programs at the same time.
Open two terminals in the host environment to run them.

#### Run Reader

In the terminal for `Reader`, log in to the container environment from the host environment.
Please execute the following command.

> You do not need to execute it if you are already logged in to the container environment.

```console
[user00@host-reader]$ docker exec -it -u user01 reader bash
```

Change to the directory for `Reader`.

```console
[user01@reader]$ cd ~/sinetstream/reader
```

Run the `Reader` program.
In the argument, the service name of the Kafka broker `service-tutorial-kafka` is specified.

```console
[user01@reader]$ ./consumer.py -s service-tutorial-kafka
Press ctrl-c to exit the program.
: service=service-tutorial-kafka
```

The service name specified in the command line is shown after the colon(`:`).

#### Run Writer

In the terminal for `Writer`, log in to the container environment from the host environment.
Please execute the following command.

> You do not need to execute it if you are already logged in to the container environment.

```console
[user00@host-writer]$ docker exec -it -u user01 writer bash
```

Change to the directory for `Writer`.

```console
[user01@writer]$ cd ~/sinetstream/writer
```

Run the `Writer` program. In the argument, the service name of the Kafka broker `service-tutorial-kafka` is specified.

```console
[user01@writer]$ ./producer.py -s service-tutorial-kafka
Press ctrl-c to exit the program.
: service=service-tutorial-kafka
```

The service name specified in the command line is shown after the colon(`:`).

#### Send and receive messages

In the terminal for `Writer`, enter some text and hit the Enter (Return) key.
The text will be sent as a message to the Kafka broker.

The `Reader` program receives the message from the Kafka broker and shows it in the terminal.
Check that the message you sent from `Writer` is shown in the terminal for `Reader`.

#### Verify that the message is being delivered by Broker

Temporarily stop Broker to make sure that the messages sent by `Writer` are being delivered by `Broker`.

Execute the following command in the `Broker`'s host environment to stop the `Broker`'s container.

```console
[user00@host-broker]$ docker stop broker
```

Try to send a message from the terminal for `Writer` using `producer.py`.
Since Broker is stopped, `Reader` cannot receive the message from `Writer`, and you will see nothing shown in the terminal for `Reader`.

Once confirmed, execute the following command in the Broker's host environment to restart the Broker container.

```console
[user00@host-broker]$ docker start broker
```

#### Stop Reader and Writer

Stop the sample programs of `Reader` and `Writer`.
Please type `ctrl-c` at each terminal.

### 3.2 To send and receive messages to/from MQTT broker (Mosquitto)

Perform the same operation as for the Kafka broker.
Confirm that messages can be sent and received via the MQTT broker.
In the argument, specify the service name of the MQTT broker `service-tutorial-mqtt` in place of `service-tutorial-kafka` in the above example.

#### Run Reader

Execute the following command in the terminal for `Reader`.

```console
[user01@reader]$ ./consumer.py -s service-tutorial-mqtt
Press ctrl-c to exit the program.
: service=service-tutorial-mqtt
```

> The current directory of the terminal for `Reader` is assumed to be the `Reader`'s directory in the container environment.

#### Run Writer

To execute the following command in the terminal of `Writer`.

```console
[user01@writer]$ ./producer.py -s service-tutorial-mqtt
Press ctrl-c to exit the program.
: service=service-tutorial-mqtt
```

> The current directory of the terminal for `Writer` is assumed to be the `Writer`'s directory in the container environment.

#### Send and receive messages

Perform the same operations as for the Kafka broker.
In the `Writer`'s terminal enter some text and hit the Enter (Return) key.
Check that the text is shown in the `Reader`'s terminal.

#### Stop Reader and Writer

Once the messages have been transmitted, stop the sample programs of `Reader` and `Writer`.
Please type `ctrl-c` at each terminal.

### 3.3. Stopping and deleting containers

Finally, stop and delete the container used in this tutorial.

Operations on containers should be performed on the host environment.
To log out from the container environment, execute `exit` within the container.

```console
[user01@reader]$ exit
exit
[user00@host-reader]$
```

After logging out of the host environment, execute the following command on each machine.

`Broker`
```console
[user00@host-broker]$ docker stop broker
[user00@host-broker]$ docker rm broker
```

`Reader`
```console
[user00@host-reader]$ docker stop reader
[user00@host-reader]$ docker rm reader
```

`Writer`
```console
[user00@host-writer]$ docker stop writer
[user00@host-writer]$ docker rm writer
```

## 4. About SINETStream

Here is a brief description of the SINETStream configuration file and the API used in this tutorial.

> The description in this page is exactly the same as the one in STEP1.

### 4.1. Configuration file

The contents of the SINETStream configuration file `.sinetstream_config.yml` is as follows.

```yaml
service-tutorial-kafka:
    type: kafka
    brokers: "broker:9092"
    topic: topic-tutorial-kafka
    value_type: text

service-tutorial-mqtt:
    type: mqtt
    brokers: "broker:1883"
    topic: topic-tutorial-mqtt
    value_type: text
```

The configuration file contains hierarchical keys and values in YAML format.

The top level key is the service name defined in SINETStream.
In the above example, `service-tutorial-kafka` and `service-tutorial-mqtt` are the service name.
The service name is a label to collectively handle various parameters related to the broker.
The service name specified in the arguments of the `Reader` and `Writer` sample programs corresponds to this label defined in the configuration file.

Specify the parameters related to the broker in the child element of the service name.
The parameters corresponding to the service name `service-tutorial-kafka` are as follows.

```yaml
    type: kafka
    brokers: "broker:9092"
    topic: topic-tutorial-kafka
    value_type: text
```

Below is a brief description of each parameter.

* type
    - Specify the type of the messaging system.
    - In SINETStream, use either `kafka` or `mqtt`.
* brokers
    - Specify the address of the broker.
    - The format of the address is the host name and port number concatenated by a colon.
* topic
    - Specify the topic name to/from which the messages are sent and received.
* value_type
    - Specify the message type.
    - One of the following values can be used.
        - `text`
        - `byte_array`
    - The `text` type used in the tutorial specifies that the message is of type string

Please refer to the [user guide](../userguide/config.en.md) for more information about configuration files.

### 4.2. SINETStream API

#### Reader

In the `Reader` sample program `consumer.py`, the SINETStream API is used as follows.

```python
with MessageReader(service) as reader:
    for message in reader:
        print(f"topic={message.topic} value='{message.value}'")
```

> The entire code of the sample program consumer.py is available on [GitHub](https://github.com/nii-gakunin-cloud/sinetstream/blob/main/python/sample/text/consumer.py).

First, create a `MessageReader` object to receive a message, specifying the service name in the argument.
`MessageReader` is usually executed by Python's `with` statement.
As a result, connection and disconnection to/from the broker are executed at the boundary of the `with` block.
The object named `reader` given by the `with` statement is iterable.
The object named `message` given by the `for` statement is a message received from the broker.

#### Writer

In the `writer` sample program `producer.py`, the SINETStream API is used as follows.

```python
with MessageWriter(service) as writer:
    while True:
        message = get_message()
        writer.publish(message)
```

> The entire code of the sample program `producer.py` is available on [GitHub](https://github.com/nii-gakunin-cloud/sinetstream/blob/main/python/sample/text/producer.py).

First, create a `MessageWriter` object to send a message, specifying the service name in the argument.
`MessageWriter` is usually executed by Python's `with` statement.
As a result, connection and disconnection to/from the broker are executed at the boundary of the `with` block.
Send a message to the broker by invoking the `publish(message)` method of the object named `writer` given by the `with` statement.

Please refer to the [user guide](../userguide/api-python.en.md) for more information about the SINETStream Python API.

### 4.3. SINETStream API (asynchronous API)
SINETStream v1.4 now supports asynchronous API.
We have prepared sample programs on Github that executes the same processing as described above with asynchronous API.

*[Reader](https://github.com/nii-gakunin-cloud/sinetstream/blob/main/python/sample/text_async/consumer.py)
*[Writer](https://github.com/nii-gakunin-cloud/sinetstream/blob/main/python/sample/text_async/producer.py)

#### Reader (asynchronous API)
In the sample program consumer.py of the ‘Reader’ of the asynchronous API,
We use the following example to show how to use SINETStream API.

```python
reader = AsyncMessageReader(service)
reader.on_message = show_message
reader.open()
```

First, let's create an ‘AsyncMessageReader’ object to receive the message. At that time, please specify the service name as an argument.

Next, specify the callback function to be called when a message is received in the ‘.on_message’ property. The callback function can receive the messages from the argument. The sample program uses the following callback function.

```python
def show_message(message):
    ts = datetime.fromtimestamp(message.timestamp)
    print(f"[{ts}] topic={message.topic} value='{message.value}'")
```

Finally, you call reader ’.open()’ to connect to the broker.

#### Writer (asynchronous API)

In the sample program producer.py of the ‘Writer’ of the asynchronous API, we use the following example to show how to use SINETStream API.

```python
with AsyncMessageWriter(service) as writer:
    while True:
        message = get_message()
        writer.publish(message)
```

First, create an ‘AsyncMessageWriter’ object for sending messages. At that time, specify the service name as an argument. ‘AsyncMessageWriter’ is usually executed by with statement of Python. This will connect to and disconnect from the broker within the block of the with statement. You can send a message to the broker by calling ‘.publish(message)’ on the value ‘writer’ which returned by the with statement.

In synchronous API, ‘.publish()’ blocks the sending process until it completed, but in asynchronous API, it returns without blocking the process. In addition, ‘.publish()’ of asynchronous API returns a [Promise Object](https://github.com/syrusakbary/promise) to specify the processing after the processing result is confirmed.

