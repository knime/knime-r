### R code from vignette source 'BBvignetteJSS.Stex'

###################################################
### code chunk number 1: BBvignetteJSS.Stex:84-85
###################################################
options(prompt="R> ", continue="+  ")


###################################################
### code chunk number 2: BBvignetteJSS.Stex:144-145 (eval = FALSE)
###################################################
## vignette("BB", package = "BB")


###################################################
### code chunk number 3: BBvignetteJSS.Stex:149-150 (eval = FALSE)
###################################################
## vignette("BBvignetteJSS", package = "BB")


###################################################
### code chunk number 4: BBvignetteJSS.Stex:156-158
###################################################
nsim  <- 10 # 1000  
nboot <- 50 # 500


###################################################
### code chunk number 5: BBvignetteJSS.Stex:381-393
###################################################
require("BB")  
froth <- function(p){
  r <- rep(NA, length(p))
  r[1] <- -13 + p[1] + (p[2] * (5 - p[2]) - 2)  * p[2]
  r[2] <- -29 + p[1] + (p[2] * (1 + p[2]) - 14) * p[2]
  r
  }

p0 <- rep(0, 2)  
dfsane(par = p0, fn = froth, control = list(trace = FALSE)) 
sane(par = p0, fn = froth, control = list(trace = FALSE))
BBsolve(par = p0, fn = froth)


###################################################
### code chunk number 6: BBvignetteJSS.Stex:434-438
###################################################
require("setRNG")
test.rng <- list(kind = "Mersenne-Twister", normal.kind = "Inversion", 
		    seed = 1234)
old.seed <- setRNG(test.rng)


###################################################
### code chunk number 7: BBvignetteJSS.Stex:509-856
###################################################
expo3 <- function(p) {
   n <- length(p)
   r <- rep(NA, n)
   onm1 <- 1:(n-1) 
   r[onm1] <- onm1/10 * (1 - p[onm1]^2 - exp(-p[onm1]^2))
   r[n] <- (n/10) * (1 - exp(-p[n]^2))
   r
}

dfsane1.expo3 <- dfsane2.expo3 <- sane1.expo3 <- sane2.expo3 <-  bbs.expo3  <- 
   bbs.expo3 <- matrix(NA, nsim, 5, 
             dimnames = list(NULL, c("value", "feval", "iter", "conv", "cpu")))

old.seed <- setRNG(test.rng)

cat("Simulation test 1: ")
for (i in 1:nsim) {
cat(i, " ")
p0 <- rnorm(500)
t1 <- system.time(ans <- 
    sane(par = p0, fn = expo3, method = 1, control = list(trace = FALSE)))[1]
sane1.expo3[i, ] <- c(ans$residual, ans$feval, ans$iter, ans$convergence, t1)

t2 <- system.time(ans <- 
    sane(par = p0, fn = expo3, method = 2, control = list(trace = FALSE)))[1]
sane2.expo3[i, ] <- c(ans$residual, ans$feval, ans$iter, ans$convergence, t2)

t3 <- system.time(ans <-
    dfsane(par = p0, fn = expo3, method = 1, control = list(trace = FALSE)))[1]
dfsane1.expo3[i, ] <- c(ans$residual, ans$feval, ans$iter, ans$convergence, t3)

t4 <- system.time(ans <-
    dfsane(par = p0, fn = expo3, method = 2, control = list( trace = FALSE)))[1]
dfsane2.expo3[i, ] <- c(ans$residual, ans$feval, ans$iter, ans$convergence, t4)

t5 <- system.time(ans <- 
    BBsolve(par = p0, fn = expo3,  control = list(trace = FALSE)))[1]
bbs.expo3[i, ] <- c(ans$residual, ans$feval, ans$iter, ans$convergence, t5)
}
cat("\n")

table1.test1 <- rbind(
  c(apply(  sane1.expo3, 2, summary)[c(4, 2,5), c(3, 2, 5)], sum(  sane1.expo3[,4] > 0)),
  c(apply(dfsane1.expo3, 2, summary)[c(4, 2,5), c(3, 2, 5)], sum(dfsane1.expo3[,4] > 0)),
  c(apply(  sane2.expo3, 2, summary)[c(4, 2,5), c(3, 2, 5)], sum(  sane2.expo3[,4] > 0)),
  c(apply(dfsane2.expo3, 2, summary)[c(4, 2,5), c(3, 2, 5)], sum(dfsane2.expo3[,4] > 0)),
  c(apply(    bbs.expo3, 2, summary)[c(4, 2,5), c(3, 2, 5)], sum(    bbs.expo3[,4] > 0))
 )

dimnames(table1.test1) <- list(
    c("sane-1", "dfsane-1", "sane-2", "dfsane-2", "BBsolve"), NULL)

table1.test1


###################### test function 2 ######################

