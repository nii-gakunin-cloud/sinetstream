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

[日本語](certificate.md)

**準備中** (2020-06-05 18:35:01 JST)

# How to create a certificate with a private certificate authority

## Overview

This page describes how to create a certificate required for SSL/TLS connection with a private certificate authority (CA).

The description will be made in the following order.

1. Prerequisites
1. Build a private certificate authority
1. Create a server certificate
1. Create a client certificate

## Prerequisites

The following conditions are assumed for simplicity in this document.

* A private CA is running on CentOS 7
* The certificate created by the private CA is in PEM format

The following values are used in the examples.
> In practice, use appropriate values for your environment.

* Private CA
    * OpenSSL configuration file
        * /etc/pki/tls/openssl.cnf
    * Directory for related files
        * /etc/pki/CA
* Certificates
    * CA certificate
        * Certificate file path
            * /etc/pki/CA/cacert.pem
        * Private key file path
            * /etc/pki/CA/private/cakey.pem
        * Subject
            * /C=JP/ST=Example_State/O=Example_Organization/CN=private-ca
        * Validity period (in days)
            * 3650
    * Server certificate of the broker
        * Certificate file path
            * /etc/pki/CA/certs/broker.crt
        * Private key file path
            * /etc/pki/CA/private/broker.key
        * Subject
            * /C=JP/CN=broker.example.org
    * Client certificate
        * Certificate file path
            * /etc/pki/CA/certs/client0.crt
        * Private key file path
            * /etc/pki/CA/private/client0.key
        * Subject
            * /C=JP/CN=client0

## Build a private certificate authority

Here we show how to build a private certificate authority (CA) on CentOS 7.

> The private certificate authority does not necessarily need to be built on the machine running the broker.

Install the `openssl` package.
> Skip it if it is already installed.

```bash
$ sudo yum -y install openssl
```

Create a directory to store certificates and private keys.

```bash
$ sudo mkdir -p /etc/pki/CA/certs /etc/pki/CA/crl /etc/pki/CA/newcerts /etc/pki/CA/private
```

Edit the OpenSSL configuration file.

* unique_subject
    * Specify `no` to simplify CA certificate rollover
* copy_extensions
    * Specify `copy` to allow certificate request to copy SAN (subjectAltName)

Edit the `[ CA_default ]` section of `openssl.cnf` as follows.

> From the default `openssl.cnf`, just remove the comment symbol `#` before `unique_subject` and `copy_extensions`.

```
[ CA_default ]
(omit)
unique_subject  = no                    # Set to 'no' to allow creation of
                                        # several ctificates with same subject.
(omit)
# Extension copying option: use with caution.
copy_extensions = copy
```

Create a file named `index.txt` to record the certificate signed by the private CA.

```bash
$ sudo touch /etc/pki/CA/index.txt
```

Create a CSR and private key for the CA certificate.

```bash
$ sudo openssl req -new -keyout /etc/pki/CA/private/cakey.pem \
       -out /etc/pki/CA/careq.pem -nodes \
       -subj /C=JP/ST=Example_State/O=Example_Organization/CN=private-ca
```

Create a self-signed CA certificate.

```bash
$ sudo openssl ca -batch -in /etc/pki/CA/careq.pem -selfsign -extensions v3_ca \
       -keyfile /etc/pki/CA/private/cakey.pem -days 3650 -create_serial \
       -out /etc/pki/CA/cacert.pem
```

Check the content of the created CA certificate.

```bash
$ openssl x509 -in /etc/pki/CA/cacert.pem -noout -text
```

The output should look like this:

```
Certificate:
    Data:
        Version: 3 (0x2)
        Serial Number:
            ef:f1:65:60:87:be:24:2d
    Signature Algorithm: sha256WithRSAEncryption
        Issuer: C=JP, ST=Example_State, O=Example_Organization, CN=private-ca
        Validity
            Not Before: Feb 01 00:00:00 2020 GMT
            Not After : Jan 31 00:00:00 2023 GMT
        Subject: C=JP, ST=Default_Organization, O=Default_Organization, CN=private-ca
(omit)
        X509v3 extensions:
            X509v3 Subject Key Identifier:
                73:16:96:F1:00:79:31:FE:16:FD:73:C7:E7:C9:99:02:7D:0C:50:68
            X509v3 Authority Key Identifier:
                keyid:73:16:96:F1:00:79:31:FE:16:FD:73:C7:E7:C9:99:02:7D:0C:50:68

            X509v3 Basic Constraints:
                CA:TRUE
(omit)
```

