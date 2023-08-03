FROM python:3.8-slim

RUN apt-get update \
  && apt-get install -y curl dnsutils

COPY entrypoint.sh /usr/local/bin/

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]

WORKDIR /opt/ss_test
