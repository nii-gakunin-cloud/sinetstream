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
  local ssl_broker=(${KAFKA_SSL_BROKER//:/ })
  local ip_addr=$(dig +short ${ssl_broker[0]})
  export KAFKA_SSL_BROKER_BAD_HOSTNAME="${ip_addr}:${ssl_broker[1]}"
}

install_packages() {
  if [ -d wheelhouse ]; then
    for pkg in wheelhouse/*.whl; do
      pip install -U --exists-action a ${pkg}
    done
  fi
}

install_packages

if [ -n "${CERT_URL}" ]; then
  setup_certs
  if [ -n "${KAFKA_SSL_BROKER}" ]; then
    setup_bad_hostname
  fi
fi

install_packages

if [ "$#" -eq 0 ]; then
  set -- python setup.py test
fi

exec "$@"
