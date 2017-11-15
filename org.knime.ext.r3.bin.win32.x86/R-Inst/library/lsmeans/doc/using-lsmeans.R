### R code from vignette source 'using-lsmeans.rnw'

###################################################
### code chunk number 1: using-lsmeans.rnw:69-71
###################################################
options(show.signif.stars=FALSE, prompt="R> ", continue="   ", 
    useFancyQuotes=FALSE, width=100, digits=6)


###################################################
### code chunk number 2: using-lsmeans.rnw:155-158
###################################################
library("lsmeans")
oranges.lm1 <- lm(sales1 ~ price1 + price2 + day + store, data = oranges)
anova(oranges.lm1)


###################################################
### code chunk number 3: using-lsmeans.rnw:161-162
###################################################
( oranges.rg1 <- ref.grid(oranges.lm1) )


###################################################
### code chunk number 4: using-lsmeans.rnw:165-166
###################################################
summary(oranges.rg1)


###################################################
### code chunk number 5: using-lsmeans.rnw:171-172
###################################################
lsmeans(oranges.rg1, "day")   ## or lsmeans(oranges.lm1, "day")


###################################################
### code chunk number 6: using-lsmeans.rnw:175-176
###################################################
with(oranges, tapply(sales1, day, mean))


###################################################
### code chunk number 7: using-lsmeans.rnw:187-189
###################################################
lsmeans(oranges.lm1, "day", at = list(price1 = 50, 
    price2 = c(40,60), day = c("2","3","4")) )


###################################################
### code chunk number 8: using-lsmeans.rnw:198-201
###################################################
org.lsm <- lsmeans(oranges.lm1, "day", by = "price2", 
    at = list(price1 = 50, price2 = c(40,60), day = c("2","3","4")) )
org.lsm


###################################################
### code chunk number 9: using-lsmeans.rnw:204-207 (eval = FALSE)
###################################################
## lsmeans(oranges.lm1, ~ day | price, at = ... )         # Ex 1
## lsmeans(oranges.lm1, c("day","price2"), at = ... )     # Ex 2
## lsmeans(oranges.lm1, ~ day * price, at = ... )         # Ex 3


###################################################
### code chunk number 10: using-lsmeans.rnw:217-218
###################################################
str(org.lsm)


###################################################
### code chunk number 11: using-lsmeans.rnw:224-226
###################################################
( org.sum <- summary(org.lsm, infer = c(TRUE,TRUE), 
                    level = .90, adjust = "bon", by = "day") )


###################################################
### code chunk number 12: using-lsmeans.rnw:231-232
###################################################
class(org.sum)


###################################################
### code chunk number 13: using-lsmeans.rnw:236-237
###################################################
transform(org.sum, lsrubles = lsmean * 34.2)


###################################################
### code chunk number 14: using-lsmeans.rnw:245-247
###################################################
org.lsm2 <- update(org.lsm, by.vars = NULL, level = .99)
org.lsm2


###################################################
### code chunk number 15: org-plot
###################################################
plot(org.lsm, by = "price2")


###################################################
### code chunk number 16: using-lsmeans.rnw:270-271
###################################################
contrast(org.lsm, method = "eff")


###################################################
### code chunk number 17: using-lsmeans.rnw:276-278
###################################################
days.lsm <- lsmeans(oranges.rg1, "day")
( days_contr.lsm <- contrast(days.lsm, "trt.vs.ctrl", ref = c(5,6)) )


###################################################
### code chunk number 18: using-lsmeans.rnw:283-284 (eval = FALSE)
###################################################
## confint(contrast(days.lsm, "trt.vs.ctrlk"))


###################################################
### code chunk number 19: using-lsmeans.rnw:292-293
###################################################
pairs(org.lsm)


###################################################
### code chunk number 20: using-lsmeans.rnw:296-297
###################################################
cld(days.lsm, alpha = .10)


###################################################
### code chunk number 21: days-cmp
###################################################
plot(days.lsm, comparisons = TRUE, alpha = .10)


###################################################
### code chunk number 22: using-lsmeans.rnw:324-325
###################################################
rbind(pairs(org.lsm)[1:3], pairs(org.lsm, by = "day")[1])


