#all:

test:
	cd .. && $(MAKE) install-local
	#cd .. && python3 setup.py test -v --addopts "--log-level=DEBUG tests/test_interop.py"
	#cd .. && python3 setup.py test -v --addopts "tests/test_msgv3.py"
	#cd .. && env QUICKTEST=yes python3 setup.py test -v
	#cd .. && python3 setup.py test -v --addopts "tests/test_crypto.py::test_thru"
	tox -- --log-level=info

test-interop:
	cd .. && $(MAKE) install-local
	#cd .. && python3 setup.py test -v --addopts "--log-level=INFO tests/test_interop.py"
	ENV1=`tox list | grep 'tests' | head -1 | cut -d' ' -f1`; tox -v -e $$ENV1 -- --capture=no --log-level=info tests/test_interop.py

clean:
	-rm -f data/*/*.in
	-rm -f data/*/*.out
