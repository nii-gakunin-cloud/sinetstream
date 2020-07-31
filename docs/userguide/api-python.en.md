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

[日本語](api-python.md)

SINETStream User Guide

# Python API

* Example
* Summary of Python API classes
    * MessageReader Class
    * MessageWriter Class
    * Message Class
    * Metrics Class
    * Summary of exception
* Messaging system-specific parameters
    * Apache Kafka
    * MQTT (Eclipse Paho)
* How to show a cheat sheet


## Example

First, a simple example is shown.

This example uses two services, namely `service-1` and `service-2`, each with a different messaging system as its backend.
The backend of `service-1` is Apache Kafka, which consists of four brokers, namely `kafka-1` thru `kafka-4`.
The backend of `service-2` is MQTT, which consists of one broker, `192.168.2.105`.

### Creating a configuration file

The configuration file contains settings for the clients to connect to the broker.
Please refer to the [Configuration file](config.en.md) for details.

In this example, we create the following configuration file `.sinetstream_config.yml` in the current directory on the client machine.

```
service-1:
  type: kafka
  brokers:
    - kafka-1:9092
    - kafka-2:9092
    - kafka-3:9092
    - kafka-4:9092
service-2:
  type: mqtt
  brokers: 192.168.2.105:1883
  username_pw_set:
    username: user01
    password: pass01
```

### Sending Messages

The following code sends two messages to the topic `topic-1` of the messaging system associated with the service `service-1`.

```python
from sinetstream import MessageWriter

writer = MessageWriter('service-1', 'topic-1')
with writer as f:
    f.publish(b'Hello! This is the 1st message.')
    f.publish(b'Hello! This is the 2nd message.')
```

First, create an instance of the MessageWriter object by specifying the service name and the topic name.
Open this instance with a `with` statement and send messages to the broker by invoking the `publish()` method in the block.

> The MessageWriter object automatically connects to the messaging system when entering the `with` block, and
> it automatically closes the connection when exiting the `with` block.

