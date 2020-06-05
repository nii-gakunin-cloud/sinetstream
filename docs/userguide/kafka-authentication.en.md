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

[日本語](kafka-authentication.md)

**準備中** (2020-06-05 18:35:01 JST)

# How to use a Kafka broker with authentication

## Overview

A Kafka broker provides the following authentication methods for client connection.

* SSL/TLS authentication (client authentication)
* SASL/GSSAPI (Kerberos)
* SASL/PLAIN
* SASL/SCRAM-SHA-256, SAL/SCRAM-SHA-512
* SASL/OAUTHBEARER
* Delegation token authentication

Among them, SINETStream supports the following methods.

1. [SSL/TLS authentication (client authentication)](kafka-authentication-ssl.en.md)
1. [SASL/PLAIN](kafka-authentication-sasl-plain.en.md)
1. [SASL/SCRAM-SHA-256, SASL/SCRAM-SHA-512](kafka-authentication-sasl-scram.en.md)

Refer to the respective pages for instructions on how to configure SINETStream and a Kafka broker to enable authentication.
