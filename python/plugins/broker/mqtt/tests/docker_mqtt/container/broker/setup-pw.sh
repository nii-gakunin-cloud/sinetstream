#!/bin/bash

touch /etc/mosquitto/pwfile
chown mosquitto:mosquitto /etc/mosquitto/pwfile
chmod 0600 /etc/mosquitto/pwfile
mosquitto_passwd -b /etc/mosquitto/pwfile user01 user01
mosquitto_passwd -b /etc/mosquitto/pwfile user02 user02
mosquitto_passwd -b /etc/mosquitto/pwfile user03 user03

cat > /etc/mosquitto/aclfile <<EOF
topic read $SYS/#
topic readwrite mss-test-001

user user01
#Invalid ACL: topic readwrite mss-test-#
topic readwrite mss-test-001
topic readwrite mss-test-002
topic readwrite mss-test-003


user user02
#Invalid ACL: topic read mss-test-#
topic readwrite mss-test-001
topic readwrite mss-test-002
topic readwrite mss-test-003

user user03
#Invalid ACL: topic write mss-test-#
topic readwrite mss-test-001
topic readwrite mss-test-002
topic readwrite mss-test-003
EOF
chown mosquitto:mosquitto /etc/mosquitto/aclfile
chmod 0600 /etc/mosquitto/aclfile
