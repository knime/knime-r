### R code from vignette source 'pbkrtest-introduction.Rnw'
### Encoding: ISO8859-1

###################################################
### code chunk number 1: pbkrtest-introduction.Rnw:15-18
###################################################
require( pbkrtest )
prettyVersion <- packageDescription("pbkrtest")$Version
prettyDate <- format(Sys.Date())


###################################################
### code chunk number 2: pbkrtest-introduction.Rnw:63-65
###################################################
options(prompt = "R> ", continue = "+  ", width = 80, useFancyQuotes = FALSE)
dir.create("figures")


###################################################
### code chunk number 3: pbkrtest-introduction.Rnw:70-71
###################################################
library(pbkrtest)


###################################################
### code chunk number 4: pbkrtest-introduction.Rnw:82-84 (eval = FALSE)
###################################################
## library(devtools)
## install_github("lme4", user = "lme4")


###################################################
### code chunk number 5: pbkrtest-introduction.Rnw:97-99
###################################################
data(shoes, package="MASS")
shoes


###################################################
### code chunk number 6: pbkrtest-introduction.Rnw:105-108
###################################################
plot(A~1, data=shoes, col='red',lwd=2, pch=1, ylab="wear", xlab="boy")
points(B~1, data=shoes, col='blue',lwd=2,pch=2)
points(I((A+B)/2)~1, data=shoes, pch='-', lwd=2)


###################################################
### code chunk number 7: pbkrtest-introduction.Rnw:116-119
###################################################
r1<-t.test(shoes$A, shoes$B, paired=T)
r2<-t.test(shoes$A-shoes$B)
r1


###################################################
### code chunk number 8: pbkrtest-introduction.Rnw:127-135
###################################################
boy <- rep(1:10,2)
boyf<- factor(letters[boy])
mat <- factor(c(rep("A", 10), rep("B",10)))
## Balanced data:
shoe.b <- data.frame(wear=unlist(shoes), boy=boy, boyf=boyf, mat=mat)
head(shoe.b)
## Imbalanced data; delete (boy=1, mat=1) and (boy=2, mat=b)
shoe.i <-  shoe.b[-c(1,12),]


###################################################
### code chunk number 9: pbkrtest-introduction.Rnw:141-145
###################################################
lmm1.b  <- lmer( wear ~ mat + (1|boyf), data=shoe.b )
lmm0.b  <- update( lmm1.b, .~. - mat)
lmm1.i  <- lmer( wear ~ mat + (1|boyf), data=shoe.i )
lmm0.i  <- update(lmm1.i, .~. - mat)


###################################################
### code chunk number 10: pbkrtest-introduction.Rnw:152-154
###################################################
anova( lmm1.b, lmm0.b, test="Chisq" ) ## Balanced data
anova( lmm1.i, lmm0.i, test="Chisq" ) ## Imbalanced data


###################################################
### code chunk number 11: pbkrtest-introduction.Rnw:165-166
###################################################
( kr.b<-KRmodcomp(lmm1.b, lmm0.b) )


###################################################
### code chunk number 12: pbkrtest-introduction.Rnw:170-171
###################################################
summary( kr.b )


###################################################
### code chunk number 13: pbkrtest-introduction.Rnw:177-178
###################################################
getKR(kr.b, "ddf")


###################################################
### code chunk number 14: pbkrtest-introduction.Rnw:183-184
###################################################
( kr.i<-KRmodcomp(lmm1.i, lmm0.i) )


###################################################
### code chunk number 15: pbkrtest-introduction.Rnw:191-193
###################################################
shoes2 <- list(A=shoes$A[-(1:2)], B=shoes$B[-(1:2)])
t.test(shoes2$A, shoes2$B, paired=T)


###################################################
### code chunk number 16: pbkrtest-introduction.Rnw:206-207
###################################################
( pb.b <- PBmodcomp(lmm1.b, lmm0.b, nsim=500) )


###################################################
### code chunk number 17: pbkrtest-introduction.Rnw:211-212
###################################################
summary( pb.b )


###################################################
### code chunk number 18: pbkrtest-introduction.Rnw:220-221
###################################################
( pb.i<-PBmodcomp(lmm1.i, lmm0.i, nsim=500) )


###################################################
### code chunk number 19: pbkrtest-introduction.Rnw:225-226
###################################################
summary( pb.i )


###################################################
### code chunk number 20: pbkrtest-introduction.Rnw:258-262
###################################################
shoe3 <- subset(shoe.b, boy<=5)
shoe3 <- shoe3[order(shoe3$boy), ]
lmm1  <- lmer( wear ~ mat + (1|boyf), data=shoe3 )
str( SG <- get_SigmaG( lmm1 ), max=2)


###################################################
### code chunk number 21: pbkrtest-introduction.Rnw:266-267
###################################################
round( SG$Sigma*10 )


###################################################
### code chunk number 22: pbkrtest-introduction.Rnw:271-272
###################################################
SG$G


