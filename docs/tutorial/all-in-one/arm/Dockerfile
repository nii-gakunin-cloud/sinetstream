FROM debian:buster

SHELL ["/bin/bash", "-o", "pipefail", "-c"]

# Install Apache Kafka
RUN apt-get update \
 && apt-get -y install --no-install-recommends \
      openjdk-11-jdk-headless \
      curl \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/*

ARG KAFKA_VERSION=2.7.0
ENV KAFKA_VERSION $KAFKA_VERSION
ARG KAFKA_URL=https://ftp.jaist.ac.jp/pub/apache/kafka/${KAFKA_VERSION}/kafka_2.13-${KAFKA_VERSION}.tgz
RUN curl -sL $KAFKA_URL | tar xzf - -C /srv
RUN ln -sf /srv/kafka_2.13-${KAFKA_VERSION} /srv/kafka

# Install Eclipse Mosquitto
RUN apt-get update  \
 && apt-get -y install --no-install-recommends \
      mosquitto \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/*

# Install openssh
RUN apt-get update \
 && apt-get -y install --no-install-recommends \
      openssh-server \
      openssh-client \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/*


# Install Python 3.6
RUN apt-get update \
 && apt-get -y install --no-install-recommends \
      python3 \
      python3-pip \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/* \
 && pip3 install --no-cache-dir -U pip setuptools PyYAML


# intall tools
RUN apt-get update \
 && apt-get -y install --no-install-recommends \
      sudo \
      vim \
      iproute2 \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/*

# Setup OpenSSL
RUN apt-get update \
 && apt-get -y install --no-install-recommends \
      openssl \
      ca-certificates \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/*

# for SINETStream
RUN apt-get update \
 && apt-get -y install --no-install-recommends \
      build-essential \
      python3-dev \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/*

# Install Supervisor
RUN pip3 install --no-cache-dir -U supervisor \
 && mkdir -m 0700 /var/run/supervisor /var/log/supervisor
COPY supervisord.conf /etc/supervisord.conf

COPY init-debian.sh /usr/local/bin/init.sh
CMD ["/usr/local/bin/init.sh"]

# Add user
ARG USERNAME=user01
RUN useradd -m $USERNAME \
 && echo "${USERNAME}:${USERNAME}" | chpasswd \
 && echo "${USERNAME} ALL=(ALL) ALL" > /etc/sudoers.d/90-${USERNAME}

EXPOSE 9092 9093 1883 8883 8080
ENV ENABLE_BROKER=true ENABLE_SSHD=false ENABLE_SSL=true \
  GENERATE_CLIENT_CERTS=true SSL_CLIENT_AUTH=requested

WORKDIR /home/$USERNAME
