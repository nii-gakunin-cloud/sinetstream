#!/bin/sh

/srv/kafka/bin/kafka-configs.sh --zookeeper localhost:2181 --alter --add-config 'SCRAM-SHA-256=[password=user01],SCRAM-SHA-512=[password=user01]' --entity-type users --entity-name user01
/srv/kafka/bin/kafka-configs.sh --zookeeper localhost:2181 --alter --add-config 'SCRAM-SHA-256=[password=user02],SCRAM-SHA-512=[password=user02]' --entity-type users --entity-name user02
/srv/kafka/bin/kafka-configs.sh --zookeeper localhost:2181 --alter --add-config 'SCRAM-SHA-256=[password=user03],SCRAM-SHA-512=[password=user03]' --entity-type users --entity-name user03