trigexp <- function(x) {
   n <- length(x)
   r <- rep(NA, n)
   r[1] <- 3*x[1]^2 + 2*x[2] - 5 + sin(x[1] - x[2]) * sin(x[1] + x[2])
   tn1 <- 2:(n-1)
   r[tn1] <- -x[tn1-1] * exp(x[tn1-1] - x[tn1]) + x[tn1] * ( 4 + 3*x[tn1]^2) +
        2 * x[tn1 + 1] + sin(x[tn1] - x[tn1 + 1]) * sin(x[tn1] + x[tn1 + 1]) - 8 
   r[n] <- -x[n-1] * exp(x[n-1] - x[n]) + 4*x[n] - 3
   r
}


old.seed <- setRNG(test.rng)

dfsane1.trigexp <- dfsane2.trigexp <- sane1.trigexp <- sane2.trigexp <- 
   matrix(NA, nsim, 5, 
      dimnames=list(NULL,c("value", "feval", "iter", "conv", "cpu")))

cat("Simulation  test 2: ")
for (i in 1:nsim) {
cat(i, " ")
p0 <- rnorm(500)
t1 <- system.time(ans <- 
   sane(par=p0, fn=trigexp, method=1, control=list( trace=FALSE)))[1]
sane1.trigexp[i, ] <- c(ans$residual, ans$feval, ans$iter, ans$convergence, t1)

t2 <- system.time(ans <- 
   sane(par=p0, fn=trigexp, method=2, control=list(   trace=FALSE)))[1]
sane2.trigexp[i, ] <- c(ans$residual, ans$feval, ans$iter, ans$convergence, t2)

t3 <- system.time(ans <- 
   dfsane(par=p0, fn=trigexp, method=1, control=list(   trace=FALSE)))[1]
dfsane1.trigexp[i, ] <- c(ans$residual, ans$feval, ans$iter, ans$convergence, t3)

t4 <- system.time(ans <- 
   dfsane(par=p0, fn=trigexp, method=2, control=list(   trace=FALSE)))[1]
dfsane2.trigexp[i, ] <- c(ans$residual, ans$feval, ans$iter, ans$convergence, t4)
}
cat("\n")

table1.test2 <- rbind(
  c(apply(  sane1.trigexp, 2, summary)[c(4, 2,5), c(3, 2, 5)], sum(  sane1.trigexp[,4] > 0)),
  c(apply(dfsane1.trigexp, 2, summary)[c(4, 2,5), c(3, 2, 5)], sum(dfsane1.trigexp[,4] > 0)),
  c(apply(  sane2.trigexp, 2, summary)[c(4, 2,5), c(3, 2, 5)], sum(  sane2.trigexp[,4] > 0)),
  c(apply(dfsane2.trigexp, 2, summary)[c(4, 2,5), c(3, 2, 5)], sum(dfsane2.trigexp[,4] > 0))
 )

dimnames(table1.test2) <- list(
    c("sane-1", "dfsane-1", "sane-2", "dfsane-2"), NULL)

table1.test2

###################### test function 3 ######################

broydt <- function(x, h=2) {
   n <- length(x)
   r <- rep(NA, n)
   r[1] <- ((3 - h * x[1]) * x[1]) - 2 * x[2] + 1
   tnm1 <- 2:(n-1)
   r[tnm1] <- ((3 - h * x[tnm1]) * x[tnm1]) - x[tnm1-1] - 2 * x[tnm1+1] + 1
   r[n] <- ((3 - h * x[n]) * x[n]) - x[n-1] + 1
   r
   }


old.seed <- setRNG(test.rng)

dfsane1.broydt <- dfsane2.broydt <- sane1.broydt <- sane2.broydt <-
     matrix(NA, nsim, 5, 
       dimnames=list(NULL,c("value", "feval", "iter", "conv", "cpu")))

cat("Simulation  test 3: ")
for (i in 1:nsim) {
cat(i, " ")
p0 <- -runif(500)
t1 <- system.time(ans <- 
   sane(par=p0, fn=broydt, method=1, control=list(trace=FALSE)))[1]
sane1.broydt[i, ] <- c(ans$residual, ans$feval, ans$iter, ans$convergence, t1)

t2 <- system.time(ans <- 
   sane(par=p0, fn=broydt, method=2, control=list(trace=FALSE)))[1]
sane2.broydt[i, ] <- c(ans$residual, ans$feval, ans$iter, ans$convergence, t2)

t3 <- system.time(ans <- 
   dfsane(par=p0, fn=broydt, method=1, control=list(trace=FALSE)))[1]
dfsane1.broydt[i, ] <- c(ans$residual, ans$feval, ans$iter, ans$convergence, t3)

t4 <- system.time(ans <- 
   dfsane(par=p0, fn=broydt, method=2, control=list(trace=FALSE)))[1]
dfsane2.broydt[i, ] <- c(ans$residual, ans$feval, ans$iter, ans$convergence, t4)
}
cat("\n")

table1.test3 <- rbind(
  c(apply(  sane1.broydt, 2, summary)[c(4, 2,5), c(3, 2, 5)], sum(  sane1.broydt[,4] > 0)),
  c(apply(dfsane1.broydt, 2, summary)[c(4, 2,5), c(3, 2, 5)], sum(dfsane1.broydt[,4] > 0)),
  c(apply(  sane2.broydt, 2, summary)[c(4, 2,5), c(3, 2, 5)], sum(  sane2.broydt[,4] > 0)),
  c(apply(dfsane2.broydt, 2, summary)[c(4, 2,5), c(3, 2, 5)], sum(dfsane2.broydt[,4] > 0))
 )

