FROM sinetstream/tutorial:20250416

RUN yum install -y nmap-ncat \
 && yum clean all

COPY setup-pw.sh /usr/local/bin/
COPY setup-pw.conf /tmp/
RUN cat /tmp/setup-pw.conf >> /etc/supervisord.conf \
  && rm /tmp/setup-pw.conf \
  && mkdir -p /etc/mosquitto/conf.d

COPY conf/*.conf /etc/mosquitto/conf.d/

