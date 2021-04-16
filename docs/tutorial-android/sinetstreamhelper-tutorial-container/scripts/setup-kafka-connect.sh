#!/bin/bash

CONNECTOR_URL=http://localhost:8083/connectors
retry=10

setup_mqtt_connector() {
curl -f -s ${CONNECTOR_URL}/mqtt-android-sensor-data || \
curl -f -X POST ${CONNECTOR_URL} \
  -H "Content-Type: application/json" -d @- <<'EOS'
{
  "name": "mqtt-android-sensor-data",
  "config": {
    "connector.class": "com.datamountaineer.streamreactor.connect.mqtt.source.MqttSourceConnector",
    "task.max": "1",
    "connect.mqtt.converter.throw.on.error": "true",
    "connect.mqtt.hosts": "tcp://localhost:1883",
    "connect.mqtt.service.quality": "1",
    "connect.mqtt.kcql": "INSERT INTO sensor-data SELECT * FROM sensor-data WITHCONVERTER=`jp.ad.sinet.sinetstream.converter.SinetStreamConverter`",
    "value.converter": "jp.ad.sinet.sinetstream.common.connect.SinetStreamJsonConverter"
  }
}
EOS
}

setup_s3_connector() {
curl -f -s ${CONNECTOR_URL}/s3-sink || \
curl -f -X POST ${CONNECTOR_URL} \
  -H "Content-Type: application/json" -d @- <<'EOS'
{
  "name": "s3-sink",
  "config": {
    "connector.class": "io.confluent.connect.s3.S3SinkConnector",
    "tasks.max": 1,
    "storage.class": "io.confluent.connect.s3.storage.S3Storage",
    "store.url": "http://localhost:9000",
    "s3.bucket.name": "sensor-data",
    "topics": "sensor-data",
    "flush.size": "1",
    "partitioner.class": "io.confluent.connect.storage.partitioner.DefaultPartitioner",
    "value.converter": "jp.ad.sinet.sinetstream.common.connect.SinetStreamJsonConverter",
    "value.converter.schemas.enable": "false",
    "format.class": "io.confluent.connect.s3.format.json.JsonFormat",
    "schema.compatibility": "NONE"
  }
}
EOS
}

if /usr/local/bin/wait-for-it.sh localhost:8083 -t 1200 ; then
  for ((i=0; i < $retry; i++)); do
      if setup_mqtt_connector; then
          break
      fi
      sleep 1
  done
  if [[ $i -ge $retry ]]; then
      exit 1
  fi
  for ((i=0; i < $retry; i++)); do
      if setup_s3_connector; then
          break
      fi
      sleep 1
  done
  if [[ $i -ge $retry ]]; then
      exit 1
  fi
fi
