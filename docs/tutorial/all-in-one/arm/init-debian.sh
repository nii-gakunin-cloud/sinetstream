#!/bin/bash

: ${CATOP:=/etc/pki/CA}
: ${CA_COUNTRY:=JP}
: ${CA_STATE:=Default_State}
: ${CA_ORGANIZATION:=Default_Organization}
: ${CA_CNAME:=private-ca}
: ${CA_DAYS:=1095}
: ${OPENSSL_CNF:=/etc/ssl/openssl.cnf}
: ${BROKER_HOSTNAME:=$(hostname -f)}
: ${KAFKA_CERTS_DIR:=/srv/kafka/config/certs}
: ${TRUSTSTORE_PASSWORD:=ca-pass}
: ${KEYSTORE_PASSWORD:=broker-pass}
: ${MOSQUITTO_CONF_D_DIR:=/etc/mosquitto/conf.d}
: ${CLIENT_CNAME:=client0}
: ${BAD_CLIENT_CNAME:=badclient}

setup_ca() {
  if [[ ! -f ${CATOP}/serial ]]; then
    mkdir -p ${CATOP}
    mkdir -p ${CATOP}/certs
    mkdir -p ${CATOP}/crl
    mkdir -p ${CATOP}/newcerts
    mkdir -p ${CATOP}/private
    touch ${CATOP}/index.txt
  fi

  sed -i -e '/demoCA/s#\./demoCA#/etc/pki/CA#' ${OPENSSL_CNF}

  sed -i -r -e '/unique_subject/s/^#//' -e '/copy_extensions/s/^#[ ]*//' \
    ${OPENSSL_CNF}

  if [[ ! -f ${CATOP}/private/cakey.pem ]]; then
    openssl req -new -keyout ${CATOP}/private/cakey.pem \
      -out ${CATOP}/careq.pem -nodes \
      -subj /C=${CA_COUNTRY}/ST=${CA_STATE}/O=${CA_ORGANIZATION}/CN=${CA_CNAME}

    openssl ca -create_serial -out ${CATOP}/cacert.pem -days ${CA_DAYS} \
      -batch -keyfile ${CATOP}/private/cakey.pem -selfsign -extensions v3_ca \
      -infiles ${CATOP}/careq.pem
  fi
}

generate_server_cert() {
  local openssl_conf=${OPENSSL_CNF}

  sed -i -r -e '/req_extensions/s/^#[ ]*//' ${openssl_conf}

  if ! grep -q alt_names ${openssl_conf}; then
    sed -i -r -e '/\[ v3_req \]/asubjectAltName = @alt_names' ${openssl_conf}
    cat >> ${openssl_conf} <<EOF
[ alt_names ]
DNS = ${BROKER_HOSTNAME}
EOF
  fi

  if [[ ! -f ${CATOP}/private/broker.key ]]; then
    openssl req -new -keyout ${CATOP}/private/broker.key \
      -out ${CATOP}/broker.csr -nodes \
      -subj /C=${CA_COUNTRY}/CN=${BROKER_HOSTNAME}
    openssl ca -batch -keyfile ${CATOP}/private/cakey.pem \
      -cert ${CATOP}/cacert.pem -in ${CATOP}/broker.csr \
      -out ${CATOP}/certs/broker.crt -policy policy_anything
  fi
}

setup_kafka_ssl() {
  local kafka_server_props=//srv/kafka/config/server.properties

  mkdir -p ${KAFKA_CERTS_DIR}
  if [[ ! -f ${KAFKA_CERTS_DIR}/broker.p12 ]]; then
    openssl pkcs12 -export -in ${CATOP}/cacert.pem \
      -inkey ${CATOP}/private/cakey.pem \
      -out ${KAFKA_CERTS_DIR}/ca.p12 -name ${CA_CNAME} \
      -CAfile ${CATOP}/cacert.pem -caname ${CA_CNAME} \
      -passout pass:${TRUSTSTORE_PASSWORD}

    openssl pkcs12 -export -in ${CATOP}/certs/broker.crt \
      -inkey ${CATOP}/private/broker.key \
      -out ${KAFKA_CERTS_DIR}/broker.p12 -name ${BROKER_HOSTNAME} \
      -CAfile ${CATOP}/cacert.pem -caname ${CA_CNAME} \
      -passout pass:${KEYSTORE_PASSWORD}
  fi

  if ! grep -q broker.p12 ${kafka_server_props}; then
    cat >> ${kafka_server_props} <<EOF
listeners=PLAINTEXT://:9092,SSL://:9093
advertised.listeners=PLAINTEXT://${BROKER_HOSTNAME}:9092,SSL://${BROKER_HOSTNAME}:9093
ssl.truststore.location=${KAFKA_CERTS_DIR}/ca.p12
ssl.truststore.password=${TRUSTSTORE_PASSWORD}
ssl.truststore.type=pkcs12
ssl.keystore.location=${KAFKA_CERTS_DIR}/broker.p12
ssl.keystore.password=${KEYSTORE_PASSWORD}
ssl.keystore.type=pkcs12
ssl.client.auth=${SSL_CLIENT_AUTH}
EOF
  fi
}

