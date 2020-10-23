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

[日本語](index.md)

# SINETStream User Guide

## What is SINETStream

SINETStream is a wrapper library that provides functions to unify the following operations for various messaging systems such as Apache Kafka and MQTT Broker.

1. Connect to a Broker
1. Disconnect from a broker
1. Send messages to a broker
1. Receive messages from a broker

Currently, SINETStream supports Apache Kafka and MQTT Broker as backend messaging systems.
The backend is extensible as plugins to support other messaging systems.

SINETStream provides three API versions, i.e., Python API, Java API and Android API (MQTT only). 

## Contents

* [User Guide for Python](api-python.en.md)
* [User Guide for Java](api-java.en.md)
* [User Guide for Android](https://translate.google.com/translate?hl=en&sl=ja&tl=en&u=https://nii-gakunin-cloud.github.io/sinetstream/docs/userguide/android.html)
    * [Android API](api-android.en.md)
    * [Android API (Javadoc)](http://javadoc.android.sinetstream.net/)
    * [Configuration file for Android](https://translate.google.com/translate?hl=en&sl=ja&tl=en&u=https://nii-gakunin-cloud.github.io/sinetstream/docs/userguide/config-android.html)
* [Configuration file](config.en.md)
* [Authentication and authorization](auth.en.md)

## Definition of technical terms

| Word | Meaning |
| :--- | :--- |
| Reader | The program that receives the message |
| Writer | The program that sends the message |
| Broker | The program that relays messages from Writer to Reader |
| Topic | A logical channel for sending and receiving messages |
