FROM node:14-buster-slim AS build

COPY chart /opt/chart/

WORKDIR /opt/chart

RUN npm install -g @vue/cli \
 && rm -rf node_modules package-lock.json \
 && npm install

RUN npm run build

FROM centos:7.9.2009

SHELL ["/bin/bash", "-o", "pipefail", "-c"]

RUN sed -i -e '/tsflags/s/^tsflags/#tsflags/' /etc/yum.conf

# Install Apache Kafka(Confluent)
ARG CONFLUENT_VERSION=6.0
ARG CONFLUENT_COMMUNITY_VERSION=2.13
RUN rpm --import https://packages.confluent.io/rpm/${CONFLUENT_VERSION}/archive.key \
 && echo -e "[Confluent.dist]\n\
name=Confluent repository (dist)\n\
baseurl=https://packages.confluent.io/rpm/${CONFLUENT_VERSION}/7\n\
gpgcheck=1\n\
gpgkey=https://packages.confluent.io/rpm/${CONFLUENT_VERSION}/archive.key\n\
enabled=0\n\
\n\
[Confluent]\n\
name=Confluent repository\n\
baseurl=https://packages.confluent.io/rpm/${CONFLUENT_VERSION}\n\
gpgcheck=1\n\
gpgkey=https://packages.confluent.io/rpm/${CONFLUENT_VERSION}/archive.key\n\
enabled=0\n\
" > /etc/yum.repos.d/confluent.repo \
 && yum -y install --enablerepo=Confluent \
      confluent-community-${CONFLUENT_COMMUNITY_VERSION} \
 && yum -y install java-11-openjdk-headless \
 && yum clean all \
 && rm -rf /var/cache/yum/*
VOLUME /var/lib/kafka
VOLUME /var/lib/zookeeper

# Install Eclipse Mosquitto
RUN yum -y install epel-release \
 && yum -y install mosquitto \
 && yum clean all \
 && rm -rf /var/cache/yum/* \
 && yum-config-manager --disable epel

# Install Python 3.8
ARG PYTHON3="scl enable rh-python38"
ARG PIP3INSTALL="pip3_install_-U_--no-cache-dir_pip_setuptools"
RUN yum -y install centos-release-scl \
 && yum -y install rh-python38 rh-python38-python-pyyaml \
 && yum clean all \
 && rm -rf /var/cache/yum/* \
 && ${PYTHON3} "pip3 install -U --no-cache-dir pip setuptools"

# Kafka Connect: S3
RUN yum -y install --enablerepo=Confluent confluent-hub-client \
 && yum clean all \
 && rm -rf /var/cache/yum/* \
 && confluent-hub install --no-prompt confluentinc/kafka-connect-s3:latest

# Kafka Connect: MQTT
# ARG STREAM_REACTOR_VERSION=2.1.3
ARG STREAM_REACTOR_VERSION=2.1.1
ARG STREAM_REACTOR_KAFKA_VERSION=2.5.0
RUN mkdir -p /usr/share/java/stream-reactor \
 && curl -L https://github.com/lensesio/stream-reactor/releases/download/${STREAM_REACTOR_VERSION}/kafka-connect-mqtt-${STREAM_REACTOR_VERSION}-${STREAM_REACTOR_KAFKA_VERSION}-all.tar.gz | \
      tar xzf - -C /usr/share/java/stream-reactor

# Nginx
RUN echo $'[nginx-stable]\n\
name=nginx stable repo\n\
baseurl=http://nginx.org/packages/centos/$releasever/$basearch/\n\
gpgcheck=1\n\
enabled=0\n\
gpgkey=https://nginx.org/keys/nginx_signing.key\n\
module_hotfixes=true\n\
\n\
[nginx-mainline]\n\
name=nginx mainline repo\n\
baseurl=http://nginx.org/packages/mainline/centos/$releasever/$basearch/\n\
gpgcheck=1\n\
enabled=0\n\
gpgkey=https://nginx.org/keys/nginx_signing.key\n\
module_hotfixes=true\n\
' > /etc/yum.repos.d/nginx.repo \
 && yum -y install --enablerepo=nginx-mainline nginx \
 && yum clean all \
 && rm -rf /var/cache/yum/*
COPY nginx/* /etc/nginx/conf.d/

# MinIO
RUN curl -L -o /usr/bin/minio https://dl.min.io/server/minio/release/linux-amd64/minio \
 && curl -L -o /usr/bin/mc https://dl.min.io/client/mc/release/linux-amd64/mc \
 && chmod +x /usr/bin/minio /usr/bin/mc \
 && mkdir -p /var/lib/minio
VOLUME /var/lib/minio

# intall tools
RUN yum -y install \
      sudo \
      vim \
      iproute \
      nmap-ncat \
 && yum clean all \
 && rm -rf /var/cache/yum/*

# Add user
ARG USERNAME=user01
RUN useradd -m $USERNAME \
 && echo "${USERNAME}:${USERNAME}" | chpasswd \
 && echo "${USERNAME} ALL=(ALL) ALL" > /etc/sudoers.d/90-${USERNAME} \
 && chmod 400 /etc/sudoers.d/90-${USERNAME}
WORKDIR /home/$USERNAME

# Install Supervisor
RUN ${PYTHON3} "pip3 install --no-cache-dir supervisor" \
 && mkdir -m 0700 /var/run/supervisor /var/log/supervisor
COPY supervisord.conf /etc/supervisord.conf

RUN curl -L -o /usr/local/bin/wait-for-it.sh \
      https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh \
 && chmod a+x /usr/local/bin/wait-for-it.sh

CMD ["scl", "enable", "rh-python38", "/opt/rh/rh-python38/root/usr/local/bin/supervisord -n -c /etc/supervisord.conf"]

EXPOSE 80 1883

COPY java /usr/share/java/
COPY scripts/* /usr/local/bin/
COPY --from=build /opt/chart/dist /var/www/html
RUN ln -s /var/www/html/index.html /var/www/html/chart.html
