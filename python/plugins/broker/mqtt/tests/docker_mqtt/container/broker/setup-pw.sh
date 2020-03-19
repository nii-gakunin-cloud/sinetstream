#!/bin/bash

touch /etc/mosquitto/pwfile
mosquitto_passwd -b /etc/mosquitto/pwfile user01 user01
mosquitto_passwd -b /etc/mosquitto/pwfile user02 user02
mosquitto_passwd -b /etc/mosquitto/pwfile user03 user03

cat > /etc/mosquitto/aclfile <<EOF
topic read $SYS/#
topic readwrite mss-test-001

user user01
topic readwrite mss-test-#


user user02
topic read mss-test-#

user user03
topic write mss-test-#
EOF
