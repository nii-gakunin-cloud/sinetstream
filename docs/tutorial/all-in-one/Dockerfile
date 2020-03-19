FROM centos:7.7.1908

# Install Apache Kafka
ARG KAFKA_VERSION=2.4.0
ENV KAFKA_VERSION $KAFKA_VERSION
ARG KAFKA_URL=http://ftp.riken.jp/net/apache/kafka/${KAFKA_VERSION}/kafka_2.12-${KAFKA_VERSION}.tgz
SHELL ["/bin/bash", "-o", "pipefail", "-c"]
RUN curl -s $KAFKA_URL | tar xzf - -C /srv \
 && yum -y install java-1.8.0-openjdk-headless \
 && yum clean all \
 && ln -sf /srv/kafka_2.12-${KAFKA_VERSION} /srv/kafka


# Install Eclipse Mosquitto
RUN yum -y install epel-release \
 && yum -y install mosquitto \
 && yum clean all \
 && yum-config-manager --disable epel


# Install openssh
RUN yum -y install \
      openssh-server \
      openssh-clients \
 && yum clean all


# Install Python 3.6
RUN yum -y --enablerepo=epel install \
      python36 \
      python36-PyYAML \
 && yum clean all \
 && pip3 install -U pip setuptools


# intall tools
RUN yum -y install \
      sudo \
      vim \
      iproute \
 && yum clean all


# Add user
ARG USERNAME=user01
RUN useradd -m $USERNAME \
 && echo "${USERNAME}:${USERNAME}" | chpasswd \
 && echo "${USERNAME} ALL=(ALL) ALL" > /etc/sudoers.d/90-${USERNAME} \
 && chmod 400 /etc/sudoers.d/90-${USERNAME}


# Install Supervisor
RUN pip3 install supervisor \
 && mkdir -m 0700 /var/run/supervisor /var/log/supervisor
COPY supervisord.conf /etc/supervisord.conf

# Setup OpenSSL
RUN yum -y install \
      openssl \
      ca-certificates \
 && yum clean all
COPY init.sh /usr/local/bin/
CMD ["/usr/local/bin/init.sh"]

EXPOSE 9092 9093 1883 8883 8080
ENV ENABLE_BROKER=true ENABLE_SSHD=false ENABLE_SSL=true \
  GENERATE_CLIENT_CERTS=true SSL_CLIENT_AUTH=requested

WORKDIR /home/$USERNAME
