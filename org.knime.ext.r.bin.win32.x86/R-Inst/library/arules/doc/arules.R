###################################################
### chunk number 1: 
###################################################
#line 29 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
options(width = 75)
### for sampling
set.seed <- 1234


###################################################
### chunk number 2: 
###################################################
#line 1076 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
library("arules")


###################################################
### chunk number 3: epub1
###################################################
#line 1080 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
data("Epub")
Epub


###################################################
### chunk number 4: epub2
###################################################
#line 1091 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
summary(Epub)


###################################################
### chunk number 5: 
###################################################
#line 1102 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
year <- strftime(as.POSIXlt(transactionInfo(Epub)[["TimeStamp"]]), "%Y")
table(year)


###################################################
### chunk number 6: 
###################################################
#line 1112 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
Epub2003 <- Epub[year == "2003"]
length(Epub2003)
image(Epub2003)


###################################################
### chunk number 7: epub
###################################################
#line 1119 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
print(image(Epub2003))


###################################################
### chunk number 8: 
###################################################
#line 1141 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
transactionInfo(Epub2003[size(Epub2003) > 20])


###################################################
### chunk number 9: 
###################################################
#line 1154 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
inspect(Epub2003[1:5])


###################################################
### chunk number 10: 
###################################################
#line 1161 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
as(Epub2003[1:5], "list")


###################################################
### chunk number 11: 
###################################################
#line 1168 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
EpubTidLists <- as(Epub, "tidLists")
EpubTidLists


###################################################
### chunk number 12: 
###################################################
#line 1177 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
as(EpubTidLists[1:3], "list") 


###################################################
### chunk number 13: data
###################################################
#line 1207 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
data("AdultUCI")
dim(AdultUCI)
AdultUCI[1:2,]


###################################################
### chunk number 14: 
###################################################
#line 1224 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
AdultUCI[["fnlwgt"]] <- NULL
AdultUCI[["education-num"]] <- NULL


###################################################
### chunk number 15: 
###################################################
#line 1242 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
AdultUCI[[ "age"]] <- ordered(cut(AdultUCI[[ "age"]], c(15,25,45,65,100)),
    labels = c("Young", "Middle-aged", "Senior", "Old"))

AdultUCI[[ "hours-per-week"]] <- ordered(cut(AdultUCI[[ "hours-per-week"]],
      c(0,25,40,60,168)),
    labels = c("Part-time", "Full-time", "Over-time", "Workaholic"))
			    
AdultUCI[[ "capital-gain"]] <- ordered(cut(AdultUCI[[ "capital-gain"]],
      c(-Inf,0,median(AdultUCI[[ "capital-gain"]][AdultUCI[[ "capital-gain"]]>0]),Inf)),
    labels = c("None", "Low", "High"))

AdultUCI[[ "capital-loss"]] <- ordered(cut(AdultUCI[[ "capital-loss"]],
      c(-Inf,0,
	median(AdultUCI[[ "capital-loss"]][AdultUCI[[ "capital-loss"]]>0]),Inf)),
    labels = c("none", "low", "high"))


###################################################
### chunk number 16: coerce
###################################################
#line 1264 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
Adult <- as(AdultUCI, "transactions")
Adult


###################################################
### chunk number 17: summary
###################################################
#line 1277 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
summary(Adult)


###################################################
### chunk number 18: itemFrequencyPlot eval=FALSE
###################################################
## #line 1294 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
## itemFrequencyPlot(Adult, support = 0.1, cex.names=0.8)


###################################################
### chunk number 19: 
###################################################
#line 1299 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
#line 1294 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw#from line#1299#"
itemFrequencyPlot(Adult, support = 0.1, cex.names=0.8)
#line 1300 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"


###################################################
### chunk number 20: apriori
###################################################
#line 1310 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
rules <- apriori(Adult, 
                 parameter = list(support = 0.01, confidence = 0.6))
rules


###################################################
### chunk number 21: summary
###################################################
#line 1340 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
summary(rules)


###################################################
### chunk number 22: rules
###################################################
#line 1350 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
rulesIncomeSmall <- subset(rules, subset = rhs %in% "income=small" & lift > 1.2)
rulesIncomeLarge <- subset(rules, subset = rhs %in% "income=large" & lift > 1.2)


###################################################
### chunk number 23: subset
###################################################
#line 1360 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
inspect(head(SORT(rulesIncomeSmall, by = "confidence"), n = 3))
inspect(head(SORT(rulesIncomeLarge, by = "confidence"), n = 3))


