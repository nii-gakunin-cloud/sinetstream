#!/bin/bash
set -euo pipefail

setup_kafka() {
  local kafka_server_props=/srv/kafka/config/server.properties

  # Move CONTROLLER listener from 9093 to 19093
  sed -i -e 's|CONTROLLER://:9093|CONTROLLER://:19093|' \
         -e 's|CONTROLLER://localhost:9093|CONTROLLER://localhost:19093|' \
         -e 's|controller.quorum.bootstrap.servers=localhost:9093|controller.quorum.bootstrap.servers=localhost:19093|' \
    ${kafka_server_props}

  logdir="$(grep log.dir ${kafka_server_props} | cut -d = -f 2 | cut -d , -f 1)"
  if [[ ! -f "${logdir}/meta.properties" ]]; then
    pushd /srv/kafka
    KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
    bin/kafka-storage.sh format --standalone -t $KAFKA_CLUSTER_ID -c config/server.properties
    popd
  fi
}

setup_mosquitto() {
  local conf_d_dir=/etc/mosquitto/conf.d
  mkdir -p ${conf_d_dir}
  mkdir -p /var/lib/mosquitto /run/mosquitto
  chown mosquitto:mosquitto /var/lib/mosquitto /run/mosquitto

  if [[ ! -f ${conf_d_dir}/00-plain.conf ]]; then
    cat > ${conf_d_dir}/00-plain.conf <<EOF
listener 1883
allow_anonymous true
EOF
  fi
}

setup_sshd() {
  if [[ ! -f /etc/ssh/ssh_host_rsa_key ]]; then
    ssh-keygen -q -f /etc/ssh/ssh_host_rsa_key -N "" -t rsa
  fi
  if [[ ! -f /etc/ssh/ssh_host_ecdsa_key ]]; then
    ssh-keygen -q -f /etc/ssh/ssh_host_ecdsa_key -N "" -t ecdsa
  fi
  if [[ ! -f /etc/ssh/ssh_host_ed25519_key ]]; then
    ssh-keygen -q -f /etc/ssh/ssh_host_ed25519_key -N "" -t ed25519
  fi

  # Disable password authentication
  sed -i -e 's/^#\?PasswordAuthentication.*/PasswordAuthentication no/' \
    /etc/ssh/sshd_config

  # Set random password for user01 (not usable via SSH)
  local password
  password="$(head -c 32 /dev/urandom | base64 | tr -dc 'a-zA-Z0-9' | head -c 16)"
  echo "user01:${password}" | chpasswd
}

if [[ ${ENABLE_BROKER} = "true" ]]; then
  setup_kafka
  setup_mosquitto
fi
if [[ ${ENABLE_SSHD} = "true" ]]; then
  setup_sshd
fi
if [[ $# -gt 0 ]]; then
  exec "$@"
fi
exec /usr/bin/supervisord -n -c /etc/supervisor/supervisord.conf