###################################################
### code chunk number 23: using-lsmeans.rnw:330-331
###################################################
rbind(pairs(lsmeans(org.lsm, "day")), pairs(lsmeans(org.lsm, "price2")))


###################################################
### code chunk number 24: using-lsmeans.rnw:337-340
###################################################
oranges.mlm <- lm(cbind(sales1,sales2) ~ price1 + price2 + day + store, 
                 data = oranges)
ref.grid(oranges.mlm)


###################################################
### code chunk number 25: using-lsmeans.rnw:343-345
###################################################
org.mlsm <- lsmeans(oranges.mlm, ~ day | variety, mult.name = "variety")
cld(org.mlsm, sort = FALSE)


###################################################
### code chunk number 26: using-lsmeans.rnw:353-354
###################################################
org.vardiff <- update(pairs(org.mlsm, by = "day"), by = NULL)


###################################################
### code chunk number 27: using-lsmeans.rnw:357-358
###################################################
cld(org.vardiff)


###################################################
### code chunk number 28: using-lsmeans.rnw:364-366
###################################################
org.icon <- contrast(org.mlsm, interaction = c("poly", "pairwise"))
org.icon


###################################################
### code chunk number 29: using-lsmeans.rnw:370-371
###################################################
coef(org.icon)


###################################################
### code chunk number 30: using-lsmeans.rnw:379-381
###################################################
# Ensure we see the same results each time
set.seed(123454321)


###################################################
### code chunk number 31: using-lsmeans.rnw:383-386
###################################################
library("multcomp")
days.glht <- as.glht(days_contr.lsm)
summary(days.glht, test = adjusted("Westfall"))


###################################################
### code chunk number 32: using-lsmeans.rnw:389-391 (eval = FALSE)
###################################################
## days.glht1 <- glht(oranges.lm1, 
##                    lsm("day", contr = "trt.vs.ctrl", ref = c(5,6)))


###################################################
### code chunk number 33: using-lsmeans.rnw:395-397 (eval = FALSE)
###################################################
## summary(days_contr.lsm, adjust = "mvt")
## summary(days.glht)


###################################################
### code chunk number 34: using-lsmeans.rnw:403-404 (eval = FALSE)
###################################################
## summary(as.glht(pairs(org.lsm)))


###################################################
### code chunk number 35: using-lsmeans.rnw:407-408 (eval = FALSE)
###################################################
## summary(as.glht(pairs(org.lsm), by = NULL))


###################################################
### code chunk number 36: using-lsmeans.rnw:411-412 (eval = FALSE)
###################################################
## summary(as.glht(pairs(org.lsm, by = NULL)))


###################################################
### code chunk number 37: using-lsmeans.rnw:424-429
###################################################
data("Oats", package = "nlme")
library("lme4")
Oats.lmer <- lmer(log(yield) ~ Variety*factor(nitro) + (1|Block/Variety), 
                 data = Oats)
anova(Oats.lmer)


###################################################
### code chunk number 38: oatcontr (eval = FALSE)
###################################################
## contrast(lsmeans(Oats.lmer, "nitro"), "poly")


###################################################
### code chunk number 39: using-lsmeans.rnw:436-437
###################################################
cat("NOTE: Results may be misleading due to involvement in interactions")


###################################################
### code chunk number 40: using-lsmeans.rnw:439-440
###################################################
contrast(lsmeans(Oats.lmer, "nitro"), "poly")


###################################################
### code chunk number 41: using-lsmeans.rnw:444-446
###################################################
Oats.lmer2 <- lmer(log(yield) ~ Variety + poly(nitro,2) 
                                + (1|Block/Variety),  data = Oats)


###################################################
### code chunk number 42: using-lsmeans.rnw:450-451
###################################################
Oats.lsm2 <- lsmeans(Oats.lmer2, ~ nitro | Variety, cov.reduce = FALSE)


###################################################
### code chunk number 43: using-lsmeans.rnw:463-468
###################################################
library("xtable")
xtbl <- xtable(Oats.lsm2, caption = "Example using \\texttt{xtable}",
    label = "xtable:example")
