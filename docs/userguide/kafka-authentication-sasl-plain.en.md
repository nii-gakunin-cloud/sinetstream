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

[日本語](kafka-authentication-sasl-plain.md)

# How to use a Kafka broker with SASL/PLAIN authentication

## Overview

This page describes how to connect from SINETStream to a Kafka broker that requires SASL/PLAIN authentication.

The description will be made in the following order.

1. Prerequisites
1. Configurations on the Kafka broker (server side)
1. Configurations on SINETStream (client side)
1. Behavior on authentication errors

## Prerequisites

Though the configuration and setting of a Kafka broker may vary, the following conditions are assumed for simplicity in this document.

* The Kafka broker consists of one node.
* The Kafka broker does not offer authentication services other than SASL/PLAIN.
* SSL/TLS is used to connect between SINETStream and the Kafka broker.
* You trust any certificate signed by a CA certificate registered in the Kafka broker's trust store.
* A CA certificate has been created in PEM format in advance by a private certificate authority. (*1)
* A server certificate and a client certificate have also been created in PEM format in advance. (*1)

(*1) Refer to [How to create a certificate with a private certificate authority](certificate.en.md) for details.

The following values are used in the examples.
> In practice, use appropriate values for your environment.

* Kafka broker
    * Hostname
        * broker.example.org
    * Port
        * 9094
    * Installed directory
        * /srv/kafka
    * Username/password
        * `user01`/`user01-pass`
        * `user02`/`user02-pass`
        * `user03`/`user03-pass`
    * Property file path
        * /srv/kafka/config/server.properties
    * Trust store
        * File path
            * /srv/kafka/config/cert/truststore.p12
        * Password
            * trust-pass-00
        * Registred name of CA certificate in the trust store
            * private-ca
    * Key store
        * File path
            * /srv/kafka/config/cert/keystore.p12
        * Password
            * key-pass-00
        * Registred name of server certificate of the Kafka broker
            * broker
* Certificate (server side)
    * CA certificate
        * Certificate file path
            * /etc/pki/CA/cacert.pem
        * Private key file path
            * /etc/pki/CA/private/cakey.pem
    * Server certificate of the Kafka broker
        * Certificate file path
            * /etc/pki/CA/certs/broker.crt
        * Private key file path
            * /etc/pki/CA/private/broker.key
* Certificate (client side)
    * CA certificate
        * /opt/certs/cacert.pem

## Configurations on the Kafka broker (server side)

The following procedure is needed for a Kafka broker to perform SASL/PLAIN authentication.

1. Edit the Kafka broker's properties file for SASL authentication
1. Convert the file format
1. Edit the Kafka broker's properties file for SSL/TLS connection

You should enable SSL/TLS connection because username and password are sent in plain text in SASL/PLAIN authentication.

### Edit the Kafka broker's properties file for SASL authentication

Add the following lines to the Kafka broker's properties file `/srv/kafka/config/server.properties`

```properties
listeners=SASL_SSL://:9094
advertised.listeners=SASL_SSL://broker.example.org:9094
sasl.enabled.mechanisms=PLAIN
listener.name.sasl_ssl.plain.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required \
  user_user01="user01-pass" \
  user_user02="user02-pass" \
  user_user03="user03-pass";
```

The meanings of the above settings are:

* `listeners`, `advertised.listeners`
    * Add `SASL_SSL://` to provide SSL/TLS connection services with SASL authentication
* `sasl.enabled.mechanisms`
    * The SASL mechanism to be enabled
    * Specify `PLAIN` for SASL/PLAIN
* `listener.name.sasl_ssl.plain.sasl.jaas.config`
    * The JAAS login context parameters for SASL connection
    * Specify usernames and passwords for SASL/PLAIN authentication

### Convert the file format

Convert the certificate and the private key files from PEM format to PKCS#12 format so that the Kafka broker can read them.
The CA certificate and its private key are converted and stored in a trust store, while the server certificate and its private key are converted and stored in a key store.

