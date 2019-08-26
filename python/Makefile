# vim:noexpandtab tabstop=8 shiftwidth=8

PYTHON3 = python3.6
SETUP = $(PYTHON3) setup.py
LINT = flake8-3.6

all:: lint

recurse-plugins:
	for dir in plugins/*; do $(MAKE) -C $$dir $(TARGET); done

lint: lint-local #lint-plugins
lint-local:
	#$(LINT) src tests || true
	$(SETUP) flake8 || true
lint-plugins:
	$(MAKE) recurse-plugins TARGET="lint"

sdist: sdist-local sdist-plugins
sdist-local:
	$(SETUP) sdist
sdist-plugins:
	$(MAKE) recurse-plugins TARGET="sdist"

bdist: bdist-local bdist-plugins
bdist-local:
	$(SETUP) bdist
bdist-plugins:
	$(MAKE) recurse-plugins TARGET="bdist"

install: install-local install-plugins
	$(MAKE) show-usage
install-local:
	$(SETUP) install --user
install-plugins:
	$(MAKE) recurse-plugins TARGET="install"
show-usage:
	$(PYTHON3) etc/usage.py

uninstall: uninstall-local uninstall-plugins
uninstall-local:
	$(PYTHON3) -m pip uninstall -y sinetstream
uninstall-plugins:
	$(MAKE) recurse-plugins TARGET="uninstall"

reinstall:
	$(MAKE) uninstall
	$(MAKE) install

test: test-local #test-plugins
test-local:
	-$(SETUP) test
test-plugins:
	$(MAKE) recurse-plugins TARGET="test"

tags::
	uctags -R --exclude=".*" . $(HOME)/.local/lib

doc:: README.html
README.html: README.md
	markdown README.md > README.html