print(xtbl, table.placement = "t")    
cat("See Table~\\ref{xtable:example}.\n")


###################################################
### code chunk number 44: oatslmer
###################################################
lsmip(Oats.lmer, Variety ~ nitro, ylab = "Observed log(yield)")


###################################################
### code chunk number 45: oatslmer2
###################################################
lsmip(Oats.lsm2, Variety ~ nitro, ylab = "Predicted log(yield)")


###################################################
### code chunk number 46: using-lsmeans.rnw:504-505
###################################################
str(Oats.lsm2)


###################################################
### code chunk number 47: using-lsmeans.rnw:508-509
###################################################
summary(Oats.lsm2, type = "response")


###################################################
### code chunk number 48: using-lsmeans.rnw:520-524
###################################################
Oats.log1 <- lmer(log(yield + 5) ~ Variety + factor(nitro) 
                  + (1|Block/Variety), data = Oats)
( Oats.rg1 <- update(ref.grid(Oats.log1), 
                    tran = make.tran("genlog", 5)) )


###################################################
### code chunk number 49: using-lsmeans.rnw:529-530
###################################################
round(predict(Oats.rg1, type = "response"), 1)


###################################################
### code chunk number 50: using-lsmeans.rnw:535-537
###################################################
my.tran <- make.tran("boxcox", c(.567, 10))
my.tran$linkfun(10:15)


###################################################
### code chunk number 51: using-lsmeans.rnw:545-549
###################################################
Oats.bc <- with(my.tran, lmer(linkfun(yield) ~ Variety + factor(nitro)
                              + (1|Block/Variety), data = Oats))
( rg.bc <- ref.grid(Oats.bc) )
round(predict(rg.bc, type = "response"), 1)


###################################################
### code chunk number 52: using-lsmeans.rnw:554-555
###################################################
rg.bc.regrid <- regrid(rg.bc)


###################################################
### code chunk number 53: using-lsmeans.rnw:558-559
###################################################
round(rg.bc.regrid@bhat, 1)


###################################################
### code chunk number 54: using-lsmeans.rnw:564-566
###################################################
summary(lsmeans(rg.bc, "Variety"), type = "response")
lsmeans(rg.bc.regrid, "Variety")


###################################################
### code chunk number 55: using-lsmeans.rnw:585-589
###################################################
rg.log <- regrid(rg.bc, "log")
lsm.log <- lsmeans(rg.log, "Variety")
summary(lsm.log, type = "response")
summary(pairs(lsm.log), type = "response")


###################################################
### code chunk number 56: using-lsmeans.rnw:599-601
###################################################
warp.glm <- glm(sqrt(breaks) ~ wool * tension, family = gaussian(link = "log"),
                data = warpbreaks)


###################################################
### code chunk number 57: using-lsmeans.rnw:604-606
###################################################
warp.rg <- ref.grid(warp.glm)
warp.rg


###################################################
### code chunk number 58: using-lsmeans.rnw:609-612
###################################################
predict(warp.rg, type = "linear")    ### log(sqrt) scale - no back-transformation
predict(warp.rg, type = "unlink")    ### sqrt scale
predict(warp.rg, type = "response")  ### response scale


###################################################
### code chunk number 59: using-lsmeans.rnw:622-624
###################################################
Oats.Vlsm = lsmeans(Oats.lmer2, "Variety")
test(Oats.Vlsm, null = log(100), type = "response")


###################################################
### code chunk number 60: using-lsmeans.rnw:636-637
###################################################
test(Oats.Vlsm, null = log(100), delta = 0.20, type = "r")


###################################################
### code chunk number 61: using-lsmeans.rnw:644-645
###################################################
test(contrast(Oats.Vlsm, "trt.vs.ctrlk"), side = ">")


###################################################
### code chunk number 62: using-lsmeans.rnw:649-650
###################################################
test(contrast(Oats.Vlsm, "trt.vs.ctrlk"), side = "nonsup", delta = .25)


###################################################
### code chunk number 63: chick-plot
###################################################
require("lattice")
xyplot(weight ~ Time | Diet, groups = ~ Chick, data = ChickWeight, 
    type = "o", layout=c(4, 1))


