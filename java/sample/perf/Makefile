all:
	-rm -rf libs
	mkdir libs
	cp ../../*/build/libs/SINETStream*.jar libs
	./gradlew build
	tar xvf build/distributions/sinetstream-binary-producer-*tar
	-cd sinetstream-binary-producer-* && test -L .sinetstream_config.yml || ln -s $(HOME)/repo/sinetstream/python/dot.sinetstream_config.yml .sinetstream_config.yml
clean::
	./gradlew clean
