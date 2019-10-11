all:
	env JAVA_HOME=/usr/lib/jvm/java-1.8.0/jre ./gradlew assemble
	ls -l */build/libs/*jar
	cd sample/text && $(MAKE) $@
