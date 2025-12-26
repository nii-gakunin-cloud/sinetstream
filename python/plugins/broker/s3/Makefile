venvdir = ../../../venv
include ../../Makefile.comm


# https://github.com/minio/minio#test-using-minio-client-mc
MINIODATA = $(PWD)/tests/docker_s3/miniodata
NAME = sinetstream-minio-server
ls:: ./mc
	$(MAKE) run-minio
	./mc ls -r local
	$(MAKE) kill-minio
run-minio::
	docker run --name "$(NAME)" --detach --rm -p 9000:9000 -p 9001:9001 -v $(MINIODATA):/data quay.io/minio/minio server /data --console-address ":9001"
kill-minio::
	docker rm -f "$(NAME)"

test:
	cd tests/docker_s3 && docker compose build && docker compose up

./mc:
	wget https://dl.min.io/client/mc/release/linux-amd64/mc
