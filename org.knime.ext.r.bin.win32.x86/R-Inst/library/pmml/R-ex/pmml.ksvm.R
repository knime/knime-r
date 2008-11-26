### Name: pmml.ksvm
### Title: Generate PMML for a ksvm object
### Aliases: pmml.ksvm
### Keywords: interface

### ** Examples

# Train a support vector machine to perform binary classification.
require(kernlab)
data(spam)
index <- sample(1:dim(spam)[1])
ds <- spam[index[1:300],] # For illustration only use a small dataset.
fit <- ksvm(type~., data=ds, kenrel="rbfdot")

# Genetate the PMML.
pmml(fit, data=ds)



