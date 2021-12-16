# vim:noexpandtab tabstop=8 shiftwidth=8

PYTHON3 = python3
SETUP = $(PYTHON3) setup.py
LINT = flake8

all:: lint

recurse-plugins:
	find plugins -name Makefile -print | xargs dirname | while read dir; do $(MAKE) -C $$dir $(TARGET); done

lint: lint-local #lint-plugins
lint-local:
	$(SETUP) $(LINT) || true
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

bdist_wheel: bdist_wheel-local bdist_wheel-plugins
bdist_wheel-local:
	$(SETUP) bdist_wheel
bdist_wheel-plugins:
	$(MAKE) recurse-plugins TARGET="bdist_wheel"

install: install-local install-plugins
	$(MAKE) show-usage
install-local:
	$(SETUP) install --user
install-plugins:
	$(MAKE) recurse-plugins TARGET="install"
show-usage:
	$(PYTHON3) -m sinetstream

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
	uctags -R --exclude=".*" --exclude="build" --exclude="*.css" --exclude="*.js" src plugins tests #$(HOME)/.local/lib

doc:: README.html
README.html: README.md
	markdown README.md > README.html

testenv:
	IMAGE=`$(PYTHON3) -c 'with open(".gitlab-ci.yml") as f: import yaml; print(yaml.safe_load(f)["test-api"]["image"]);'`; docker run --interactive --tty --rm --volume $(PWD):$(PWD) --env $(HOME)=$(HOME) --workdir $(PWD) $$IMAGE bash
