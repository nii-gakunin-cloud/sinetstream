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

[日本語](config.md)

SINETStream User Guide

# Configuration files

* Overview
    * Location of configuration files
    * Priority of setting values
* Common parameters
    * Basic parameters
    * Parameters for SSL / TLS
    * Parameters for encryption
* Messaging system-specific parameters
* Notes
    * Difference between Python API and Java API
    * Parameter aliases
    * Unsupported

## Overview

The parameters to the SINETStream API functions may also be written in the configuration file.
By doing so you can make your source code more concise.
The configuration file contains the service name and the parameters associated with the service.
By specifying the service name in the API function, the parameters specified in the configuration file will be applied.

The format of the configuration file is YAML.
The block that describes one service in the configuration file is as follows.

```
{Service Name}:
  type: {type of Message System}
  brokers:
    - {host Name 1}:{port number 1}
    - {host Name 2}:{port number 2}
  {other parameters 1}: {value 1}
  {other parameters 2}: {value 2}
```

`type` specifies the type of the messaging system.
`brokers` specify the addresses of the brokers of the messaging system.

The other parameters and values depend on the messaging system.
For instance, the following parameters can be specified.

* Parameters for communication protocol
    * MQTT protocol version (3.1, 3.1.1)
    * MQTT transport layer (TCP, WebSocket)
* Parameters for TLS connection
    * Settings for CA certificates
    * Settings for client certificate and private key
* Parameters for authentication for connecting to the broker
    * User name
    * Password

### Location of the configuration file

Configuration file is searched in the following order.
Only the first file found will be used.

1. Location (URL) specified in environment variable `SINETSTREAM_CONFIG_URL`
    * Configuration files are allowed to be located on a remote web server.
    * When specifying a local file, specify it in the format `file://{absolute path of the configuration file}`.
2. `.sinetstream_config.yml` in the current directory
3. `$HOME/.config/sinetstream/config.yml`
    * `C:\Users\{userXX}\.Config\sinetstream\config.yml` on Windows 10

> Multiple configuration files cannot be cascaded;
> e.g., if `parameter 1: value 1` is written in `.sinetstream_config.yml`, and
> `parameter 2: value 2` is written in `$HOME/.config/sinetstream/config.yml`,
> the latter will be ignored.

### Priority of setting values

If the parameters specified in the configuration file conflict with the parameters specified in the API functions, or
if the common parameters conflict with the messaging system-specific parameters, the first value found in the following order is used.

1. The messaging system-specific parameter value specified in the API function
1. The common parameter value specified in the API function
1. The messaging system-specific parameter value specified in the configuration file
1. The common parameter value specified in the configuration file

## Common parameters

Below are the common parameters that can be specified regardless of the messaging system.

* Basic parameters
* API parameters
* Parameters for SSL / TLS
* Parameters for encryption

### Basic parameters
* type
    * Specify the type of the messaging system (in string).
    * Currently, the following values can be used.
        * `kafka`
        * `mqtt`
* brokers
    * Specify the broker address as {host name} or {host name}:{port number}.
        * No white space is allowed around the colon “:”.
    * If the port number is omitted, the following default port numbers will be used.
        * Kafka: 9092
        * MQTT
            * TCP: 1883 (plain text), 8883 (TLS)
            * WebSocket: 80 (plain text), 443 (TLS)
    * To specify multiple brokers, use one of the following ways.
        * Enumerate as a YAML sequence.
            ```
            brokers:
              - {host name 1}:{port number 1}
              - {host name 2}:{port number 2}
            ```
        * Concatenate with commas `,`.
            ```
            brokers: {host name 1}:{port number 1},{host name 2}:{port number 2}
            ```

### API parameters

Set the default value of the parameters of the SINETStream API functions.
If a parameter is not specified in the API function, the value specified in the configuration file will be used as the default value.

* topic
    * Topic name.
* client_id
    * Client ID.
* consistency
    * Specify the reliability of message delivery.
    * The following values are valid.
        * AT_MOST_ONCE
            * A message may not be delivered.
        * AT_LEAST_ONCE
            * A messages is always delivered but may be delivered many times
        * EXACTLY_ONCE
            * The message is always delivered only once
* value_type
    * Message type.
    * The following values are valid.
        * `byte_array`
            * Set `"byte_array"` (default) to treat the payload as `bytes`.
        * `text`
            * Set `"text"` to treat the payload as `str`.
    * When using a plugin pacakge, other type names may be supported.
    * When using the image type plugin provided with SINETStream v1.1 (or later), the following type name is supported.
        * Set `"image"` to treat the payload as `numpy.ndarray`, which is the image data type in OpenCV.
        * The color order in `numpy.ndarray` is BGR, which is consistent with OpenCV.
* value_serializer
    * The class name that serializes the message value.
    * The specified class must have a public default constructor.
    * Valid only with `MessageWriter`.
* value_deserializer
    * Class name that deserialize the message value.
    * The specified class must have a public default constructor.
    * Valid only with `MessageReader`.
* data_encryption
    * Enable or disable message encryption and decryption.
* receive_timeout_ms
    * Maximum time (in milliseconds) for `MessageReader` to wait for message arrival.

`value_serializer` and `value_deserializer` take precedence over value_type.

> If `value_deserializer` and `value_type` are specified and `value_serializer` is not, value_desirializer will be enabled for MessageReader and value_type will be enabled for MessageWriter.

> Limitation of Python API:
> In SINETStream v1.*, `value_serializer`/`value_deserializer` can be specified only by the API function parameters and cannot be specified in the configuration file.

### Parameters for SSL/TLS

While the names of the parameters for SSL/TLS differ depending on the messaging system, `SINETStream` provides a common parameter named `tls` for specifying them uniformly.
`SINETStream` internally maps them to the messaging system-specific parameters.

