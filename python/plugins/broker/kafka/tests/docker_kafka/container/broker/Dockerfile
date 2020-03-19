FROM sinetstream/tutorial:1.0.1

COPY setup-acl.sh /usr/local/bin/
COPY server.properties /srv/kafka/config/
COPY setup-acl.conf /tmp/
RUN cat /tmp/setup-acl.conf >> /etc/supervisord.conf && rm /tmp/setup-acl.conf
