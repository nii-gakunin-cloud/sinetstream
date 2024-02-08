FROM 31z4/tox

USER root

RUN apt-get update \
  && apt-get install -y curl dnsutils

COPY entrypoint.sh /usr/local/bin/

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]

WORKDIR /opt/ss_test
