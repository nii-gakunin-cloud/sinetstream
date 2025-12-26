#!/bin/bash
set -x
: ${CERT_DIR:=/opt/certs}

PIP=pip3.11
PYTHON=python3.11

## XXX START
#export S3_ENDPOINT_URL=http://broker:9000
#export S3_BUCKET=sstest
#export S3_PREFIX=testprefix
#export S3_SUFFIX=.test
#export S3_NAME=minute
#export S3_AWS_ACCESS_KEY_ID=sinetstream-s3-access
#export S3_AWS_SECRET_ACCESS_KEY=sinetstream-s3-secret
## XXX END

setup_certs() {
  return # XXX
  mkdir -p ${CERT_DIR}
  pushd ${CERT_DIR}

  sleep 1
  until curl -s -f ${CERT_URL}cacert.pem > /dev/null; do
    sleep 1
  done

  curl -s -O ${CERT_URL}cacert.pem
  curl -s -O ${CERT_URL}client0.crt
  curl -s -O ${CERT_URL}client0.key
  curl -s -O ${CERT_URL}bad-client.crt
  curl -s -O ${CERT_URL}bad-client.key
  popd
}

setup_bad_hostname() {
  local ssl_broker=(${KAFKA_SSL_BROKER//:/ })
  local ip_addr=$(dig +short ${ssl_broker[0]})
  export KAFKA_SSL_BROKER_BAD_HOSTNAME="${ip_addr}:${ssl_broker[1]}"
}

install_packages() {
  for wheelhouse in wheelhouse*; do
    if [ -d ${wheelhouse} ]; then
      for pkg in ${wheelhouse}/*.whl; do
        ${PIP} install --upgrade --exists-action a ${pkg}
      done
    fi
  done
}

case "${INSTALL_PACKAGES:-NO}" in
[Yy][Ee][Ss])
  install_packages ;;
esac

if [ -n "${CERT_URL}" ]; then
  setup_certs
  if [ -n "${KAFKA_SSL_BROKER}" ]; then
    setup_bad_hostname
  fi
fi

# install_packages

create_bucket() {
    ${PIP} install boto3
    ${PYTHON} - << _END_
import boto3
s3 = boto3.resource(
        service_name="s3",
        endpoint_url="$S3_ENDPOINT_URL",
        aws_access_key_id="$S3_AWS_ACCESS_KEY_ID",
        aws_secret_access_key="$S3_AWS_SECRET_ACCESS_KEY")
bucket = s3.Bucket("$S3_BUCKET")
if bucket.creation_date:
    bucket.objects.all().delete()  # no versioning expected.
else:
    bucket.create()
bucket.put_object(Body=b"DUMMY", Key="testprefix/README")
_END_
}
create_bucket

# setup minio client
setup_mc() {
curl --output /root/mc --remote-name https://dl.min.io/client/mc/release/linux-amd64/mc
chmod a+x /root/mc
mkdir /root/.mc
cat >/root/.mc/config.json <<__END__
{
  "version": "10",
  "aliases": {
    "broker": {
      "url": "$S3_ENDPOINT_URL",
      "accessKey": "$S3_AWS_ACCESS_KEY_ID",
      "secretKey": "$S3_AWS_SECRET_ACCESS_KEY",
      "api": "S3v4",
      "path": "auto"
    }
  }
/root/mc tree --files broker
}
__END__
}
setup_mc

# show dependency tree
${PIP} install pipdeptree
pipdeptree -fl  # XXX this doesn't show the pkgs that will be installed by "setup.py test"

if [ "$#" -eq 0 ]; then
  set -- python setup.py test
  #set -- python setup.py test --addopts '-vv --log-level=debug'
fi

exec "$@"
