topdir = $(PWD)/../..
repo_python = $(topdir)/../sinetstream-python
repo_java = $(topdir)/../sinetstream-java
test_python = $(repo_python)/tests
test_java = $(repo_java)/api/src/test/java/jp/ad/sinet/stream/interop
test_dummy = ./dummy
test_android = $(test_dummy)

langs = python java android

all: $(test_dummy)
	time $(MAKE) test 2>&1 | tee log

test:
	time $(MAKE) clean-for-langs
	time $(MAKE) test-for-langs 2>&1 | tee log1
	time $(MAKE) gather-for-langs
	time $(MAKE) scatter-for-langs
	time $(MAKE) test-for-langs 2>&1 | tee log2
	time $(MAKE) stat

clean-for-langs:
	$(MAKE) for-langs TARGET=clean

test-for-langs:
	$(MAKE) for-langs TARGET=test-interop

gather-for-langs:
	mkdir -p data
	for L in $(langs); do $(MAKE) gather L=$$L; done
gather:
	test $(test_$L) = $(test_dummy) || rsync -av $(test_$L)/data/$L/ data/$L

scatter-for-langs:
	for L in $(langs); do $(MAKE) scatter L=$$L; done
scatter:
	mkdir -p $(test_$L)/data
	rsync -av data/ $(test_$L)/data

for-langs:
	for L in $(langs); do $(MAKE) for-lang L=$$L; done
for-lang: dot.sinetstream_config.yml
	cp dot.sinetstream_config.yml $(test_$L)/data/dot.sinetstream_config.yml
	cd $(test_$L) && $(MAKE) $(TARGET)

dot.sinetstream_config.yml: gen-sinetstream_config_yml.sh
	./gen-sinetstream_config_yml.sh >$@

stat:
	./stat.sh log2

docker:
	docker build --tag sinetstream-dtree dtree
	docker run --rm --interactive --tty --workdir=/dtree --volume $(PWD)/dtree:/dtree sinetstream-dtree bash

dot: dtree/dtree.dot
	cat dtree/dtree.dot
	@echo ""
dtree/dtree.dot: dtree/result.csv
	docker build --tag sinetstream-dtree dtree
	docker run --rm --workdir=/dtree --volume $(PWD)/dtree:/dtree sinetstream-dtree python3 dtree.py
dtree/result.csv: result.txt
	dtree/result.sh <result.txt >dtree/result.csv
