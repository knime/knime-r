### R code from vignette source 'xtableGallery.snw'

###################################################
### code chunk number 1: xtableGallery.snw:14-20 (eval = FALSE)
###################################################
## makeme <- function() {
## 	# I am a convenience function for debugging and can be ignored
## 	setwd("C:/JonathanSwinton/PathwayModeling/src/R/SourcePackages/xtable/inst/doc")
## 	Sweave("xtableGallery.RnW",stylepath=FALSE)
## }
## makeme()


###################################################
### code chunk number 2: xtableGallery.snw:45-46
###################################################
library(xtable)


###################################################
### code chunk number 3: xtableGallery.snw:52-57
###################################################
data(tli)

## Demonstrate data.frame
tli.table <- xtable(tli[1:10,])
digits(tli.table)[c(2,6)] <- 0


###################################################
### code chunk number 4: xtableGallery.snw:59-60
###################################################
print(tli.table,floating=FALSE)


###################################################
### code chunk number 5: xtableGallery.snw:64-66
###################################################
design.matrix <- model.matrix(~ sex*grade, data=tli[1:10,])
design.table <- xtable(design.matrix)


###################################################
### code chunk number 6: xtableGallery.snw:68-69
###################################################
print(design.table,floating=FALSE)


###################################################
### code chunk number 7: xtableGallery.snw:73-75
###################################################
fm1 <- aov(tlimth ~ sex + ethnicty + grade + disadvg, data=tli)
fm1.table <- xtable(fm1)


###################################################
### code chunk number 8: xtableGallery.snw:77-78
###################################################
print(fm1.table,floating=FALSE)


###################################################
### code chunk number 9: xtableGallery.snw:81-83
###################################################
fm2 <- lm(tlimth ~ sex*ethnicty, data=tli)
fm2.table <- xtable(fm2)


###################################################
### code chunk number 10: xtableGallery.snw:85-86
###################################################
print(fm2.table,floating=FALSE)


###################################################
### code chunk number 11: xtableGallery.snw:90-91
###################################################
print(xtable(anova(fm2)),floating=FALSE)


###################################################
### code chunk number 12: xtableGallery.snw:94-95
###################################################
fm2b <- lm(tlimth ~ ethnicty, data=tli)


###################################################
### code chunk number 13: xtableGallery.snw:97-98
###################################################
print(xtable(anova(fm2b,fm2)),floating=FALSE)


###################################################
### code chunk number 14: xtableGallery.snw:104-108
###################################################

## Demonstrate glm
fm3 <- glm(disadvg ~ ethnicty*grade, data=tli, family=binomial())
fm3.table <- xtable(fm3)


###################################################
### code chunk number 15: xtableGallery.snw:110-111
###################################################
print(fm3.table,floating=FALSE)


###################################################
### code chunk number 16: xtableGallery.snw:116-117
###################################################
print(xtable(anova(fm3)),floating=FALSE)


###################################################
### code chunk number 17: xtableGallery.snw:122-137
###################################################

## Demonstrate aov
## Taken from help(aov) in R 1.1.1
## From Venables and Ripley (1997) p.210.
N <- c(0,1,0,1,1,1,0,0,0,1,1,0,1,1,0,0,1,0,1,0,1,1,0,0)
P <- c(1,1,0,0,0,1,0,1,1,1,0,0,0,1,0,1,1,0,0,1,0,1,1,0)
K <- c(1,0,0,1,0,1,1,0,0,1,0,1,0,1,1,0,0,0,1,1,1,0,1,0)
yield <- c(49.5,62.8,46.8,57.0,59.8,58.5,55.5,56.0,62.8,55.8,69.5,55.0,
           62.0,48.8,45.5,44.2,52.0,51.5,49.8,48.8,57.2,59.0,53.2,56.0)
npk <- data.frame(block=gl(6,4), N=factor(N), P=factor(P), K=factor(K), yield=yield)
npk.aov <- aov(yield ~ block + N*P*K, npk)
op <- options(contrasts=c("contr.helmert", "contr.treatment"))
npk.aovE <- aov(yield ~  N*P*K + Error(block), npk)
options(op)
#summary(npk.aov)


###################################################
### code chunk number 18: xtableGallery.snw:139-140
###################################################
print(xtable(npk.aov),floating=FALSE)


