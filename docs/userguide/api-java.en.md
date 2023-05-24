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

[日本語](api-java.md)

SINETStream User Guide

# Java API

<pre>
1. Example
2. Summary of Java API Class
 2.1 MessageWriterFactory Class
 2.2 MessageWriter Class
 2.3 AsyncMessageWriter Class
 2.4 MessageReaderFactory Class
 2.5 MessageReader Class
 2.6 AsyncMessageReader Class
 2.7 Message Class
 2.8 Metrics Class
 2.9 The summary of exception
3. Messaging system-specific parameters
4. How to show a cheat sheet
</pre>


## 1. Example

First, a simple example is shown.

This example uses two services, namely `service-1` and `service-2`, each with a different messaging system as its backend.
The backend of `service-1` is Apache Kafka, which consists of four brokers, namey `kafka-1` thru `kafka-4`.
The backend of `service-2` is MQTT, which consists of one broker, `192.168.2.105`.

### Creating a configuration file

The configuration file contains settings for the clients to connect to the broker.
Please refer to the [Configuration files](config.en.md) for details.

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

```
MessageWriterFactory<String> factory =
    MessageWriterFactory.<String>builder()
        .service("service-1")
        .topic("topic-1")
        .consistency(AT_LEAST_ONCE)
        .build();
try (MessageWriter<String> writer = factory.getWriter()) {
    writer.write("Hello! This is the 1st message.");
    writer.write("Hello! This is the 2nd message.");
}
```

First, create a factory object `factory` by specifying the `service` name, the `topic` name, and `consistency`.
Invoke the `getWriter()` method on this `factory` to get a writer object for sending messages.
Then, invoke the `write()` method of the writer object to send a message to the broker.

### Receiving Messages

The following code receives messages from the topic `topic-1` of the messaging system associated with the service `service-1`.

```
MessageReaderFactory<String> factory =
    MessageReaderFactory.<String>builder()
        .service("service-1")
        .topic("topic-1")
        .consistency(AT_LEAST_ONCE)
        .receiveTimeout(Duration.ofSeconds(60))
        .build();

try (MessageReader<String> reader = factory.getReader()) {
    Message<String> msg;
    while (Objects.nonNull(msg = reader.read())) {
        System.out.println(msg.getValue());
    }
}
```

First, create a factory object `factory` by specifying the `service` name, the `topic` name, `consistency`, and the `receiveTimeout`.
Invoke the `getReader()` method on this `factory` to get a reader object to receive messages.
Then, invoke the `read()` method of the reader object to receive a message from the broker.
If it receives no message for the time specified by the `receiveTimeout` parameter after being invoked, it will return `null` and exit the loop.

## 2. Summary of Java API Class

### Main Class

* jp.ad.sinet.stream.api.MessageWriter
    * The class to send messages to the messaging system.
* jp.ad.sinet.stream.api.AsyncMessageWriter
    * The class to send messages to the messaging system. (asynchronous API)
* jp.ad.sinet.stream.api.MessageReader
    * The class to receive messages from the messaging system.
* jp.ad.sinet.stream.api.AsyncMessageReader
    * The class to receive messages from the messaging system. (asynchronous API)
* jp.ad.sinet.stream.utils.MessageWriterFactory
    * The factory class to create the MessageWriter objects.
* jp.ad.sinet.stream.utils.MessageReaderFactory
    * The factory class to create the MessageReader objects.

### 2.1 MessageWriterFactory Class

The Factory class for acquiring `MessageWriter`.

The `MessageWriterFactoryBuilder` is provided as an inner class to build a `MessageWriter` instance by specifying multiple parameters.
The following parameters can be specified in the builder class.

* service(String)
    * Service name.
    * The name must be defined in the configuration file.
* topic(String)
    * Topic name for sending the messages to.
* clientId(String)
    * Client ID.
    * If not specified, a value is automatically generated.
* consistency(Consistency)
    * The reliability of the message delivery.
    * Takes one of the enumerated values, namely `AT_MOST_ONCE`, `AT_LEAST_ONCE`, or `EXACTLY_ONCE`.
    * The default value is `AT_MOST_ONCE`.
