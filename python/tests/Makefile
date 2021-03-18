all:

test:
	cd .. && python3 setup.py test -v --addopts "--log-level=DEBUG tests/test_interop.py"

clean:
	-rm -f data/*/*.in
	-rm -f data/*/*.out
