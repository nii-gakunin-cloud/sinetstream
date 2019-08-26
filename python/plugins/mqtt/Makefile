# vim:noexpandtab tabstop=8 shiftwidth=8

PYTHON3 = python3.6
SETUP = $(PYTHON3) setup.py
LINT = flake8-3.6

all: lint

lint:
	#$(LINT) src tests || true
	$(SETUP) flake8 || true

sdist:
	$(SETUP) sdist

bdist:
	$(SETUP) bdist

install:
	$(SETUP) install --user

uninstall:
	$(PYTHON3) -m pip uninstall -y sinetstream-mqtt

reinstall:
	$(MAKE) uninstall
	$(MAKE) install

test:
	-$(SETUP) test

tags::
	uctags -R --exclude=".*" . ../../src $(HOME)/.local/lib