setup_mosquitto_ssl() {
  mkdir -p ${MOSQUITTO_CONF_D_DIR}
  mkdir -p /var/lib/mosquitto
  chown 1883:1883 /var/lib/mosquitto

  if [[ -f ${MOSQUITTO_CONF_D_DIR}/01-ssl.conf ]]; then
    return
  fi

  if [[ ! -f ${MOSQUITTO_CONF_D_DIR}/00-plain.conf ]]; then
    cat >> /etc/mosquitto/mosquitto.conf <<EOF
per_listener_settings true
EOF
    cat > ${MOSQUITTO_CONF_D_DIR}/00-plain.conf <<EOF
listener 1883
EOF
    cat > ${MOSQUITTO_CONF_D_DIR}/01-ssl.conf <<EOF
listener 8883
cafile ${CATOP}/cacert.pem
keyfile ${CATOP}/private/broker.key
certfile ${CATOP}/certs/broker.crt
EOF
  fi
}

expose_cert() {
  mkdir -p /srv/http
  cp ${CATOP}/cacert.pem /srv/http
}

setup_http_server() {
  expose_cert
  if ! grep -q program:http /etc/supervisord.conf; then
    cat >> /etc/supervisord.conf <<EOF
[program:http]
command=/usr/bin/python3 -m http.server 8080
directory=/srv/http
autostart=true
autorestart=true
stdout_logfile=/var/log/%(program_name)s.log
redirect_stderr=true
EOF
  fi
}

generate_client_cert() {
  local cert_dir=/srv/http
  if [[ ! -f ${cert_dir}/client0.key ]]; then
    openssl req -new -days 365 -nodes \
      -keyout ${cert_dir}/client0.key \
      -out ${cert_dir}/client0.csr \
      -subj /C=${CA_COUNTRY}/CN=${CLIENT_CNAME}
    openssl ca -batch -keyfile ${CATOP}/private/cakey.pem \
      -cert ${CATOP}/cacert.pem -in ${cert_dir}/client0.csr \
      -out ${cert_dir}/client0.crt -policy policy_anything
  fi
}

generate_bad_client_cert() {
  local cert_dir=/srv/http
  if [[ ! -f ${cert_dir}/bad-client.key ]]; then
    openssl req -new -x509 -newkey rsa:2048 -nodes \
      -keyout ${cert_dir}/bad-client.key \
      -out ${cert_dir}/bad-client.crt \
      -subj /C=${CA_COUNTRY}/CN=${BAD_CLIENT_CNAME}
  fi
}

generate_client_certs() {
  generate_client_cert
  generate_bad_client_cert
}

setup_etc_hosts() {
  if [[ ${BROKER_HOSTNAME} != $(hostname -f) ]]; then
    if ! grep -q ${BROKER_HOSTNAME} /etc/hosts; then
      echo "$(hostname -I) ${BROKER_HOSTNAME}" >> /etc/hosts
    fi
  fi
}

setup() {
  setup_ca
  generate_server_cert
  setup_etc_hosts
  setup_kafka_ssl
  setup_mosquitto_ssl
  setup_http_server
  if [[ ${GENERATE_CLIENT_CERTS} = "true" ]]; then
    generate_client_certs
  fi
}

if [[ ${ENABLE_BROKER} = "true" && ${ENABLE_SSL} = "true" ]]; then
  setup
fi
exec /usr/local/bin/supervisord -n -c /etc/supervisord.conf
