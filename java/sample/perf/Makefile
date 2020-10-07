all:
	-rm -rf libs
	mkdir libs
	cp ../../*/build/libs/SINETStream*.jar libs
	./gradlew build
	tar xvf build/distributions/sinetstream-binary-producer-*tar
	cp libs/*.jar sinetstream-binary-producer-*/lib		# XXX QUICKHACK: "gradlew build" doesn't use libs/*.jar
clean::
	./gradlew clean
	-rm -rf .gradle build libs
	-rm -rf sinetstream-binary-producer-*