dimnames(table1.test3) <- list(
    c("sane-1", "dfsane-1", "sane-2", "dfsane-2"), NULL)

table1.test3

###################### test function 4 ######################

extrosbk <- function(x) {
   n <- length(x)
   r <- rep(NA, n)
   j <- 2 * (1:(n/2))
   jm1 <- j - 1
   r[jm1] <- 10 * (x[j] - x[jm1]^2)
   r[j] <-  1 - x[jm1]
   r
}

old.seed <- setRNG(test.rng)

dfsane1.extrosbk <- dfsane2.extrosbk <- sane1.extrosbk <- sane2.extrosbk <- 
  bbs.extrosbk <- matrix(NA, nsim, 5, 
      dimnames = list(NULL,c("value", "feval", "iter", "conv", "cpu")))

cat("Simulation  test 4: ")
for (i in 1:nsim) {
cat(i, " ")
p0 <- runif(500)
t1 <- system.time(ans <- 
   sane(par = p0, fn = extrosbk, method = 1, control = list( M = 10, noimp = 100, trace = FALSE)))[1]
sane1.extrosbk[i, ] <- c(ans$residual, ans$feval, ans$iter, ans$convergence, t1)

t2 <- system.time(ans <- 
   sane(par = p0, fn = extrosbk, method = 2, control = list( M = 10, noimp = 100,  trace = FALSE)))[1]
sane2.extrosbk[i, ] <- c(ans$residual, ans$feval, ans$iter, ans$convergence, t2)

t3 <- system.time(ans <- 
   dfsane(par = p0, fn = extrosbk, method = 1, control = list( M = 10, noimp = 100,  trace = FALSE)))[1]
dfsane1.extrosbk[i, ] <- c(ans$residual, ans$feval, ans$iter, ans$convergence, t3)

t4 <- system.time(ans <- 
   dfsane(par = p0, fn = extrosbk, method = 2, control = list( M = 10, noimp = 100, trace = FALSE)))[1]
dfsane2.extrosbk[i, ] <- c(ans$residual, ans$feval, ans$iter, ans$convergence, t4)

t5 <- system.time(ans <- 
   BBsolve(par = p0, fn = extrosbk, control = list(trace = FALSE)))[1]
bbs.extrosbk[i, ] <- c(ans$residual, ans$feval, ans$iter, ans$convergence, t5)
}
cat("\n")

table1.test4 <- rbind(
  c(apply(  sane1.extrosbk, 2, summary)[c(4, 2,5), c(3, 2, 5)], sum(  sane1.extrosbk[,4] > 0)),
  c(apply(dfsane1.extrosbk, 2, summary)[c(4, 2,5), c(3, 2, 5)], sum(dfsane1.extrosbk[,4] > 0)),
  c(apply(  sane2.extrosbk, 2, summary)[c(4, 2,5), c(3, 2, 5)], sum(  sane2.extrosbk[,4] > 0)),
  c(apply(dfsane2.extrosbk, 2, summary)[c(4, 2,5), c(3, 2, 5)], sum(dfsane2.extrosbk[,4] > 0)),
  c(apply(    bbs.extrosbk, 2, summary)[c(4, 2,5), c(3, 2, 5)], sum(    bbs.extrosbk[,4] > 0))
 )

dimnames(table1.test4) <- list(
    c("sane-1", "dfsane-1", "sane-2", "dfsane-2", "BBsolve"), NULL)

table1.test4


###################### test function 5 ######################

troesch <- function(x) {
  n <- length(x)
  tnm1 <- 2:(n-1)
  r <- rep(NA, n)
    h <- 1 / (n+1)
    h2 <- 10 * h^2
    r[1] <- 2 * x[1] + h2 * sinh(10 * x[1]) - x[2] 
    r[tnm1] <- 2 * x[tnm1] + h2 * sinh(10 * x[tnm1]) - x[tnm1-1] - x[tnm1+1]    

    r[n] <- 2 * x[n] + h2 * sinh(10 * x[n]) - x[n-1] - 1
  r
  }
  


old.seed <- setRNG(test.rng)

dfsane1.troesch <- dfsane2.troesch <- sane1.troesch <- sane2.troesch <- 
    matrix(NA, nsim, 5,
       dimnames = list(NULL,c("value", "feval", "iter", "conv", "cpu")))

