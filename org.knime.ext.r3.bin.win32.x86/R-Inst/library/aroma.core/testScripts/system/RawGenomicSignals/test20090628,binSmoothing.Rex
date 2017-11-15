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

cnS <- binnedSmoothing(cn, byCount=TRUE, by=10);
print(cnS);
lines(cnS, col="red", lwd=3);
