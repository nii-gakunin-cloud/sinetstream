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

[日本語](api-android.md)

SINETStream User Guide

# SINETStream for Android

* Package
    * jp.ad.sinet.stream.android.api

* Interfaces
    * SinetStreamReader.SinetStreamReaderListener
    * SinetStreamWriter.SinetStreamWriterListener

* Classes
    * SinetStreamReader
    * SinetStreamWriter


## Interface SinetStreamReader.SinetStreamReaderListener

### Method Summary
* onError
    * Called when any error condition has met.
* onMessageReceived
    * Called when a message has received on any subscribed topic.
* onReaderStatusChanged
    * Called when availability status has changed.

### Method Detail
#### onReaderStatusChanged

```
void onReaderStatusChanged(boolean isReady)
```

* Description:
    * Called when availability status has changed.
* Parameters:
    * isReady - true if "connected and subscribed" to the broker, false otherwise

#### onMessageReceived

```
void onMessageReceived(@NonNull
                       java.lang.String data)
```

* Description:
    * Called when a message has received on any subscribed topic.
* Parameters:
    * data - received message contents

#### onError

```
void onError(@NonNull
             java.lang.String description)
```

* Description:
    * Called when any error condition has met. The error might be detected either at the sinetstream-android level, or at underneath library level.
* Parameters:
    * description - brief description of the error.


## Interface SinetStreamWriter.SinetStreamWriterListener

### Method Summary
* onError
    * Called when any error condition has met.
* onWriterStatusChanged
    * Called when availability status has changed.

### Method Detail
#### onWriterStatusChanged

```
void onWriterStatusChanged(boolean isReady)
```

* Description:
    * Called when availability status has changed.
* Parameters:
    * isReady - true if "connected" to the broker, false otherwise

#### onError

```
void onError(@NonNull
             java.lang.String description)
```

* Description:
    * Called when any error condition has met. The error might be detected either at the sinetstream-android level, or at underneath library level.
* Parameters:
    * description - brief description of the error.


## Class SinetStreamReader

* Provides a set of API functions to be a Reader (= subscriber) in the SINETStream system.
* Due to the nature of messaging system, all methods listed below should be handled as asynchronous requests.
    * initialize
    * terminate
* User of this class must implement the `SinetStreamReader.SinetStreamReaderListener` in the calling [Activity](https://developer.android.com/guide/components/activities/intro-activities), so that the result of an asynchronous request or any error condition can be notified.


### Nested Class Summary
* SinetStreamReader.SinetStreamReaderListener
    * static interface


### Constructor Summary
* SinetStreamReader
    * Constructs a SinetStreamReader instance.


### Method Summary
* initialize
    * Connects to the broker and prepares oneself as a subscriber.
* terminate
    * Disconnects from the broker and cleans up allocated resources.


### Constructor Detail

```
public SinetStreamReader(@NonNull
                         android.content.Context context)
```

* Description:
    * Constructs a SinetStreamReader instance.
* Parameters:
    * context - the Application [context](https://developer.android.com/reference/android/content/Context) which implements `SinetStreamReader.SinetStreamReaderListener`, usually it is the calling Activity itself.
* Throws:
    * java.lang.RuntimeException - if given context does not implement the required listener.

### Method Detail
#### initialize

```
public void initialize(@NonNull
                       java.lang.String serviceName)
```

* Description:
    * Connects to the broker and prepares oneself as a subscriber.
    * Connection parameters will be specified by external configuration file.  
      See [Configuration files](config.en.md) for details.
* Parameters:
    * serviceName - the service name to match configuration parameters.

#### terminate

```
public void terminate()
```

* Descripton:
    * Disconnects from the broker and cleans up allocated resources.


## Class SinetStreamWriter

* Provides a set of API functions to be a Writer (= publisher) in the SINETStream system.
* Due to the nature of messaging system, all methods listed below should be handled as asynchronous requests.
    * initialize
    * terminate
    * publish
* User of this class must implement the `SinetStreamWriter.SinetStreamWriterListener` in the calling [Activity](https://developer.android.com/guide/components/activities/intro-activities), so that the result of an asynchronous request or any error condition can be notified.

### Nested Class Summary
* SinetStreamWriter.SinetStreamWriterListener
    * static interface


### Constructor Summary
* SinetStreamWriter
    * Constructs a SinetStreamWriter instance.


### Method Summary
* initialize
    * Connects to the broker and prepares oneself as a publisher.
* terminate
    * Disconnects from the broker and cleans up allocated resources.
* publish
    * Publishes given message to the broker.


### Constructor Detail

```
public SinetStreamWriter(@NonNull
                         android.content.Context context)
```

* Description:
    * Constructs a SinetStreamWriter instance.
* Parameters:
    * context - the Application [context](https://developer.android.com/reference/android/content/Context) which implements `SinetStreamReader.SinetStreamReaderListener`, usually it is the calling Activity itself.
* Throws:
    * java.lang.RuntimeException - if given context does not implement the required listener.

### Method Detail
#### initialize

```
public void initialize(@NonNull
                       java.lang.String serviceName)
```

* Description:
    * Connects to the broker and prepares oneself as a publisher.
    * Connection parameters will be specified by external configuration file.  
      See [Configuration files](config.en.md) for details.
* Parameters:
    * serviceName - the service name to match configuration parameters.

#### terminate

```
public void terminate()
```

* Description:
    * Disconnects from the broker and cleans up allocated resources.


#### publish

```
public void publish(@NonNull
                    java.lang.String message,
                    @Nullable
                    java.lang.Object userData)
```

* Description:
    * Publishes given message to the broker.
* Parameters:
    * message - a message to be published.
    * userData - User specified opaque object, to be returned by `WriterMessageCallback#onPublished()` as is.

