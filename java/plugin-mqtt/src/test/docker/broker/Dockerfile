FROM sinetstream/tutorial:1.0.1

RUN sed -i -e '/autostart/s/%(ENV_ENABLE_BROKER)s/false/' \
      -e '/program:mosquitto/,/autostart/s/autostart=.*/autostart=true/' \
      /etc/supervisord.conf

COPY mosquitto.conf.append /tmp
RUN cat /tmp/mosquitto.conf.append >> /etc/mosquitto/mosquitto.conf \
 && rm /tmp/mosquitto.conf.append

COPY etc /etc
COPY init.sh /usr/local/bin/
