if (interactive()) savehistory();
library("aroma.core");

verbose <- Arguments$getVerbose(-8, timestamp=TRUE);

# - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Simulating copy-number data
# - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Number of loci
J <- 1000;

stateFcn <- function(x, ...) {
  states <- integer(length(x));
  states[200 <= x & x <= 300] <- +1L;
  states[650 <= x & x <= 800] <- -1L;
  states;
}

x <- sort(runif(J, max=J));
y <- rnorm(J, sd=1/2);
w <- runif(J);

cn <- SegmentedCopyNumbers(y, x, w=w, states=stateFcn);
cn$y <- cn$y + 1.0*getStates(cn);
cn$w[getStates(cn) < 0] <- 0.001;
cn$fullname <- "Sample #1";
print(cn);


layout(matrix(1:2, ncol=1))

plot(cn, cex=0.8, col="#999999");
title(main="Weighted smoothing by state (byCount=FALSE)");

cnS <- binnedSmoothingByState(cn, by=10);
print(cnS);
points(cnS, cex=1.3);

plot(cn, cex=0.8, col="#999999");
title(main="Weighted smoothing by state (byCount=TRUE)");

cnS <- binnedSmoothingByState(cn, by=10, byCount=TRUE);
print(cnS);
points(cnS, cex=1.3);
