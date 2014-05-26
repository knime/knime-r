### R code from vignette source 'margintable.Rnw'

###################################################
### code chunk number 1: margintable.Rnw:52-55
###################################################
library(xtable)
x <- matrix(rnorm(6), ncol = 2)
x.small <- xtable(x, label = 'tabsmall', caption = 'A margin table')


###################################################
### code chunk number 2: margintable.Rnw:58-61
###################################################
print(x.small,floating.environment='margintable',
      latex.environments = "",
      table.placement = NULL)


