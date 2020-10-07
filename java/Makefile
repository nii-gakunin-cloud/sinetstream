all:
	./gradlew assemble
	ls -l */build/libs/*jar
	cd sample/text && $(MAKE) $@
	cd sample/perf && $(MAKE) $@

clean::
	find * -name build -print | grep -v docker/build | xargs rm -rf
	cd sample/text && $(MAKE) $@
	cd sample/perf && $(MAKE) $@

test:
	./gradlew test

tags::
	uctags --recurse --exclude="build" *
