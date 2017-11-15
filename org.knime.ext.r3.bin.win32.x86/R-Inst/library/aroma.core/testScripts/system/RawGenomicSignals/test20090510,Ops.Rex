if (interactive()) savehistory();
library("aroma.core");

verbose <- Arguments$getVerbose(-8, timestamp=TRUE);

# - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Simulating copy-number data
# - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Number of loci
J <- 1000;

mu <- double(J);
mu[200:300] <- mu[200:300] + 1;
mu[650:800] <- mu[650:800] - 1;
eps <- rnorm(J, sd=1/2);
y <- mu + eps;
x <- sort(runif(length(y), max=length(y)));
w <- runif(J);
w[650:800] <- 0.001;

cnT <- RawCopyNumbers(mu, x, w=w);
print(cnT);

cn <- RawCopyNumbers(y, x, w=w);
print(cn);

plot(cn, col="#aaaaaa");
points(cnT, col="red");

cnD <- subtractBy(cn, cnT);
print(cnD);
points(cnD, col="blue");

