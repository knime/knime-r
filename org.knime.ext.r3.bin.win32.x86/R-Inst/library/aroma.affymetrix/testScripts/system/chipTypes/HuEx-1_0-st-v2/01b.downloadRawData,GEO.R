path <- system.file("testScripts/R", package="aroma.affymetrix")
pathname <- file.path(path, "downloadUtils.R")
source(pathname)

verbose && enter(verbose, "Downloading raw data")


##########################################################################
# Data set:
# GSE34184,testset/
#   HuEx-1_0-st-v2/
#     GSM843555.CEL
#     GSM843556.CEL
#     GSM605957.CEL
#
#
# URL: http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSE34184
##########################################################################
dataSet <- "GSE34184"
tags <- "testset"
chipType <- "HuEx-1_0-st-v2"
sampleNamesMap <- c(
  GSM843555="GSM843555_JC-110",
  GSM843556="GSM843556_JC-99",
  GSM843557="GSM843557_C59"
)

verbose && cat(verbose, "Data set: ", dataSet)

urls <- sprintf("ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM843nnn/%s/suppl/%s.CEL.gz", names(sampleNamesMap), sampleNamesMap)

dfList <- lapply(urls, FUN = function(url) {
  downloadGeoRawDataFile(dataSet, tags=tags, chipType=chipType, url = url)
})
ds <- AffymetrixCelSet$byName(dataSet, tags=tags, chipType=chipType)
print(ds)
stopifnot(length(ds) == length(sampleNamesMap))
## AffymetrixCelSet:
## Name: GSE34184
## Tags: testset
## Path: rawData/GSE34184,testset/HuEx-1_0-st-v2
## Platform: Affymetrix
## Chip type: HuEx-1_0-st-v2
## Number of arrays: 3
## Names: GSM843555_JC-110, GSM843556_JC-99, GSM843557_C59 [3]
## Time period: 2009-07-31 14:22:16 -- 2009-08-04 20:36:08
## Total file size: 188.61 MiB

verbose && exit(verbose)
