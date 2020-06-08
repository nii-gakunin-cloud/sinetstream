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

[日本語](mqtt-authentication-ssl.md)

# How to use an MQTT broker with SSL/TLS authentication (client authentication)

## Overview

This page describes how to connect from SINETStream to an MQTT broker that requires SSL/TLS two-way authentication.

The description will be made in the following order.

1. Prerequisites
1. Configurations on the Mosquitto MQTT broker (server side)
1. Configurations on SINETStream (client side)
1. Behavior on authentication errors

## Prerequisites

Though the configuration and setting of an MQTT broker may vary, the following conditions are assumed for simplicity in this document.

* [Mosquitto](https://mosquitto.org/) is used as the MQTT broker
* A CA certificate has been created in PEM format in advance by a private certificate authority (*1)
* A server certificate and a client certificate have also been created in PEM format in advance (*1)

(*1) Refer to [How to create a certificate with a private certificate authority](certificate.en.md) for details.

The following values are used in the examples.
> In practice, use appropriate values for your environment.

* MQTT broker
    * Hostname
        * broker.example.org
    * Port
        * 8883
    * Configuration file path
        * /etc/mosquitto/mosquitto.conf
* Certificate (server side)
    * CA certificate
        * Certificate file path
            * /etc/pki/CA/cacert.pem
    * Server certificate of the MQTT broker
        * Certificate file path
            * /etc/pki/CA/certs/broker.crt
        * Private key file path
            * /etc/pki/CA/private/broker.key
* Certificate (client side)
    * CA certificate
        * /opt/certs/cacert.pem
    * Certificate for client authentication
        * /home/user01/certs/client0.crt
    * Private key for client authentication
        * /home/user01/certs/client0.key

## Configurations on the MQTT broker (server side)

The following procedure is needed for the Mosquitto MQTT broker to perform SSL/TLS two-way authentication.

1. Edit the MQTT broker's configuration file
1. Reload the configuration file

### Edit the MQTT broker's configuration file

Add the following lines to the MQTT broker's configuration file `/etc/mosquitto/mosquitto.conf`.

```properties
per_listener_settings true
listener 8883
cafile /etc/pki/CA/cacert.pem
certfile /etc/pki/CA/certs/broker.crt
keyfile /etc/pki/CA/private/broker.key
require_certificate true
```

The meanings of the above settings are:

* `per_listener_settings`
    * Whether to configure authentication and access control on a per-lister basis or globally.
* `listener`
    * Specify the port number to listen on.
* `cafile`
    * Specify the CA certificate (PEM) file path.
* `certfile`
    * Specify the server certificate (PEM) file path.
* `keyfile`
    * Specify the private key file path for the server certificate.
* `require_certificate`
    * Whether to require client certificate for incoming connections.

### Reload the configuration file

Send a SIGHUP signal to reload the configuration file.

```bash
$ sudo killall -HUP mosquitto
```

## Configurations on SINETStream (client side)

The following procedure is needed for SINETStream to connect to the MQTT broker with authentication.

1. Prepare certificate
1. Edit the SINETStream's configuration file
1. Create a program that uses SINETStream

### Prepare certificate

The following certificates are required on the client side to use SSL/TLS two-way authentication.

* A certificate for client authentication
* A private key for client authentication
* A CA certificate

Put the certificates created by a private CA etc. to your convenient location.
SINETStream reads the certificate from the path specified in the configuration file.

### Edit the SINETStream's configuration file

An example of SINETStream's configuration file is shown below.

```yaml
service-mqtt-ssl:
  brokers: broker.example.org:9093
  type: mqtt
  topic: topic-001
  tls:
    ca_certs: /opt/certs/cacert.pem
    certfile: /home/user01/certs/client0.crt
    keyfile: /home/user01/certs/client0.key
```

The settings for `brokers`, `type`, `topic`, `consistency`, `tls` are identical to those without authentication.
Settings related to SSL/TLS authentication are under `tls:`.

* `ca_certs`
    * Specify the CA certificate (PEM) file path.
* `certfile`
    * Specify the client certificate (PEM) file path.
* `keyfile`
    * Specify the private key file path for the client certificate.

### Create a program that uses SINETStream

Your program will be identical with or without SSL/TLS authentication.
For example, a program that uses `MessageWriter` of the SINETStream's Python API is shown below.

```python
with sinetstream.MessageWriter(service='service-mqtt-ssl') as writer:
    writer.publish(b'message 001')
```

As you see, no code is written for authentication.

If you want to configure the authentication within your program, add parameters to the constructor arguments.

```python
tls = {
    'ca_certs': '/opt/certs/cacert.pem',
    'certfile': '/home/user01/certs/client0.crt',
    'keyfile': '/home/user01/certs/client0.key',
}
with sinetstream.MessageWriter(service='service-mqtt', tls=tls) as writer:
    writer.publish(b'message 001')
```

## Behavior on authentication errors

### Python API

The methods listed below raises the `sinetstream.error.ConnectionError` exception when an authentication error occurs.

* `sinetstream.MessageWriter.__enter__()`
* `sinetstream.MessageWriter.open()`
* `sinetstream.MessageReader.__enter__()`
* `sinetstream.MessageReader.open()`

### Java API

The methods listed below throws the `jp.ad.sinet.stream.api.AuthenticationException` exception when an authentication error occurs.

* `jp.ad.sinet.stream.utils.MessageWriterFactory#getWriter()`
* `jp.ad.sinet.stream.utils.MessageReaderFactory#getReader()`
