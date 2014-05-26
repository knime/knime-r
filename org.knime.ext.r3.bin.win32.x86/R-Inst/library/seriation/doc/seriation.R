### R code from vignette source 'seriation.Rnw'
### Encoding: UTF-8

###################################################
### code chunk number 1: seriation.Rnw:120-123
###################################################
options(scipen=3, digits=4)
### for sampling
set.seed(1234)


###################################################
### code chunk number 2: seriation.Rnw:930-936
###################################################
library("seriation")

data("iris")
x <- as.matrix(iris[-5])
x <- x[sample(seq_len(nrow(x))),]
d <- dist(x)


###################################################
### code chunk number 3: seriation.Rnw:942-944
###################################################
o <- seriate(d)
o


###################################################
### code chunk number 4: seriation.Rnw:955-956
###################################################
head(get_order(o), 15)


###################################################
### code chunk number 5: pimage1
###################################################
pimage(d, main = "Random", colorkey=TRUE)


###################################################
### code chunk number 6: pimage1-2
###################################################
pimage(d, o, main = "Reordered", colorkey=TRUE)


###################################################
### code chunk number 7: seriation.Rnw:981-982
###################################################
cbind(random = criterion(d), reordered = criterion(d, o))


###################################################
### code chunk number 8: pimage2
###################################################
library("colorspace")
pimage(scale(x), main = "Random", colorkey=TRUE, 
    col=diverge_hcl(100), range=c(-3.5,3.5))


###################################################
### code chunk number 9: pimage2-2
###################################################
o_2mode <- c(o, ser_permutation(seq_len(ncol(x))))
#o_2mode
pimage(scale(x), o_2mode, main = "Reordered", 
    colorkey=TRUE, col=diverge_hcl(100), range=c(-3.5,3.5))


###################################################
### code chunk number 10: seriation.Rnw:1034-1036
###################################################
methods <- c("TSP","Chen", "ARSA", "HC", "GW", "OLO")
o <- sapply(methods, FUN = function(m) seriate(d, m), simplify = FALSE)


###################################################
### code chunk number 11: seriation.Rnw:1039-1041
###################################################
timing <- sapply(methods, FUN = function(m) system.time(seriate(d, m)), 
    simplify = FALSE)


###################################################
### code chunk number 12: pimage3-pre (eval = FALSE)
###################################################
## tmp <- lapply(o, FUN = function(x) pimage(d, x, main = get_method(x[[1]])))


###################################################
### code chunk number 13: pimage3
###################################################
for(i in 1:length(o)) {
    pdf(file=paste("seriation-pimage_comp_", i , ".pdf", sep=""))
    pimage(d, o[[i]], main = get_method(o[[i]][[1]]))
    dev.off()
}



###################################################
### code chunk number 14: pimage3_dend (eval = FALSE)
###################################################
## plot(o[["HC"]][[1]], labels = FALSE, main = "Dendrogram HC")
## plot(o[["GW"]][[1]], labels = FALSE, main = "Dendrogram GW")


###################################################
### code chunk number 15: seriation.Rnw:1135-1147
###################################################
def.par <- par(no.readonly = TRUE)
pdf(file="seriation-pimage3_dendrogram.pdf", width=9, height=4) 
layout(t(1:2))
plot(o[["HC"]][[1]], labels = FALSE, main = "Dendrogram HC")
symbols(74.7,.5, rect = matrix(c(4, 3), ncol=2), add= TRUE, 
    inches = FALSE, lwd =2)

plot(o[["GW"]][[1]], labels = FALSE, main = "Dendrogram GW")
symbols(98.7,.5, rect = matrix(c(4, 3), ncol=2), add= TRUE, 
    inches = FALSE, lwd =2)
par(def.par)
tmp <- dev.off()


###################################################
### code chunk number 16: seriation.Rnw:1165-1167
###################################################
crit <- sapply(o, FUN = function(x) criterion(d, x))
t(crit)


###################################################
### code chunk number 17: crit1
###################################################
def.par <- par(no.readonly = TRUE)
m <- c("Path_length", "AR_events", "Moore_stress")
layout(matrix(seq_along(m), ncol=1))
#tmp <- apply(crit[m,], 1, dotchart, sub = m)
tmp <- lapply(m, FUN = function(i) dotchart(crit[i,], sub = i))
par(def.par)


###################################################
### code chunk number 18: seriation.Rnw:1209-1211
###################################################
show_seriation_methods("dist")
show_seriation_methods("matrix")


###################################################
### code chunk number 19: seriation.Rnw:1218-1219
###################################################
list_seriation_methods("matrix")


###################################################
### code chunk number 20: seriation.Rnw:1235-1238
###################################################
seriation_method_identity <- function(x, control) {
           lapply(dim(x), seq)
}


###################################################
### code chunk number 21: seriation.Rnw:1246-1251
###################################################
set_seriation_method("matrix", "identity", seriation_method_identity,
    "Identity order")

set_seriation_method("array", "identity", seriation_method_identity,
    "Identity order")


###################################################
### code chunk number 22: seriation.Rnw:1256-1263
###################################################
show_seriation_methods("matrix")

o <- seriate(matrix(1, ncol=3, nrow=4), "identity")
o

