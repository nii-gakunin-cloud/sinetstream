FROM rockylinux:9.1.20230215

# Install Apache Kafka
ARG KAFKA_VERSION=3.4.0
ENV KAFKA_VERSION $KAFKA_VERSION
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
ARG PYTHON_VERSION=3.10.10
# hadolint ignore=DL3003
RUN dnf -y install libgcc zlib bzip2 readline sqlite openssl libffi libuuid xz expat \
 && dnf -y install gcc make zlib-devel bzip2-devel readline-devel sqlite-devel openssl-devel libffi-devel libuuid-devel xz-devel expat-devel \
 && curl "https://www.python.org/ftp/python/${PYTHON_VERSION}/Python-${PYTHON_VERSION}.tgz" | tar xzf - \
 && ( \
       cd "Python-${PYTHON_VERSION}" \
    && ./configure --enable-loadable-sqlite-extensions --enable-optimizations --enable-option-checking=fatal --enable-shared --with-lto --with-system-expat --without-ensurepip \
    && make -j "$(nproc)" \
    && make altinstall \
    && cd .. \
    && rm -r "Python-${PYTHON_VERSION}" \
    ) \
 && dnf -y remove gcc make zlib-devel bzip2-devel readline-devel sqlite-devel openssl-devel libffi-devel libuuid-devel xz-devel expat-devel \
 && dnf clean all
RUN echo "/usr/local/lib" > /etc/ld.so.conf.d/local.conf \
 && ldconfig \
 && curl https://bootstrap.pypa.io/get-pip.py | "/usr/local/bin/python$(echo ${PYTHON_VERSION} | cut -d . -f 1-2)"

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
 && alternatives --install /usr/bin/python3 python3 "/usr/local/bin/python${suffix}" 10 \
 && alternatives --install /usr/bin/pip3 pip3 "/usr/local/bin/pip${suffix}" 10 \
 && sed -i -e 's%#!/usr/bin/python3$%#!/usr/bin/python3.9%' \
      /usr/bin/supervisord /usr/bin/supervisorctl /usr/bin/dnf

EXPOSE 9092 9093 1883 8883 8080
ENV ENABLE_BROKER=true ENABLE_SSHD=false ENABLE_SSL=true \
  GENERATE_CLIENT_CERTS=true SSL_CLIENT_AUTH=requested

WORKDIR /home/${USERNAME}