* tls
    * One of the following:
        * A boolean specifying whether to use a TLS connection.
        * Specify the parameters related to TLS connection as YAML mapping (`{key}`:`{value}`)

The mapping may contain the following keys.

* ca_certs
    * The path of the CA certificate file (PEM).
* certfile
    * Client certificate (PEM) path
* keyfile
    * Private key (PEM) path
* keyfilePassword
    * Private key (PEM) password
* ciphers
    * A string specifying the ciphers available for SSL / TLS connections
* check_hostname
    * A boolean indicating whether the SSL handshake verifies that the certificate matches the broker's hostname
* trustStore
    * trustStore path ※
* trustStoreType
    * trustStore file format (jks, pkcs12,…) ※
* trustStorePassword
    * trustStore password ※
* keyStore
    * Keystore path ※
* keyStoreType
    * Keystore file format (jks, pkcs12,…) ※
* keyStorePassword
    * Keystore password ※

> ※  `TrustStore`, `trustStoreType`, `trustStorePassword`, `keyStore`, `keyStoreType`, `keyStorePassword`, `keyfilePassword` are for Java API ony.
> They cannot be specified with the Python API.

#### Setting Example

Below is an example of specifying a boolean value for the parameter `tls`.
In this case, a default messaging system-specific value is used as the setting value.

```
service-tls-1:
  type: mqtt
  brokers: mqtt.example.org
  tls: true
```

Below is an example of specifying a mapping for the parameter `tls`.

```
service-tls-2:
  type: kafka
  brokers:
    - kafka-1:9092
  tls:
    ca_certs: /etc/sinetstream/ca.pem
    certfile: certs/client.pem
    keyfile: certs/client.key
```

#### Priority

Instead of using the `tls` parameter, it is also possible to directly specify messaging system-specific parameters.

If you specify both the `tls` parameter and a messaging system-specific parameter for a service, the first value found in the following order is used.

1. The messaging system-specific parameters specified in the API function.
1. The `tls` parameter specified in the API function.
1. The messaging system-specific parameters specified in the configuration file.
1. The `tls` parameter specified in the configuration file.

### Parameters for encryption

`SINETStream` is capable of message content encryption at the frontend, which is independent from the communication encryption by the backend SSL/TLS.
By encrypting the message encryption you can protect your information even if a malicious third party peeks into the message stored in the broker.

* crypto
    * Set message encryption parameters as YAML mapping (`{key}`: `{value}`)
    * Just setting `crypto` does not enable the encryption.
      To encrypt the message content, enable the encryption by the `data_encryption` parameter or the API function parameter.

The mapping may contain the following keys.

* algorithm (mandatory)
    * Specify the encryption algorithm.
    * Valid values: "AES"
* key_length (optional)
    * Specify the key length (in bits).
    * Valid values: 128, 192, 256
    * Default value: 128
* mode (mandatory)
    * Specify the encryption mode.
    * Valid values: "CBC", "OFB", "CTR", "EAX", "GCM"
        * for Android: "CBC", "GCM"
    * Note: If "CBC" used, specify a value other than "none" for padding.
* padding (optional)
    * Specify the padding method.
    * Valid values: "none", "pkcs7"
    * Default value: "none"
* password (mandatory)
    * Specify the password.
* key_derivation (optional)
    * Specify the parameters related to key derivation function by YAML mapping
    * algorithm (optional)
        * Specify the algorithm for the key derivation function.
        * Valid values: "pbkdf2"
        * Default value: "pbkdf2"
    * salt_bytes (optional)
        * Specify the number of bytes for the salt.
	* Default value: 8
    * iteration (optional)
        * Specify the number of iterations.
	* Default value: 10000
    * prf (optional)
        * Specify the key derivation function (pseudorandom function).
	* Valid values: "HMAC-SHA256"
	* Default value: "HMAC-SHA256"

#### Setting Example

Below is an example of setting `crypto`.

```
service-aes-1:
  type: kafka
  brokers:
    - kafka0.example.org:9092
  crypto:
    algorithm: AES
    key_length: 256
    mode: EAX
    key_derivation:
      algorithm: pbkdf2
      iteration: 10000
    password: secret-000
```

## Messaging system-specific parameters

Parameters specific to the backend messaging system can be specified transparently.

* [Kafka-specific parameters](config-kafka.en.md)
* [MQTT-specific parameters](config-mqtt.en.md)
* [S3-specific parameters](https://translate.google.com/translate?hl=en&sl=ja&tl=en&u=https://nii-gakunin-cloud.github.io/sinetstream/docs/userguide/config-s3.html)

## Notes

### Difference between the Python API and the Java API

The following parameters are only valid for the Python API. They will be ignored if specified in the Java API.

* socket_options
* consumer_timeout_ms
* ssl_context
* ssl_crlfile
* api_version
* api_version_auto_timeout_ms
* selector
* value_serializer
* value_deserializer

The following parameters are only valid for the Java API. They will be ignored even if specified in the Python API.

* delivery_timeout_ms
* enable_idempotence
* transaction_timeout_ms
* transactional_id
* allow_auto_create_topics
* auto_offset_reset
* default_api_timeout_ms
* group_instance_id
* isolation_level
* client_rack
* client_dns_lookup
* ssl_truststore_location
* ssl_truststore_password
* ssl_truststore_type
* ssl_keystore_location
* ssl_keystore_password
* ssl_keystore_type

### Unsupported

SINETStream v1.* does not support the following parameters.

* metric_reporters
* metrics_num_samples
* metrics_sample_window_ms
* sasl_kerberos_service_name
* sasl_kerberos_domain_name
* sasl_oauth_token_provider
