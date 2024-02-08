#!/bin/bash

: ${CERT_DIR:=/opt/certs}

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

setup_bad_hostname() {
  local ssl_broker=(${MQTT_SSL_BROKER//:/ })
  local ip_addr=$(dig +short ${ssl_broker[0]})
  export MQTT_SSL_BROKER_BAD_HOSTNAME="${ip_addr}:${ssl_broker[1]}"
}

if [ -n "${CERT_URL}" ]; then
  setup_certs
  if [ -n "${MQTT_SSL_BROKER}" ]; then
    setup_bad_hostname
  fi
fi

if [ "$#" -eq 0 ]; then
  set -- tox
fi

exec "$@"
