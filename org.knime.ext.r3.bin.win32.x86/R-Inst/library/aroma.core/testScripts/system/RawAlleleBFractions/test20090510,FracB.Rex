if (interactive()) savehistory();
library("aroma.core");

verbose <- Arguments$getVerbose(-8, timestamp=TRUE);

# - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Simulating allele B fractions
# - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Number of loci
J <- 1000;

# Normal genotypes
muN <- sample(c(0,1/2,1), size=J, replace=TRUE);

epsN <- rnorm(J, sd=0.05);

# Observed allele B fractions
betaN <- muN + epsN;

# Genomic locations
x <- sort(runif(J, max=J));

betaN <- RawAlleleBFractions(betaN, x);
print(betaN);
plot(betaN, col="gray", ylim=c(-0.2,1.2));


# - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Simulating with SNP effects
# - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# SNP effects
deltaN <- rnorm(J, sd=0.2);
jj <- which(muN != 1/2);
deltaN[jj] <- sign(1/2-muN[jj])*rexp(length(jj), rate=30);

deltaN <- RawAlleleBFractions(deltaN, x);

betaN <- addBy(betaN, deltaN);
print(betaN);
points(betaN, col="black");