get_order(o, 1)
get_order(o, 2)


###################################################
### code chunk number 23: seriation.Rnw:1297-1298
###################################################
x <- scale(x, center = FALSE)


###################################################
### code chunk number 24: seriation.Rnw:1304-1305 (eval = FALSE)
###################################################
## hmap(x)


###################################################
### code chunk number 25: seriation.Rnw:1315-1316 (eval = FALSE)
###################################################
## hmap(x, hclustfun = NULL)


###################################################
### code chunk number 26: seriation.Rnw:1326-1331
###################################################
#bitmap(file = "seriation-heatmap1.png", type = "pnggray", 
#    height = 6, width = 6, res = 300, pointsize=14)
pdf(file = "seriation-heatmap1.pdf")
hmap(x, cexCol=1, labRow = "", margin =c(7,7))
tmp <- dev.off()


###################################################
### code chunk number 27: seriation.Rnw:1333-1336
###################################################
pdf(file = "seriation-heatmap2.pdf")
hmap(x, hclustfun = NULL)
tmp <- dev.off()


###################################################
### code chunk number 28: seriation.Rnw:1402-1404
###################################################
data("Irish")
orig_matrix <- apply(Irish[,-6], 2, rank)


###################################################
### code chunk number 29: seriation.Rnw:1414-1419
###################################################
o <- c(
    seriate(dist(orig_matrix, "minkowski", p = 1), method ="TSP"),
    seriate(dist(t(orig_matrix), "minkowski", p = 1), method = "TSP")
)
o


###################################################
### code chunk number 30: seriation.Rnw:1424-1426 (eval = FALSE)
###################################################
## bertinplot(orig_matrix)
## bertinplot(orig_matrix, o)


###################################################
### code chunk number 31: bertin1
###################################################
bertinplot(orig_matrix)


###################################################
### code chunk number 32: bertin2
###################################################
bertinplot(orig_matrix, o)


###################################################
### code chunk number 33: binary1
###################################################
data("Townships")

bertinplot(Townships, options = list(panel=panel.squares, spacing = 0, 
    frame = TRUE))


###################################################
### code chunk number 34: seriation.Rnw:1504-1506
###################################################
## to get consistent results
set.seed(5)


###################################################
### code chunk number 35: binary2
###################################################
o <- seriate(Townships, method = "BEA", control = list(rep = 10))
bertinplot(Townships, o, options = list(panel=panel.squares, spacing = 0,
    frame = TRUE))


###################################################
### code chunk number 36: seriation.Rnw:1546-1547
###################################################
rbind(original = criterion(Townships), reordered = criterion(Townships, o))


###################################################
### code chunk number 37: seriation.Rnw:1614-1617
###################################################
data("iris")
iris <- iris[sample(seq_len(nrow(iris))),]
d <- dist(as.matrix(iris[-5]), method = "euclidean")


###################################################
### code chunk number 38: dissplot1 (eval = FALSE)
###################################################
## ## plot original matrix
## dissplot(d, method = NA)


###################################################
### code chunk number 39: dissplot2 (eval = FALSE)
###################################################
## ## plot reordered matrix
## dissplot(d, options = list(main = "Dissimilarity plot with seriation"))


###################################################
### code chunk number 40: seriation.Rnw:1639-1645
###################################################
pdf(file = "seriation-dissplot1.pdf")
## plot original matrix
dissplot(d, method = NA)
tmp <- dev.off()
pdf(file = "seriation-dissplot2.pdf")
## plot reordered matrix
dissplot(d, options = list(main = "Dissimilarity plot with seriation"))
tmp <- dev.off()


###################################################
### code chunk number 41: seriation.Rnw:1672-1673
###################################################
set.seed(1234)


###################################################
### code chunk number 42: seriation.Rnw:1675-1677
###################################################
l <- kmeans(d, 10)$cluster
#$


###################################################
### code chunk number 43: dissplot3 (eval = FALSE)
###################################################
## res <- dissplot(d, labels = l,
##     options = list(main = "Dissimilarity plot - standard"))


###################################################
### code chunk number 44: seriation.Rnw:1690-1703
###################################################
pdf(file = "seriation-dissplot3.pdf")

## visualize the clustering
res <- dissplot(d, labels = l,
    options = list(main = "Dissimilarity plot - standard"))
tmp <- dev.off()


pdf(file = "seriation-dissplot4.pdf")
## threshold
plot(res, options = list(main = "Dissimilarity plot - threshold", 
    threshold = 1.5))

tmp <- dev.off()


###################################################
### code chunk number 45: seriation.Rnw:1718-1719
###################################################
res


###################################################
### code chunk number 46: seriation.Rnw:1738-1740 (eval = FALSE)
###################################################
## plot(res, options = list(main = "Seriation - threshold",
##     threshold = 1.5))


###################################################
### code chunk number 47: seriation.Rnw:1754-1757
###################################################
#names(res)
table(iris[res$order, 5], res$label)[,res$cluster_order]
#$


###################################################
### code chunk number 48: ruspini
###################################################
data("ruspini", package="cluster")
d <- dist(ruspini)
l <- kmeans(d, 3)$cluster
dissplot(d, labels = l)