###################################################
### code chunk number 19: xtableGallery.snw:144-145
###################################################
print(xtable(anova(npk.aov)),floating=FALSE)


###################################################
### code chunk number 20: xtableGallery.snw:149-150
###################################################
print(xtable(summary(npk.aov)),floating=FALSE)


###################################################
### code chunk number 21: xtableGallery.snw:153-154
###################################################
#summary(npk.aovE)


###################################################
### code chunk number 22: xtableGallery.snw:156-157
###################################################
print(xtable(npk.aovE),floating=FALSE)


###################################################
### code chunk number 23: xtableGallery.snw:161-162
###################################################
print(xtable(summary(npk.aovE)),floating=FALSE)


###################################################
### code chunk number 24: xtableGallery.snw:166-176
###################################################

## Demonstrate lm
## Taken from help(lm) in R 1.1.1
## Annette Dobson (1990) "An Introduction to Generalized Linear Models".
## Page 9: Plant Weight Data.
ctl <- c(4.17,5.58,5.18,6.11,4.50,4.61,5.17,4.53,5.33,5.14)
trt <- c(4.81,4.17,4.41,3.59,5.87,3.83,6.03,4.89,4.32,4.69)
group <- gl(2,10,20, labels=c("Ctl","Trt"))
weight <- c(ctl, trt)
lm.D9 <- lm(weight ~ group)


###################################################
### code chunk number 25: xtableGallery.snw:178-179
###################################################
print(xtable(lm.D9),floating=FALSE)


###################################################
### code chunk number 26: xtableGallery.snw:183-184
###################################################
print(xtable(anova(lm.D9)),floating=FALSE)


###################################################
### code chunk number 27: xtableGallery.snw:188-198
###################################################

## Demonstrate glm
## Taken from help(glm) in R 1.1.1
## Annette Dobson (1990) "An Introduction to Generalized Linear Models".
## Page 93: Randomized Controlled Trial :
counts <- c(18,17,15,20,10,20,25,13,12)
outcome <- gl(3,1,9)
treatment <- gl(3,3)
d.AD <- data.frame(treatment, outcome, counts)
glm.D93 <- glm(counts ~ outcome + treatment, family=poisson())


###################################################
### code chunk number 28: xtableGallery.snw:200-201
###################################################
print(xtable(glm.D93,align="r|llrc"),floating=FALSE)


###################################################
### code chunk number 29: prcomp
###################################################
if(require(stats,quietly=TRUE)) {
  ## Demonstrate prcomp
  ## Taken from help(prcomp) in mva package of R 1.1.1
  data(USArrests)
  pr1 <- prcomp(USArrests)
}


###################################################
### code chunk number 30: xtableGallery.snw:213-216
###################################################
if(require(stats,quietly=TRUE)) {
  print(xtable(pr1),floating=FALSE)
}


###################################################
### code chunk number 31: xtableGallery.snw:221-222
###################################################
  print(xtable(summary(pr1)),floating=FALSE)


###################################################
### code chunk number 32: xtableGallery.snw:227-231
###################################################
#  ## Demonstrate princomp
#  ## Taken from help(princomp) in mva package of R 1.1.1
#  pr2 <- princomp(USArrests)
#  print(xtable(pr2))


###################################################
### code chunk number 33: xtableGallery.snw:235-238
###################################################
temp.ts <- ts(cumsum(1+round(rnorm(100), 0)), start = c(1954, 7), frequency=12)
   temp.table <- xtable(temp.ts,digits=0)
    caption(temp.table) <- "Time series example"


###################################################
### code chunk number 34: xtableGallery.snw:240-241
###################################################
    print(temp.table,floating=FALSE)


###################################################
### code chunk number 35: savetofile
###################################################
if (FALSE) {
  for(i in c("latex","html")) {
    outFileName <- paste("xtable.",ifelse(i=="latex","tex",i),sep="")
    print(xtable(lm.D9),type=i,file=outFileName,append=TRUE,latex.environments=NULL)
    print(xtable(lm.D9),type=i,file=outFileName,append=TRUE,latex.environments="")
    print(xtable(lm.D9),type=i,file=outFileName,append=TRUE,latex.environments="center")
    print(xtable(anova(glm.D93,test="Chisq")),type=i,file=outFileName,append=TRUE)
    print(xtable(anova(glm.D93)),hline.after=c(1),size="small",type=i,file=outFileName,append=TRUE)
      # print(xtable(pr2),type=i,file=outFileName,append=TRUE)
         }
}


