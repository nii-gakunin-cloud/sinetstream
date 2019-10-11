all:
	-rm -rf libs
	mkdir libs
	cp ../../*/build/libs/SINETStream*.jar libs
	./gradlew build
	tar xvf build/distributions/text-producer-*tar
	-cd text-producer-* && ln -s $(HOME)/repo/sinetstream/dot.sinetstream_config.yml .sinetstream_config.yml
