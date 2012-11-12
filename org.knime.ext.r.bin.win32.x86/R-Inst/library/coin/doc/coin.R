### R code from vignette source 'coin.Rnw'

###################################################
### code chunk number 1: setup
###################################################
options(width = 60)
require("coin")
require("Biobase")
require("multcomp")
set.seed(290875)


###################################################
### code chunk number 2: YOY-kruskal
###################################################
library("coin")
YOY <- data.frame(
    length = c(46, 28, 46, 37, 32, 41, 42, 45, 38, 44, 
               42, 60, 32, 42, 45, 58, 27, 51, 42, 52, 
               38, 33, 26, 25, 28, 28, 26, 27, 27, 27, 
               31, 30, 27, 29, 30, 25, 25, 24, 27, 30),
    site = factor(c(rep("I", 10), rep("II", 10),
                    rep("III", 10), rep("IV", 10))))

it <- independence_test(length ~ site, data = YOY,
    ytrafo = function(data) trafo(data, numeric_trafo = rank),
    teststat = "quadtype")
it


###################################################
### code chunk number 3: YOY-T
###################################################
statistic(it, "linear")


###################################################
### code chunk number 4: YOY-EV
###################################################
expectation(it)
covariance(it)


###################################################
### code chunk number 5: YOY-S
###################################################
statistic(it, "standardized")


###################################################
### code chunk number 6: YOY-c
###################################################
statistic(it)


###################################################
### code chunk number 7: YOY-p
###################################################
pvalue(it)


###################################################
### code chunk number 8: YOY-KW
###################################################
kw <- kruskal_test(length ~ site, data = YOY, 
                   distribution = approximate(B = 9999))
kw


###################################################
### code chunk number 9: YOY-KWp
###################################################
pvalue(kw)


###################################################
### code chunk number 10: jobsatisfaction-cmh
###################################################
data("jobsatisfaction", package = "coin")

it <- cmh_test(jobsatisfaction)
it


###################################################
### code chunk number 11: jobsatisfaction-s
###################################################
statistic(it, "standardized")


###################################################
### code chunk number 12: jobsatisfaction-lbl
###################################################
lbl_test(jobsatisfaction)


###################################################
### code chunk number 13: jobsatisfaction-lbl-sc
###################################################
lbl_test(jobsatisfaction, 
    scores = list(Job.Satisfaction = c(1, 3, 4, 5), 
                  Income = c(3, 10, 20, 35)))


###################################################
### code chunk number 14: eggs-Durbin
###################################################
egg_data <- data.frame(
    scores = c(9.7, 8.7, 5.4, 5.0, 9.6, 8.8, 5.6, 3.6, 9.0,
               7.3, 3.8, 4.3, 9.3, 8.7, 6.8, 3.8, 10.0, 7.5,
               4.2, 2.8, 9.6, 5.1, 4.6, 3.6, 9.8, 7.4, 4.4, 
               3.8, 9.4, 6.3, 5.1, 2.0, 9.4, 9.3, 8.2, 3.3,
               8.7, 9.0, 6.0, 3.3, 9.7, 6.7, 6.6, 2.8, 9.3,
               8.1, 3.7, 2.6, 9.8, 7.3, 5.4, 4.0, 9.0, 8.3,
               4.8,3.8,9.3,8.3,6.3,3.8),
    sitting = factor(rep(c(1:15), rep(4,15))),
    product = factor(c(1, 2, 4, 5, 2, 3, 6, 10, 2, 4, 6, 7,
                       1, 3, 5, 7, 1, 4, 8, 10, 2, 7, 8, 9,
                       2, 5, 8, 10, 5, 7, 9, 10, 1, 2, 3, 9,
                       4, 5, 6, 9, 1, 6, 7, 10, 3, 4, 9, 10,
                       1, 6, 8, 9, 3, 4, 7, 8, 3, 5, 6, 8)))
yt <- function(data) trafo(data, numeric_trafo = rank, 
                           block = egg_data$sitting)
independence_test(scores ~ product | sitting, 
                  data = egg_data, teststat = "quadtype", 
                  ytrafo = yt)


###################################################
### code chunk number 15: eggs-Durbin-approx
###################################################
pvalue(independence_test(scores ~ product | sitting, 
    data = egg_data, teststat = "quadtype", ytrafo = yt,
    distribution = approximate(B = 19999)))


###################################################
### code chunk number 16: eggs-Durbin-approx
###################################################
independence_test(scores ~ product | sitting, data = egg_data, 
                  scores = list(product = 1:10),
                  ytrafo = yt)


###################################################
### code chunk number 17: warpbreaks-Tukey
###################################################
if (require("multcomp")) {
    xt <- function(data) trafo(data, factor_trafo = function(x)
        model.matrix(~x - 1) %*% t(contrMat(table(x), "Tukey")))
    it <- independence_test(length ~ site, data = YOY, xtrafo = xt,
        teststat = "max", distribution = approximate(B = 9999))
    print(pvalue(it))
    print(pvalue(it, method = "single-step"))
}


###################################################
### code chunk number 18: biobase-resampling (eval = FALSE)
###################################################
## if (require("Biobase")) {
## 
##    p <- 100
##    pd <- new("AnnotatedDataFrame",
##              data = data.frame(group = gl(2, 20)),
##              varMetadata = data.frame(labelDescription = "1/2"))
##    exprs <- matrix(rnorm(p * 40), nrow = p)
##    exprs[1, 1:20] <- exprs[1, 1:20] + 1.5
##    ex <- new("ExpressionSet", exprs = exprs, phenoData = pd)
## 
##    it <- independence_test(group ~ ., data = ex,
##                            distribution = approximate(B = 1000))
## 
##    print(pvalue(it))
##    print(which(pvalue(it, method = "step-down") < 0.05))
## 
## }


