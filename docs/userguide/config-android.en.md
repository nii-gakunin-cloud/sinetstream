<!--
Copyright (C) 2021 National Institute of Informatics

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

[日本語](config-android.md)

# SINETStream User Guide

## Configuration file for Android

## Overview

The Android version of SINETStream library, unlike the Java or Python
versions, currently uses only
[Paho MQTT Android](https://www.eclipse.org/paho/index.php?page=clients/android/index.php)
as the underlying messaging system.

Some of the configuration parameters, to be shown in this document,
are used to populate the
[MqttConnectOptions](https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttConnectOptions.html)
at the initialization of the `Paho MQTT Android` library.
For omitted optional items, corresponding default values are used
in the `Paho MQTT Android` library.


## Description blocks per service

* The SINETStream configuration file treats each service as a block.
* Since multiple services can be specified in a single configuration
file, `service` is used as the search key.

```
   sinetstream_config.yml
   +------------------------+
   |  +------------------+  |
   |  | service #1       |  |
   |  |   ...            |  |
   |  +------------------+  |
   |      ...               |
   |  +------------------+  |
   |  | service #n       | <---- target service
   |  |   ...            |  |
   |  +------------------+  |
   +------------------------+
```

## List of setting items
### Basic parameters

|Major classification|Middle classification|Subcategory|Type|Range|Mandatory|Remarks|
|:-----|:-----|:-----|:-|:---|:---|:---|
|service|||String|Any|YES|Service Identifier|
||type||String|{"mqtt"}|YES|Currently `"mqtt"` only|
||brokers||String|hostport1[,hostport2[, ...]]|YES|Concatenate with commas for multiple elements|


### API parameters

|Major classification|Middle classification|Subcategory|Type|Range|Mandatory|Remarks|
|:-----|:-----|:-----|:-|:---|:---|:---|
||topics||String|topic1[,topic2[, ...]]|YES|Concatenate with commas for multiple elements|
||client_id||String|Any|NO|Default: automatically generated|
||consistency||String|{"AT_MOST_ONCE","AT_LEAST_ONCE","EXACTLY_ONCE"}|NO|Default: "AT_LEAST_ONCE"|


### SSL/TLS parameters
#### Format 1

|Major classification|Middle classification|Subcategory|Type|Range|Mandatory|Remarks|
|:-----|:-----|:-----|:-|:---|:---|:---|
||tls||Boolean|{true,false}|NO|Default: false|

#### Format 2

|Major classification|Middle classification|Subcategory|Type|Range|Mandatory|Remarks|
|:-----|:-----|:-----|:-|:---|:---|:---|
||tls|ca_certs|String|Any|NO|File name of the self-signed server certificate (xxx.crt)|
||tls|certfile|String|Any|NO|File name of the client certificate (xxx.pfx)|
||tls|keyfilePassword|String|Any|NO|Password of the client certificate (xxx.pfx)|
||tls|check_hostname|Boolean|{true,false}|NO|Default: true|


### MQTT specific parameters
#### Basic

|Major classification|Middle classification|Subcategory|Type|Range|Mandatory|Remarks|
|:-----|:-----|:-----|:-|:---|:---|:---|
||clean_session||Boolean|{true,false}|NO|Default: true|
||protocol||String|{"MQTTv31","MQTTv311","DEFAULT"}|NO|Default: "DEFAULT"|
||transport||String|{"tcp","websockets"}|NO|Default: "tcp"|
||qos||Integer|{0,1,2}|NO|Default: 1|
||retain||Boolean|{true,false}|NO|Default: true|
||max_inflight_messages_set|inflight|Integer|Positive integer|NO|Default: 0|
||reconnect_delay_set|max_delay|Integer|Positive integer|NO|Default: 0|
||connect|keepalive|Integer|Positive integer|NO|Default: 0|
||connect|automatic_reconnect|Boolean|{true,false}|NO|Default: true|
||connect|connection_timeout|Integer|Positive integer|NO|Default: 0|

* If `"DEFAULT"` is specified as the value of item `protocol`, it is
interpreted as to
[try v3.1.1 first, then try v3.1 if that fails](https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttConnectOptions.html#MQTT_VERSION_DEFAULT).
* The item `qos` has a higher precedence over the item `consistency`
in the "API parameters" above.


#### MQTT user authentication parameters

|Major classification|Middle classification|Subcategory|Type|Range|Mandatory|Remarks|
|:-----|:-----|:-----|:-|:---|:---|:---|
||username_pw_set|username|String|Any|NO||
||username_pw_set|password|String|Any|NO||

* The parameter items under `username_pw_set` should be handled as
the pack.
That is, the items `username` and `pasword` are being set, or the
both are omitted, at the same time.


#### MQTT LWT (Last Will and Testament) parameters

|Major classification|Middle classification|Subcategory|Type|Range|Mandatory|Remarks|
|:-----|:-----|:-----|:-|:---|:---|:---|
||will_set|topic|String|Any|YES||
||will_set|payload|String|Any|YES||
||will_set|qos|Integer|Positive integer|YES||
||will_set|retain|Boolean|{true,false}|YES||

* The parameter items under `will_set` have the identical names with
the ones in the "Basic parameters", such like `topic`.
* To prevent ambiguity, all of four child items must be set along with
the parent item `will_set`.


#### MQTT SSL/TLS parameters

|Major classification|Middle classification|Subcategory|Type|Range|Mandatory|Remarks|
|:-----|:-----|:-----|:-|:---|:---|:---|
||tls_set|ca_certs|String|Any|NO|File name of the self-signed server certificate (xxx.crt)|
||tls_set|certfile|String|Any|NO|File name of the client certificate (xxx.pfx)|
||tls_set|keyfilePassword|String|Any|NO|Password of the client certificate (xxx.pfx)|
||tls_insecure_set|value|Boolean|{true,false}|NO|Default: true|

* The parameter items under `tls_set` or `tls_insecure_set` have a
higher precedence over the ones in the "SSL/TLS parameters".

