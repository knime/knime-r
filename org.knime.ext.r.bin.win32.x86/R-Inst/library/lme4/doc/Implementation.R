### R code from vignette source 'Implementation.Rnw'

###################################################
### code chunk number 1: preliminaries
###################################################
options(width=80, show.signif.stars = FALSE,
        lattice.theme = function() canonical.theme("pdf", color = FALSE))
library(lattice)
library(Matrix)
library(lme4)
data("Rail", package = "MEMSS")
data("ScotsSec", package = "mlmRev")


###################################################
### code chunk number 2: strRail
###################################################
str(Rail)


###################################################
### code chunk number 3: Raildotplot
###################################################
print(dotplot(reorder(Rail,travel)~travel,Rail,xlab="Travel time (ms)",ylab="Rail"))


###################################################
### code chunk number 4: Raildata
###################################################
Rail


###################################################
### code chunk number 5: RailfitML
###################################################
Rm1ML <- lmer(travel ~ 1 + (1|Rail), Rail, REML = FALSE, verbose = TRUE)


###################################################
### code chunk number 6: Railfitshow
###################################################
Rm1ML


###################################################
### code chunk number 7: ZXytRail
###################################################
Rm1ML@Zt
as(Rail[["Rail"]], "sparseMatrix")


###################################################
### code chunk number 8: fewdigits
###################################################
op <- options(digits=4)


###################################################
### code chunk number 9: LRm1ML
###################################################
as(Rm1ML@L, "sparseMatrix")


###################################################
### code chunk number 10: unfewdigits
###################################################
options(op)


###################################################
### code chunk number 11: devRm1ML
###################################################
Rm1ML@deviance


###################################################
### code chunk number 12: ldZRm1ML
###################################################
L <- as(Rm1ML@L, "sparseMatrix")
2 * sum(log(diag(L)))


###################################################
### code chunk number 13: lr2Rm1ML
###################################################
(RX <- Rm1ML@RX)


###################################################
### code chunk number 14: ldXRm1ML
###################################################
2 * sum(log(diag(Rm1ML@RX)))


###################################################
### code chunk number 15: dimsRm1ML
###################################################
(dd <- Rm1ML@dims)


###################################################
### code chunk number 16: s2Rm1ML
###################################################
Rm1ML@deviance["pwrss"]/dd["n"]


###################################################
### code chunk number 17: devcomp
###################################################
mm <- Rm1ML
sg <- seq(0, 20, len = 101)
dev <- mm@deviance
nc <- length(dev)
nms <- names(dev)
vals <- matrix(0, nrow = length(sg), ncol = nc, dimnames = list(NULL, nms))
for (i in seq(along = sg)) {
    .Call(lme4:::mer_ST_setPars, mm, sg[i])
    .Call(lme4:::mer_update_L, mm)
    res <- try(.Call(lme4:::mer_update_RX, mm), silent = TRUE)
    if (inherits(res, "try-error")) {
        vals[i,] <- NA
    } else {
        .Call(lme4:::mer_update_ranef, mm)
        vals[i,] <- mm@deviance
    }
}
print(xyplot(ML + ldL2 + sigmaML + pwrss + disc + usqr ~ sg, as.data.frame(vals),
             type = c("g", "l"), outer = TRUE, layout = c(1,6),
             xlab = expression(sigma[b]/sigma), ylab = NULL,
             scales = list(x = list(axs = 'i'),
             y = list(relation = "free", rot = 0)),
             strip = FALSE, strip.left = TRUE))


###################################################
### code chunk number 18: leftlr2
###################################################
18 * log(deviance(lm(travel ~ 1, Rail)))


###################################################
### code chunk number 19: rightlr2
###################################################
18 * log(deviance(lm(travel ~ Rail, Rail)))


###################################################
### code chunk number 20: devcomp2
###################################################
mm <- Rm1ML
sg <- seq(3.75, 8.25, len = 101)
vals <- matrix(0, nrow = length(sg), ncol = length(dev),
               dimnames = list(NULL, names(dev)))
for (i in seq_along(sg)) {
    .Call(lme4:::mer_ST_setPars, mm, sg[i])
    .Call(lme4:::mer_update_L, mm)
    res <- try(.Call(lme4:::mer_update_RX, mm), silent = TRUE)
    if (inherits(res, "try-error")) {
        vals[i,] <- NA
    } else {
        .Call(lme4:::mer_update_ranef, mm)
        vals[i,] <- mm@deviance
    }
}
print(xyplot(ML + ldL2 + I(18 * log(pwrss)) ~ sg, as.data.frame(vals),
             type = c("g", "l"), outer = TRUE, layout = c(1,3),
             xlab = expression(sigma[b]/sigma), ylab = NULL,
             scales = list(x = list(axs = 'i'),
             y = list(relation = "free", rot = 0)),
             strip = FALSE, strip.left = TRUE))
## base <- 18 * log(deviance(lm(travel ~ Rail, Rail)))
## print(xyplot(devC + ldL2 ~ sg,
##              data.frame(ldL2 = vals[,"ldL2"], devC =
##                         vals[, "ldL2"] + mm@dims['n'] * log(vals[, "pwrss"]) - base,
##                         sg = sg), type = c("g", "l"),
##              scales = list(x = list(axs = 'i')), aspect = 1.8,
##              xlab = expression(sigma[1]), ylab = "Shifted deviance",
##              auto.key = list(text = c("deviance", "log|SZ'ZS + I|"),
##              space = "right", points = FALSE, lines = TRUE)))


