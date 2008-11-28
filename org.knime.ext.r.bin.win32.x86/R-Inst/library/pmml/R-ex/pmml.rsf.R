### Name: pmml.rsf
### Title: Generate PMML for a Random Survival Forest (rsf) object
### Aliases: pmml.rsf
### Keywords: interface survival tree

### ** Examples

library(randomSurvivalForest)
data(veteran, package = "randomSurvivalForest")
veteran.out <- rsf(Survrsf(time, status)~.,
        data = veteran,
        ntree = 5,
        forest = TRUE)
veteran.forest <- veteran.out$forest
pmml(veteran.forest)



