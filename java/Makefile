all:
	$(MAKE) build
	$(MAKE) all-sample

build::
	./gradlew assemble
	ls -l */build/libs/*jar

all-sample::
	cd sample/text && $(MAKE) all
	cd sample/perf && $(MAKE) all
	cd sample/cli && $(MAKE) all

clean::
	find * -name build -print | grep -v docker/build | xargs rm -rf
	cd sample/text && $(MAKE) $@
	cd sample/perf && $(MAKE) $@

test::
	./gradlew test

tags::
	uctags --recurse --exclude="build" *

shark::
	env LANG=C sudo termshark -i any -f "port 1883"
