all:

test-interop:
	cd `git rev-parse --show-toplevel` && ./gradlew SINETStream-api:assemble
	cd `git rev-parse --show-toplevel` && ./gradlew SINETStream-api:test --info --rerun-tasks --tests jp.ad.sinet.stream.api.InteropTest

clean:
	-rm -f data/*/*.in
	-rm -f data/*/*.out
