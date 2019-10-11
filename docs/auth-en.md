<!--
Copyright (C) 2019 National Institute of Informatics

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
--->

# Configure Authentication and Authorization in SINETStream

## Authentication for Kafka

### Supported security_protocol and sasl_mechanism

| security_protocol | Java | Python | note |
| --- | --- | --- | --- |
| PLAINTEXT | Supported | Supported | no authentication, no encryption |
| SSL | Supported | Supported | authentication iff enabled, encrypted | 
| SASL_PLAINTEXT | Supported | Supported | authenticated, no encryption |
| SASL_SSL | Supported | Supported | authenticated, encrypted |

| sasl_mechanism | Java | Python | note |
| --- | --- | --- | --- |
| PLAIN | Supported | Supported | simple username/password |
| SCRAM-SHA-256 | Supported | Not supported | Salted Challenge Response Authentication |
| SCRAM-SHA-512 | Supported | Not supported | |
| GSSAPI | (not tested) | (not tested) | Kerberos |
| OAUTHBEARER |  (not tested) | (not tested) | OAuth2 |

* Combination of protocol and mechanism

    | security_protocol | sasl_mechanism  | authentication | encryption |
    | --- | --- | --- | --- |
    | PLAINTEXT | (none) | No | No |
    | SASL_PLAINTEXT | PLAIN | Yes | No |
    | SASL_PLAINTEXT| SCRAM-SHA-* | Yes | No |
    | SSL | (none) | Yes(by client's cert)/No | Yes |
    | SASL_SSL | PLAIN | Yes | Yes |
    | SASL_SSL | SCRAM-SHA-* | Yes | Yes |

### Kafka Server

Note: the following port number `9092` can be changed.

#### security_protocol=PLAINTEXT

* Edit file `server.properties`

    ```
    listeners=PLAINTEXT://:9092
    ```

#### security_protocol=SSL

* Edit file `server.properties`

    ```
    listeners=SSL://:9092
    ```

    if client authentication is required:

    ```
    listeners=SSL://:9092
    ssl.client.auth=required
    ```

#### security_protocol=SASL_PLAINTEXT

* Edit file `server.properties`

    ```
    listeners=SASL_PLAINTEXT://:9092
    ```

#### security_protocol=SASL_SSL

* Edit file `server.properties`

    ```
    listeners=SASL_SSL://:9092
    ```

#### sasl_mechanism=PLAIN

* Edit file `server.properties`

    ```
    sasl.mechanism.inter.broker.protocol=PLAIN
    sasl.enabled.mechanisms=PLAIN
    ```

* Edit file `kafka_server_jaas.conf`

    set admin password for inter-broker communication

    ```
    KafkaServer {
        org.apache.kafka.common.security.plain.PlainLoginModule required
        username="admin"
        password="admin-secret";
    };
    ```

    To add a user,

    ```
    KafkaServer {
        org.apache.kafka.common.security.plain.PlainLoginModule required
        username="admin"
        password="admin-secret"
        user_alice="alice-secret";
    };
    ```

    * the format is user_<USER>="<PASSWORD>"
    * Restart Kafka server after edit kafka_server_jaas.conf to reload.

* Edit start script

    ```
    EXTRA_ARGS=-Djava.security.auth.login.config=config/kafka_server_jaas.conf bin/kafka-server-start.sh config/server.properties
    ```

#### sasl_mechanism=SCRAM-SHA-*

* Edit file `server.properties`

    ```
    sasl.mechanism.inter.broker.protocol=SCRAM-SHA-256,SCRAM-SHA-512
    sasl.enabled.mechanisms=SCRAM-SHA-256,SCRAM-SHA-512
    ```

* Set admin password for inter-broker communication

    ```
    $ bin/kafka-configs.sh --zookeeper localhost:2181 --alter --add-config 'SCRAM-SHA-256=[password=admin-secret],SCRAM-SHA-512=[password=admin-secret]' --entity-type users --entity-name admin
    ```

* Set user password

    ```
    $ bin/kafka-configs.sh --zookeeper localhost:2181 --alter --add-config 'SCRAM-SHA-256=[iterations=8192,password=alice-secret],SCRAM-SHA-512=[password=alice-secret]' --entity-type users --entity-name alice
    ```

    * No need to restart server.

### Kafka Client

* the following port number `9092` should be mached the serve setting.
* the following server host `localhost` is the server hostname.
* the following `service-foo` can be changed.

#### security_protocol=PLAINTEXT

* Edit file `.sinetstream_config.yml`

    ```
    service-foo:
        broker:
            - localhost:9092
    ```

#### security_protocol=SSL

* Edit file `.sinetstream_config.yml`

    ```
    service-foo:
        broker:
            - localhost:9092
        tls: true
    ```

    If using private CA:

    ```
    service-foo:
        broker:
            - localhost:9092
        tls:
            - ca_certs: /path/to/ca.pem
    ```

#### security_protocol=SASL_PLAINTEXT

* Edit file `.sinetstream_config.yml`

    ```
    service-foo:
        broker:
            - localhost:9092
        security_protocol: SASL_PLAINTEXT
    ```

#### security_protocol=SASL_SSL

* Edit file `.sinetstream_config.yml`

    ```
    service-foo:
        broker:
            - localhost:9092
        tls: true
        security_protocol: SASL_SSL
    ```

#### sasl_mechanism=PLAIN

* Edit file `.sinetstream_config.yml`

    ```
    service-foo:
        ...
        sasl_mechanism: PLAIN
        sasl_plain_username: alice
        sasl_plain_password: alice-secret
    ```

#### sasl_mechanism=SCRAM-SHA-*

* Edit file `.sinetstream_config.yml`

    ```
    service-foo:
        ...
        sasl_mechanism: SCRAM-SHA-512
        sasl_plain_username: alice
        sasl_plain_password: alice-secret
    ```

## Authentication for MQTT

### Mosquitto (MQTT server)

1. Edit file `mosquitto.conf`

    ```
    password_file /path/to/password
    ```

1. Edit file `password`

    ```
    $ mosquitto_passwd -c /path/to/password <USER1>
    Password: <PASSWORD1>
    reenter password: <PASSWORD1>
    ```
    
    Note: password file is hash'ed.

1. Restart server

1. Add more users,

    ```
    $ mosquitto_passwd -b /path/to/password <USER2> <PASSWORD2>
    ```

    Reload password by SIGHUP:

    ```
    pkill -HUP mosquitto
    ```

### MQTT Client

* Edit file `.sinetstream_config.yml`

    ```
    service-foo:
        username_pw_set:
            username: <USER1>
            password: <PASSWORD1>
    ```

## Authorization for Kafka

* Edit file `server.properties`

    ```
    authorizer.class.name=kafka.security.auth.SimpleAclAuthorizer
    super.users=User:admin
    ```

* Add producer

    the producer <USER> can publish any topics:

    ```
    kafka-acls --authorizer-properties zookeeper.connect=localhost:2181 --add --allow-principal User:<USER> --producer --topic "*"
    ```

* Add consumer like this:

    the consumer <USER> can subscribe any topics:

    ```
    kafka-acls --authorizer-properties zookeeper.connect=localhost:2181 --add --allow-principal User:<USER> --consumer --topic "*" --group "*"
    ```

* List ACL

    list all ACL:

    ```
    kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 --list
    ```

    list only specific ACL:

    ```
    kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 --list --topic <TOPIC>
    ```

    list only wildcard ACL:

    ```
    kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 --list --topic "*"
    ```

## Authorization for Mosquitto (MQTT server)

* Edit file `mosquitto.conf`

    ```
    acl_file /path/to/aclfile
    ```

* Restart server.    

* Edit the `aclfile`:

    ```
    user <USER1>
    topic read <TOPIC-A>
    topic write <TOPIC-B>
    topic readwrite <TOPIC-C>

    user <USER2>
    topic read <TOPIC-B>
    topic read <TOPIC-C>
    ```

    Reload config after edit aclfile:

    ```
    pkill -HUP mosquitto
    ```

## References

* Kafka
    * https://docs.confluent.io/2.0.0/kafka/sasl.html
    * http://kafka.apache.org/documentation/#security_sasl_plain_clientconfig
    * https://docs.confluent.io/current/kafka/authorization.html
* Mosquitto
    * http://www.steves-internet-guide.com/mqtt-username-password-example/
    * https://mosquitto.org/man/mosquitto_passwd-1.html
    * https://mosquitto.org/man/mosquitto-conf-5.html
    * http://www.steves-internet-guide.com/topic-restriction-mosquitto-configuration/

## Example

### Setup Kafka server

1. Download Kafka release tarball and un-tar.

    * [Download the code](https://kafka.apache.org/quickstart#quickstart_download)

    ```
    $ wget -q http://ftp.jaist.ac.jp/pub/apache/kafka/2.3.0/kafka_2.12-2.3.0.tgz
    $ tar -xzf kafka_2.12-2.3.0.tgz
    $ cd kafka_2.12-2.3.0
    $ ls
    bin/  config/  libs/  LICENSE  NOTICE  site-docs/
    $
    ```

1. Edit config files.

    1. Edit the Kafka server's property file.

        ```
        $ echo '
        # authn
        listeners=PLAINTEXT://:9092,SASL_PLAINTEXT://:9099
        security.inter.broker.protocol=SASL_PLAINTEXT
        sasl.mechanism.inter.broker.protocol=PLAIN
        sasl.enabled.mechanisms=PLAIN,SCRAM-SHA-256,SCRAM-SHA-512

        # authz
        authorizer.class.name=kafka.security.auth.SimpleAclAuthorizer
        super.users=User:admin
        ' >> config/server.properties
        $
        ```

    1. Edit the ZooKeeper's property file.

        ```
        $ echo '
        authProvider.1 = org.apache.zookeeper.server.auth.SASLAuthenticationProvider
        ' >> config/zookeeper.properties
        $
        ```

    1. Create the Kafka server's [JAAS](https://docs.oracle.com/javase/jp/8/docs/technotes/guides/security/jaas/JAASRefGuide.html) file.

        ```
        $ echo '
        KafkaServer {
            org.apache.kafka.common.security.plain.PlainLoginModule required
            username="admin"
            password="admin-secret"
            user_admin="admin-secret"
            user_alice="alice-secret";

            org.apache.kafka.common.security.scram.ScramLoginModule required
            username="admin"
            password="admin-secret";
        };

        Client {
            org.apache.zookeeper.server.auth.DigestLoginModule required
            username="kafka"
            password="kafka-secret";
        };
        ' > config/kafka_server_jaas.conf
        $
        ```

    1. Create the ZooKeeper's [JAAS](https://docs.oracle.com/javase/jp/8/docs/technotes/guides/security/jaas/JAASRefGuide.html) file.

        ```
        echo '
        Server {
            org.apache.zookeeper.server.auth.DigestLoginModule required
            user_super="admin-secret"
            user_kafka="kafka-secret";
        };
        ' > config/zookeeper_jaas.conf
        $
        ```

1. Start the servers.

    1. Start the ZooKeeper.

        ```
        $ env KAFKA_OPTS="-Djava.security.auth.login.config=config/zookeeper_jaas.conf" bin/zookeeper-server-start.sh config/zookeeper.properties
        [2019-10-09 16:56:04,785] INFO Reading configuration from: config/zookeeper.properties (org.apache.zookeeper.server.quorum.QuorumPeerConfig)
        ...
        [2019-10-09 16:56:04,837] INFO Server successfully logged in. (org.apache.zookeeper.Login)
        [2019-10-09 16:56:04,841] INFO binding to port 0.0.0.0/0.0.0.0:2181 (org.apache.zookeeper.server.NIOServerCnxnFactory)
        ```

    1. Start the Kafka server

        ```
        $ env EXTRA_ARGS="-Djava.security.auth.login.config=config/kafka_server_jaas.conf" bin/kafka-server-start.sh config/server.properties
        [2019-10-09 16:58:52,376] INFO Registered kafka:type=kafka.Log4jController MBean (kafka.utils.Log4jControllerRegistration$)
        ...
        ```

    1. Check the servers are ready.

        ```
        $ ss --listening --numeric |grep -w -e 2181 -e 9092 -e 9099
        tcp    LISTEN     0      50       :::9099                 :::*
        tcp    LISTEN     0      50       :::9092                 :::*
        tcp    LISTEN     0      50       :::2181                 :::*
        $
        ```

### Setup MQTT server

1. Download Mosquitto and build.

    ```
    $ wget -q https://mosquitto.org/files/source/mosquitto-1.6.2.tar.gz
    $ ls mosquitto-1.6.2.tar.gz
    $ tar -xzf mosquitto-1.6.2.tar.gz
    $ cd mosquitto-1.6.2
    $ make
    ...
    $
    ```

1. Setup config files.

    1. Edit config file.

        ```
        $ echo '
        password_file ./mosquitto.passwd
        acl_file ./mosquitto.acl
        ' >> mosquitto.conf
        $
        ```

    1. Register a user.

        ```
        $ src/mosquitto_passwd -c mosquitto.passwd alice
        Password: alice-secret
        Reenter password: alice-secret
        $
        ```

        To register another user:

        ```
        $ src/mosquitto_passwd -b mosquitto.passwd bob bob-secret
        $
        ```

    1. Create ACL file.

        ```
        $ echo '
        user alice
        topic readwrite #
        ' > mosquitto.acl
        $
        ```

1. Start the MQTT server.

    ```
    $src/mosquitto -c mosquitto.conf
    1570609944: mosquitto version 1.6.2 starting
    1570609944: Config loaded from mosquitto.conf.
    1570609944: Opening ipv4 listen socket on port 1883.
    1570609944: Opening ipv6 listen socket on port 1883.
    ```

### Check

It is assumed SINETStream has already been installed.

1. Edit file `.sinetstream_config.yml`.

    ```
    $ echo '
    local-kafka-authn-plain:
      type: kafka
      brokers:
        - localhost:9099
      security_protocol: SASL_PLAINTEXT
      sasl_mechanism: PLAIN
      sasl_plain_username: alice
      sasl_plain_password: alice-secret

    local-mqtt-authn:
      type: mqtt
      brokers:
        - localhost:1883
      username_pw_set:
        username: alice
        password: alice-secret
    ' >> .sinetstream_config.yml
    $ 
    ```

1. Check via Kafka.

    1. Run consumer.

        1. Python:

            ```
            $ python36 ~/path/to/sinetstream-x.x.x/python/sample/text/consumer.py -s local-kafka-authn-plain -t test1
            # service=local-kafka-authn-plain
            # topics=test1
            # group-id=None
            ```        

            When receive message:
            
            ```
            topic=test1 value(utf-8)='hogehoge'
            ```

        1. Java:

            ```
            $ /path/to/sinetstream-x.x.x/java/sample/text/text-producer-x.x.x/bin/text-consumer -s local-kafka-authn-plain -t test1
            ```

            When receive message:

            ```
            hogehoge
            ```

    1. Run producer.

        1. Python:

            ```
            $ echo "hogehoge" | python36 ~/repo/sinetstream-x.x.x/python/sample/text/producer.py -s local-kafka-authn-plain -t test1
            # service=local-kafka-authn-plain
            # topic=test1
            $
            ```
            
        1. Java:

            ```
            $ echo "hogehoge" | /path/to/sinetstream-x.x.x/java/sample/text/text-producer-x.x.x/bin/text-producer -s local-kafka-authn-plain -t test1
            $
            ```

1. Check via MQTT.

    1. Run consumer.

        1. Python:

            ```
            $ python36 ~/repo/sinetstream-x.x.x/python/sample/text/consumer.py -s local-mqtt-authn -t test1
            # service=local-mqtt-authn
            # topics=test1
            # group-id=None
            ```        

            When receive message:
            
            ```
            topic=test1 value(utf-8)='hogehoge'
            ```

        1. Java:

            ```
            $ /path/to/sinetstream-x.x.x/java/sample/text/text-producer-x.x.x/bin/text-consumer -s local-mqtt-authn -t test1
            ```

            When receive message:

            ```
            hogehoge
            ```

    1. Run producer.

        1. Python:

            ```
            [koie@vm00 tmp]$ echo "hogehoge" | python36 ~/repo/sinetstream-x.x.x/python/sample/text/producer.py -s local-mqtt-authn -t test1
            # service=local-mqtt-authn
            # topic=test1
            $
            ```

        1. Java:

            ```
            $ echo "hogehoge" | /path/to/sinetstream-x.x.x/java/sample/text/text-producer-x.x.x/bin/text-producer -s local-mqtt-authn -t test1
            $
            ```

