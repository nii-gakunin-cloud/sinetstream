# vim:noexpandtab tabstop=8 shiftwidth=8

PYTHON3 = python3

all:: lint

recurse-plugins:
	find plugins -name Makefile -print | xargs dirname | while read dir; do $(MAKE) -C $$dir $(TARGET); done

lint: lint-local lint-plugins
lint-local:
	tox -e lint || true
lint-plugins:
	$(MAKE) recurse-plugins TARGET="lint"

lint2: lint2-local lint2-plugins
lint2-local:
	tox -e lint2 || true
lint2-plugins: distribute-pylintrc
	$(MAKE) recurse-plugins TARGET="lint2"
distribute-pylintrc::
	find plugins -name Makefile -print | xargs dirname | while read dir; do f=$$dir/pylintrc; $(MAKE) S=pylintrc T=$$f $$f; done
$T: $S
	-rm -f $@
	(echo "# THIS IS A COPY OF $S"; cat $S) >$@
	chmod a-w $@

sdist: sdist-local sdist-plugins
sdist-local:
	$(PYTHON3) -m build --sdist .
sdist-plugins:
	$(MAKE) recurse-plugins TARGET="sdist"

bdist_wheel: bdist_wheel-local bdist_wheel-plugins
bdist_wheel-local:
	$(PYTHON3) -m build --wheel .
bdist_wheel-plugins:
	$(MAKE) recurse-plugins TARGET="bdist_wheel"

install: install-local install-plugins
	$(MAKE) show-usage
install-local:
	$(PYTHON3) -m pip install .
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

clean:: clean-local clean-plugins
clean-local::
	-rm -rf dist
clean-plugins:
	$(MAKE) recurse-plugins TARGET="clean"

test: test-local #test-plugins
test-local:
	tox
test-plugins:
	$(MAKE) recurse-plugins TARGET="test"

tags::
	uctags -R --exclude=".*" --exclude="build" --exclude="*.css" --exclude="*.js" src plugins tests #$(HOME)/.local/lib

doc:: README.html
README.html: README.md
	markdown README.md > README.html

testenv:
	#IMAGE=`$(PYTHON3) -c 'with open(".gitlab-ci.yml") as f: import yaml; print(yaml.safe_load(f)["test-api"]["image"]);'`; docker run --interactive --tty --rm --volume $(PWD):$(PWD) --env $(HOME)=$(HOME) --workdir $(PWD) $$IMAGE bash
	IMAGE=`$(PYTHON3) -c 'with open(".gitlab-ci.yml") as f: import yaml; print(yaml.safe_load(f)["default"]["image"]);'`; IMAGE=`basename $$IMAGE`; docker run --interactive --tty --rm --volume $(PWD):$(PWD) --env $(HOME)=$(HOME) --workdir $(PWD) $$IMAGE bash

# How to upload to PyPI:
# pip install twine  # if not installed yet
# make clean
# make bdist_wheel
# (cd sample/cli && make clean && make build)
# make twine-upload
twine-upload::
	export TWINE_USERNAME=__token__ && \
	read -p "PyPI API TOKEN: " TWINE_PASSWORD && \
	export TWINE_PASSWORD && \
	twine upload --verbose dist/* && \
	$(MAKE) recurse-plugins TARGET="twine-upload" && \
        (cd sample/cli && twine upload --verbose dist/*)
list-dist::
	ls -l `find . -name .tox -prune -o -name "dist" -type d -print | sed 's|$$|/*|'`
