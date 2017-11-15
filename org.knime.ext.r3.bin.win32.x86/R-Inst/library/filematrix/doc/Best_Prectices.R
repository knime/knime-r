## ----setup, echo=FALSE---------------------------------------------------
  library(knitr)
  opts_knit$set(root.dir=tempdir())

## ----message=FALSE-------------------------------------------------------
library(filematrix)
fm = fm.create(filenamebase = tempfile(), nrow = 5000, ncol = 10000, type = "double")

## ------------------------------------------------------------------------
step1 = 512
runto = ncol(fm)
nsteps = ceiling(runto/step1)
for( part in seq_len(nsteps) ) { # part = 1
 	fr = (part-1)*step1 + 1
 	to = min(part*step1, runto)
	cat( "Filling in columns", fr, "to", to, "\n")
 	fm[,fr:to] = runif(nrow(fm) * (to-fr+1))
}
rm(part, step1, runto, nsteps, fr, to)

## ------------------------------------------------------------------------
fmcolsums = double(ncol(fm))

step1 = 512
runto = ncol(fm)
nsteps = ceiling(runto/step1)
for( part in seq_len(nsteps) ) { # part = 1
 	fr = (part-1)*step1 + 1
 	to = min(part*step1, runto)
	cat( "Calculating column sums, processing columns", fr, "to", to, "\n")
 	fmcolsums[fr:to] = colSums(fm[,fr:to])
}
rm(part, step1, runto, nsteps, fr, to)

cat("Sums of first and last columns are", fmcolsums[1], "and", tail(fmcolsums,1), "\n")

## ------------------------------------------------------------------------
fmrowsums = double(nrow(fm))

step1 = 512
runto = ncol(fm)
nsteps = ceiling(runto/step1)
for( part in seq_len(nsteps) ) { # part = 1
 	fr = (part-1)*step1 + 1
 	to = min(part*step1, runto)
	cat( "Calculating row sums, processing columns", fr, "to", to, "\n")
 	fmrowsums = fmrowsums + rowSums(fm[,fr:to])
}
rm(part, step1, runto, nsteps, fr, to)

cat("Sums of first and last rows are", fmrowsums[1], "and", tail(fmrowsums,1), "\n")

## ------------------------------------------------------------------------
closeAndDeleteFiles(fm)

