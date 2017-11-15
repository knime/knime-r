## ----echo = FALSE--------------------------------------------------------
library(knitr)
opts_chunk$set(fig.pos = "")

library(circlize)
chordDiagram = function(...) {
    circos.par(unit.circle.segments = 200)
    circlize::chordDiagram(...)
}

