#JAVA_HOME=/usr/lib/jvm/java-1.8.0/jre
all::
	#env JAVA_HOME=$(JAVA_HOME) $(MAKE) inst
	$(MAKE) inst
inst:: build
	tar xvf build/distributions/perftool-*tar
build::
	-rm -rf libs
	mkdir libs
	cp ../../*/build/libs/SINETStream*.jar libs
	./gradlew build
clean::
	./gradlew clean

run::
	-./perftool-*/bin/perftool -h

