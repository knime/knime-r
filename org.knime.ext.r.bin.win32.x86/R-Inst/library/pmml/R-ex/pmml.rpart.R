### Name: pmml.rpart
### Title: Generate PMML for an rpart object
### Aliases: pmml.rpart
### Keywords: interface tree

### ** Examples

library(rpart)
fit <- rpart(Kyphosis ~ Age + Number + Start, data=kyphosis)
pmml(fit)



