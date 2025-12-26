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

[日本語](kafka-authorization.md)

# How to use a Kafka broker with authorization

## Overview

This page describes how to connect from SINETStream to a Kafka broker that requires authorization.

The description will be made in the following order.

1. Prerequisites
1. Configurations on the Kafka broker (server side)
1. Configurations on SINETStream (client side)
1. Behavior on authorization errors

## Prerequisites

Though the configuration and setting of a Kafka broker may vary, the following conditions are assumed for simplicity in this document.

* The Kafka broker consists of one node
* ZooKeeper is running on the same host as the Kafka broker
* The Kafka broker has been configured to use SASL/SCRAM authentication (*1)(*2)
* The Kafka broker has been configured to use SSL/TLS connection (*1)
* The target user of an access control list (ACL) has been registered (*1)

(*1) Refer to [How to use a Kafka broker with SASL/SCRAM authentication](kafka-authentication-sasl-scram.en.md) for configuring authentication.

(*2) The configuration procedure for a Kafka broker is similar for authentication methods other than SASL/SCRAM.

The following values are used in the examples.
> In practice, use appropriate values for your environment.

* Kafka broker
    * Hostname
        * broker.example.org
    * Port
        * 9094
    * Installed directory
        * /srv/kafka
    * Property file path
        * /srv/kafka/config/server.properties
    * Username/password
        * `user01`/`user01-pass`
            * Permission: write
        * `user02`/`user02-pass`
            * Permission: read
        * `user03`/`user03-pass`
            * Permission: read, write
* ZooKeeper
    * Hostname
        * broker.example.org
    * Port
        * 2181
* Certificate (client side)
    * CA certificate
        * /opt/certs/cacert.pem

## Configurations on the Kafka broker (server side)

The following procedure is needed for the Kafka broker to perform authorization.

1. Edit the Kafka broker's properties file
1. Register ACL settings in ZooKeeper

### Edit the Kafka broker's properties file

Add the following line to the Kafka broker's properties file `/srv/kafka/config/server.properties`.

```properties
authorizer.class.name=kafka.security.auth.SimpleAclAuthorizer
```

`authorizer.class.name` is the class name used for authorization.
Use `kafka.security.auth.SimpleAclAuthorizer` to perform authorization according to the ACL set in ZooKeeper.

The following lines may also be added.

```properties
allow.everyone.if.no.acl.found=true
super.users=User:Admin1;User:Admin2
```

* `allow.everyone.if.no.acl.found`
    * When no ACL is set, whether to allow all users to access.
* `super.users`
    * List of superusers (each preceded by "User:" and separated by semicolon ";").

Restart the Kafka broker to apply the changes in the properties file.

```bash
$ sudo /srv/kafka/bin/kafka-server-stop.sh
$ sudo /srv/kafka/bin/kafka-server-start.sh /srv/kafka/config/server.properties
```

> In order to change the settings without interrupting the service,
> configure multiple Kafka brokers and reflect the changes by rolling restart.

### Register ACL settings in ZooKeeper

In this example, the procedure to grant the following permissions is shown.

| Username | Permission |
| --- | --- |
| user01 | write |
| user02 | read |
| user03 | read, write |

Use the `kafka-acls.sh` command to configure ACL.
The important arguments for the command are:

* `--authorizer-properties`
    * `zookeeper.connect`
        * The address of ZooKeeper where the ACL is saved
* `--allow-principal`
    * The user name to be authorized (each preceded by "User:")
* `--topic`
    * The topic name that requires authorization
    * `*` means all topics.
* `--group`
    * The consumer group name that requires authorization
    * `*` means all consumer groups.
* `--producer`
    * Configure ACL for producer.
* `--consumer`
    * Configure ACL for consumer.

Examples:

Grant write permission to user01.
```bash
$ sudo /srv/kafka/bin/kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 \
       --add --allow-principal User:user03 --producer --topic '*'
```

Grant read permission to user02.
```bash
$ sudo /srv/kafka/bin/kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 \
       --add --allow-principal User:user02 --consumer --topic '*' --group '*'
```

Grant read and write permission to user03.
```bash
$ sudo /srv/kafka/bin/kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 \
       --add --allow-principal User:user01 --producer --topic '*'
$ sudo /srv/kafka/bin/kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 \
       --add --allow-principal User:user01 --consumer --topic '*' --group '*'
```

Verify that the ACL is set on ZooKeeper.
```bash
$ sudo /srv/kafka/bin/kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 --list
```

