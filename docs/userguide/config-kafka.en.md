**準備中** (2020-06-04 18:27:50 JST)

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

[日本語](config-kafka.md)

SINETStream User Guide

# Kafka-specific parameters

## Parameters for `MessageWriter`

* acks
    * The request is considered as completed when this number of acks are received.
* compression_type
    * The type of data compression.
    * Choose one of `none`, `gzip`, `snappy`, `lz4`, or `zstd`.
* retries
    * Set a value larger than zero to re-send the message failed to be sent due to a temporary error.
* batch_size
    * The batch size of the sending process.
* linger_ms
    * The delay time (in milliseconds) for performing collective transmission.
* buffer_memory
    * The amount of memory (in bytes) used for buffering.
* max_block_ms
    * The blocking time (in milliseconds) for the `send()` method and the `partitionsFor()` method.
* max_request_size
    * Maximum size of a request.
* max_in_flight_requests_per_connection
    * Maximum number of requests submitted without ACK response.
* delivery_timeout_ms
    * The maximum time (in milliseconds) to make a response from the time when the `send()` method is invoked.
* enable_idempotence
    * Enable or disable the idempotent producer mode.
* transaction_timeout_ms
    * The maximum time (in milliseconds) that the transaction coordinator waits for updates from the producer.
* transactional_id
    * The transaction ID.
* key_serializer
    * The serializer of the message key.

## Parameters for `MessageReader`

* group_id
    * The name of the consumer group.
* fetch_min_bytes
    * The minimum amount of data returned by the server for a fetch request
* fetch_max_wait_ms
    * The maximum time (in milliseconds) that the server will wait for a response before satisfying the `fetch_min_bytes` condition.
* fetch_max_bytes
    * The maximum amount of data returned by the server for a fetch request.
* max_partition_fetch_bytes
    * The maximum amount of data per partition returned by the server.
* enable_auto_commit
    * Whether to commit offsets regularly in the background.
* auto_commit_interval_ms
    * The time interval (in milliseconds) for the automatic commit of the offset.
* check_crcs
    * Whether to automatically check CRC32 for consumed records.
* partition_assignment_strategy
    * List of the class names for consumer group partition assignment strategies.
* max_poll_records
    * The maximum number of records returned by the `poll()` method.
* max_poll_interval_ms
    * The maximum delay time (in milliseconds) of the `poll()` method.
* session_timeout_ms
    * The maximum time (in milliseconds) used to detect consumer failure.
* heartbeat_interval_ms
    * Expected heartbeat time (in milliseconds) between the consumer and the coordinator.
* allow_auto_create_topics
    * Whether to create topics automatically.
* auto_offset_reset
    * Specify the behavior when Kafka has no initial offset.
    * Choose one of `latest`, `earliest`, or `none`.
* default_api_timeout_ms
    * The maximum time (in milliseconds) for default timeout of the consumer API.
* group_instance_id
    * Consumer instance ID.
* isolation_level
    * Isolation level.
    * Choose one of `read_committed` or `read_uncommitted`.
* key_deserializer
    * Message key deserializer.

## Parameters for both `MessageWriter` and `MessageReader`

* request_timeout_ms
    * Timeout (in milliseconds) for request from the client
* retry_backoff_ms
    * Time to backoff (in milliseconds) when retrying on error.
* reconnect_backoff_ms
    * The time (in milliseconds) to wait before trying to reconnect to a particular host.
* reconnect_backoff_max_ms
    * The maximum time (in milliseconds) to wait when reconnecting to a broker that has repeatedly failed to connect
* receive_buffer_bytes
    * The TCP receiving buffer size.
* send_buffer_bytes
    * The TCP sending buffer size.
* metadata_max_age_ms
    * The time (in milliseconds) to force updating metadata.
* security_protocol
    * The protocol used to communicate with the broker.
* connections_max_idle_ms
    * The time (in milliseconds) after which an idle connection is closed.
* exclude_internal_topics
    * Whether to publish the internal topics to the consumers.
* client_dns_lookup
    * The method for the client to lookup DNS.
* ssl_check_hostname
    * Whether to verify that the SSL certificate matches the host name.
* ssl_cafile
    * The path of the CA certificate file.
* ssl_certfile
    * The path of the client certificate file.
* ssl_keyfile
    * The path of the client private key file.
* ssl_password
    * The password for loading the certificate file.
* ssl_ciphers
    * Available ciphers for SSL connections.
* ssl_truststore_location
    * The path of the trust store file.
* ssl_truststore_password
    * The password of the trust store file.
* ssl_truststore_type
    * The file format of the trust store file.
* ssl_keystore_location
    * The path of the keystore file.
* ssl_keystore_password
    * The password of the keystore file.
* ssl_keystore_type
    * The file format of keystore

## The configuration example of Kafka

When compressing a batch of records with gzip using the Kafka's function, set the `compression_type` parameter as follows.
(It is only necessary to specify `compression_type` on the Writer side)

```
service-kafka:
  type: kafka
  brokers:
    - kafka0.example.org:9092
  compression_type: gzip
```
