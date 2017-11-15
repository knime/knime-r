library("aroma.affymetrix")

ovars <- ls(all.names=TRUE)
oplan <- future::plan()

## Setup dataset
dataset <- "GSE8605"
chipType <- "Mapping10K_Xba142"

csR <- AffymetrixCelSet$byName(dataset, chipType=chipType)
csR <- csR[1:2]
print(csR)

## Create unique CDF
cdf <- getCdf(csR)
print(cdf)
cdfU <- getUniqueCdf(cdf)
print(cdfU)

checksum <- NULL

strategies <- future:::supportedStrategies()
strategies <- setdiff(strategies, "multiprocess")
if (require("future.BatchJobs")) {
  strategies <- c(strategies, "batchjobs_local")
  if (any(grepl("PBS_", names(Sys.getenv())))) {
    strategies <- c(strategies, "batchjobs_torque")
  }
}
if (require("future.batchtools")) {
  strategies <- c(strategies, "batchtools_local")
  if (any(grepl("PBS_", names(Sys.getenv())))) {
    strategies <- c(strategies, "batchtools_torque")
  } else if (any(grepl("SGE_", names(Sys.getenv())))) {
    strategies <- c(strategies, "batchtools_sge")
  }
}

message("Future strategies: ", paste(sQuote(strategies), collapse = ", "))
mprint(future::sessionDetails())
mprint(list(
  availableCores = future::availableCores(which = "all"),
  availableWorkers = future::availableWorkers(which = "all")
))

for (strategy in strategies) {
  message(sprintf("*** Using %s futures ...", sQuote(strategy)))

  future::plan(strategy)
  tags <- c("*", strategy)

  ## (a) Process a single array
  csU1 <- convertToUnique(csR[1], tags=c(tags, "one-array"), verbose=verbose)
  print(csU1)
  csU1z <- getChecksumFileSet(csU1)
  print(csU1z[[1]])

  ## Compare file checksum to previous runs
  checksumT <- readChecksum(csU1z[[1]])
  if (is.null(checksum)) checksum <- checksumT
  ## FIXME: File checksums does not work for comparison since
  ## the CEL header has a timestamp of the creation time.
##  stopifnot(identical(checksumT, checksum))


  ## (b) Process two arrays
  csU <- convertToUnique(csR, tags=tags, verbose=verbose)
  print(csU)
  csUz <- getChecksumFileSet(csU)
  print(csU[[1]])
  res <- equals(csU1z[[1]], csUz[[1]])
  ## FIXME: File checksums does not work for comparison since
  ## the CEL header has a timestamp of the creation time.
##  if (!res) throw(res)  ## FIXME

  message(sprintf("*** Using %s futures ... DONE", sQuote(strategy)))
}


## CLEANUP
future::plan(oplan)
rm(list=setdiff(ls(all.names=TRUE), ovars))