* valueType(ValueType)
    * The type of message payload.
    * `MessageWriter.write()` will treat the given data as the type specified here.
    * When using the standard package, the following two types are supported.
        * Set `SimpleValueType.BYTE_ARRAY` (default) to treat the payload as `byte[]`.
        * Set `SimpleValueType.TEXT` to treat the payload as `java.lang.String`.
    * When using a plugin pacakge, other types may be supported.
    * When using the image type plugin provided with SINETStream v1.1 (or later) , the following type is supported.
        * Set `new ValueTypeFactory().get("image")` to treat the payload as `java.awt.image.BufferedImage`.
* serializer(Serializer\<T\>)
    * Message serializer.
    * If not specified, the default serializer (depending on `valueType`) will be used.
* dataEncryption(Boolean)
    * Enable or disable message encryption.
    * To enable it, the `crypto` parameter must be specified in the configuration file.
* parameter(String key, Object value)
    * Specify the messaging system-specific parameters.
* parameters(Map\<String, Object\> parameters)
    * Specify the messaging system-specific parameters.

Invoke the `MessageWriterFactory.builder()` method to get an instance of the builder class.
Then, invoke the `build()` method to get the factory object.
Below is an example.

```
MessageWriterFactory<String> factory =
    MessageWriterFactory.<String>builder()
        .service("service-1")
        .topic("topic-1")
        .consistency(AT_LEAST_ONCE)
        .build();
```

### 2.2 MessageWriter Class

The class to send messages to the broker.

Invoke the `getWriter()` method of the factory instance to get an instance of `MessageWriter`.
Since `MessageWrite` implements `AutoCloseable`, the try-with-resources statement can be used.
The method `write()` that sends a message blocks the sending process until it completed.
Below is an example.

```
MessageWriterFactory<String> factory = MessageWriterFactory.<String>builder()
        .service("service-1").build();

try (MessageWriter<String> writer = factory.getWriter()) {
    writer.write("message-1");
}
```

### 2.3 AsyncMessageWriter Class

The class to send messages to the broker.

