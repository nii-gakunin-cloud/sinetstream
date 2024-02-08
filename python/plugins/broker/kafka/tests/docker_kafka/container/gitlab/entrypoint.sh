#!/bin/bash

: "${BROKER_HOST:=broker}"
: "${CERT_URL:=http://$BROKER_HOST:8080/}"

setup_certs() {
  mkdir -p ${CERT_DIR}
  pushd ${CERT_DIR}
  sleep 1
  until curl -s -f ${CERT_URL}cacert.pem > /dev/null; do
    sleep 1
  done
  curl -s -O ${CERT_URL}cacert.pem
  curl -s -O ${CERT_URL}client0.crt
  curl -s -O ${CERT_URL}client0.key
  curl -s -O ${CERT_URL}bad-client.crt
  curl -s -O ${CERT_URL}bad-client.key
  popd
}

wait_broker() {
  /usr/local/bin/wait-for-it.sh -t 300 -h ${BROKER_HOST} -p ${BROKER_PORT}
}

if [ -n "${BROKER_PORT}" ]; then
  wait_broker
fi

if [ -n "${CERT_URL}" ]; then
  setup_certs
fi

exec "$@"