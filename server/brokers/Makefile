files_ipynb = *.ipynb *.md
files_md = *.md
files = $(files_ipynb) $(files_md)
baksfx = .BAK

all:: anonymize correct

anonymize::
	for F in $(files); do $(MAKE) $@.1 FILE="$$F"; done
anonymize.1::
	sed -i$(baksfx) \
	-e 's/koie/piyo/g' \
	-e 's/vm00.dp1.nii.ac.jp/server1.example.jp/g' \
	"$(FILE)"

correct::
	for F in $(files_md); do $(MAKE) $@.1 FILE="$$F"; done

correct.1::
	sed -i$(baksfx) \
	-e 's/\.ipynb)/\.md)/g' \
	"$(FILE)"

revert::
	git restore $(files)

clean::
	-rm -f *$(baksfx)