###################################################
### code chunk number 64: using-lsmeans.rnw:672-674
###################################################
Chick.lmer <- lmer(sqrt(weight) ~ Diet * Time + (0 + Time | Chick), 
    data = ChickWeight)


###################################################
### code chunk number 65: using-lsmeans.rnw:677-678
###################################################
Chick.lst <- lstrends (Chick.lmer, ~ Diet, var = "Time")


###################################################
### code chunk number 66: using-lsmeans.rnw:681-682
###################################################
cld (Chick.lst)


###################################################
### code chunk number 67: using-lsmeans.rnw:687-689
###################################################
lstrends(Chick.lmer, ~ Diet | Time, var = "Time", 
    transform = "response", at = list(Time = c(5, 15)))


###################################################
### code chunk number 68: using-lsmeans.rnw:701-704
###################################################
lsm.options(ref.grid = list(level = .90),
            lsmeans = list(),
            contrast = list(infer = c(TRUE, TRUE)))


###################################################
### code chunk number 69: using-lsmeans.rnw:709-710
###################################################
get.lsm.option("estble.tol")


###################################################
### code chunk number 70: using-lsmeans.rnw:727-728
###################################################
lsmeans(Oats.lmer2, pairwise ~ Variety)


###################################################
### code chunk number 71: using-lsmeans.rnw:732-733
###################################################
lsm.options(ref.grid = NULL, contrast = NULL)


###################################################
### code chunk number 72: using-lsmeans.rnw:743-746
###################################################
nutr.lm <- lm(gain ~ (age + group + race)^2, data = nutrition)
library("car")
Anova(nutr.lm)


###################################################
### code chunk number 73: nutr-intplot
###################################################
lsmip(nutr.lm, race ~ age | group)
lsmeans(nutr.lm, ~ group*race)


###################################################
### code chunk number 74: using-lsmeans.rnw:763-765
###################################################
nutr.lsm <- lsmeans(nutr.lm, ~ group * race, weights = "proportional",
    at = list(age = c("2","3"), race = c("Black","White")))


###################################################
### code chunk number 75: using-lsmeans.rnw:768-771
###################################################
nutr.lsm    
summary(pairs(nutr.lsm, by = "race"), by = NULL)
summary(pairs(nutr.lsm, by = "group"), by = NULL)


###################################################
### code chunk number 76: using-lsmeans.rnw:784-788
###################################################
lsmeans(nutr.lm, "race", weights = "equal")
lsmeans(nutr.lm, "race", weights = "prop")
lsmeans(nutr.lm, "race", weights = "outer")
lsmeans(nutr.lm, "race", weights = "cells")


###################################################
### code chunk number 77: using-lsmeans.rnw:797-799
###################################################
temp = lsmeans(nutr.lm, c("group","race"), weights = "prop")
lsmeans(temp, "race", weights = "prop")


###################################################
### code chunk number 78: using-lsmeans.rnw:804-805
###################################################
with(nutrition, tapply(gain, race, mean))


###################################################
### code chunk number 79: using-lsmeans.rnw:816-823
###################################################
cows = data.frame (
    route = factor(rep(c("injection", "oral"), c(5, 9))),
    drug = factor(rep(c("Bovineumab", "Charloisazepam", 
              "Angustatin", "Herefordmycin", "Mollycoddle"), c(3,2,  4,2,3))),
    resp = c(34, 35, 34, 44, 43, 36, 33, 36, 32, 26, 25, 25, 24, 24)
)
cows.lm <- lm(resp ~ route + drug, data = cows)


###################################################
### code chunk number 80: using-lsmeans.rnw:826-827
###################################################
( cows.rg <- ref.grid(cows.lm) )


###################################################
### code chunk number 81: using-lsmeans.rnw:831-832
###################################################
( route.lsm <- lsmeans(cows.rg, "route") )


###################################################
### code chunk number 82: using-lsmeans.rnw:835-836
###################################################
( drug.lsm <- lsmeans(cows.rg, "drug") )


###################################################
### code chunk number 83: using-lsmeans.rnw:839-841
###################################################
pairs(route.lsm, reverse = TRUE)
pairs(drug.lsm, by = "route", reverse = TRUE)