By default, the argument of `publish()` is a byte sequence.
To transfer an object other than a byte sequence, specify value_type or value_serializer in the constructor of the [MessageWriter Class](#messagewriter-class).

### Receiving Messages

The following code receives messages from the topic `topic-1` of the messaging system associated with the service `service-1`.

```python
from sinetstream import MessageReader

reader = MessageReader('service-1', 'topic-1')
with reader as f:
    for msg in f:
        print(msg.value)
```

First, create an instance of the MessageReader object by specifying the service name and the topic name.
Open this instance with a `with` statement, loop the iterator for `f` in the block, and receive messages from the broker by referring to the `value` property of the iterator.

> The MessageReader object automatically connects to the messaging system when entering the `with` block, and
> it automatically closes the connection when exiting the `with` block.

By default, the message reader process does not time out and the `for` statement becomes an infinite loop.
To exit the `for` loop, specify the `receive_timeout_ms` parameter in the constructor of the [MessageReader class](@messager3eader-class) or perform a signal handling.

## Summary of Python API classes

* sinetstream.MessageReader
    * The class to receive messages from the messaging system.
* sinetstream.MessageWriter
    * The class to send messages to the messaging system.
* sinetstream.Message
    * The class to represent a message.
* sinetstream.SinetError
    * The parent class of all the exception classes in SINETStream

### MessageReader Class

#### `MessageReader()`

The constructor of the MessageReader class

```
MessageReader(
    service,
    topics=None,
    consistency=AT_MOST_ONCE,
    client_id=DEFAULT_CLIENT_ID,
    value_type=None,
    value_deserializer=None,
    receive_timeout_ms=float("inf"),
    **kwargs)
```

##### Parameters

* service
    * Service name.
    * The name must be defined in the configuration file.
* topics
    * Topic name.
    * Specify a `str` or a `list` for a single topic.
    * Specify a `list` when subscribing for multiple topics.
    * If not specified, the value specified in the configuration file will be used as default.
* consistency
    * The reliability of the message delivery.
    * AT_MOST_ONCE (=0)
        * A message may not arrive.
    * AT_LEAST_ONCE (=1)
        * A message always arrives but may arrive many times.
    * EXACTLY_ONCE (=2)
        * A message always arrives only once.
* client_id
    * Client name
    * If any of DEFAULT_CLIENT_ID, None, or an empty string is specified, the library will automatically generate a value.
    * The generated value can be obtained as a property of this object.
* value_type
    * The type name of message payload.
    * `MessageReader` will treat the payload as the type specified here.
    * When using the standard package, the following two type names are supported.
        * Set `"byte_array"` (default) to treat the payload as `bytes`.
        * Set `"text"` to treat the payload as `str`.
    * When using a plugin pacakge, other type names may be supported.
    * When using the image type plugin provided with SINETStream v1.1 (or later), the following type name is supported.
        * Set `"image"` to treat the payload as `numpy.ndarray`, which is the image data type in OpenCV.
        * The color order in `numpy.ndarray` is BGR, which is consistent with OpenCV.
* value_deserializer
    * The function used to decode the value from the byte array in the message.
    * If not specified, an appropriate deserializer function will be used according to `value_type`.
* receive_timeout_ms
    * Maximum time (ms) to wait for message to arrive.
    * Once timed out, no more messages can be read from this connection.
* data_encryption
    * Enable or disable message encryption and decryption.
* kwargs
    * Specify the messaging system-specific parameters as YAML mappings.

The parameters specified in `kwargs` are passed to the constructor of the backend messaging system.
Please refer to the [Messaging system-specific parameters](#messaging-system-specific-parameters) of for details.

For the arguments other than `service`, their default values can be specified in the configuration file.
If the same parameter is specified in both the configuration file and the constructor argument, the value specified in the constructor argument takes precedence.

** Limitation: SINETStream downgrades to `AT_LEAST_ONCE` even if `EXACTLY_ONCE` is specified for Kafka `consistency`. **

##### Exception

* NoServiceError
    * The specified service name is not defined in the configuration file.
* NoConfigError
    * The configuration file does not exist or cannot be read
* InvalidArgumentError
    * The format of the specified argument is incorrect,
      e.g., the value of consistency is out of range or the topic name includes invalid character, etc.
* UnsupportedServiceTypeError
    * The plugin for the specified service type is not installed.

#### Properties

The following properties can be used to get the parameter value specified in the configuration file or in the constructor.

* `client_id`
* `consistency`
* `topics`
* `value_type`

#### `MessageReader.open()`

Connects to the broker of the messaging system.
Implicitly invoked when using MessageReader in a `with` statement; not intended for explicit invocation.

##### Returned value

A handler that mentains the connection status with the messaging system.

##### Exception

* ConnectionError
    * Error connecting to the broker.
* AlreadyConnectedError
    * `open()` is called again for an object that is already connected.

#### `MessageReader.close()`

Disconnects from the broker of the messaging system.
Implicitly invoked when using MessageReader in a `with` statement; not intended for explicit invocation.

#### `MessageReader.__iter__()`

Returns an iterator for the messages received from the messaging system.

##### Exception

The following errors may occur when calling `next()` to the iterator.

* AuthorizationError
    * Tried to receive messages from an unauthorized topic.

AuthorizationError does not occur in the following cases:

1. When using MQTT (Mosquitto)
    * Because the MQTT broker raises no error for unauthorized operation.

### MessageWriter Class

#### `MessageWriter()`

```
MessageWriter(
    service,
    topic,
    consistency=AT_MOST_ONCE,
    client_id=DEFAULT_CLIENT_ID,
    value_serializer=None,
    **kwargs)
```

The constructor of MessageWriter class

##### Parameters

* service
    * Service name
    * The name must be defined in the configuration file.
* topic
    * Topic name
    * If not specified, the value specified in the configuration file will be used as default.
* consistency
    * Specify the reliability of message delivery.
    * AT_MOST_ONCE (=0)
        * A message may not be delivered.
    * AT_LEAST_ONCE (=1)
        * A message is always delivered but may be delivered many times.
    * EXACTLY_ONCE (=2)
        * A message is always delivered only once.
* client_id
    * Client name.
    * If any of DEFAULT_CLIENT_ID, None, or an empty string is specified, the library will automatically generate a value.
* value_type
    * The type name of message payload.
    * `MessageWriter.publish()` will treat the given data as the type specified here.
    * When using the standard package, the following two type names are supported.
        * Set `"byte_array"` (default) to treat the payload as `bytes`.
        * Set `"text"` to treat the payload as `str`.
    * When using a plugin pacakge, other type names may be supported.
    * When using the image type plugin provided with SINETStream v1.1 or laster, the following type name is supported.
        * Set `"image"` to treat the payload as `numpy.ndarray`, which is the image data type in OpenCV.
        * The color order in `numpy.ndarray` is BGR, which is consistent with OpenCV.
* value_serializer
    * The function used to encode the value to the byte array in the message.
    * If not specified, an appropriate serializer function will be used according to `value_type`.
* data_encryption
    * Enable or disable message encryption and decryption.
* kwargs
    * Specify the messaging system-specific parameters as YAML mappings.

The parameters specified in `kwargs` are passed to the constructor of the backend messaging system.
Please refer to the [Messaging system-specific parameters](#messaging-system-specific-parameters) of for details.

For the arguments other than `service`, their default values can be specified in the configuration file.
If the same parameter is specified in both the configuration file and the constructor argument, the value specified in the constructor argument takes precedence.

** Limitations: SINETStream downgrades to `AT_LEAST_ONCE` even if `EXACTLY_ONCE` is specified for Kafka `consistency`. **

##### Exception

* NoServiceError
    * The `service` corresponding to the value specified for service does not exist in the configuration file
* NoConfigError
    * The configuration file does not exist or cannot be read
* InvalidArgumentError
    * The format of the specified argument is incorrect.
      When the value of `consistency` is out of range or a character string that is not allowed as a `topic` name
* UnsupportedServiceTypeError
    * The plugin for the messaging system corresponding to `type` specified in the configuration file is not installed

#### Properties

The following properties can be used to get the parameter value specified in the configuration file or in the constructor.

* `client_id`
* `consistency`
* `topic`
* `value_type`

#### `MessageWriter.open()`

Connects to the broker of the messaging system.
Implicitly invoked when using MessageReader in a `with` statement; not intended for explicit invocation.

##### Returned value

A handler that mentains the connection status with the messaging system

##### Exception

* ConnectionError
    * Error connecting to the broker
* AlreadyConnectedError
    * `open()` is called again for an object that is already connected

#### `MessageWriter.close()`

Disconnects from the broker of the messaging system.
Implicitly invoked when using MessageWriter in a `with` statement; not intended for explicit invocation.

#### `MessageWriter.publish(message)`

Sends a message to the broker of the messaging system.
The message is serialized according to the `value_type` parameter value or using the function specified by `value_serializer`.

##### Exception

* InvalidMessageError
    * The type of `message` does not match `value_type` or the function specified by `value_serializer`.
* AuthorizationError
    * Tried to send messages to an unauthorized topic.

AuthorizationError does not occur in the following cases:

1. When using MQTT (Mosquitto)
    * Because the MQTT broker raises no error for unauthorized operation.
1. When using Kafka with `Consistency` set to `AT_MOST_ONCE`
    * Because the client does not wait for a response from the broker after sending a message.
      Therefore, the client cannot detect an error on the broker side.

### Message class

The wrapper class for the message object provided by the messaging system.

#### Property

All the properties are read only.

* value
    * The payload of the message (given by `Raw.value` for Kafka and `raw.payload` for MQTT).
    * By default, a byte sequence is obtained.
    * The type of `value` depends on the `value_type` parameter or the `value_deserializer` function specified in MessageReader.
        * If `value_type` is `"byte_array"` (default), the type is `bytes`.
        * If `value_type` is `"text"`, the type is `str`.
        * If `value_deserializer` is specified, an object converted by this function is obtained.
* topic
    * The topic name.
* timestamp
    * The time the message was sent (UNIX time)
         * Unit: second
         * Type: float
    * `0` indicates no time is set
* timestamp_us
    * The time the message was sent (UNIX time)
         * Unit: microsecond
         * Type: int
    * `0` indicates no time is set
* raw
    * The message object provided by the messaging system.

### Metrics Class

Metrics class
You can get metrics information by referencing the metrics property for Reader/Writer objects.

* MessageReader.metrics
* MessageWriter.metrics
* AsyncMessageReader.metrics
* AsyncMessageWriter.metrics

The Reader/Writer metrics are reset when the reset_metrics() method was called from the Reader/Writer class.
If the `reset_raw` argument is set to True, the metrics of the backend messaging system will also be reset if possible.

* MessageReader.reset_metrics(reset_raw=False)
* MessageWriter.reset_metrics(reset_raw=False)
* AsyncMessageReader.reset_metrics(reset_raw=False)
* AsyncMessageWriter.reset_metrics(reset_raw=False)

> Eclipse Paho, an MQTT client library used in the SINETStream MQTT plugin, does not provide metrics collection capability.
> The Kafka client library has the capability, but does not provide the reset function.

The metrics are measured at the boundary of the SINETStream main library and the specified messaging system plugin.
Therefore, a stream of encrypted massages will be measured if the data encryption function provided by SINETStream is used.

#### Property

* start_time, start_time_ms
    * float
    * The Unix time when the measurement was started.
        * The unit of the start_time is seconds.
        * The unit of the start_time_ms is milliseconds.
    * The time when the Reader/Writer object was created or reset.
* end_time, end_time_ms
    * float
    * The Unix time when the measurement was completed.
        * The unit of the end_time is seconds.
        * The unit of the end_time_ms is milliseconds.
    * The time referenced in the metrics property.
* time, time_ms
    * float
    * Measurement time (= EndTime - StartTime).
        * The unit of the time is seconds.
        * The unit of the time_ms is milliseconds.
    * = end_time - start_time
* msg_count_total
    * int
    * The cumulative number of messages sent and received.
* msg_count_rate
    * float
    * The rate of the number of messages sent and received.
    * = msg_count_total / time
* msg_bytes_total
    * int
    * The Cumulative amount of messages sent and received in bytes.
* msg_bytes_rate
    * float
    * The rate of the amount of messages sent and received.
    * = msg_bytes_total / time
* msg_size_min
    * int
    * The minimum size of messages sent and received in bytes.
* msg_size_avg
    * float
    * The average size of messages sent and received in bytes.
* msg_size_max
    * int
    * The maximum size of messages sent and received in bytes.
* error_count_total
    * int
    * The cumulative number of errors.
* error_count_rate
    * float
    * The error rate.
    * = error_count_total / time
* raw
    * The metrics provided by the specified messaging system client library.

#### Examples

Display the number of received messages and its amount in bytes:

```python
from sinetstream import MessageReader

reader = MessageReader('service-1', 'topic-001')
# (1)
with reader as f:
    for msg in f:
        pass
    m = reader.metrics  # Statistics on the accumulation from (1)
    print(f'COUNT: {m.msg_count_total}')
    print(f'BYTES: {m.msg_bytes_total}')
```

Display the receive rate for every 10 messages:

```python
from sinetstream import MessageReader

reader = MessageReader('service-1', 'topic-001')
with reader as f:
    count = 0
    for msg in f:
        count += 1
        if (count == 10):
            count = 0
            m = reader.metrics
            reader.reset_metrics()
            print(f'COUNT/s: {m.msg_count_rate}')
            print(f'BYTES/s: {m.msg_bytes_rate}')
```

### Summary of exception

| exception | origin method | reason |
|---|---|---|
| `NoServiceError` | `MessageReader()`,`MessageWriter()` | The specified service name is not defined in the configuration file. |
| `UnsupportedServiceTypeError` | `MessageReader()`, `MessageWriter()` | The specified service type is not supported or the corresponding plugin is not installed. |
|` NoConfigError` | `MessageReader()`, `MessageWriter()` | The configuration file does not exist. |
| `InvalidArgumentError` | `MessageReader()`, `MessageWriter()`, `MqttReader.open()`, `MqttWriter.open()`, `MqttWriter.publish()` |  The argument is incorrect. |
| `ConnectionError` | `KafkaReader.open()`, `KafkaWriter.open()`, `MqttReader.open()`, `MqttWriter.open()`, `MqttWriter.publish()` |  Error connecting to the broker. |
| `AlreadyConnectedError` | `KafkaReader.open()`, `KafkaWriter.open()`, `MqttReader.open()`, `MqttWriter.open()` |  Already connected to a broker. |

## Messaging system-specific parameters

`kwargs` can be used to transparently pass parameters to the backend messaging system.
The actual parameters that can be passed depend on the backend.
No validation is performed.

### Apache Kafka

Basically, the constructor arguments to
[KafkaConsumer](https://kafka-python.readthedocs.io/en/master/apidoc/KafkaConsumer.html) and
[KafkaProducer](https://kafka-python.readthedocs.io/en/master/apidoc/KafkaProducer.html) of
[kafka-python](https://kafka-python.readthedocs.io/en/master/) can be specified as parameters.
If the parameter is valid only in `KafkaConsumer` or `KafkaProducer`, it affects `MessageReader` or `MessageWriter`, respectively.

[Kafka-specific parameters](config-kafka.en.md)

### MQTT (Eclipse Paho)

Basically, the constructor arguments and the setter function (`XXX_set`) arguments of
[paho.mqtt.client.Client](https://www.eclipse.org/paho/clients/python/docs/#client)
can be specified as parameters.

[MQTT-specific parameters](config-mqtt.en.md)

## How to show a cheat sheet

After installing SINETStream, run `python3 -m sinetstream` to show a cheat sheet.

```
$ python3.6 -m sinetstream
==================================================
Default parameters:
MessageReader(
    service=SERVICE,                 # Service name defined in the configuration file. (REQUIRED)
    topics=TOPICS,                   # The topic to receive.
    consistency=AT_MOST_ONCE,        # consistency: AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE
    client_id=DEFAULT_CLIENT_ID,     # If not specified, the value is automatically generated.
    value_type="byte_array",         # The type of message.
    value_deserializer=None          # If not specified, use default deserializer according to valueType.
)
MessageWriter(
    service=SERVICE,                 # Service name defined in the configuration file. (REQUIRED)
    topic=TOPIC,                     # The topic to send.
    consistency=AT_MOST_ONCE,        # consistency: AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE
    client_id=DEFAULT_CLIENT_ID,     # If not specified, the value is automatically generated.
    value_type="byte_array",         # The type of message.
    value_serializer=None            # If not specified, use default serializer according to valueType.
)
```
