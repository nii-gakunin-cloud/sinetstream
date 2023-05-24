all::
	$(MAKE) inst
inst:: build
	-rm -rf sinetstream_cli
	tar xvf build/distributions/sinetstream_cli-*tar
build::
	-rm -rf libs
	mkdir libs
	cp ../../*/build/libs/SINETStream*.jar libs
	./gradlew build
clean::
	./gradlew clean

run::
	$(MAKE) run1 cli=`readlink -f ./sinetstream_cli-*/bin/sinetstream_cli`
run1::
	-$(cli) write --help
	-$(cli) read --help
