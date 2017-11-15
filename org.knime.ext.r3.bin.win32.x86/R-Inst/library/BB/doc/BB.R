### R code from vignette source 'BB.Stex'

###################################################
### code chunk number 1: BB.Stex:9-10
###################################################
 options(continue="  ")


###################################################
### code chunk number 2: BB.Stex:24-25
###################################################
library("BB") 


###################################################
### code chunk number 3: BB.Stex:31-32 (eval = FALSE)
###################################################
## help(package=BB)


###################################################
### code chunk number 4: BB.Stex:56-58
###################################################
require("setRNG") 
setRNG(list(kind="Wichmann-Hill", normal.kind="Box-Muller", seed=1236))


###################################################
### code chunk number 5: BB.Stex:69-82
###################################################
expo3 <- function(p) {
#  From La Cruz and Raydan, Optim Methods and Software 2003, 18 (583-599)
n <- length(p)
f <- rep(NA, n)
onm1 <- 1:(n-1) 
f[onm1] <- onm1/10 * (1 - p[onm1]^2 - exp(-p[onm1]^2))
f[n] <- n/10 * (1 - exp(-p[n]^2))
f
}

p0 <- runif(10)
ans <- dfsane(par=p0, fn=expo3)
ans


###################################################
### code chunk number 6: BB.Stex:95-112
###################################################

trigexp <- function(x) {
n <- length(x)
F <- rep(NA, n)
F[1] <- 3*x[1]^2 + 2*x[2] - 5 + sin(x[1] - x[2]) * sin(x[1] + x[2])
tn1 <- 2:(n-1)
F[tn1] <- -x[tn1-1] * exp(x[tn1-1] - x[tn1]) + x[tn1] * ( 4 + 3*x[tn1]^2) +
        2 * x[tn1 + 1] + sin(x[tn1] - x[tn1 + 1]) * sin(x[tn1] + x[tn1 + 1]) - 8 
F[n] <- -x[n-1] * exp(x[n-1] - x[n]) + 4*x[n] - 3
F
}

n <- 10000
p0 <- runif(n)
ans <- dfsane(par=p0, fn=trigexp, control=list(trace=FALSE))
ans$message
ans$resid


###################################################
### code chunk number 7: BB.Stex:118-124
###################################################
froth <- function(p){
f <- rep(NA,length(p))
f[1] <- -13 + p[1] + (p[2]*(5 - p[2]) - 2) * p[2]
f[2] <- -29 + p[1] + (p[2]*(1 + p[2]) - 14) * p[2]
f
}


###################################################
### code chunk number 8: BB.Stex:130-133
###################################################
p0 <- c(3,2) 
dfsane(par=p0, fn=froth, control=list(trace=FALSE))
BBsolve(par=p0, fn=froth)


###################################################
### code chunk number 9: BB.Stex:142-146
###################################################

p0 <- c(1,1)  
BBsolve(par=p0, fn=froth)
dfsane(par=p0, fn=froth, control=list(trace=FALSE))


###################################################
### code chunk number 10: BB.Stex:156-161
###################################################
# two values generated independently from a poisson distribution with mean = 10
p0 <- rpois(2,10) 

BBsolve(par=p0, fn=froth)
dfsane(par=p0, fn=froth, control=list(trace=FALSE))


###################################################
### code chunk number 11: BB.Stex:177-188
###################################################
# Example 
# A high-degree polynomial system (R.B. Kearfoot, ACM 1987)
# There are 12 real roots (and 126 complex roots to this system!)
#
hdp <- function(x) {
  f <- rep(NA, length(x))
  f[1] <- 5 * x[1]^9 - 6 * x[1]^5 * x[2]^2 + x[1] * x[2]^4 + 2 * x[1] * x[3]
  f[2] <- -2 * x[1]^6 * x[2] + 2 * x[1]^2 * x[2]^3 + 2 * x[2] * x[3]
  f[3] <- x[1]^2 + x[2]^2 - 0.265625
  f
  }


###################################################
### code chunk number 12: BB.Stex:195-201
###################################################
setRNG(list(kind="Wichmann-Hill", normal.kind="Box-Muller", seed=123))

p0 <- matrix(runif(300), 100, 3)  # 100 starting values, each of length 3
ans <- multiStart(par=p0, fn=hdp, action="solve")
sum(ans$conv)  # number of successful runs = 190
pmat <- ans$par[ans$conv, ] # selecting only converged solutions


###################################################
### code chunk number 13: BB.Stex:205-207
###################################################
ans <- round(pmat, 4)
ans[!duplicated(ans), ]


###################################################
### code chunk number 14: BB.Stex:212-214
###################################################
pc <- princomp(pmat)
biplot(pc)  # you can see all 12 solutions beautifully like on a clock!


###################################################
### code chunk number 15: BB.Stex:223-234
###################################################
fleishman <- function(x, r1, r2) {
b <- x[1]
c <- x[2]
d <- x[3]
f <- rep(NA, 3)
f[1] <- b^2 + 6 * b * d + 2 * c^2 + 15 * d^2 - 1
f[2] <- 2*c * (b^2 + 24*b*d + 105*d^2 + 2) - r1 
f[3] <- b*d + c^2 * (1 + b^2 + 28 * b * d) + d^2 * (12 + 48 * b* d +
              141 * c^2 + 225 * d^2) - r2/24
f
}


