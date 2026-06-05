# syntax=docker/dockerfile:1
# check=skip=SecretsUsedInArgOrEnv
FROM rockylinux/rockylinux:10.1.20251126 AS base
SHELL ["/bin/bash", "-o", "pipefail", "-c"]

RUN dnf -y install \
    java-21-openjdk-headless \
    python3 \
    python3-pip \
    gzip \
    ca-certificates \
  && dnf clean all

# Install Eclipse Mosquitto, supervisor
RUN dnf install -y dnf-plugins-core \
  && dnf config-manager --set-enabled crb \
  && dnf install -y epel-release \
  && dnf install -y \
    supervisor \
    mosquitto \
  && dnf clean all

# Install Apache Kafka
ARG KAFKA_VERSION=4.2.0
RUN curl -sO https://downloads.apache.org/kafka/${KAFKA_VERSION}/kafka_2.13-${KAFKA_VERSION}.tgz \
  && curl -s https://downloads.apache.org/kafka/${KAFKA_VERSION}/kafka_2.13-${KAFKA_VERSION}.tgz.sha512 \
      | tr -d ' \n' | sed 's/.*://' \
      | xargs -I{} echo "{}  kafka_2.13-${KAFKA_VERSION}.tgz" \
      | sha512sum -c - \
  && tar xzf kafka_2.13-${KAFKA_VERSION}.tgz -C /srv \
  && mv /srv/kafka_2.13-${KAFKA_VERSION} /srv/kafka \
  && rm kafka_2.13-${KAFKA_VERSION}.tgz

# Project license files
COPY LICENSE NOTICE /opt/licenses/

# --- Tutorial image ---
FROM base AS tutorial
SHELL ["/bin/bash", "-o", "pipefail", "-c"]

RUN dnf -y install \
    openssh-server \
    openssh-clients \
    vim \
    iproute \
    procps-ng \
  && dnf clean all

COPY scripts/tutorial/supervisord.conf /etc/supervisord.conf
RUN mkdir -p /etc/supervisord.d

# Add user
ARG USERNAME=user01
RUN useradd -m $USERNAME
WORKDIR /home/${USERNAME}

EXPOSE 9092 1883
ENV ENABLE_BROKER=true \
    ENABLE_SSHD=false

COPY scripts/tutorial/init.sh /usr/local/bin/init.sh
ENTRYPOINT ["/usr/local/bin/init.sh"]

# --- CI test image ---
FROM base AS ci
SHELL ["/bin/bash", "-o", "pipefail", "-c"]

RUN dnf -y install openssl && dnf clean all

COPY scripts/ci/supervisord.conf /etc/supervisord.conf

EXPOSE 9092 9093 1883 8883 8080
ENV ENABLE_BROKER=true \
    ENABLE_SSL=true \
    GENERATE_CLIENT_CERTS=true \
    SSL_CLIENT_AUTH=requested

COPY scripts/ci/init.sh /usr/local/bin/
ENTRYPOINT ["/usr/local/bin/init.sh"]