###################################################
### code chunk number 84: using-lsmeans.rnw:846-848 (eval = FALSE)
###################################################
## lsmeans(city.model, "county", 
##         nesting = list(county = "state", city = c("county", "state")))


###################################################
### code chunk number 85: using-lsmeans.rnw:857-861
###################################################
library("mediation")
levels(framing$educ) = c("NA","Ref","< HS", "HS", "> HS","Coll +")
framing.glm = glm(cong_mesg ~ age + income + educ + emo + gender * factor(treat),
                  family = binomial, data = framing)


###################################################
### code chunk number 86: framinga
###################################################
lsmip(framing.glm, treat ~ educ | gender, type = "response")


###################################################
### code chunk number 87: framingb
###################################################
lsmip(framing.glm, treat ~ educ | gender, type = "response",
      cov.reduce = emo ~ treat*gender + age + educ + income)


###################################################
### code chunk number 88: using-lsmeans.rnw:889-891
###################################################
ref.grid(framing.glm, 
    cov.reduce = emo ~ treat*gender + age + educ + income)@grid


###################################################
### code chunk number 89: using-lsmeans.rnw:922-924 (eval = FALSE)
###################################################
## rg <- ref.grid(my.model, at = list(x1 = c(5,10,15)),
##                cov.reduce = list(x2 ~ x1,  x3 ~ x1 + x2))


###################################################
### code chunk number 90: housing-plot
###################################################
library("ordinal")
data(housing, package = "MASS")
housing.clm <- clm(Sat ~ (Infl + Type + Cont)^2,
                   data = housing, weights = Freq, link = "probit")
lsmip(housing.clm, Cont ~ Infl | Type, layout = c(4,1))


###################################################
### code chunk number 91: using-lsmeans.rnw:976-977
###################################################
test(pairs(lsmeans(housing.clm, ~ Infl | Type)), joint = TRUE)


###################################################
### code chunk number 92: using-lsmeans.rnw:980-981
###################################################
test(pairs(lsmeans(housing.clm, ~ Cont | Type)), joint = TRUE)


###################################################
### code chunk number 93: using-lsmeans.rnw:986-987
###################################################
ref.grid(housing.clm, mode = "cum.prob")


###################################################
### code chunk number 94: using-lsmeans.rnw:990-992
###################################################
lsmeans(housing.clm, ~ Infl, at = list(cut = "Medium|High"), 
        mode = "cum.prob")


###################################################
### code chunk number 95: using-lsmeans.rnw:995-997
###################################################
summary(lsmeans(housing.clm, ~ Infl, at = list(cut = "Medium|High"), 
                mode = "linear.predictor"), type = "response")


###################################################
### code chunk number 96: using-lsmeans.rnw:1005-1013
###################################################
require("nlme")
options(contrasts = c("contr.treatment", "contr.poly"))
Chick.nlme = nlme(weight ~ SSlogis(Time, asym, xmid, scal), 
    data = ChickWeight,
    fixed = list(asym + xmid ~ Diet, scal ~ 1),
    random = asym ~ 1 | Chick, 
    start = c(200, 100, 200, 100,   10, 0, 0, 0,   7))
Chick.nlme


###################################################
### code chunk number 97: using-lsmeans.rnw:1016-1018
###################################################
cld(lsmeans(Chick.nlme, ~ Diet, param = "asym"))    
cld(lsmeans(Chick.nlme, ~ Diet, param = "xmid"))    


###################################################
### code chunk number 98: using-lsmeans.rnw:1030-1035
###################################################
library("MCMCpack")
counts <- c(18, 17, 15,   20, 10, 20,   25, 13, 12)
outcome <- gl(3, 1, 9)
treatment <- gl(3, 3)
posterior <- MCMCpoisson(counts ~ outcome + treatment, mcmc = 1000)


###################################################
### code chunk number 99: using-lsmeans.rnw:1038-1039
###################################################
( post.lsm <- lsmeans(posterior, "treatment") )


###################################################
### code chunk number 100: using-lsmeans.rnw:1042-1044
###################################################
library("coda")
summary(as.mcmc(post.lsm))