First, create a trust store using the follwing command.
Specify the CA certificate filename after `-in`, its private key filename after `-inkey`, the output filename of the trust store after `-out`, and the password to be set for the trust store after `-passout`.
```bash
$ sudo mkdir -p /srv/kafka/config/cert
$ sudo openssl pkcs12 -export -in /etc/pki/CA/cacert.pem \
         -inkey /etc/pki/CA/private/cakey.pem -name private-ca \
         -CAfile /etc/pki/CA/cacert.pem -caname private-ca \
         -out /srv/kafka/config/cert/truststore.p12 \
         -passout pass:trust-pass-00
```

Next, create a key store using the following command.
Specify the server certificate filename after `-in`, its private key filename after `-inkey`, the output filename of the key store after `-out`, and the password to be set for the key store after `-passout`.

```bash
$ sudo openssl pkcs12 -export -in /etc/pki/CA/certs/broker.crt \
         -inkey /etc/pki/CA/private/broker.key -name broker \
         -CAfile /etc/pki/CA/cacert.pem -caname private-ca \
         -out /srv/kafka/config/cert/keystore.p12 \
         -passout pass:key-pass-00
```

### Edit the Kafka broker's properties file for SSL/TLS connection

Add the following lines to the Kafka broker's properties file `/srv/kafka/config/server.properties`.

```properties
ssl.truststore.location=/srv/kafka/config/cert/truststore.p12
ssl.truststore.password=trust-pass-00
ssl.truststore.type=pkcs12
ssl.keystore.location=/srv/kafka/config/cert/keystore.p12
ssl.keystore.password=key-pass-00
ssl.keystore.type=pkcs12
```

The meanings of the above settings are:

* `ssl.truststore.location`
    * File path of the trust store
* `ssl.truststore.password`
    * Password of the trust store
* `ssl.truststore.type`
    * Format of the trust store
* `ssl.keystore.location`
    * File path of the key store
* `ssl.keystore.password`
    * Password of the key store
* `ssl.keystore.type`
    * Format of the key store

Restart the Kafka broker to apply the changes in the properties file.

```bash
$ sudo /srv/kafka/bin/kafka-server-stop.sh
$ sudo /srv/kafka/bin/kafka-server-start.sh /srv/kafka/config/server.properties
```

> In order to change the settings without interrupting the service,
> configure multiple Kafka brokers and reflect the changes by rolling restart.

## Configurations on SINETStream (client side)

The following procedure is needed for SINETStream to connect to the Kafka broker with SASL/PLAIN authentication.

1. Prepare certificate
1. Edit the SINETStream's configuration file
1. Create a program that uses SINETStream

### Prepare certificate

The following certificate is required on the client side to use SSL/TLS connection.

* A CA certificate

Deploy the certificate created by a private CA etc. to your convenient location.
SINETStream reads the certificate from the path specified in the configuration file.

### Edit the SINETStream's configuration file

An example of SINETStream's configuration file is shown below.

```yaml
service-kafka-sasl-plain:
  brokers: broker.example.org:9094
  type: kafka
  topic: topic-001
  tls:
    ca_certs: /opt/certs/cacert.pem
  security_protocol: SASL_SSL
  sasl_mechanism: PLAIN
  sasl_plain_username: user01
  sasl_plain_pasword: user01-pass
```

The settings for `brokers`, `type`, `topic`, `consistency`, `tls` are identical to those without authentication.
Settings related to SASL authentication are:

* `security_protocol`
    * Communication protocol with the broker
    * Specify `SASL_SSL` to use SSL/TLS connection with SASL authentication
* `sasl_mechanism`
    * The SASL mechanism to be used
    * Specify `PLAIN` for SASL/PLAIN
* `sasl_plain_username`
    * Username for SASL/PLAIN authentication
* `sasl_plain_password`
    * Password for SASL/PLAIN authentication

### Create a program that uses SINETStream

Your program will be identical with or without SASL/PLAIN authentication.
For example, a program that uses `MessageWriter` of the SINETStream's Python API is shown below.

```python
with sinetstream.MessageWriter(service='service-kafka-sasl-plain') as writer:
    writer.publish(b'message 001')
```

As you see, no code is written for authentication.

If you want to configure the authentication within your program, add parameters to the constructor arguments.

```python
user_passwd = {
    'sasl_plain_username': 'user01',
    'sasl_plain_password': 'user01-pass',
}
with sinetstream.MessageWriter(service='service-kafka-sasl-plain', **user_passwd) as writer:
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