###################################################
### code chunk number 36: xtableGallery.snw:258-261
###################################################
insane <- data.frame(Name=c("Ampersand","Greater than","Less than","Underscore","Per cent","Dollar","Backslash","Hash", "Caret", "Tilde","Left brace","Right brace"),
				Character = I(c("&",">",		"<",		"_",		"%",		"$",		"\\", "#",	"^",		"~","{","}")))
colnames(insane)[2] <- paste(insane[,2],collapse="")


###################################################
### code chunk number 37: pxti
###################################################
print( xtable(insane))


###################################################
### code chunk number 38: xtableGallery.snw:268-269
###################################################
wanttex <- xtable(data.frame( label=paste("Value_is $10^{-",1:3,"}$",sep="")))


###################################################
### code chunk number 39: xtableGallery.snw:271-272
###################################################
print(wanttex,sanitize.text.function=function(str)gsub("_","\\_",str,fixed=TRUE))


###################################################
### code chunk number 40: xtableGallery.snw:279-283
###################################################
mat <- round(matrix(c(0.9, 0.89, 200, 0.045, 2.0), c(1, 5)), 4)
rownames(mat) <- "$y_{t-1}$"
colnames(mat) <- c("$R^2$", "$\\bar{R}^2$", "F-stat", "S.E.E", "DW")
mat <- xtable(mat)


###################################################
### code chunk number 41: xtableGallery.snw:285-286
###################################################
print(mat, sanitize.text.function = function(x){x})


###################################################
### code chunk number 42: xtableGallery.snw:291-292
###################################################
money <- matrix(c("$1,000","$900","$100"),ncol=3,dimnames=list("$\\alpha$",c("Income (US$)","Expenses (US$)","Profit (US$)")))


###################################################
### code chunk number 43: xtableGallery.snw:294-295
###################################################
print(xtable(money),sanitize.rownames.function=function(x) {x})


###################################################
### code chunk number 44: xtableGallery.snw:300-303
###################################################
   print(xtable(lm.D9,caption="\\tt latex.environments=NULL"),latex.environments=NULL)
    print(xtable(lm.D9,caption="\\tt latex.environments=\"\""),latex.environments="")
    print(xtable(lm.D9,caption="\\tt latex.environments=\"center\""),latex.environments="center")


###################################################
### code chunk number 45: xtableGallery.snw:307-308
###################################################
tli.table <- xtable(tli[1:10,])


###################################################
### code chunk number 46: xtableGallery.snw:310-311
###################################################
align(tli.table) <- rep("r",6)


###################################################
### code chunk number 47: xtableGallery.snw:313-314
###################################################
print(tli.table,floating=FALSE)


###################################################
### code chunk number 48: xtableGallery.snw:317-318
###################################################
align(tli.table) <- "|rrl|l|lr|"


###################################################
### code chunk number 49: xtableGallery.snw:320-321
###################################################
print(tli.table,floating=FALSE)


###################################################
### code chunk number 50: xtableGallery.snw:324-325
###################################################
align(tli.table) <- "|rr|lp{3cm}l|r|"


###################################################
### code chunk number 51: xtableGallery.snw:327-328
###################################################
print(tli.table,floating=FALSE)


###################################################
### code chunk number 52: xtableGallery.snw:335-336
###################################################
digits(tli.table) <- 3


###################################################
### code chunk number 53: xtableGallery.snw:338-339
###################################################
print(tli.table,floating=FALSE,)


###################################################
### code chunk number 54: xtableGallery.snw:344-345
###################################################
digits(tli.table) <- 1:(ncol(tli)+1)


###################################################
### code chunk number 55: xtableGallery.snw:347-348
###################################################
print(tli.table,floating=FALSE,)


###################################################
### code chunk number 56: xtableGallery.snw:353-354
###################################################
digits(tli.table) <- matrix( 0:4, nrow = 10, ncol = ncol(tli)+1 )


###################################################
### code chunk number 57: xtableGallery.snw:356-357
###################################################
print(tli.table,floating=FALSE,)


