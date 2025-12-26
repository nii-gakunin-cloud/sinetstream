FROM sinetstream/tutorial:20250416

RUN yum install -y nmap-ncat \
 && yum clean all

COPY setup-acl.sh /usr/local/bin/
#COPY server.properties /srv/kafka/config/
COPY server.properties.kraft /srv/kafka/config/kraft/server.properties
COPY setup-acl.conf /tmp/
RUN cat /tmp/setup-acl.conf >> /etc/supervisord.conf && rm /tmp/setup-acl.conf
