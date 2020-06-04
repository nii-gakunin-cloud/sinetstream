FROM sinetstream/tutorial:1.0.1

COPY server.properties /srv/kafka/config/
COPY setup-acl.sh setup-sasl.sh /usr/local/bin/
COPY setup-acl.conf setup-sasl.conf /tmp/
RUN cat /tmp/setup-acl.conf /tmp/setup-sasl.conf >> /etc/supervisord.conf && rm /tmp/setup-acl.conf /tmp/setup-sasl.conf

RUN sed -i -e '/program:mosquitto/,/autostart/s/autostart=.*/autostart=false/' /etc/supervisord.conf
