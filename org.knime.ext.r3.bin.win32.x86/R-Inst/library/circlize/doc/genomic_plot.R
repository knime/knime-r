## ----echo = FALSE--------------------------------------------------------
library(knitr)
opts_chunk$set(fig.pos = "", fig.align = "center")

library(circlize)
circos.genomicInitialize = function(...) {
    circos.par(unit.circle.segments = 200)
    circlize::circos.genomicInitialize(...)
}

circos.initializeWithIdeogram = function(...) {
    circos.par(unit.circle.segments = 200)
    circlize::circos.initializeWithIdeogram(...)
}

