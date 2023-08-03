all::

lint::
	flake8 src

build::
	python3 -mvenv .
	bin/pip install -U pip setuptools
	bin/pip install build
	bin/python -mbuild

clean::
	-rm -rf dist bin include lib lib64 share pyvenv.cfg
	-rm -rf src/sinetstream_cli.egg-info

run::
	cd src/sinetstream_cli; ./sinetstream_cli.py write --help; ./sinetstream_cli.py read --help
