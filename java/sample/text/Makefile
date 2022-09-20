#JAVA_HOME=/usr/lib/jvm/java-1.8.0/jre
all::
	#env JAVA_HOME=$(JAVA_HOME) $(MAKE) inst
	$(MAKE) inst
inst:: build
	-rm -rf text-producer-*
	tar xvf build/distributions/text-producer-*tar
build::
	-rm -rf libs
	mkdir libs
	cp ../../*/build/libs/SINETStream*.jar libs
	./gradlew build
clean::
	./gradlew clean

run::
	$(MAKE) run1 producer=`readlink -f ./text-producer-*/bin/text-producer` consumer=`readlink -f ./text-producer-*/bin/text-consumer`
run1::
	-$(producer) -s service-1
	-$(consumer) -s service-1
	#-$(producer) -h
	#-$(consumer) -h

./mc:
	wget -O "$@" https://dl.min.io/client/mc/release/linux-amd64/mc
	chmod +x "$@"

NAME = sinetstream-minio-server
mb:: ./mb
	./mc mb local/hoge

ls:: ./mc
	./mc ls -r local
run-minio::
	docker run --name "$(NAME)" --detach --rm -p 9000:9000 -p 9001:9001 quay.io/minio/minio server /data --console-address ":9001"
kill-minio::
	docker rm -f "$(NAME)"
