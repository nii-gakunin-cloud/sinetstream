#!/bin/sh

# /srv/kafka/bin/kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 --add --allow-principal User:user02 --operation READ --topic 'mss-test-003' --group '*'
/srv/kafka/bin/kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 --add --allow-principal User:user02 --consumer --topic 'mss-test-003' --group '*'

#/srv/kafka/bin/kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 --add --allow-principal User:user03 --operation WRITE --topic 'mss-test-003'
/srv/kafka/bin/kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 --add --allow-principal User:user03 --producer --topic 'mss-test-003'