###################################################
### chunk number 24: write_rules eval=FALSE
###################################################
## #line 1380 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
## WRITE(rulesIncomeSmall, file = "data.csv", sep = ",", col.names = NA)


###################################################
### chunk number 25: pmml eval=FALSE
###################################################
## #line 1390 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
## library("pmml")
## rules_pmml <- pmml(rulesIncomeSmall)
## saveXML(rules_pmml, file = "data.xml")


###################################################
### chunk number 26: 
###################################################
#line 1425 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
data("Adult")
fsets <- eclat(Adult, parameter = list(support = 0.05), 
	control = list(verbose=FALSE))


###################################################
### chunk number 27: 
###################################################
#line 1436 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
singleItems <- fsets[size(items(fsets)) == 1]

## Get the col numbers we have support for
singleSupport <- quality(singleItems)$support
names(singleSupport) <- unlist(LIST(items(singleItems),
	    decode = FALSE))
head(singleSupport, n = 5)


###################################################
### chunk number 28: 
###################################################
#line 1452 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
itemsetList <- LIST(items(fsets), decode = FALSE)

allConfidence <- quality(fsets)$support / 
    sapply(itemsetList, function(x) 
    max(singleSupport[as.character(x)]))

quality(fsets) <- cbind(quality(fsets), allConfidence)


###################################################
### chunk number 29: 
###################################################
#line 1463 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
summary(fsets)


###################################################
### chunk number 30: 
###################################################
#line 1472 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
fsetsEducation <- subset(fsets, subset = items %pin% "education")
inspect(SORT(fsetsEducation[size(fsetsEducation)>1], 
	by = "allConfidence")[1 : 3])


###################################################
### chunk number 31: 
###################################################
#line 1488 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
data("Adult")
Adult


###################################################
### chunk number 32: 
###################################################
#line 1499 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
supp <- 0.05
epsilon <- 0.1
c <- 0.1

n <- -2 * log(c)/ (supp * epsilon^2)
n


###################################################
### chunk number 33: 
###################################################
#line 1514 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
AdultSample <- sample(Adult, n, replace = TRUE)


###################################################
### chunk number 34: itemFrequencyPlot2 eval=FALSE
###################################################
## #line 1527 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
## itemFrequencyPlot(AdultSample, population = Adult, support = supp,
##     cex.names = 0.7)


###################################################
### chunk number 35: 
###################################################
#line 1533 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
#line 1527 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw#from line#1533#"
itemFrequencyPlot(AdultSample, population = Adult, support = supp,
    cex.names = 0.7)
#line 1534 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"


###################################################
### chunk number 36: itemFrequencyPlot3 eval=FALSE
###################################################
## #line 1555 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
## itemFrequencyPlot(AdultSample, population = Adult, 
##     support = supp, lift = TRUE, 
##     cex.names = 0.9)


###################################################
### chunk number 37: 
###################################################
#line 1562 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
#line 1555 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw#from line#1562#"
itemFrequencyPlot(AdultSample, population = Adult, 
    support = supp, lift = TRUE, 
    cex.names = 0.9)
#line 1563 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"


###################################################
### chunk number 38: 
###################################################
#line 1574 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
time <- system.time(itemsets <- eclat(Adult, 
    parameter = list(support = supp), control = list(verbose = FALSE)))
time

timeSample <- system.time(itemsetsSample <- eclat(AdultSample, 
    parameter = list(support = supp), control = list(verbose = FALSE)))
timeSample


###################################################
### chunk number 39: 
###################################################
#line 1590 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
# speed up
time[1] / timeSample[1]


###################################################
### chunk number 40: 
###################################################
#line 1600 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
itemsets
itemsetsSample


###################################################
### chunk number 41: 
###################################################
#line 1610 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
match <- match(itemsets, itemsetsSample, nomatch = 0)
## remove no matches
sum(match>0) / length(itemsets)


###################################################
### chunk number 42: 
###################################################
#line 1622 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
summary(quality(itemsets[which(!match)])$support)
summary(quality(itemsetsSample[-match])$support)


###################################################
### chunk number 43: 
###################################################
#line 1633 "d:/Rcompile/CRANpkg/local/2.12/arules/inst/doc/arules.Rnw"
supportItemsets <- quality(itemsets[which(match > 0)])$support
supportSample <- quality(itemsetsSample[match])$support

accuracy <- 1 - abs(supportSample - supportItemsets) / supportItemsets

summary(accuracy)