###################################################
### code chunk number 21: sleepxyplot
###################################################
print(xyplot(Reaction ~ Days | Subject, sleepstudy, aspect = "xy",
             layout = c(6,3), type = c("g", "p", "r"),
             index.cond = function(x,y) coef(lm(y ~ x))[1],
             xlab = "Days of sleep deprivation",
             ylab = "Average reaction time (ms)"))


###################################################
### code chunk number 22: sm1
###################################################
print(sm1 <- lmer(Reaction ~ Days + (Days|Subject), sleepstudy))


###################################################
### code chunk number 23: sm1struct
###################################################
print(image(tcrossprod(sm1@A), colorkey = FALSE, sub = NULL),
      split = c(1,1,2,1), more = TRUE)
print(image(sm1@L, colorkey = FALSE, sub = NULL),
      split = c(2,1,2,1))


###################################################
### code chunk number 24: A1212
###################################################
as(sm1@L, "sparseMatrix")[1:2,1:2]
sm1@RX


###################################################
### code chunk number 25: sm1deviance
###################################################
sm1@deviance


###################################################
### code chunk number 26: sm1reconstruct
###################################################
sm1@RX
str(dL <- diag(as(sm1@L, "sparseMatrix")))
c(ldL2 = sum(log(dL^2)), ldRX2 = sum(log(diag(sm1@RX)^2)), log(sm1@deviance["pwrss"]))


###################################################
### code chunk number 27: sm1ST
###################################################
show(st <- sm1@ST[[1]])


###################################################
### code chunk number 28: sm1VarCorr
###################################################
show(vc <- VarCorr(sm1))


###################################################
### code chunk number 29: sm1VarCorrRec
###################################################
T <- st
diag(T) <- 1
S <- diag(diag(st))
T
S
T %*% S %*% S %*% t(T) * attr(vc, "sc")^2


###################################################
### code chunk number 30: Oatsxy
###################################################
data(Oats, package = 'MEMSS')
print(xyplot(yield ~ nitro | Block, Oats, groups = Variety, type = c("g", "b"),
             auto.key = list(lines = TRUE, space = 'top', columns = 3),
             xlab = "Nitrogen concentration (cwt/acre)",
             ylab = "Yield (bushels/acre)",
             aspect = 'xy'))


###################################################
### code chunk number 31: Om1
###################################################
print(Om1 <- lmer(yield ~ nitro + Variety + (1|Block/Variety), Oats), corr = FALSE)


###################################################
### code chunk number 32: Om1struct
###################################################
print(image(tcrossprod(Om1@A), colorkey = FALSE, sub = NULL),
       split = c(1,1,2,1), more = TRUE)
print(image(Om1@L, colorkey = FALSE, sub = NULL), split = c(2,1,2,1))


###################################################
### code chunk number 33: Om1a
###################################################
print(Om1a <- lmer(yield ~ nitro + (1|Block/Variety), Oats), corr = FALSE)


###################################################
### code chunk number 34: Om1astruct
###################################################
print(image(tcrossprod(Om1a@A), colorkey = FALSE, sub = NULL),
      split = c(1,1,2,1), more = TRUE)
print(image(Om1a@L, colorkey = FALSE, sub = NULL), split = c(2,1,2,1))


###################################################
### code chunk number 35: sm2
###################################################
print(sm2 <- lmer(Reaction ~ Days + (1|Subject) + (0+Days|Subject), sleepstudy), corr = FALSE)


###################################################
### code chunk number 36: sm2struct
###################################################
print(image(tcrossprod(sm2@A), colorkey = FALSE, sub = NULL),
      split = c(1,1,2,1), more = TRUE)
print(image(sm2@L, colorkey = FALSE, sub = NULL), split = c(2,1,2,1))


###################################################
### code chunk number 37: sm2perm
###################################################
str(sm2@L@perm)


###################################################
### code chunk number 38: Om2
###################################################
print(Om2 <- lmer(yield ~ nitro + (1|Variety:Block) + (nitro|Block), Oats), corr = FALSE)


###################################################
### code chunk number 39: Om2struct
###################################################
print(image(tcrossprod(Om2@A), colorkey = FALSE, sub = NULL),
      split = c(1,1,2,1), more = TRUE)
print(image(Om2@L, colorkey = FALSE, sub = NULL),
      split = c(2,1,2,1))


###################################################
### code chunk number 40: Om2ALsize
###################################################
length(tcrossprod(Om2@A)@x)
length(Om2@L@x)


###################################################
### code chunk number 41: ScotsSec
###################################################
str(ScotsSec)


###################################################
### code chunk number 42: Sm1
###################################################
print(Sm1 <- lmer(attain ~ verbal * sex + (1|primary) + (1|second), ScotsSec), corr = FALSE)


###################################################
### code chunk number 43: Sm1dims
###################################################
Sm1@dims


###################################################
### code chunk number 44: Scotscrosstab
###################################################
head(xtabs(~ primary + second, ScotsSec))


###################################################
### code chunk number 45: Sm1struct
###################################################
print(image(tcrossprod(Sm1@A), colorkey = FALSE, sub = NULL),
      split = c(1,1,2,1), more = TRUE)
print(image(Sm1@L, colorkey = FALSE, sub = NULL), split = c(2,1,2,1))


###################################################
### code chunk number 46: Sm1fill
###################################################
c(A = length(tcrossprod(Sm1@A)@x), L = length(Sm1@L@x))


