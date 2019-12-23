#!/bin/sh
set -eu
set -x

# pub    bro    sub
# aws -- aws -- aws

#export JAVA_HOME=/usr/lib/jvm/java-1.8.0/jre
PKGDIR="repo/sinetstream-java/sample/perf/sinetstream-binary-producer-1.0.0"

SERVICE_KAFKA=service-kafka-aws-tls
SERVICE_MQTT=service-mqtt-aws-tls
BROKER_KAFKA=kafka-aws:9092
BROKER_MQTT=tcp://mqtt-aws:1883

FPS=0	# full speed
TIMEOUT=60000	# 60s
TOPIC=perftest
QOS=1

RUN() {
        local MEMO="$1"; shift
	local OPT_BROKER="$1"; shift
	local BROKER="$1"; shift
	local PUBLISHER="$1"; shift
	local SUBSCRIBER="$1"

	local UTOPIC="$TOPIC-$(date '+%s')"
        local LOGFILE="$TOPIC-$(date '+%Y%m%d-%H%M%S')-$MEMO"

	ssh -n "$SUB_HOST" "$PKGDIR/bin/$SUBSCRIBER" "$OPT_BROKER" "$BROKER" --topic_src "$UTOPIC" --qos "$QOS" --nmsg "$NMSG" --timeout "$TIMEOUT" --logfile "$LOGFILE-sub.csv" &
	local PID_SUB="$!"
	sleep 10		# XXX TEKITO-
	ssh -n "$PUB_HOST" "$PKGDIR/bin/$PUBLISHER" "$OPT_BROKER" "$BROKER" --topic_dst "$UTOPIC" --qos "$QOS" --bytes "$MSZ" --nmsg "$NMSG" --fps "$FPS" --logfile "$LOGFILE-pub.csv" || {
            echo "EEE: PUBLISHER FAILED."
        }
	wait "$PID_SUB" || {
            echo "EEE: SUBSCRIBER FAILED."
        }

	sleep 10		# XXX TEKITO-

	return 0
}

RUN_kafka() {
        RUN "$TOPO-kafkaS-$MSZ-tls" --service "$SERVICE_KAFKA" sinetstream-binary-producer sinetstream-binary-consumer
        #RUN "$TOPO-kafkaN-$MSZ-tls" --broker "$BROKER_KAFKA" kafka-binary-producer kafka-binary-consumer
}
RUN_mqtt() {
        RUN "$TOPO-mqttS-$MSZ-tls" --service "$SERVICE_MQTT" sinetstream-binary-producer sinetstream-binary-consumer
        #RUN "$TOPO-mqttN-$MSZ-tls" --broker "$BROKER_MQTT" mqtt-binary-producer mqtt-binary-consumer
}

TOPO="aws-aws"
PUB_HOST="aws2"
SUB_HOST="aws2"

MSZ=100
NMSG=10000000
RUN_kafka
NMSG=10000
RUN_mqtt

MSZ=1000
NMSG=1000000
RUN_kafka
NMSG=20000
RUN_mqtt

MSZ=64000
NMSG=20000
RUN_kafka
NMSG=2000
RUN_mqtt

####################

TOPO="raspi-aws"
PUB_HOST="raspi"
SUB_HOST="aws2"

MSZ=100
NMSG=100000
RUN_kafka
NMSG=1000
RUN_mqtt

MSZ=1000
NMSG=5000
RUN_kafka
NMSG=100
RUN_mqtt

MSZ=64000
NMSG=100
RUN_kafka
RUN_mqtt

####################

TOPO="aws-raspi"
PUB_HOST="aws2"
SUB_HOST="raspi"

MSZ=100
NMSG=100000
RUN_kafka
NMSG=1000
RUN_mqtt

MSZ=1000
NMSG=5000
RUN_kafka
NMSG=100
RUN_mqtt

MSZ=64000
NMSG=100
RUN_kafka
RUN_mqtt

####################

TOPO="raspi-raspi"
PUB_HOST="raspi"
SUB_HOST="raspi"

MSZ=100
NMSG=100000
RUN_kafka
NMSG=1000
RUN_mqtt

MSZ=1000
NMSG=5000
RUN_kafka
NMSG=1000
RUN_mqtt

MSZ=64000
NMSG=100
RUN_kafka
RUN_mqtt
