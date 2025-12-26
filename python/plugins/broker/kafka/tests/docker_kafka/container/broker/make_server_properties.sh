#!/bin/sh
set -eu
#set -x
IMAGE_NAME="$(grep "^FROM " Dockerfile | cut -d" " -f2)"
(
docker run --rm "$IMAGE_NAME" /bin/sh -c "cat /srv/kafka/config/kraft/server.properties" |
sed '/^#/d;/^$/d' |
awk -F'=' '
BEGIN { OFS="="}
$1=="controller.quorum.voters" {
    gsub(/:9093/, ":9099", $2)
    print; next
}
$1=="listeners" {
    gsub(/:9093/, ":9099")
    $2 = $2 ",SSL://:9093,SASL_PLAINTEXT://:9094"
    print; next
}
$1=="advertised.listeners" {
    gsub(/localhost/, "broker", $2)
    $2 = $2 ",SSL://broker:9093,SASL_PLAINTEXT://broker:9094"
    print; next
}
{ print }
'

cat <<'__END__'
ssl.truststore.location=/srv/kafka/config/certs/ca.p12
ssl.truststore.password=ca-pass
ssl.truststore.type=pkcs12
ssl.keystore.location=/srv/kafka/config/certs/broker.p12
ssl.keystore.password=broker-pass
ssl.keystore.type=pkcs12
ssl.client.auth=requested
sasl.enabled.mechanisms=PLAIN
listener.name.sasl_plaintext.plain.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required \
  user_user01="user01" \
  user_user02="user02" \
  user_user03="user03";
authorizer.class.name=org.apache.kafka.metadata.authorizer.StandardAuthorizer
allow.everyone.if.no.acl.found=true
__END__
) >server.properties.kraft.new