The following output indicates that the ACL is set on ZooKeeper.
```
Current ACLs for resource `Group:LITERAL:*`:
        User:user02 has Allow permission for operations: Read from hosts: *
        User:user03 has Allow permission for operations: Read from hosts: *

Current ACLs for resource `Topic:LITERAL:*`:
        User:user03 has Allow permission for operations: Write from hosts: *
        User:user03 has Allow permission for operations: Create from hosts: *
        User:user02 has Allow permission for operations: Read from hosts: *
        User:user01 has Allow permission for operations: Create from hosts: *
        User:user01 has Allow permission for operations: Write from hosts: *
        User:user03 has Allow permission for operations: Describe from hosts: *
        User:user03 has Allow permission for operations: Read from hosts: *
        User:user01 has Allow permission for operations: Describe from hosts: *
        User:user02 has Allow permission for operations: Describe from hosts: *
```

Refer to [Authorization and ACLs](https://kafka.apache.org/documentation/#security_authz) of the Apache Kafka's documentation for details on how to set ACLs.

## Configurations on SINETStream (client side)

The following procedure is needed for SINETStream to connect to the Kafka broker with authorization.

1. Prepare certificate
1. Edit the SINETStream's configuration file
1. Create a program that uses SINETStream

### Prepare certificate

The following certificate is required on the client side to use SSL/TLS connection.

* A CA certificate

Deploy the certificate to your convenient location.
SINETStream reads the certificate from the path specified in the configuration file.

### Edit the SINETStream's configuration file

To connect to the Kafka broker with authorization, some configurations are required to determine the subject of authorization.

An example of SINETStream's configuration file is shown below.

> This is identical to the configuration for SASL/SCRAM authentication.

```yaml
header:
  version: 3
config:
  service-kafka:
    brokers: broker.example.org:9094
    type: kafka
    topic: topic-001
    consistency: AT_LEAST_ONCE
    tls:
      ca_certs: /opt/certs/cacert.pem
    security_protocol: SASL_SSL
    sasl_mechanism: SCRAM-SHA-256
    sasl_plain_username: user03
    sasl_plain_pasword: user03-pass
```

The settings for `brokers`, `type`, `topic`, `consistency`, `tls` are identical to those without authentication.
Settings related to SASL authentication and SSL/TLS connection are:

* `security_protocol`
    * Communication protocol with broker
    * Specify `SASL_SSL` to use SASL authentication for SSL/TLS connection.
* `sasl_mechanism`
    * SASL authentication mechanism
    * Specify `SCRAM-SHA-256` to use SASL/SCRAM-SHA-256.
* `sasl_plain_username`
    * The username for SASL/SCRAM authentication
* `sasl_plain_password`
    * The password for SASL/SCRAM authentication

### Create a program that uses SINETStream

Your program will be identical with or without authorization.
For example, a program that uses `MessageWriter` of the SINETStream's Python API is shown below.

```python
with sinetstream.MessageWriter(service='service-kafka') as writer:
    writer.publish(b'message 001')
```

As you see, no code is written for authorization.

## Behavior on authorization errors

### Python API

`MessageReader` raises the `sinetstream.error.AuthorizationError` exception when it tries to read a message from a topic without read permission.

`MessageWriter` behaves differently depending on the `consistency` setting when it tries to write a message to a topic without write permission.

* If `consistency` is `AT_MOST_ONCE`
    * The message will not be written to the topic.
    * No exception is raised because `publish()` does not wait for the broker to respond.
* Otherwise
    * The message will not be written to the topic.
    * The `sinetstream.error.AuthorizationError` exception is raised.

The methods listed below raises the "sinetstream.error.AuthorizationError" exception when an authorization error occurs.

* `sinetstream.MessageWriter.publish()`
* `sinetstream.MessageReader.__iter___().__next__()`

### Java API

`jp.ad.sinet.stream.api.MessageReader#read` throws `jp.ad.sinet.stream.api.AuthorizationException` when it tries to read a message from a topic without read permission.

`jp.ad.sinet.stream.api.MessageWriter#write` behaves differently depending on the `consistency` setting when it tries to write a message to a topic without write permission.

* If `consistency` is `AT_MOST_ONCE`
    * The message will not be written to the topic.
    * No exception is thrown because `write()` does not wait for the broker to respond.
* Otherwise
    * The message will not be written to the topic.
    * The `jp.ad.sinet.stream.api.AuthorizationException` exception is thrown.
