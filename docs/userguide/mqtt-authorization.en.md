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

[日本語](mqtt-authorization.md)

# How to use an MQTT broker with authorization

## Overview

This page describes how to connect from SINETStream to an MQTT broker that requires authorization.

**Note: An MQTT broker does not return authorization errors to clients.
Therefore, SINETStream cannot detect authorization errors.**

The description will be made in the following order.

1. Prerequisites
1. Configurations on the MQTT broker (server side)
1. Configurations on SINETStream (client side)
1. Behavior on authorization errors

## Prerequisites

Though the configuration and setting of an MQTT broker may vary, the following conditions are assumed for simplicity in this document.

* [Mosquitto](https://mosquitto.org/) is used as the MQTT broker
* The MQTT broker has been configured to use password authentication (*1)
* The MQTT broker has been configured to use SSL/TLS connection (*1)
* The target user of an access control list (ACL) has been registered (*1)

(*1) Refer to [How to use an MQTT broker with password authentication](mqtt-authentication-password.en.md) for configuring authentication.

The following values are used in the examples.
> In practice, use appropriate values for your environment.

* MQTT broker
    * Hostname
        * broker.example.org
    * Port
        * 8884
    * Installed directory
        * /srv/mqtt
    * Username/password
        * `user01`/`user01-pass`
            * Permission: write
        * `user02`/`user02-pass`
            * Permission: read
        * `user03`/`user03-pass`
            * Permission: read, write
    * Configuration file path
        * /etc/mosquitto/mosquitto.conf
    * ACL file path
        * /etc/mosquitto/aclfile
* Certificate (client side)
    * CA certificate
        * /opt/certs/cacert.pem

## Configurations on the MQTT broker (server side)

The following procedure is needed for the MQTT broker to perform authorization.

1. Edit the MQTT broker's configuration file
1. Add settings in the ACL file
1. Reload the configuration file

### Edit the MQTT broker's configuration file

Add the following lines to the MQTT broker's configuration file `/etc/mosquitto/mosquitto.conf`.

```properties
acl_file /etc/mosquitto/aclfile
allow_anonymous false
```

The meanings of the above settings are:

* `acl_file`
    * Specify the ACL file path.
* `allow_anonymous`
    * Whether to allow anonymous users to connect.

### Add settings in the ACL file

In this example, the procedure to grant the following permissions is shown.

| Username | Permission |
| --- | --- |
| user01 | write |
| user02 | read |
| user03 | read, write|

Add the following lines to the ACL file.

```properties
user user01
topic write #

user user02
topic read #

user user03
topic readwrite #
```

The meanings of the above settings are:

* `user <username>`
    * Specify the target user name.
* `topic [read|write|readwrite] <topic>`
    * Set permission for a topic.
    * Permission can be `read`, `write`, or `readwrite` (default).
    * Specify the topic name in `<topic>`, which can also be `#` (multi-level wild card) or `+` (single-level wild card).

Refer to [mosquitto.conf man page](https://mosquitto.org/man/mosquitto-conf-5.html) for details on how to set ACLs.

### Reload the configuration file

Send a SIGHUP signal to reload the ACL file.

```bash
$ sudo killall -HUP mosquitto
```

## Configurations on SINETStream (client side)

The following procedure is needed for SINETStream to connect to the MQTT broker with authorization.

1. Prepare certificate
1. Edit the SINETStream's configuration file
1. Create a program that uses SINETStream

### Prepare certificate

The following certificate is required on the client side to use SSL/TLS connection.

* A CA certificate

Deploy the certificate to your convenient location.
SINETStream reads the certificate from the path specified in the configuration file.

### Edit the SINETStream's configuration file

To connect to the MQTT broker with authorization, some configurations are required to determine the subject of authorization.

An example of SINETStream's configuration file is shown below.

> This is identical to the configuration for password authentication.

```yaml
header:
  version: 3
config:
  service-mqtt:
    brokers: broker.example.org:8884
    type: mqtt
    topic: topic-001
    consistency: AT_LEAST_ONCE
    tls:
      ca_certs: /opt/certs/cacert.pem
    type_spec:
      username_pw:
        username: user03
        password: user03-pass
```

The settings for `brokers`, `type`, `topic`, `consistency`, `tls` are identical to those without authentication.
Settings related to password authentication are under `username_pw:`.

The meanings of the above settings are:

* `username`
    * User name
* `password`
    * Password

DEPRECATED: writing style prior to config version 2:

```yaml
header:
  version: 2
config:
  service-mqtt:
    brokers: broker.example.org:9094
    type: mqtt
    topic: topic-001
    consistency: AT_LEAST_ONCE
    tls:
      ca_certs: /opt/certs/cacert.pem
    username_pw_set:
      username: user03
      password: user03-pass
```

### Create a program that uses SINETStream

Your program will be identical with or without authorization.
For example, a program that uses `MessageWriter` of the SINETStream's Python API is shown below.

```python
with sinetstream.MessageWriter(service='service-mqtt') as writer:
    writer.publish(b'message 001')
```

As you see, no code is written for authorization.

## Behavior on authorization errors

An MQTT broker does not return authorization errors to clients.
Therefore, SINETStream does not raise any exception when an authorization error should occur.
