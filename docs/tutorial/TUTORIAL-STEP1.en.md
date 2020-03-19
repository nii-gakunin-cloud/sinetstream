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

[日本語](TUTORIAL-STEP1.md)

# Tutorial - STEP1

## 1. Overview

The components used in this tutorial are shown in the figure below.

![configuration STEP1](images/tutorial-010.png)

The role of those components shown in [Quick Start Guide](index.en.md#preface) are as follows.

* `Writer` sends messages to `Broker` via `SINETStream`
* `Reader` receives messages from `Broker` via `SINETStream`
* `Broker` aggregates and delivers messages from `Writer` to `Reader`, and separates the endpoints from each other.

`Writer`, `Reader`, and `Broker` work on a single machine in the STEP1 of the tutorial.
Specifically, one container is started in the execution environment of the tutorial, and all the components of the tutorial are executed in that container.

### Prerequisite

Docker Engine must be installed on the machine where you run the tutorial.
You can find information about installing Docker Engine in [Links to Docker Engine installation instructions](index.md#links-to-docker-engine-installation-instructions).

### Notational conventions

There will be several instructions with command execution examples.
Since this tutorial uses Docker containers, it is necessary to distinguish between the host environment and the container environment.
We use the following conventions for this purpose.

* When executing commands in the host environment
    - The command prompt is `$`.
```
$ ls
```
* When executing commands in the container environment
    - The command prompt is `[user01 @ broker]$`.
```
[user01 @ broker]$ ls
```
> The prefix `[user01@broker]` in the prompt of the container environment indicates that the container hostname is `broker` and the username is `user01`.

## 2. Prepare the execution environment

### 2.1. Prepare the backend system

Run the backend messaging system (Kafka and MQTT) used by SINETStream in a Docker container.

Execute the following command in the host environment.

```console
$ docker run -d --name tutorial --hostname broker sinetstream/tutorial:1.0.0
```

Show the status to confirm that the container has started successfully.

```console
$ docker ps -l
CONTAINER ID        IMAGE                        COMMAND                  CREATED              STATUS              PORTS                NAMES
xxxxxxxxxxxx        sinetstream/tutorial:1.0.0   "/usr/local/bin/supe…"   About a minute ago   Up About a minute   1883/tcp, 9092/tcp   tutorial
```

If the `STATUS` is shown as `Up`, the container has started successfully.

In the started container, the broker of the messaging system, i.e., Kafka and MQTT (Mosquitto), is running.

### 2.2. Install SINETStream

Install the Python3 library of SINETStream used by `Reader` and `Writer` on the container environment.

First, log in to the container environment from the host environment. Please execute the following command.

```console
$ docker exec -it -u user01 tutorial bash
```

Next, install the library of SINETStream on the container environment. Please execute the following command.

```console
[user01@broker]$ pip3 install --user sinetstream-kafka sinetstream-mqtt
Collecting sinetstream-kafka
(omit)
Successfully installed kafka-python-1.4.7 paho-mqtt-1.5.0 pycryptodome-3.9.4 sinetstream-1.0.0 sinetstream-kafka-1.0.0 sinetstream-mqtt-1.0.0
```

Finally, if the message `Successfully installed ...` is shown, the library has been successfully installed.
To show the list of installed Python 3 libraries, use the following command.

```console
[user01@broker]$ pip3 list
Package           Version
----------------- --------
kafka-python      1.4.7
paho-mqtt         1.5.0
pip               19.3.1
pycryptodome      3.9.4
PyYAML            3.12
setuptools        42.0.2
sinetstream       1.0.0
sinetstream-kafka 1.0.0
sinetstream-mqtt  1.0.0
supervisor        4.1.0
```

> The ``Version``s of other libraries than SINETStream may differ from the above example.

### 2.3.Prepare Reader

Prepare `Reader` in the container environment.
Below is the procedure.

1. Create a directory for `Reader`
2. Prepare the SINETStream configuration file
3. Prepare the `Reader` program

> In this section, the command is executed in the container environment. We use the same container environment as in section `2.2`.

Create a directory and change to that directory.

```console
[user01@broker]$ mkdir -p ~/sinetstream/reader
[user01@broker]$ cd ~/sinetstream/reader
```

Prepare the SINETStream configuration file.
Download the configuration file prepared for this tutorial from GitHub.

```console
[user01@broker]$ ss_url=https://raw.githubusercontent.com/nii-gakunin-cloud/sinetstream/master
[user01@broker]$ curl -O ${ss_url}/docs/tutorial/.sinetstream_config.yml
```

Download the sample program of Reader that uses the SINETStream Python3 API from GitHub.
Grant execute permission to the program.

```console
[user01@broker]$ curl -O ${ss_url}/python/sample/text/consumer.py
[user01@broker]$ chmod a+x consumer.py
```

Verify that the above procedure has been performed correctly.
Make sure that the directories and files are the same as in the example below.

```console
[user01@broker]$ pwd
/home/user01/sinetstream/reader
[user01@broker]$ ls -a
.  ..  .sinetstream_config.yml  consumer.py
```

### 2.4. Prepare Writer

Prepare `Writer` in the container environment.
Below is the procedure.

1. Create a directory for `Writer`
2. Prepare the SINETStream configuration file
3. Prepare the `Writer` program

Create a directory and change to that directory.

```console
[user01@broker]$ mkdir -p ~/sinetstream/writer
[user01@broker]$ cd ~/sinetstream/writer
```

Download the configuration file from GitHub.

```console
[user01@broker]$ curl -O ${ss_url}/docs/tutorial/.sinetstream_config.yml
```

Download the sample program of `Writer` that uses the SINETStream Python3 API from GitHub.
Grant execute permission to the program

```console
[user01@broker]$ curl -O ${ss_url}/python/sample/text/producer.py
[user01@broker]$ chmod a+x producer.py
```

Verify that the above procedure has been performed correctly.
Make sure that the directories and files are the same as in the example below.

```console
[user01@broker]$ pwd
/home/user01/sinetstream/writer
[user01@broker]$ ls -a
.  ..  .sinetstream_config.yml  producer.py
```

## 3. Run Reader and Writer

Run `Reader` and `Writer` to confirm that messages can be sent and received via SINETStream.

SINETStream v1.* supports [Kafka](https://kafka.apache.org/) and [MQTT(Mosquitto)](https://mosquitto.org/) as backend messaging systems.
First, make sure that you can send and receive messages to/from the Kafka broker.
After that, confirm that you can send and receive messages to/from the MQTT broker by changing the settings (without changing the program).

### 3.1. Send and receive messages to/from Kafka brokers

Thereafter, you will execute the `Reader` and `Writer` programs at the same time.
Open two terminals in the host environment to run them.

#### Run Reader

In the terminal for `Reader`, log in to the container environment from the host environment.
Please execute the following command.

```console
$ docker exec -it -u user01 tutorial bash
```

Change to the directory for `Reader`.

```console
[user01@broker]$ cd ~/sinetstream/reader
```

Run the `Reader` program.
In the argument, the service name of the Kafka broker `service-tutorial-kafka` is specified.

```console
[user01@broker]$ ./consumer.py -s service-tutorial-kafka
Press ctrl-c to exit the program.
: service=service-tutorial-kafka
```

The service name specified in the command line is shown after the colon(`:`).

### Run Writer

In the terminal for `Writer`, log in to the container environment from the host environment.
Please execute the following command.

```console
$ docker exec -it -u user01 tutorial bash
```

Change to the directory for `Writer`.

```console
[user01@broker]$ cd ~/sinetstream/writer
```

Run the `Writer` program.
In the argument, the service name of the Kafka broker `service-tutorial-kafka` is specified.

```console
[user01@broker]$ ./producer.py -s service-tutorial-kafka
Press ctrl-c to exit the program.
: service=service-tutorial-kafka
```

The service name specified in the command line is shown after the colon(`:`).

#### Send and receive messages

In the terminal for `Writer`, enter some text and hit the Enter (Return) key.
The text will be sent as a message to the Kafka broker.

The `Reader` program receives the message from the Kafka broker and shows it in the terminal.
Check that the message you sent from `Writer` is shown in the terminal for `Reader`.

#### Stop Reader and Writer

Once the messages has been transmitted, stop the sample programs of `Reader` and `Writer`.
Please type `ctrl-c` at each terminal.

### 3.2 Send and receive messages to/from MQTT broker (Mosquitto)

You can confirm that messages can be sent and received via the MQTT broker in the same manner as for the Kafka broker.
In the argument, specify the service name of the MQTT broker `service-tutorial-mqtt` in place of `service-tutorial-kafka` in the above example.

#### Run Reader

Execute the following command in the terminal for `Reader`.

```console
[user01@broker]$ ./consumer.py -s service-tutorial-mqtt
Press ctrl-c to exit the program.
: service=service-tutorial-mqtt
```

> The current directory of the terminal for `Reader` is assumed to be the `Reader`'s directory in the container environment.

#### Run Writer

Execute the following command in the terminal for `Writer`.

```console
[user01@broker]$ ./producer.py -s service-tutorial-mqtt
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

### 3.3. Stop and delete containers

Finally, stop and delete the container used in this tutorial.

Operations on containers should be performed on the host environment.
To log out from the container environment, execute `exit` within the container.

```console
[user01@broker]$ exit
exit
$
```

After returning to the host environment, please execute the following command.

```console
$ docker stop tutorial
$ docker rm tutorial
```

## 4. About SINETStream

Here is a brief description of the SINETStream configuration file and the API used in this tutorial.

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

Specify the parameters related to the broker in the child elements of the service name.
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
    - In SINETStream v1.*, use either `kafka` or `mqtt`.
* brokers
    - Specify the address(es) of the broker(s).
    - The format of the address is the host name and port number concatenated by a colon(`:`).
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

> The entire code of the sample program consumer.py is available on [GitHub](https://github.com/nii-gakunin-cloud/sinetstream/blob/master/python/sample/text/consumer.py).

First, create a `MessageReader` object to receive a message, specifying the service name in the argument.
`MessageReader` is usually executed by Python's `with` statement.
As a result, connection and disconnection to/from the broker are executed at the boundary of the `with` block.
The object named `reader` given by the `with` statement is iterable.
The object named `message` given by the `for` statement is the message received from the broker.

#### Writer

In the `Writer` sample program `producer.py`, the SINETStream API is used as follows.

```python
with MessageWriter(service) as writer:
    while True:
        message = get_message()
        writer.publish(message)
```

> The entire code of the sample program producer.py is available on [GitHub](https://github.com/nii-gakunin-cloud/sinetstream/blob/master/python/sample/text/producer.py).

First, create a `MessageWriter` object to send a message, specifying the service name in the argument.
`MessageWriter` is usually executed by Python's `with` statement.
As a result, connection and disconnection to/from the broker are executed at the boundary of the `with` block.
Send a message to the broker by invoking the `publish(message)` method of the object named `writer` given by the `with` statement.

Please refer to the [user guide](../userguide/api-python.en.md) for more information about SINETStream Python API.
