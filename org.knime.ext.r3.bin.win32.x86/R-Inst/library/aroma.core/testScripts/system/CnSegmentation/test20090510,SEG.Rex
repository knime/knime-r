if (interactive()) savehistory();
library("aroma.core");

verbose <- Arguments$getVerbose(-8, timestamp=TRUE);

# - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Simulating copy-number data
# - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Number of loci
J <- 1000;

y <- rnorm(J, sd=1/2);
y[200:300] <- y[200:300] + 1;
y[650:800] <- y[650:800] - 1;
x <- sort(sample(J, size=J, replace=TRUE));

cn <- RawCopyNumbers(y, x, chromosome=9);
cn$fullname <- "Sample #1";
print(cn);

plot(cn, col="#aaaaaa");
title(main="Non-weighted segmentation");

cols <- c(CBS="red", GLAD="blue", HaarSeg="orange");


# - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Segmentation
# - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
fit <- segmentByCBS(cn);
cnr <- extractCopyNumberRegions(fit);
print(cnr);
drawLevels(cnr, col=cols["CBS"]);

fit <- segmentByGLAD(cn);
cnr <- extractCopyNumberRegions(fit);
print(cnr);
drawLevels(cnr, col=cols["GLAD"]);

fit <- segmentByHaarSeg(cn);
cnr <- extractCopyNumberRegions(fit);
print(cnr);
drawLevels(cnr, col=cols["HaarSeg"]);

legend("topleft", horiz=TRUE, bty="n", lwd=3, col=cols, names(cols));


# - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Tabular listing of segmentation data
# - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
data <- as.data.frame(cnr);
print(data);



# - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Compare memory usage with integer and double positions
# Note, don't use doubles for genomic locations unless
# needed, because it takes up more memory or disk space
# if saved to file.
# - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
cnList <- list(
  "integer" = RawCopyNumbers(y, as.integer(x), chromosome=9),
  "double" = RawCopyNumbers(y, as.double(x), chromosome=9)
)
print(cnList)
print(sapply(cnList, FUN=objectSize));

fitList <- lapply(cnList, FUN=segmentByCBS);
print(sapply(fitList, FUN=objectSize));

fitList <- lapply(cnList, FUN=segmentByGLAD);
print(sapply(fitList, FUN=objectSize));

fitList <- lapply(cnList, FUN=segmentByHaarSeg);
print(sapply(fitList, FUN=objectSize));
