JAVA_HOME=/usr/lib/jvm/java-1.8.0/jre
all::
	env JAVA_HOME=$(JAVA_HOME) $(MAKE) inst
inst:: build
	tar xvf build/distributions/text-producer-*tar
build::
	-rm -rf libs
	mkdir libs
	cp ../../*/build/libs/SINETStream*.jar libs
	./gradlew build
clean::
	./gradlew clean

run::
	-./text-producer-*/bin/text-producer -h
	-./text-producer-*/bin/text-consumer -h