###################################################
### code chunk number 16: BB.Stex:243-254
###################################################
rmat <- matrix(NA, 10, 2)
rmat[1,] <- c(1.75, 3.75)
rmat[2,] <- c(1.25, 2.00)
rmat[3,] <- c(1.00, 1.75)
rmat[4,] <- c(1.00, 0.50)
rmat[5,] <- c(0.75, 0.25)
rmat[6,] <- c(0.50, 3.00)
rmat[7,] <- c(0.50, -0.50)
rmat[8,] <- c(0.25, -1.00)
rmat[9,] <- c(0.0, -0.75)
rmat[10,] <- c(-0.25, 3.75)


###################################################
### code chunk number 17: BB.Stex:260-301
###################################################
# 1
setRNG(list(kind="Mersenne-Twister", normal.kind="Inversion", seed=13579))

ans1 <- matrix(NA, nrow(rmat), 3)
for (i in 1:nrow(rmat)) {
  x0 <- rnorm(3)  # random starting value
  temp <- BBsolve(par=x0, fn=fleishman, r1=rmat[i,1], r2=rmat[i,2])
  if (temp$conv == 0) ans1[i, ] <- temp$par
  }
ans1 <- cbind(rmat, ans1)
colnames(ans1) <- c("skew", "kurtosis", "B", "C", "D")
ans1


# 2
setRNG(list(kind="Mersenne-Twister", normal.kind="Inversion", seed=91357))

ans2 <- matrix(NA, nrow(rmat), 3)
for (i in 1:nrow(rmat)) {
  x0 <- rnorm(3)  # random starting value
  temp <- BBsolve(par=x0, fn=fleishman, r1=rmat[i,1], r2=rmat[i,2])
  if (temp$conv == 0) ans2[i, ] <- temp$par
  }
ans2 <- cbind(rmat, ans2)
colnames(ans2) <- c("skew", "kurtosis", "B", "C", "D")
ans2


# 3
setRNG(list(kind="Mersenne-Twister", normal.kind="Inversion", seed=79135))

ans3 <- matrix(NA, nrow(rmat), 3)
for (i in 1:nrow(rmat)) {
  x0 <- rnorm(3)  # random starting value
  temp <- BBsolve(par=x0, fn=fleishman, r1=rmat[i,1], r2=rmat[i,2])
  if (temp$conv == 0) ans3[i, ] <- temp$par
  }
ans3 <- cbind(rmat, ans3)
colnames(ans3) <- c("skew", "kurtosis", "B", "C", "D")
ans3



###################################################
### code chunk number 18: BB.Stex:329-339
###################################################
poissmix.loglik <- function(p,y) {
# Log-likelihood for a binary Poisson mixture distribution
i <- 0:(length(y)-1)
loglik <- y * log(p[1] * exp(-p[2]) * p[2]^i / exp(lgamma(i+1)) + 
        (1 - p[1]) * exp(-p[3]) * p[3]^i / exp(lgamma(i+1)))
return (sum(loglik) )
}
# Data from Hasselblad (JASA 1969)
poissmix.dat <- data.frame(death=0:9,
          freq=c(162,267,271,185,111,61,27,8,3,1))


###################################################
### code chunk number 19: BB.Stex:345-347
###################################################
lo <- c(0,0,0)  # lower limits for parameters
hi <- c(1, Inf, Inf) # upper limits for parameters


###################################################
### code chunk number 20: BB.Stex:353-362
###################################################
p0 <- runif(3,c(0.2,1,1),c(0.8,5,8))  # a randomly generated vector of length 3
y <- c(162,267,271,185,111,61,27,8,3,1)

ans1 <- spg(par=p0, fn=poissmix.loglik, y=y, 
          lower=lo, upper=hi, control=list(maximize=TRUE, trace=FALSE))
ans1
ans2 <- BBoptim(par=p0, fn=poissmix.loglik, y=y, 
         lower=lo, upper=hi, control=list(maximize=TRUE))
ans2


###################################################
### code chunk number 21: BB.Stex:375-381
###################################################
require(numDeriv)
hess <- hessian(x=ans2$par, func=poissmix.loglik, y=y)  
# Note that we have to supplied data vector `y'
hess
se <- sqrt(diag(solve(-hess)))
se


###################################################
### code chunk number 22: BB.Stex:389-400
###################################################
# 3 randomly generated starting values
p0 <- matrix(runif(30, c(0.2,1,1), c(0.8,8,8)), 10, 3, byrow=TRUE)  
ans <- multiStart(par=p0, fn=poissmix.loglik, action="optimize",
      y=y, lower=lo, upper=hi, control=list(maximize=TRUE))

# selecting only converged solutions
pmat <-  round(cbind(ans$fvalue[ans$conv], ans$par[ans$conv, ]), 4)
dimnames(pmat) <- list(NULL, c("fvalue","parameter 1","parameter 2","parameter 3"))

pmat[!duplicated(pmat), ]