cat("Simulation  test 5: ")
for (i in 1:nsim) {
cat(i, " ")
p0 <- sort(runif(500))

t1 <- system.time(ans <- 
   sane(par = p0, fn = troesch, method = 1, control = list(trace = FALSE)))[1]
   sane1.troesch[i, ] <- c(ans$residual, ans$feval, ans$iter, ans$convergence, t1)

t2 <- system.time(ans <- 
   sane(par = p0, fn = troesch, method = 2, control = list(trace = FALSE)))[1]
   sane2.troesch[i, ] <- c(ans$residual, ans$feval, ans$iter, ans$convergence, t2)

t3 <- system.time(ans <- 
   dfsane(par = p0, fn = troesch, method = 1, control = list(trace = FALSE)))[1]
   dfsane1.troesch[i, ] <- c(ans$residual, ans$feval, ans$iter, ans$convergence, t3)

t4 <- system.time(ans <- 
   dfsane(par = p0, fn = troesch, method = 2, control = list(trace = FALSE)))[1]
   dfsane2.troesch[i, ] <- c(ans$residual, ans$feval, ans$iter, ans$convergence, t4)
}
cat("\n")

table1.test5 <- rbind(
  c(apply(  sane1.troesch, 2, summary)[c(4, 2,5), c(3, 2, 5)], sum(  sane1.troesch[,4] > 0)),
  c(apply(dfsane1.troesch, 2, summary)[c(4, 2,5), c(3, 2, 5)], sum(dfsane1.troesch[,4] > 0)),
  c(apply(  sane2.troesch, 2, summary)[c(4, 2,5), c(3, 2, 5)], sum(  sane2.troesch[,4] > 0)),
  c(apply(dfsane2.troesch, 2, summary)[c(4, 2,5), c(3, 2, 5)], sum(dfsane2.troesch[,4] > 0))
 )

dimnames(table1.test5) <- list(
    c("sane-1", "dfsane-1", "sane-2", "dfsane-2"), NULL)

table1.test5

###################### test function 6 ######################

chandraH <- function(x, c=0.9) {
   n <- length(x)
   k <- 1:n
   mu <- (k - 0.5)/n
   dterm <- outer(mu, mu, function(x1,x2) x1 / (x1 + x2) )
   x - 1 / (1 - c/(2*n) * rowSums(t(t(dterm) * x)))
   } 


old.seed <- setRNG(test.rng)

dfsane1.chandraH <- dfsane2.chandraH <- sane1.chandraH <- sane2.chandraH <-  
    matrix(NA, nsim, 5, 
       dimnames = list(NULL,c("value", "feval", "iter", "conv", "cpu")))

cat("Simulation  test 6: ")
for (i in 1:nsim) {
   cat(i, " ")
   p0 <- runif(500)
   t1 <- system.time(ans <-
      sane(par = p0, fn = chandraH, method = 1, control = list(trace = FALSE)))[1]
   sane1.chandraH[i, ] <- c(ans$residual, ans$feval, ans$iter, ans$convergence, t1)

   t2 <- system.time(ans <- 
      sane(par = p0, fn = chandraH, method = 2, control = list(trace = FALSE)))[1]
   sane2.chandraH[i, ] <- c(ans$residual, ans$feval, ans$iter, ans$convergence, t1)

   t3 <- system.time(ans <- 
      dfsane(par = p0, fn = chandraH, method = 1, control = list(trace = FALSE)))[1]
   dfsane1.chandraH[i, ] <- c(ans$residual, ans$feval, ans$iter, ans$convergence, t1)

   t4 <- system.time(ans <- 
      dfsane(par = p0, fn = chandraH, method = 2, control = list(trace = FALSE)))[1]
   dfsane2.chandraH[i, ] <- c(ans$residual, ans$feval, ans$iter, ans$convergence, t1)
   }
cat("\nSimulations for table 1 complete.\n")

table1.test6 <- rbind(
  c(apply(  sane1.chandraH, 2, summary)[c(4, 2, 5), c(3, 2, 5)], sum(  sane1.chandraH[,4] > 0)),
  c(apply(dfsane1.chandraH, 2, summary)[c(4, 2, 5), c(3, 2, 5)], sum(dfsane1.chandraH[,4] > 0)),
  c(apply(  sane2.chandraH, 2, summary)[c(4, 2, 5), c(3, 2, 5)], sum(  sane2.chandraH[,4] > 0)),
  c(apply(dfsane2.chandraH, 2, summary)[c(4, 2, 5), c(3, 2, 5)], sum(dfsane2.chandraH[,4] > 0))
 )

dimnames(table1.test6) <- list(
    c("sane-1", "dfsane-1", "sane-2", "dfsane-2"), NULL)


