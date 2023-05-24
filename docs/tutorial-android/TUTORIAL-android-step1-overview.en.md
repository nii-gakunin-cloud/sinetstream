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

[日本語](TUTORIAL-android-step1-overview.md)

# TUTORIAL - STEP1: Send and receive text messages

<em>Table of contents</em>
<pre>
1. Introduction
2. System configuration
3. The flow of work
3.1 Works on the back-end side (part 1)
3.2 Works on the Android side
3.3 Works on the back-end side (part 2)
4. Restrictions
5. If something goes wrong
5.1 Want to reset the Android application settings
5.2 The Android application cannot connect to the Broker
5.3 An error dialog is shown while the Android application is running
</pre>


## 1. Introduction

As the first step of a sample program which uses the Android
SINETStream library, we demonstrate a simple system configuration.

It is a kind of echo back system. An Android application sends a
message (such like "Hello World") to the `Broker`,
then the `Broker` sends it back, and finally the Android application
receives it.


## 2. System configuration

In this scenario, we use an Android application "Echo", which has
both `Writer` and `Reader` functionalities, and a `Broker`
functionality on the back-end side.

![System model](images/step1/system_model.png)

For better readability of this tutorial, we describe details per
functional elements

* Works on the back-end side
* Works on the Android side

in the latter part of this document.
Please proceed along with those descriptions.

Once you went through the running environment installations,
you need to fill-in the connection settings between the Android and
the `Broker`, before actually run the Android Application.
Please set parameters to fit with your network configurations.


## 3. The flow of work
### 3.1 Works on the back-end side (part 1)

The Android application for this tutorial contains both `Writer`
and `Reader` functionalities, and both of them use the identical
`Broker` connection parameter sets (Address, Port, Topic Name).
Therefore, no special handling for the `Broker` is needed.

It is enough only if the Android application can connect to the
peer `Broker`.

If there is an active `Broker` which has used for other tutorial,
you can use it as is.
Here we demonstrate a sample usage that only the MQTT `Broker`
functionality is used while other elements of the back-end system
for the other tutorial (TUTORIAL-ANDROID-STEP2) left untouched.

If the back-end system for the TUTORIAL-ANDROID-STEP2 has not yet
started, go through the chain of works (install the `Docker Engine`,
install and run the container image) by following the
[Tutorial DOCKER-CONTAINER](sinetstreamhelper-tutorial-container/TUTORIAL-docker-container.en.md)
document.

```console
     % sudo docker run -d --name broker -p 1883:1883 -p 80:80 harbor.vcloud.nii.ac.jp/sinetstream/android-tutorial:latest
```

With this `docker run` command, installation of the back-end system
and starting of server processes are automatically executed.


### 3.2 Works on the Android side

We describe procedures such like installation, settings and operations
of the Android application "Echo".

Please see the companion document
[TUTORIAL - Android:STEP1](TUTORIAL-android-step1.en.md)
for details.


### 3.3 Works on the back-end side (part 2)

Once you have done this tutorial, computational resources for it
must be freed.
Please stop and remove the container image with the following commands.

> If the Android application is still running, you will see an error
> dialog "EOF exception", because the connection is closed by `Broker`
> shutdown.

```console
     % sudo docker stop broker
     % sudo docker rm broker
```

Please see the companion document
[TUTORIAL-DOCKER-CONTAINER](sinetstreamhelper-tutorial-container/TUTORIAL-docker-container.en.md)
for details.


## 4. Restrictions

Because of the nature of tutorial which aims to experience
the system behavior, this tutorial does not fully cover the
Android `SINETStream` library functionalities.

If you use the `Broker` shown in this tutorial, beware that
there are some functional restrictions as follows.

* Connection method with `Broker`
    * The Android application connects to the `Broker` with the
simplest way; no user authorization, no SSL/TLS, no encryption.

    * For the sake of simple processing, the `Broker` does not
distinguish each Android clients. Actually, multiple Android clients
can connect to the same `Broker` at the same time.
In that case, a message sent by a `Writer` will be re-distributed
to all `Readers`.


## 5. If something goes wrong
### 5.1 Want to reset the Android application settings

* Reset everything to restart from scratch.
    * Exercise the following steps to clear local data of the application.
    * There are two kind of areas, `Storage` (or `DATA` in older
Android versions) and `Cache`.
    * We need to clear `Storage` area for reset operation.

```
    Settings
    --> Apps & notifications
      --> App info
        --> Echo
          [FORCE STOP]
          --> Storage & cache
            --> Clear storage (or CLEAR DATA)
```

### 5.2 The Android application cannot connect to the `Broker`

* An error dialog something like "Cannot connect to the Broker" is shown.
    * Make sure the Android device is connected to the external network
      via cellular or Wi-Fi (= not in airplane mode).
    * Make sure the `Broker` is up and running on the back-end side.

* Connection attempt to the `Broker` timeouts.
    * Check the IP address (or FQDN) and port number of the `Broker`.
        * FQDN: Fully Qualified Domain Name
    * Check the IP routing between the Android device and the `Broker`.
    * There may exist firewall along the route. If so, check its settings.


### 5.3 An error dialog is shown while the Android application is running

* An error dialog something like "EOF exception" is shown.
    * It means the network connection with the `Broker` is lost.
    * Check the connection status of the Android device.
    * Check the status of the back-end container.

