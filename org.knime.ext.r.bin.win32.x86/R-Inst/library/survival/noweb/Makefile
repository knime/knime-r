PARTS = main.Rnw \
	predict.coxph.Rnw \
	residuals.survreg.Rnw

SFUN =  predict.coxph.S \
	residuals.survreg.S

RDIR = ../../R

all.pdf: all.tex
	pdflatex all

all.nw: $(PARTS)
	cat $(PARTS) > all.nw
	echo "\\end{document}" >> all.nw

all.tex: all.nw
	noweave -delay all.nw > all.tex

RFUN = $(SFUN:%=$(RDIR)/%)

fun:  $(RFUN)

$(SFUN) : all.nw

%.S: 
	echo "# Automatically generated from inst/noweb/all.nw" > $@
	notangle -R$(*F) all.nw >> $@

clean:
	-rm all.nw all.log all.aux all.toc all.tex all.pdf
	-rm $(RFUN)