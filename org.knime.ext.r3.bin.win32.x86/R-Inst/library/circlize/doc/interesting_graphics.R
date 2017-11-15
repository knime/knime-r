## ----echo = FALSE--------------------------------------------------------
library(knitr)
opts_chunk$set(fig.pos = "", fig.align = "center")

library(circlize)
circos.initialize = function(...) {
    circos.par(unit.circle.segments = 300)
    circlize::circos.initialize(...)
}

