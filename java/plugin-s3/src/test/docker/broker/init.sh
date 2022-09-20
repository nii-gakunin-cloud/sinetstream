#!/bin/sh

echo "XXX starting init.sh (sinetstream-python/plugins/broker/s3/tests/docker_s3/container/broker/init.sh)"
export MINIO_ROOT_USER=sinetstream-s3-access
export MINIO_ROOT_PASSWORD=sinetstream-s3-secret
/usr/bin/docker-entrypoint.sh minio server /data --console-address ":9001"