Invoke the `getAsyncWriter()` method of the factory instance to get an instance of `AsyncMessageWriter`.
Since ` AsyncMessageWriter ` implements `AutoCloseable`, the try-with-resources statement can be used.
The method 'write()' that sends a message is an asynchronous process and returns a [JDeferred](http://jdeferred.org/) Promise object.

Below is an example.

```
MessageWriterFactory<String> factory = MessageWriterFactory.<String>builder()
        .service("service-1").build();

try (AsyncMessageWriter<String> writer = factory.getAsyncWriter()) {
    writer.write("message-1")
          .done(result -> System.out.println("write task done")
          .fail(result -> System.out.println("write task failed")
}
```

By using the Promise object methods '.done()' and '.fail()' that returned by the 'write()' method, it is possible to set processing according to the transmission result (success or failure).

The main methods of Promise are shown below.

* 'done()'
   – Triggered when the deferred object process completes successfully
* 'fail()'
   – Triggered when an exception occurs while processing a deferred object
* 'always()'
   – Triggered in all cases even if regardless of the processing result of the deferred object


### 2.4 MessageReaderFactory Class

The Factory class to acquire an `MessageReader` instance.

The `MessageReaderFactoryBuilder` is provided as an inner class to build a `MessageReader` instance by specifying multiple parameters.
The following parameters can be specified in the builder class.

* service(String)
    * Service name.
    * The name must be defined in the configuration file.
* topic(String)
    * Topic name for receiving the messages from.
    * If topics is specified, topic is ignored.
* topics(Collection\<String\>)
    * Collection of topics for receiving the messages from.
    * MessageReader can receive messages from multiple topics.
* clientId(String)
    * Client ID
    * If not specified, a value is automatically generated.
* consistency(Consistency)
    * Takes one of the enumerated values, namely `AT_MOST_ONCE`, `AT_LEAST_ONCE`, or `EXACTLY_ONCE`.
    * Default value is `AT_MOST_ONCE`.
* valueType(ValueType)
    * The type of message payload.
    * `MessageReader.read()` will treat the payload as the type specified here.
    * When using the standard package, the following two types are supported.
        * Set `SimpleValueType.BYTE_ARRAY` (default) to treat the payload as `byte[]`.
        * Set `SimpleValueType.TEXT` to treat the payload as `java.lang.String`.
    * When using a plugin pacakge, other types may be supported.
    * When using the image type plugin provided with SINETStream v1.1 (or later), the following type is supported.
        * Set `new ValueTypeFactory().get("image")` to treat the payload as `java.awt.image.BufferedImage`.
* deserializer(Serializer\<T\>)
    * Message deserializer.
    * If not specified, the default serializer (depending on valueType) will be used.
* dataEncryption(Boolean)
    * Enable or disable message decryption.
    * To enable it, the `crypto` parameter must be specified in the configuration file.
* receiveTimeout(Duration)
    * The timeout for the `read()` method to wait for a message to arrive.
* parameter(String key, Object value)
    * Specify the messaging system-specific parameters.
* parameters(Map\<String, Object\> parameters)
    * Specify the messaging system-specific parameters.

Invoke the `MessageReaderFactory.builder()` method to get an instance of the builder class.
Then, invoke the `build()` method to get a factory object.
Below is an example.

```
MessageReaderFactory<String> factory =
    MessageReaderFactory.<String>builder()
        .service("service-1")
        .topic("topic-1")
        .consistency(AT_LEAST_ONCE)
        .build();
```

### 2.5 MessageReader Class

The class to receive messages from the brokers.

Invoke the `getReader()` method of the factory instance to get an instance of `MessageReader`.
Since `MessageReader` implements `AutoCloseable`, the try-with-resources statement can be used.
The method 'read()' that receives the message receives the message or is specified in 'receiveTimeout()'.
Block until the timeout expires.

Below is an example.

```
MessageReaderFactory<String> factory = MessageReaderFactory.<String>builder()
        .service("service-1").receiveTimeout(Duration.ofSeconds(60)).build();
try (MessageReader<String> reader = factory.getReader()) {
    Message<String> msg;
    while (Objects.nonNull(msg = reader.read())) {
        System.out.println("TOPIC: " + msg.getTopic() + " MESSAGE: " + msg.getValue());
    }
}
```

The return value of the `read()` method is an instance of the `Message<T>` class.
The topic name can be obtained by the `getTopic()` method and the message value can be obtained by the `getValue()` method.

### 2.6 AsyncMessageReader Class

The class to receive messages from the brokers.

Invoke the `getAsyncReader()` method of the factory instance to get an instance of `AsyncMessageReader`.
Set a callback to be invoked when processing the received message by using 'addOnMessageCallback()' method.
The received message is passed by the argument of the callback.

Below is an example.

```
MessageReaderFactory<String> factory = MessageReaderFactory.<String>builder()
        .service("service-1").receiveTimeout(Duration.ofSeconds(60)).build();

AsyncMessageReader<String> reader = factory.getAsyncReader();
reader.addOnMessageCallback((msg) -> {
    System.out.println("TOPIC: " + msg.getTopic() + " MESSAGE: " + msg.getValue());
});

// other processing

reader.close();
```


### 2.7 Message Class

The class to represent a message received from the brokers.

* getTopic()
    * The topic name where the message came from.
* getValue()
    * The payload of the message.
* getTimestamp()
    * The time the message was sent (UNIX time) in second.
    * `0` indicates no timestamp is set
* getTimestampMicroseconds()
    * The time the message was sent (UNIX time) in microsecond.
    * `0` indicates no timestamp is set

### 2.8 Metrics Class

Metrics class
You can get metrics information by invoking the getMetrics() method for Reader/Writer objects.

* MessageReader#getMetrics()
* AsyncMessageReader#getMetrics()
* MessageWriter#getMetrics()
* AsyncMessageWriter#getMetrics()

The Reader/Writer metrics are reset when the resetMetrics() method was called from the Reader/Writer class.
If the `reset_raw` argument is set to True, the metrics of the backend messaging system will also be reset if possible.

* MessageReader#resetMetrics(boolean reset_raw)
* MessageWriter#resetMetrics(boolean reset_raw)
* AsyncMessageReader#resetMetrics(boolean reset_raw)
* AsyncMessageWriter#resetMetrics(boolean reset_raw)

The resetMetrics() method without the argument `reset_raw` is the same as specifying reset_raw=false.

* MessageReader#resetMetrics()
* MessageWriter#resetMetrics()
* AsyncMessageReader#resetMetrics()
* AsyncMessageWriter#resetMetrics()

> Eclipse Paho, an MQTT client library used in the SINETStream MQTT plugin, does not provide metrics collection capability.
> The Kafka client library has the capability, but does not provide the reset function.

The metrics are measured at the boundary of the SINETStream main library and the specified messaging system plugin.
Therefore, a stream of encrypted massages will be measured if the data encryption function provided by SINETStream is used.

#### Property

* getStartTime(), getStartTimeMillis()
    * The Unix time when the measurement was started.
        * The unit of time returned by getStartTime() is seconds.
        * The unit of time returned by getStartTimeMillis() is milliseconds.
    * The time when the Reader/Writer object was created or reset.
* getEndTime(), getEndTimeMillis()
    * The Unix time when the measurement was completed.
        * The unit of time returned by getEndTime() is seconds.
        * The unit of time returned by getEndTimeMillis() is milliseconds.
    * The time when the getMetrics() method was called.
* getTime(), getTimeMillis()
    * Measurement time (= EndTime - StartTime).
        * The unit of time returned by getTime() is seconds.
        * The unit of time returned by getTimeMillis() is milliseconds.
* getMsgCountTotal()
    * The cumulative number of messages sent and received.
* getMsgCountRate()
    * The rate of the number of messages sent and received.
    * = msg_count_total / time
    * return 0 if time is 0.
* getMsgBytesTotal()
    * The Cumulative amount of messages sent and received in bytes.
* getMsgBytesRate()
    * The rate of the amount of messages sent and received.
    * = msg_bytes_total / time
    * return 0 if time is 0.
* getMsgSizeMin()
    * The minimum size of messages sent and received in bytes.
* getMsgSizeAvg()
    * The average size of messages sent and received in bytes.
    * = msg_bytes_total / msg_count_total
    * return 0 if msg_count_total is 0.
* getMsgSizeMax()
    * The maximum size of messages sent and received in bytes.
* getErrorCountTotal()
    * The cumulative number of errors.
* getErrorCountRate()
    * The error rate.
    * = error_count_total / time
    * return 0 if time is 0.
* getRaw()
    * The metrics provided by the specified messaging system client library.

#### Examples

Display the number of received messages and its amount in bytes:

```
try (MessageReader<String> reader = factory.getReader()) {
    // (1)
    Message<String> msg;
    while (Objects.nonNull(msg = reader.read())) {
        ;
    }
    Metrics metrics = reader.getMetrics();  // Metrics on the accumulation from (1)
    System.out.println("COUNT: " + metrics.getMsgCountTotal());
    System.out.println("BYTES: " + metrics.getMsgBytesTotal());
}
```

Display the receive rate for every 10 messages:

```
try (MessageReader<String> reader = factory.getReader()) {
    Message<String> msg;
    int count = 0;
    while (Objects.nonNull(msg = reader.read())) {
        count++;
        if (count == 10) {
            count = 0;
            Metrics metrics = reader.getMetrics();
            reader.resetMetrics();
            System.out.println("COUNT/s: " + metrics.getMsgCountRate());
            System.out.println("BYTES/s: " + metrics.getMsgBytesRate());
        }
    }
}
```

### 2.9 The summary of exception

| Exception name | Method name | |
| ---  | --- | --- |
| NoConfigException | MessageReaderFactory#getReader() MessageReaderFactory#getAsyncReader() MessageWriterFactory#getWriter() MessageWriterFactory#getAsyncWriter() | The configuration file does not exist or cannot be read.|
| NoServiceException | MessageReaderFactory#getReader() MessageReaderFactory#getAsyncReader() MessageWriterFactory#getWriter() MessageWriterFactory#getAsyncWriter() | The specified service name is not defined in the configuration file. |
| UnsupportedServiceException |MessageReaderFactory#getReader() MessageReaderFactory#getAsyncReader() MessageWriterFactory#getWriter() MessageWriterFactory#getAsyncWriter() | The specified service is not supported. |
| ConnectionException |MessageReaderFactory#getReader() MessageReaderFactory#getAsyncReader() MessageWriterFactory#getWriter() MessageWriterFactory#getAsyncWriter() | Error connecting to the broker |
| InvalidConfigurationException |MessageReaderFactory#getReader() MessageReaderFactory#getAsyncReader() MessageWriterFactory#getWriter() MessageWriterFactory#getAsyncWriter() | The content of the configuration file is invalid. |
| SinetStreamIOException | MessageReaderFactory#getReader() MessageReaderFactory#getAsyncReader() MessageWriterFactory#getWriter() MessageWriterFactory#getAsyncWriter()  MessageReader\<T\>#read() MessageReader\<T\>#close() MessageWriter\<T\>#write(T) MessageWriter\<T\>#close() | Error in IO between the messaging system and SINETStream. |
| SinetStreamException | MessageReaderFactory#getReader() MessageReaderFactory#getAsyncReader() MessageWriterFactory#getWriter() MessageWriterFactory#getAsyncWriter() MessageReader\<T\>#read() MessageReader\<T\>#close() MessageWriter\<T\>write(T) MessageWriter\<T\>close() | Other SINETStream errors|
| InvalidMessageException | MessageReader\<T\>#read() | The type of message does not match `valueType` |
| AuthenticationException | MessageReaderFactory#getReader() MessageReaderFactory#getAsyncReader() MessageWriterFactory#getWriter() MessageWriterFactory#getAsyncWriter() | Error authenticating with the broker |
| AuthorizationException | MessageReader\<T\>#read() MessageWriter\<T\>#write() | Error in unauthorized operation (see note below) |

Note: AuthorizationException may not raise in the following cases:

1. When using MQTT (Mosquitto)
    * The MQTT broker raises no error for unauthorized operation.
1. When using Kafka with `Consistency` set to `AT_MOST_ONCE`
    * The client does not wait for a response from the broker after sending a message. Therefore, the client cannot detect an error on the broker side.

## 3. Messaging system-specific parameters

* [Kafka-specific parameters](config-kafka.en.md)
* [MQTT-specific parameters](config-mqtt.en.md)
* [S3-specific parameters](https://translate.google.com/translate?hl=en&sl=ja&tl=en&u=https://nii-gakunin-cloud.github.io/sinetstream/docs/userguide/config-s3.html)

## 4. How to show a cheat sheet

Run `java -jar` followed by the API's jar filename to show a cheat sheet.

```
$ java -jar SINETStream-api-1.1.0.jar

==================================================
MessageWriter example
--------------------------------------------------
MessageWriterFactory<String> factory = MessageWriterFactory.<String>builder()
        .service("service-1")
        .topic("topic-1")
        .build();
try (MessageWriter<String> writer = factory.getWriter()) {
    writer.writer("message");
}
--------------------------------------------------
MessageWriterFactory parameters:
    service(String service)
        Service name defined in the configuration file. (REQUIRED)
    clientId(String clientId)
        If not specified, the value is automatically generated.
    consistency(Consistency consistency[=AT_MOST_ONCE])
        consistency: AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE
    dataEncryption(Boolean dataEncryption[=false])
        Message encryption.
    parameter(String key, Object parameter)
        Rewrites the parameters described in the configuration file only for the specified key / value pairs.
    parameters(Map parameters)
        Overwrites the parameters described in the configuration file with the specified values.
    serializer(Serializer serializer)
        If not specified, use default serializer according to valueType.
    topic(String topic)
        The topic to send.
    valueType(ValueType valueType[=SimpleValueType.BYTE_ARRAY])
        The type of message.
==================================================
MessageReader example
--------------------------------------------------
MessageReaderFactory<String> factory = MessageReaderFactory.<String>builder()
        .service("service-1")
        .topic("topic-1")
        .build();
try (MessageReader<String> reader = factory.getReader()) {
    Message<String> msg;
    while (Objects.nonNull(msg = reader.read)) {
        System.out.println(msg.getValue());
    }
}
--------------------------------------------------
MessageReaderFactory parameters:
    service(String service)
        Service name defined in the configuration file. (REQUIRED)
    clientId(String clientId)
        If not specified, the value is automatically generated.
    consistency(Consistency consistency[=AT_MOST_ONCE])
        consistency: AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE
    dataEncryption(Boolean dataEncryption[=false])
        Message encryption.
    deserializer(Deserializer deserializer)
        If not specified, use default deserializer according to valueType.
    parameter(String key, Object parameter)
        Rewrites the parameters described in the configuration file only for the specified key / value pairs.
    parameters(Map parameters)
        Overwrites the parameters described in the configuration file with the specified values.
    topic(String topic)
        The topic to receive.
    topics(Collection topics)
        A list of topics to receive.
    valueType(ValueType valueType[=SimpleValueType.BYTE_ARRAY])
        The type of message.
```
