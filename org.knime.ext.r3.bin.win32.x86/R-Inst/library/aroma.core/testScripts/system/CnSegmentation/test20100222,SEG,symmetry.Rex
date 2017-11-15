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
x <- sort(sample(J, size=J, replace=FALSE));


cnR <- RawCopyNumbers(y, x, chromosome=9);
cnR$fullname <- "Sample #1";

# Mirrored
cnL <- clone(cnR);
cnL$x <- -cnL$x;

cn <- cnR;
plot(cn, col="#aaaaaa");
title(main="Non-weighted segmentation");

cols <- c(CBS="red", GLAD="blue", HaarSeg="orange");


# - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Segmentation
# - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
segFcnList <- list(
  CBS = segmentByCBS,
  GLAD = segmentByGLAD,
  HaarSeg = segmentByHaarSeg
);

methods <- names(cols);

for (kk in seq_along(methods)) {
  method <- methods[[kk]];
  cat("Method: ", method, "\n", sep="");

  segFcn <- segFcnList[[method]];
  col <- cols[method];

#  set.seed(0xBEEF);  
  fitR <- segFcn(cnR);
  cnrR <- extractCopyNumberRegions(fitR);
  drawLevels(cnrR, col=col);
  
#  set.seed(0xBEEF);  
  fitL <- segFcn(cnL);
  cnrL <- extractCopyNumberRegions(fitL);
  t <- cnrL$start;
  cnrL$start <- -cnrL$stop;
  cnrL$stop <- -t;
  drawLevels(cnrL, col="black");

  legend("topleft", horiz=TRUE, bty="n", lwd=3, col=cols, names(cols));
  
  # Compare
  cat("Right-to-left segmentation:\n");
  dfR <- as.data.frame(cnrR);
  print(dfR);
  cat("Left-to-right segmentation:\n");
  dfL <- as.data.frame(cnrL);
  dfL <- dfL[order(dfL$start),,drop=FALSE];
  print(dfL);
  tryCatch({
    delta <- dfR - dfL;
    cat("Difference:\n");
    print(delta);
    print(colMeans(delta));
  }, error = function(ex) {
    print(ex);
  });
} # for (kk ...)
