FROM 31z4/tox

USER root

RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      curl dnsutils netcat-openbsd \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/*

RUN curl -f -o /usr/local/bin/wait-for-it.sh https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh \
 && chmod +x /usr/local/bin/wait-for-it.sh

COPY entrypoint.sh /usr/local/bin/

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]