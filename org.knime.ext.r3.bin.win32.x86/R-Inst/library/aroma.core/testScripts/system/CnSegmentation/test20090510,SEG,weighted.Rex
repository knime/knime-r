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
x <- sort(runif(length(y), max=length(y)));
w <- runif(J);
w[650:800] <- 0.001;

cn <- RawCopyNumbers(y, x, w=w);
cn$fullname <- "Sample #1";
print(cn);

plot(cn, col="#aaaaaa");
title(main="Weighted segmentation (if supported)");

cnS <- binnedSmoothing(cn, by=10);
print(cnS);
lines(cnS, col="black", lwd=3);

cols <- c(CBS="red", GLAD="blue", HaarSeg="orange");

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
