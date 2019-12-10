JAVA_HOME=/usr/lib/jvm/java-1.8.0/jre
all::
	env JAVA_HOME=$(JAVA_HOME) $(MAKE) inst
inst:: build
	tar xvf build/distributions/text-producer-*tar
	-cd text-producer-* && ln -s $(HOME)/repo/sinetstream/dot.sinetstream_config.yml .sinetstream_config.yml
build::
	-rm -rf libs
	mkdir libs
	cp ../../*/build/libs/SINETStream*.jar libs
	./gradlew build
