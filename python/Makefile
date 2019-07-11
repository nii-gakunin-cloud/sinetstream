PYTHON3 = python3.6
SETUP = $(PYTHON3) setup.py
LINT = flake8-3.6

all:: lint

lint::
	#$(LINT) src tests || true
	$(SETUP) flake8 || true

sdist::
	$(SETUP) sdist

bdist::
	$(SETUP) bdist
	cd plugins/kafka && $(MAKE) $@

install::
	$(SETUP) install --user
	cd plugins/kafka && $(MAKE) $@

uninstall::
	$(PYTHON3) -m pip uninstall -y sinetstream
	cd plugins/kafka && $(MAKE) $@

test::
	-$(SETUP) test
	-cd plugins/kafka && $(MAKE) $@

tags::
	uctags -R --exclude=".*" . $(HOME)/.local/lib

doc:: README.html
README.html: README.md
	markdown README.md > README.html
