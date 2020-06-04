#!/bin/sh

/srv/kafka/bin/kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 --add --allow-principal User:user02 --consumer --topic 'mss-test-003' --group '*'
/srv/kafka/bin/kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 --remove --allow-principal User:user02 --operation READ --group '*' --force

/srv/kafka/bin/kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 --add --allow-principal User:user03 --producer --topic 'mss-test-003'

/srv/kafka/bin/kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 --add --allow-principal User:user01 --consumer --topic '*' --group '*'
/srv/kafka/bin/kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 --add --allow-principal User:user01 --producer --topic '*'
/srv/kafka/bin/kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 --remove --allow-principal User:user01 --operation READ --group '*' --force