table1.caption <- paste("Results of numerical experiments for 6 standard test problems.",
   nsim, "randomly generated starting values were used for each problem. Means 
   and inter-quartile ranges (in parentheses) are shown.  Default control 
   parameters were used in all the algorithms.")

table1 <- rbind(table1.test1, table1.test2, table1.test3, table1.test4, 
                table1.test5, table1.test6) 

#dimnames(table1) <- list(dimnames(table1.test1)[[1]], 
#   c("", "# Iters", "", "", "# Fevals", "", "", "CPU (sec)", "", "# Failures"))

cgroups <- c("# Iters", "# Fevals", "CPU (sec)","# Failures")

rgroups <- c("\\emph{1. Exponential function 3}", 
             "\\emph{2. Trigexp function}",
             "\\emph{3. Broyden's tridiagonal function}",
             "\\emph{4. Extended Rosenbrock function}", 
	     "\\emph{5. Troesch function}",
	     "\\emph{6. Chandrasekhar's H-equation}")


###################################################
### code chunk number 8: BBvignetteJSS.Stex:860-874
###################################################
require("Hmisc")

latex(table1,  
       file="",
       caption=table1.caption, caption.loc='bottom',
       #align  = "cccccccccc",
       #colheads="Methods & \\# Iters  & \\# Fevals & CPU (sec) & \\# Failures  \\\\",
       cgroups = cgroups, n.cgroups= c(3,3,3,1),
       rgroups = rgroups, n.rgroups= c(5,4,4,5,4,4),      
       dec=3,
       label="table:stdexpmtsGENERATED",
       landscape=FALSE, size="small", 
       numeric.dollar=TRUE)
 


###################################################
### code chunk number 9: BBvignetteJSS.Stex:916-926
###################################################
hdp <- function(x) {
  r <- rep(NA, length(x))
  r[1] <- 5*x[1]^9 - 6*x[1]^5 * x[2]^2 + x[1] * x[2]^4 + 2*x[1] * x[3]
  r[2] <- -2 * x[1]^6 * x[2] + 2 * x[1]^2 * x[2]^3 + 2 * x[2] * x[3]
  r[3] <- x[1]^2 + x[2]^2 - 0.265625
  r
  }

old.seed <- setRNG(test.rng)
p0 <- matrix(runif(900), 300, 3)


###################################################
### code chunk number 10: BBvignetteJSS.Stex:929-930
###################################################
ans <- multiStart(par = p0, fn = hdp, action = "solve")


###################################################
### code chunk number 11: BBvignetteJSS.Stex:933-938
###################################################
sum(ans$conv)  
pmat <- ans$par[ans$conv, ] 
ord1 <- order(pmat[, 1])
ans <- round(pmat[ord1, ], 4)
ans[!duplicated(ans), ] 


###################################################
### code chunk number 12: BBvignetteJSS.Stex:992-1026
###################################################
U.eqn <- function(beta) {
      Xb <- c(X %*% beta)
      c(crossprod(X,  Y - (obs.period * exp(Xb))))
      }

poisson.sim <- function(beta, X, obs.period) {
      Xb <- c(X %*% beta)
      mean <- exp(Xb) * obs.period
      rpois(nrow(X), lambda = mean)
      }

old.seed <- setRNG(test.rng)

n <- 500
X <- matrix(NA, n, 8)
X[,1] <- rep(1, n)
X[,3] <- rbinom(n, 1, prob=0.5)
X[,5] <- rbinom(n, 1, prob=0.4)
X[,7] <- rbinom(n, 1, prob=0.4)
X[,8] <- rbinom(n, 1, prob=0.2)
X[,2] <- rexp(n, rate = 1/10)
X[,4] <- rexp(n, rate = 1/10)
X[,6] <- rnorm(n, mean = 10, sd = 2)

obs.period <- rnorm(n, mean = 100, sd = 30)
beta <- c(-5, 0.04, 0.3,  0.05, 0.3, -0.005, 0.1, -0.4)
Y <- poisson.sim(beta, X, obs.period)

res <- dfsane(par = rep(0,8), fn = U.eqn, 
     control = list(NM = TRUE, M = 100, trace = FALSE))
res

glm(Y ~ X[,-1], offset = log(obs.period), 
         family = poisson(link = "log"))  


###################################################
### code chunk number 13: BBvignetteJSS.Stex:1086-1129
###################################################
aft.eqn <- function (beta, X, Y, delta, weights = "logrank") {
      deltaF <- delta == 1
      Y.zeta <- Y - c(X %*% beta)
      ind <- order(Y.zeta, decreasing = TRUE) 
      dd <- deltaF[ind]
      n <- length(Y.zeta)
      tmp <- apply(X[ind, ], 2, function (x) cumsum(x))
  
      if (weights == "logrank") {
         c1 <- colSums(X[deltaF, ])
         r <- (c1 - colSums(tmp[dd, ] / (1:n)[dd])) / sqrt(n)
         }
  
      if (weights == "gehan") {
         c1 <- colSums(X[deltaF, ]* ((1:n)[order(ind)][deltaF]))
         r <- (c1 - colSums(tmp[dd, ])) / ( n * sqrt(n))
         }
  r
  }

old.seed <- setRNG(test.rng)
n <- 1000
X <- matrix(NA, n, 8)
X[,1] <- rbinom(n, 1, prob=0.5)
X[,2] <- rbinom(n, 1, prob=0.4)
X[,3] <- rbinom(n, 1, prob=0.4)
X[,4] <- rbinom(n, 1, prob=0.3)
temp <- as.factor(sample(c("0", "1", "2"), size=n, rep=T, 
                     prob=c(1/3,1/3,1/3)))
X[,5] <- temp == "1"
X[,6] <- temp == "2"
X[,7] <- rexp(n, rate=1/10)
X[,8] <- rnorm(n)

eta.true <- c(0.5, -0.4, 0.3, -0.2, -0.1, 0.4, 0.1, -0.6)
Xb <- drop(X %*% eta.true)

old.seed <- setRNG(test.rng)

par.lr <- par.gh <- matrix(NA, nsim, 8)
stats.lr <- stats.gh <- matrix(NA, nsim, 5)
sumDelta <- rep(NA, nsim)
t1 <- t2 <-0


###################################################
### code chunk number 14: BBvignetteJSS.Stex:1159-1188
###################################################
cat("Simulation for Table 2: ")
for (i in 1:nsim) {
   cat( i, " ")
   err <- rlnorm(n, mean=1)
   Y.orig <- Xb + err 
   cutoff <- floor(quantile(Y.orig, prob=0.5))
   cens <- runif(n, cutoff, quantile(Y.orig, prob=0.95))
   Y <- pmin(cens, Y.orig)
   delta <- 1 * (Y.orig <= cens)
   sumDelta[i] <- sum(delta)

   t1 <- t1 + system.time(ans.eta <- 
      dfsane(par=rep(0,8), fn=aft.eqn,
          control = list(NM = TRUE,  trace = FALSE), 
	  X=X, Y=Y, delta = delta, weights = "logrank"))[1]
   par.lr[i,] <- ans.eta$par
   stats.lr[i, ] <- c(ans.eta$iter, ans.eta$feval, as.numeric(t1), 
                            ans.eta$conv, ans.eta$resid)

   t2 <- t2 + system.time(ans.eta <- 
      dfsane(par=rep(0,8), fn=aft.eqn, 
         control = list(NM = TRUE,  trace = FALSE), 
	 X=X, Y=Y, delta = delta, weights="gehan"))[1]
   par.gh[i,] <- ans.eta$par
   stats.gh[i, ] <- c(ans.eta$iter, ans.eta$feval, as.numeric(t2), 
                            ans.eta$conv, ans.eta$resid)
   invisible({gc(); gc()})
   }
cat("\n")


###################################################
### code chunk number 15: BBvignetteJSS.Stex:1191-1209
###################################################
print(t1/nsim)
print(t2/nsim)
print(mean(sumDelta))


mean.lr <- signif(colMeans(par.lr),3)
bias.lr <- mean.lr - eta.true

sd.lr <- signif(apply(par.lr, 2, sd),3)

mean.gh <- signif(colMeans(par.gh),3)
bias.gh <- mean.gh - eta.true

sd.gh <- signif(apply(par.gh, 2, sd),3)

signif(colMeans(stats.lr),3)

signif(colMeans(stats.gh),3)


###################################################
### code chunk number 16: BBvignetteJSS.Stex:1212-1231
###################################################
table2 <- cbind( eta.true, mean.lr, bias.lr, sd.lr, mean.gh, bias.gh, sd.gh)
dimnames(table2) <- list( c("$X_1$", "$X_2$", "$X_3$", "$X_4$", 
     "$X_5$", "$X_6$", "$X_7$", "$X_8$"), NULL) 

table2.caption <- paste("Simulation results for the rank-based regression 
in accelerated failure time model (", nsim, "simulations). Estimates were obtained using 
the \\code{dfsane} algorithm with \\code{M=100}.")

latex(table2, 
         caption=table2.caption, caption.loc='bottom', 
         file="",
         colheads=c("", "Log-rank", "Gehan"),
         label="table:aftGENERATED",
         landscape=FALSE, size="small", 
	 dec=3, numeric.dollar=TRUE, 
         extracolheads=c( #"Parameter", 
            "Truth", "Mean", "Bias", "Std. Dev.",
                     "Mean", "Bias", "Std. Dev."),
	 double.slash=FALSE)


###################################################
### code chunk number 17: BBvignetteJSS.Stex:1278-1280
###################################################
require("survival") 
attach(pbc)


###################################################
### code chunk number 18: BBvignetteJSS.Stex:1283-1313
###################################################
Y <- log(time)
delta <- status == 2
X <- cbind(age,  log(albumin), log(bili), edema, log(protime))
missing <- apply(X, 1, function(x) any(is.na(x)))
Y <- Y[!missing]
X <- X[!missing, ]
delta <- delta[!missing]

####### Log-rank estimator ####### 
t1 <- system.time(ans.lr <- 
         dfsane(par=rep(0, ncol(X)), fn = aft.eqn, 
	      control=list(NM = TRUE, M = 100, noimp = 500, trace = FALSE),
	      X=X, Y=Y, delta=delta))[1]

# With maxit=5000 this fails with "Lack of improvement in objective function"
#  not with "Maximum limit for iterations exceeded"

t1

ans.lr

####### Gehan estimator ####### 
t2 <- system.time(ans.gh <- 
       dfsane(par = rep(0, ncol(X)), fn = aft.eqn, 
       control = list(NM = TRUE, M = 100, noimp = 500, trace = FALSE),
       X=X, Y=Y, delta=delta, weights = "gehan"))[1]

t2

ans.gh


###################################################
### code chunk number 19: BBvignetteJSS.Stex:1321-1334 (eval = FALSE)
###################################################
## # This source defines functions l1fit and aft.fun
## source("http://www.columbia.edu/~zj7/aftsp.R")
## # N.B. aft.fun resets the RNG seed by default to a fixed value,
## #    and does not reset it. Beware. 
## 
## 
## require("quantreg")
## t3 <- system.time(ans.jin <- 
##         aft.fun(x=X, y=Y, delta=delta, mcsize=1))[1]
## 
## t3
## 
## ans.jin$beta


###################################################
### code chunk number 20: BBvignetteJSS.Stex:1337-1344
###################################################
#  without Jin's results
U <- function(x, func, ...)  sqrt(mean(func(x, ...)^2))
# result from Jin et al. (2003) gives higher residuals
table3.ResidualNorm <- c(
   U(ans.gh$par,       func=aft.eqn, X=X, Y=Y, delta=delta,
       weights="gehan"),
   U(ans.lr$par,       func=aft.eqn, X=X, Y=Y, delta=delta))


###################################################
### code chunk number 21: BBvignetteJSS.Stex:1348-1359 (eval = FALSE)
###################################################
## #  with Jin's results
## U <- function(x, func, ...)  sqrt(mean(func(x, ...)^2))
## # result from Jin et al. (2003) gives higher residuals
## table3.ResidualNorm <- c(
##    U(ans.gh$par,       func=aft.eqn, X=X, Y=Y, delta=delta,
##        weights="gehan"),
##    U(ans.jin$beta[1,], func=aft.eqn, X=X, Y=Y, delta=delta,
##        weights="gehan"),
##    U(ans.lr$par,       func=aft.eqn, X=X, Y=Y, delta=delta),
##    U(ans.jin$beta[2,], func=aft.eqn, X=X, Y=Y, delta=delta))
##    


###################################################
### code chunk number 22: BBvignetteJSS.Stex:1363-1376
###################################################
# Bootstrap to obtain standard errors

Y <- log(time)
delta <- status==2
X <- cbind(age,  log(albumin), log(bili), edema, log(protime))
missing <- apply(X, 1, function(x) any(is.na(x)))
Y.orig <- Y[!missing]
X.orig <- X[!missing, ]
delta.orig <- delta[!missing]

old.seed <- setRNG(test.rng)
lr.boot <- gh.boot <- matrix(NA, nboot, ncol(X))
time1 <- time2 <- 0


###################################################
### code chunk number 23: BBvignetteJSS.Stex:1379-1398
###################################################
cat("Bootstrap sample: ")
for (i in 1:nboot) {
   cat(i, " ")
   select <- sample(1:nrow(X.orig), size=nrow(X.orig), rep=TRUE)
   Y <- Y.orig[select]
   X <- X.orig[select, ]
   delta <- delta.orig[select]
   time1 <- time1 + system.time(ans.lr <- 
         dfsane(par = rep(0, ncol(X)), fn = aft.eqn, 
           control = list(NM = TRUE, M = 100, noimp = 500, trace = FALSE),
	   X=X, Y=Y, delta=delta))[1]
   time2 <- time2 + system.time(ans.gh <- 
         dfsane(par = rep(0, ncol(X)), fn = aft.eqn, 
	   control = list(NM = TRUE, M = 100, noimp = 500, trace = FALSE),
	   X=X, Y=Y, delta=delta, weights = "gehan"))[1]
   lr.boot[i,] <- ans.lr$par
   gh.boot[i,] <- ans.gh$par
   }
cat("\n")


###################################################
### code chunk number 24: BBvignetteJSS.Stex:1401-1449 (eval = FALSE)
###################################################
## time3 <- system.time( ans.jin.boot <-
##       aft.fun(x = X.orig, y = Y.orig, delta = delta.orig,
##          mcsize = nboot))[1]
## 
## time1
## 
## time2
## 
## time3
## 
## colMeans(lr.boot)
## # Results on different systems and versions of R:
## # [1] -0.02744423  1.09871350 -0.59597720 -0.84169498 -0.95067376
## # [1] -0.02718006  1.01484050 -0.60553894 -0.83216296 -0.82671339
## # [1] -0.02746916  1.09371431 -0.59630955 -0.84170621 -0.94147407
## 
## sd(lr.boot) * (499/500)
## # Results on different systems and versions of R:
## # [1] 0.005778319 0.497075716 0.064839483 0.306026261 0.690452468
## # [1] 0.006005054 0.579962922 0.068367668 0.307980986 0.665742686
## # [1] 0.005777676 0.504362828 0.064742446 0.309687062 0.695128194
## 
## colMeans(gh.boot)
## # Results on different systems and versions of R:
## # [1] -0.0263899  1.4477801 -0.5756074 -0.9990443 -2.0961280
## # [1] -0.02616728  1.41126364 -0.58311902 -1.00953045 -2.01724976
## # [1] -0.02633854  1.45577255 -0.57439183 -0.99630007 -2.12363711
## 
## sd(gh.boot) * (499/500)
## # Results on different systems and versions of R:
## # [1] 0.006248941 0.519016144 0.068759981 0.294145730 0.919565487
## # [1] 0.005599693 0.571631837 0.075018323 0.304463597 1.043196254
## # [1] 0.006183826 0.518332233 0.068672881 0.291036025 0.917733660
## 
## 
## ans.jin.boot$beta
## 
## sqrt(diag(ans.jin.boot$betacov[,,2]))  # log-rank
## # Results on different systems and versions of R:
## # [1] 0.005304614 0.470080732 0.053191766 0.224331718 0.545344403
## # [1] 0.00517431 0.44904332 0.05632078 0.24613883 0.54826652
## # [1] 0.00517431 0.44904332 0.05632078 0.24613883 0.54826652
## 
## sqrt(diag(ans.jin.boot$betacov[,,1]))  # Gehan
## # Results on different systems and versions of R:
## # [1] 0.005553049 0.522259799 0.061634483 0.270337048 0.803683570
## # [1] 0.005659013 0.522871858 0.062670939 0.283731999 0.775959845
## # [1] 0.005659013 0.522871858 0.062670939 0.283731999 0.775959845


###################################################
### code chunk number 25: BBvignetteJSS.Stex:1454-1495
###################################################

table3.caption <- paste("Rank-based regression of the accelerated failure time (AFT) model 
for the primary biliary cirrhosis (PBC) data set.  Point estimates and 
standard errors (in parentheses) are provided. Standard errors 
for \\code{dfsane} are obtained from", nboot, "bootstrap samples.")

table3.part1 <- cbind(
      colMeans(gh.boot), sd(gh.boot) * (499/500),
      colMeans(lr.boot), sd(lr.boot) * (499/500)
      )

dimnames(table3.part1) <- list(
     c("age", "log(albumin)", "log(bili)", "edema", "log(protime)"), NULL) 

latex(table3.part1,  
       file="",
       #align  = "c|cc||cc",
       #halign = "c|cc||cc",
       #colheads=c("", "", "Gehan","", "Log-rank"),
       #extracolheads=c("Covariate", 
       #          "\\code{dfsane}", "",
       #          "\\code{dfsane}", ""),
       dec=3,
       label="table:pbcGENERATEDp1",
       landscape=FALSE, size="small", 
       numeric.dollar=TRUE)

table3.ResidualNorm <- matrix(table3.ResidualNorm, 1,2)
dimnames(table3.ResidualNorm) <- list(
   "Residual norm $\\frac{\\|F(x_n)\\|}{\\sqrt{p}}$" , NULL)

latex(table3.ResidualNorm,  
       file="",
       caption=table3.caption, caption.loc='bottom',
       align  = "c|c||c",
       dec=3,
       label="table:pbcGENERATEDp2",
       landscape=FALSE, size="small", 
       numeric.dollar=TRUE)




###################################################
### code chunk number 26: BBvignetteJSS.Stex:1498-1544 (eval = FALSE)
###################################################
## # This version of the table requires Jin's code results
## 
## table3.caption <- paste("Rank-based regression of the accelerated failure time (AFT) model 
## for the primary biliary cirrhosis (PBC) data set.  Point estimates and 
## standard errors (in parentheses) are provided. Standard errors 
## for \\code{dfsane} are obtained from", nboot, "bootstrap samples.")
## 
## table3.part1 <- cbind(
##       colMeans(gh.boot), sd(gh.boot) * (499/500),
##          ans.jin.boot$beta[1,], # Gehan
##          sqrt(diag(ans.jin.boot$betacov[,,1])),  # Gehan
##       colMeans(lr.boot), sd(lr.boot) * (499/500),
##          ans.jin.boot$beta[2,], # log-rank
##          sqrt(diag(ans.jin.boot$betacov[,,2]))  # log-rank
##       )
## 
## dimnames(table3.part1) <- list(
##      c("age", "log(albumin)", "log(bili)", "edema", "log(protime)"), NULL) 
## 
## latex(table3.part1,  
##        file="",
##        align  = "c|cccc||cccc",
##        halign = "c|cccc||cccc",
##        colheads=c("", "", "Gehan","", "", "","Log-rank", "", ""),
##        extracolheads=c("Covariate", 
##                  "\\code{dfsane}", "", "\\citet{JinLinWeiYin03}", "",
##                  "\\code{dfsane}", "", "\\citet{JinLinWeiYin03}", ""),
##        dec=3,
##        label="table:pbcGENERATEDp1",
##        landscape=FALSE, size="small", 
##        numeric.dollar=TRUE)
## 
## table3.ResidualNorm <- matrix(table3.ResidualNorm, 1,4)
## dimnames(table3.ResidualNorm) <- list(
##    "Residual norm $\\frac{\\|F(x_n)\\|}{\\sqrt{p}}$" , NULL)
## 
## latex(table3.ResidualNorm,  
##        file="",
##        caption=table3.caption, caption.loc='bottom',
##        align  = "c|cc||cc",
##        dec=3,
##        label="table:pbcGENERATEDp2",
##        landscape=FALSE, size="small", 
##        numeric.dollar=TRUE)
## 
## 


