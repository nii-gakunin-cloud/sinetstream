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

[日本語](config-mqtt.md)

SINETStream User Guide

# MQTT-specific parameters

* clean_session
    * Whether to remember the state on reboot and reconnect.
* protocol
    * MQTT Version.
    * Choose either `MQTTv31`, `MQTTv311`, or `MQTTv5`.
* transport
    * Choose either `tcp` or `websocket`.
* qos
    * The QoS for sending and receiving messages.
    * Choose one of 0, 1, or 2, which correspond to `AT_MOST_ONCE`, `AT_LEAST_ONCE`, `EXACTLY_ONCE` of `Consistency`, respectively.
    * The QoS setting takes precedence over the consistency setting.
    * The value of qos affects the value obtained by `getConsistency()` from `MessageReader` and `MessageWriter`.
* retain
    * Whether the server keeps this message.
* max_inflight_messages_set
    * The maximum number of messages with QoS > 0 that can pass through network flows at once.
* ws_set_options
    * Options for the WebSocket connection.
    * The following parameters can be specified.
	* path
	    * The WebSocket path.
	* headers
	    * The additional header to the standard WebSocket header. Specify by a mapping.
* tls_set
    * Parameters for TLS connection. Specify by a mapping.
    * The following parameters can be specified.
	* ca_certs
	    * The path of the CA certificate file (PEM).
	* certfile
	    * The path of the client certificate file (PEM).
	* keyfile
	    * The path of the private key file of the client certificate (PEM).
	* keyfilePassword
	    * The password of the private key file of the client certificate (PEM).
	* tls_version
	    * The TLS protocol version.
	* ciphers
	    * The ciphers allowed for this connection.
	* trustStore (*)
	    * The path of the trust store.
	* trustStoreType (*)
	    * The file format of the trust store, e.g., jks, pkcs12, etc.
	* trustStorePassword (*)
	    * The password of the trust store.
	* keyStore (*)
	    * The path of the keyStore.
	* keyStoreType (*)
	    * The file format of the keyStore, e.g., jks, pkcs12, etc.
	* keyStorePassword (*)
	    * The password of the keyStore.
> (*) `trustStore`, `trustStoreType`, `trustStorePassword`, `keyStore`, `keyStoreType`, `keyStorePassword`, and `keyfilePassword`
> are valid only in the Java API.
* tls_insecure_set
    * Configuration of host name verification on TLS connections.
        * value
            * Whether to skip host name verification.
* username_pw_set
    * The user and the password for authentication. Specify by a mapping.
    * The following parameters can be specified.
	* username
	* password
* will_set
    * Parameters related to Last Will and Testament (LWT). Specify by a mapping.
    * If the client is disconnected unexpectedly, instead the broker issues the message set in LWT.
    * The following parameters can be specified.
	* topic (required)
	* payload (required)
	* qos
	* retain
* reconnect_delay_set
    * Parameters related to the waiting time before reconnecting. Specify by a mapping.
    * The following parameters can be specified.
	* max_delay
	    * Maximum waiting time (in seconds).
	* min_delay
	    * Minimum waiting time (in seconds).
* connect
    * Connection parameters. Specify by a mapping.
    * The following parameters can be specified.
	* keepalive
	    * The keep alive interval (in seconds).
	* automatic_reconnect (*)
	    * Whether to automatically reconnect when connection is lost.
	* connection_timeout (*)
	    * The timeout value of connection (in seconds).
> (*) `automatic_reconnect` and `connection_timeout`
> are valid only in the Java API.

## The configuration example of MQTT

When using WebSocket instead of TCP to connect with an MQTT broker, set the transport parameter as follows.

```
service-mqtt:
  type: mqtt
  brokers: mqtt.example.org
  transport: websockets
```
