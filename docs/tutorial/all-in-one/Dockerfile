FROM rockylinux:9.3.20231119

# Install Apache Kafka
ARG KAFKA_VERSION=3.8.1
ENV KAFKA_VERSION=$KAFKA_VERSION
ARG KAFKA_URL=https://downloads.apache.org/kafka/${KAFKA_VERSION}/kafka_2.13-${KAFKA_VERSION}.tgz
SHELL ["/bin/bash", "-o", "pipefail", "-c"]
RUN dnf -y install java-11-openjdk-headless gzip \
 && dnf clean all
RUN curl -s ${KAFKA_URL} | tar xzf - -C /srv \
 && ln -sf /srv/kafka_2.13-${KAFKA_VERSION} /srv/kafka

# Install Eclipse Mosquitto
RUN dnf -y install epel-release \
 && dnf config-manager --disable epel \
 && dnf clean all
RUN dnf -y --enablerepo epel install mosquitto \
 && dnf clean all

# Install Python
ARG PYTHON_VERSION=3.11
RUN dnf -y install python${PYTHON_VERSION} python${PYTHON_VERSION}-pip \
 && dnf clean all
ARG PYTHON_BINDIR=/usr/bin
# note: if install Python manually, see the old commit.
# ARG PYTHON_BINDIR=/usr/local/bin

# Install openssh
RUN dnf -y install openssh-server openssh-clients \
 && dnf clean all

# intall tools
RUN dnf -y install sudo vim iproute procps-ng ca-certificates \
 && dnf clean all

# Install Supervisor
RUN dnf -y --enablerepo epel install supervisor \
 && dnf clean all
COPY supervisord.conf /etc/supervisord.conf

# Add user
ARG USERNAME=user01
RUN useradd -m $USERNAME \
 && echo "${USERNAME}:${USERNAME}" | chpasswd \
 && echo "${USERNAME} ALL=(ALL) ALL" > /etc/sudoers.d/90-${USERNAME} \
 && chmod 400 /etc/sudoers.d/90-${USERNAME}

COPY init.sh /usr/local/bin/
CMD ["/usr/local/bin/init.sh"]

RUN suffix="$(echo ${PYTHON_VERSION} | cut -d . -f 1-2)" \
 && alternatives --install /usr/bin/python3 python3 "${PYTHON_BINDIR}/python${suffix}" 10 \
 && alternatives --install /usr/bin/pip3 pip3 "${PYTHON_BINDIR}/pip${suffix}" 10 \
 && sed -i.bak -e '/^#!.*python3\./n;/^#!.*python3/s|python3|python3.9|' \
      /usr/bin/supervisord /usr/bin/supervisorctl /usr/bin/dnf

EXPOSE 9092 9093 1883 8883 8080
ENV ENABLE_BROKER=true ENABLE_SSHD=false ENABLE_SSL=true \
  GENERATE_CLIENT_CERTS=true SSL_CLIENT_AUTH=requested

WORKDIR /home/${USERNAME}
