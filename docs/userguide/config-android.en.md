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

## Cooperation with the configuration server

From the release of [SINETStream v1.6](https://www.sinetstream.net/docs/news/20211223-release_v16.html), we introduced the `configuration server` which manages the SINETStream settings at one place.

In the previous SINETStream versions, we took an operation model that a SINETStream configuration file is being placed somewhere on the Android device.
Adding to that, we take the new model that a system manager sets up SINETStream configurations on the `configuration server`, while each Android devices dynamically download it via REST-API.

In the former operation model, the Android SINETStream library simply reads the local file in YAML format. In the latter operation model, the Android SINETStream library accesses to the `configuration server` to download a JSON data and handle it only on memory.

The JSON data structure received from the `configuration server` roughly looks like the figure shown below.
The right part of the figure corresponds to the conventional SINETStream configuration file contents (expressed in JSON format).
The `configuration server` can handle multiple information blocks (basic SINETStream configurations, attachments like SSL/TLS certificates and secrets like public key) in centralized manner.
If there are additional information other than basic ones, optional elements (`attachments` and `secrets` respectively) will be added to the JSON data. Basic configuration also has some extra items in this case.
To distinguish the SINETStream configuration versions from previous releases, control header will be added as shown in the center of the figure. If the JSON data does not contain neither attachments nor secrets, and the SINETStream configurations is described within the `SINETStream v1.6` conventions, the header will be omitted.

```
    JSON data                 config part        sinetstream_config
   +---------------+     - - +----------+        part
   | name          |    /    | (header) |
   +---------------+ - -     +----------+ - - - +----------------+
   | config        |         | config   |       | +------------+ |
   +---------------+ - - - - +----------+ - -   | | service #1 | |
   | (attachments) |                         \  | |   ...      | |
   +---------------+                          \ | +------------+ |
   | (secrets)     |                            +----------------+
   +---------------+
```


## List of setting items
### Basic parameters

|Major classification|Middle classification|Subcategory|Type|Range|Mandatory|Remarks|
|:-----|:-----|:-----|:-|:---|:---|:---|
|service|||String|Any|√|Service Identifier|
||type||String|{"mqtt"}|√|Currently `"mqtt"` only|
||brokers||String|hostport1[,hostport2[, ...]]|√|Concatenate with commas for multiple elements|

* If the item `brokers` has multiple elements, connection attempt will be made for each candidates.
  * Operation will be blocked until the connection establishes, or all attempt fails.


### API parameters

|Major classification|Middle classification|Subcategory|Type|Range|Mandatory|Remarks|
|:-----|:-----|:-----|:-|:---|:---|:---|
||topics||String|topic1[,topic2[, ...]]|√|Concatenate with commas for multiple elements|
||client_id||String|Any||Default: automatically generated|
||consistency||String|{"AT_MOST_ONCE","AT_LEAST_ONCE","EXACTLY_ONCE"}||Default: "AT_LEAST_ONCE"|

* The item `client_id` is disabled.

  > There is a [known problem](https://github.com/eclipse/paho.mqtt.android/issues/238) that an error occurs inside the `Paho MQTT Android` library, if we use the `client_id` specified by user.


### SSL/TLS parameters
#### Format 1: Limited SSL/TLS certificates usage

|Major classification|Middle classification|Subcategory|Type|Range|Mandatory|Remarks|
|:-----|:-----|:-----|:-|:---|:---|:---|
||tls||Boolean|{true,false}||Default: false|

* If "tls: true" is specified without child elements, we assume that this is the very limited use case as follows.
  * Client certificate negotiation is not required from the server side.
  * Server certificate is issued from a public CA (certification authority).

    > If the usage does not match above conditions, connection attempt will be failed due to insufficient SSL/TLS settings.


#### Format 2: Use SSL/TLS certificates stored in the Android KeyChain

|Major classification|Middle classification|Subcategory|Type|Range|Mandatory|Remarks|
|:-----|:-----|:-----|:-|:---|:---|:---|
||tls|protocol|String|{TLSv1.2,TLSv1.3}||Default: "TLSv1.2"|
||tls|client_certs|Boolean|{true,false}||Default: false|
||tls|server_certs|Boolean|{true,false}||Default: false|
||tls|check_hostname|Boolean|{true,false}||Default: false|

* SSL/TLS related certificates are expected to be stored beforehand in the Android system credential storage called [KeyChain](https://developer.android.com/reference/android/security/KeyChain).
  * Set the item `client_certs` true, if preinstalled client certificate should be used.
  * Set the item `server_certs` true, if preinstalled self-signed server certificate
should be used.

    > Target SSL/TLS certificates can be found by following the system settings hierarchy.<br>
    >
    > [Client certificate]<br>
    > Settings -> Security & location -> Encryption & credentials -> User credentials
    >
    > [Self-signed server certificate]<br>
    > Settings -> Security & location -> Encryption & credentials -> Trusted credentials -> USER

* If we use the `Android KeyChain`, item `keyfilePassword` is ignored.
  * Upon registration of a client certificate in PFX format, corresponding password is required from the system. Once the registration process succeeds, an `alias` is issued.
  * To refer the registered client certificate, we need to specify this `alias`.

* The item `protocol` accepts `TLSv1.3` from Android 10 (API level 29）or later.

  > [Default configuration for different Android versions](https://developer.android.com/reference/javax/net/ssl/SSLSocket.html#default-configuration-for-different-android-versions)


#### Format 3: Use SSL/TLS certificates downloaded from the configuration server

|Major classification|Middle classification|Subcategory|Type|Range|Mandatory|Remarks|
|:-----|:-----|:-----|:-|:---|:---|:---|
||tls|protocol|String|{TLSv1.2,TLSv1.3}||Default: "TLSv1.2"|
||tls|keyfilePassword|String|Any||Password of the client certificate (xxx.pfx)|
||tls|check_hostname|Boolean|{true,false}||Default: false|

* We refer SSL/TLS related certificates received from the configuration server.
  * Client certificate is available as an attachment information (The value of the `attachments` array element in which `target` equals to `*.tls.certificate_data`).

    > The value of this element is embedded as a Base64 encoded string, but its contents must be a binary file (byte array) in PFX format.

  * Self-signed server certificate is available as an attachment information (The value of the `attachments` array element in which `target` equals to `*.tls.ca_certs_data`).

    > The value of this element is embedded as a Base64 encoded string, but its contents must be an ASCII file in PEM format.

* If client certificate is contained in the attachment information, item `keyfilePassword` must also be specified.
  * In reverse, if `keyfilePassword` is specified but client certificate does not found in the attachment information, this password is ignored.

* If we use the configuration server, items `client_certs` and `server_certs` are ignored.

* The item `protocol` accepts `TLSv1.3` from Android 10 (API level 29）or later.

  > [Default configuration for different Android versions](https://developer.android.com/reference/javax/net/ssl/SSLSocket.html#default-configuration-for-different-android-versions)


### MQTT specific parameters
#### Basic

|Major classification|Middle classification|Subcategory|Type|Range|Mandatory|Remarks|
|:-----|:-----|:-----|:-|:---|:---|:---|
||clean_session||Boolean|{true,false}||Default: true|
||protocol \[1\]||String|{"MQTTv31","MQTTv311","DEFAULT"}||Default: "DEFAULT"|
||transport||String|{"tcp","websocket"}||Default: "tcp"|
||qos||Integer|{0,1,2}||Default: 1|
||retain||Boolean|{true,false}||Default: true|
||max_inflight_messages_set|inflight|Integer|Positive integer||Default: 10|
||reconnect_delay_set|max_delay|Integer|Positive integer||Default: 128000|
||connect|keepalive|Integer|Positive integer||Default: 60|
||connect|automatic_reconnect|Boolean|{true,false}||Default: false|
||connect|connection_timeout|Integer|Positive integer||Default: 30|
||mqtt_debug \[2\]||Boolean|{true,false}||Default: false|

* If `"DEFAULT"` is specified as the value of item `protocol`, it is
interpreted as to
[try v3.1.1 first, then try v3.1 if that fails](https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttConnectOptions.html#MQTT_VERSION_DEFAULT).
* The item `qos` has a higher precedence over the item `consistency`
in the "API parameters" above.

\[1\]: `mqtt_version` can be used as an alias of the item `protocol`.
If both are specified, `protocol` is taken.

\[2\]: Option for developers; Turn on/off debug trace in MqttAndroidClient.


#### MQTT user authentication parameters

|Major classification|Middle classification|Subcategory|Type|Range|Mandatory|Remarks|
|:-----|:-----|:-----|:-|:---|:---|:---|
||username_pw_set|username|String|Any|||
||username_pw_set|password|String|Any|||

* The parameter items under `username_pw_set` should be handled as
the pack.
That is, the items `username` and `password` are being set, or the
both are omitted, at the same time.


#### MQTT LWT (Last Will and Testament) parameters

|Major classification|Middle classification|Subcategory|Type|Range|Mandatory|Remarks|
|:-----|:-----|:-----|:-|:---|:---|:---|
||will_set|topic|String|Any|√||
||will_set|payload|String|Any|√||
||will_set|qos|Integer|Positive integer|√||
||will_set|retain|Boolean|{true,false}|√||

* The parameter items under `will_set` have the identical names with
the ones in the "Basic parameters", such like `topic`.
  * To prevent ambiguity, all of four child items must be set along with
the parent item `will_set`.

* The item `payload` will be passed to the `Paho MQTT Android` library as a byte array.
  * In the SINETStream configuration, specify the `payload` value as a Base64 encoded string.


#### MQTT SSL/TLS parameters

<!-- OBSOLETED
|Major classification|Middle classification|Subcategory|Type|Range|Mandatory|Remarks|
|:-----|:-----|:-----|:-|:---|:---|:---|
||tls_set|ca_certs|String|Any||File name of the self-signed server certificate (xxx.crt)|
||tls_set|certfile|String|Any||File name of the client certificate (xxx.pfx)|
||tls_set|keyfilePassword|String|Any||Password of the client certificate (xxx.pfx)|
||tls_insecure_set|value|Boolean|{true,false}||Default: true|
-->

* Parameters in this category are <em>`disabled`</em>.
  * Use "SSL/TLS parameters" instead.