## Create a server certificate

Add the following lines to `openssl.cnf` to add the server's hostname to the SAN (subjectAltName) of the  certificate.
> Change the hostname after `DNS = ` according to your environment.

```
(omit)
[ req ]
(omit)
req_extensions = v3_req
(omit)
[ v3_req ]
subjectAltName = @alt_names
(omit)
[ alt_names ]
DNS = broker.example.org
```

Create a certificate signing request (CSR) and a private key for server certificate using the following command.
Specify the output filename of the private key after `-keyout`, the output filename of the CSR after `-out`, and the subject of certificate after `-subj`.

```bash
$ sudo openssl req -new -keyout /etc/pki/CA/private/broker.key \
       -out /etc/pki/CA/broker.csr -nodes -subj /C=JP/CN=broker.example.org
```

Create a server certificate using the following command.
Specify the private key filename after `-keyfile`, the CA certificate filename after `-cert`, the CSR filename after `-in`, and the output filename of the server certificate after `-out`.

```bash
$ sudo openssl ca -batch -keyfile /etc/pki/CA/private/cakey.pem \
      -cert /etc/pki/CA/cacert.pem -in /etc/pki/CA/broker.csr \
      -out /etc/pki/CA/certs/broker.crt -policy policy_anything
```

Check the content of the created server certificate.

```bash
$ openssl x509 -in /etc/pki/CA/certs/broker.crt -noout -text
```

The output should look like this:

```
Certificate:
    Data:
        Version: 3 (0x2)
        Serial Number:
            ef:f1:65:60:87:be:24:2e
    Signature Algorithm: sha256WithRSAEncryption
        Issuer: C=JP, ST=Example_State, O=Example_Organization, CN=private-ca
        Validity
            Not Before: Feb 01 00:00:00 2020 GMT
            Not After : Jan 31 00:00:00 2023 GMT
        Subject: C=JP, CN=broker
(omit)
        X509v3 extensions:
            X509v3 Basic Constraints:
                CA:FALSE
(omit)
            X509v3 Subject Alternative Name:
                DNS:broker.example.org
(omit)
```

## Create a client certificate

Create a certificate signing request (CSR) and a private key for client certificate using the following command.
Specify the output filename of the private key after `-keyout`, the output filename of the CSR after `-out`, and the subject of certificate after `-subj`.

```bash
$ sudo openssl req -new -keyout /etc/pki/CA/private/client0.key \
       -out /etc/pki/CA/client0.csr -nodes -subj /C=JP/CN=client0
```

Create a client certificate using the following command.
Specify the private key filename after `-keyfile`, the CA certificate filename after `-cert`, the CSR filename after `-in`, and the output filename of the client certificate after `-out`.

```bash
$ sudo openssl ca -batch -keyfile /etc/pki/CA/private/cakey.pem \
      -cert /etc/pki/CA/cacert.pem -in /etc/pki/CA/client0.csr \
      -out /etc/pki/CA/certs/client0.crt -policy policy_anything
```

Check the content of the created client certificate.

```bash
$ openssl x509 -in /etc/pki/CA/certs/client0.crt -noout -text
```

The output should look like this:

```
Certificate:
    Data:
        Version: 3 (0x2)
        Serial Number:
            ef:f1:65:60:87:be:24:2f
    Signature Algorithm: sha256WithRSAEncryption
        Issuer: C=JP, ST=Example_State, O=Example_Organization, CN=private-ca
        Validity
            Not Before: Feb 01 00:00:00 2020 GMT
            Not After : Jan 31 00:00:00 2023 GMT
        Subject: C=JP, CN=client0
(後略)
```