###################################################
### code chunk number 58: xtableGallery.snw:361-362
###################################################
print((tli.table),include.rownames=FALSE,floating=FALSE)


###################################################
### code chunk number 59: xtableGallery.snw:366-367
###################################################
align(tli.table) <- "|r|r|lp{3cm}l|r|"


###################################################
### code chunk number 60: xtableGallery.snw:369-370
###################################################
print((tli.table),include.rownames=FALSE,floating=FALSE)


###################################################
### code chunk number 61: xtableGallery.snw:374-375
###################################################
align(tli.table) <- "|rr|lp{3cm}l|r|"


###################################################
### code chunk number 62: xtableGallery.snw:379-380
###################################################
print((tli.table),include.colnames=FALSE,floating=FALSE)


###################################################
### code chunk number 63: xtableGallery.snw:384-385
###################################################
print(tli.table,include.colnames=FALSE,floating=FALSE,hline.after=c(0,nrow(tli.table)))


###################################################
### code chunk number 64: xtableGallery.snw:389-390
###################################################
print((tli.table),include.colnames=FALSE,include.rownames=FALSE,floating=FALSE)


###################################################
### code chunk number 65: xtableGallery.snw:397-398
###################################################
print((tli.table),rotate.rownames=TRUE,rotate.colnames=TRUE)


###################################################
### code chunk number 66: xtableGallery.snw:407-408
###################################################
print(xtable(anova(glm.D93)),hline.after=c(1),floating=FALSE)


###################################################
### code chunk number 67: xtableGallery.snw:436-437
###################################################
print(tli.table, booktabs=TRUE, floating = FALSE)


###################################################
### code chunk number 68: xtableGallery.snw:449-451
###################################################
bktbs <- xtable(matrix(1:10, ncol = 2))
hlines <- c(-1,0,1,nrow(bktbs))


###################################################
### code chunk number 69: xtableGallery.snw:454-455
###################################################
print(bktbs, booktabs = TRUE, hline.after = hlines, floating = FALSE)


###################################################
### code chunk number 70: xtableGallery.snw:460-461
###################################################
print(xtable(anova(glm.D93)),size="small",floating=FALSE)


###################################################
### code chunk number 71: longtable
###################################################

## Demonstration of longtable support.
x <- matrix(rnorm(1000), ncol = 10)
x.big <- xtable(x,label='tabbig',
	caption='Example of longtable spanning several pages')


###################################################
### code chunk number 72: xtableGallery.snw:475-476
###################################################
print(x.big,tabular.environment='longtable',floating=FALSE)


###################################################
### code chunk number 73: xtableGallery.snw:514-516
###################################################
x <- x[1:30,]
x.small <- xtable(x,label='tabsmall',caption='A sideways table')


###################################################
### code chunk number 74: xtableGallery.snw:519-520
###################################################
print(x.small,floating.environment='sidewaystable')


###################################################
### code chunk number 75: xtableGallery.snw:527-529
###################################################
x <- x[1:20,]
x.rescale <- xtable(x,label='tabrescaled',caption='A rescaled table')


###################################################
### code chunk number 76: xtableGallery.snw:532-533
###################################################
print(x.rescale, scalebox=0.7)


###################################################
### code chunk number 77: xtableGallery.snw:542-552
###################################################
df.width <- data.frame(
  "label 1 with much more text than is needed" = c("item 1", "A"),
  "label 2 is also very long" = c("item 2","B"),
  "label 3" = c("item 3","C"),
  "label 4" = c("item 4 but again with too much text","D"),
  check.names = FALSE)

x.width <- xtable(df.width,
  caption="Using the 'tabularx' environment")
align(x.width) <- "|l|X|X|l|X|"


###################################################
### code chunk number 78: xtableGallery.snw:555-557
###################################################
print(x.width, tabular.environment="tabularx",
  width="\\textwidth")


###################################################
### code chunk number 79: xtableGallery.snw:565-566
###################################################
x.out <- print(tli.table, print.results = FALSE)


###################################################
### code chunk number 80: xtableGallery.snw:573-576
###################################################
x.ltx <- toLatex(tli.table)
class(x.ltx)
x.ltx


###################################################
### code chunk number 81: xtableGallery.snw:582-583
###################################################
toLatex(sessionInfo())


