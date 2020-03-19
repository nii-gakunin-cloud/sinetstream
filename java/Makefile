all:
	./gradlew assemble
	ls -l */build/libs/*jar
	cd sample/text && $(MAKE) $@

test:
	./gradlew test
