### R code from vignette source 'intro_sp.Rnw'

###################################################
### code chunk number 1: intro_sp.Rnw:72-73
###################################################
library(sp)


###################################################
### code chunk number 2: intro_sp.Rnw:76-78
###################################################
set.seed(13331)
# library(lattice)


###################################################
### code chunk number 3: intro_sp.Rnw:179-183
###################################################
xc = round(runif(10), 2)
yc = round(runif(10), 2)
xy = cbind(xc, yc)
xy


###################################################
### code chunk number 4: intro_sp.Rnw:188-191
###################################################
xy.sp = SpatialPoints(xy)
xy.sp
plot(xy.sp, pch = 2)


###################################################
### code chunk number 5: intro_sp.Rnw:197-198
###################################################
plot(xy.sp, pch = 2)


###################################################
### code chunk number 6: intro_sp.Rnw:206-209
###################################################
xy.cc = coordinates(xy.sp)
class(xy.cc)
dim(xy.cc)


###################################################
### code chunk number 7: intro_sp.Rnw:213-220
###################################################
bbox(xy.sp)
dimensions(xy.sp)
xy.sp[1:2]
xy.df = as.data.frame(xy.sp)
class(xy.df)
dim(xy.df)
summary(xy.sp)


###################################################
### code chunk number 8: intro_sp.Rnw:228-242
###################################################
df = data.frame(z1 = round(5 + rnorm(10), 2), z2 = 20:29)
df
xy.spdf = SpatialPointsDataFrame(xy.sp, df)
xy.spdf
summary(xy.spdf)
dimensions(xy.spdf)
xy.spdf[1:2, ] # selects row 1 and 2
xy.spdf[1] # selects attribute column 1, along with the coordinates
xy.spdf[1:2, "z2"] # select row 1,2 and attribute "z2"
xy.df = as.data.frame(xy.spdf)
xy.df[1:2,]
xy.cc = coordinates(xy.spdf)
class(xy.cc)
dim(xy.cc)


###################################################
### code chunk number 9: intro_sp.Rnw:253-256
###################################################
df1 = data.frame(xy, df)
coordinates(df1) = c("xc", "yc")
df1


###################################################
### code chunk number 10: intro_sp.Rnw:259-263
###################################################
df2 = data.frame(xy, df)
coordinates(df2) = ~xc+yc
df2[1:2,]
as.data.frame(df2)[1:2,]


###################################################
### code chunk number 11: intro_sp.Rnw:271-272
###################################################
coordinates(df2)[1:2,]


###################################################
### code chunk number 12: intro_sp.Rnw:276-280
###################################################
df2[["z2"]]
df2[["z2"]][10] = 20
df2[["z3"]] = 1:10
summary(df2)


###################################################
### code chunk number 13: intro_sp.Rnw:284-286 (eval = FALSE)
###################################################
## bubble(df2, "z1", key.space = "bottom")
## spplot(df2, "z1", key.space = "bottom")


###################################################
### code chunk number 14: intro_sp.Rnw:292-294
###################################################
print(bubble(df2, "z1", key.space = "bottom"), split = c(1,1,2,1), more=TRUE)
print(spplot(df2, "z1", key.space = "bottom"), split =  c(2,1,2,1), more=FALSE)


###################################################
### code chunk number 15: intro_sp.Rnw:313-316
###################################################
gt = GridTopology(cellcentre.offset = c(1,1,2), cellsize=c(1,1,1), cells.dim = c(3,4,6))
grd = SpatialGrid(gt)
summary(grd)


###################################################
### code chunk number 16: intro_sp.Rnw:320-321
###################################################
gridparameters(grd)


###################################################
### code chunk number 17: intro_sp.Rnw:327-332
###################################################
pts = expand.grid(x = 1:3, y = 1:4, z=2:7)
grd.pts = SpatialPixels(SpatialPoints(pts))
summary(grd.pts)
grd = as(grd.pts, "SpatialGrid")
summary(grd)


###################################################
### code chunk number 18: intro_sp.Rnw:344-351
###################################################
attr = expand.grid(xc = 1:3, yc = 1:3)
grd.attr = data.frame(attr, z1 = 1:9, z2 = 9:1)
coordinates(grd.attr) = ~xc+yc
gridded(grd.attr)
gridded(grd.attr) = TRUE
gridded(grd.attr)
summary(grd.attr)


###################################################
### code chunk number 19: intro_sp.Rnw:360-367
###################################################
fullgrid(grd)
fullgrid(grd.pts)
fullgrid(grd.attr)
fullgrid(grd.pts) = TRUE
fullgrid(grd.attr) = TRUE
fullgrid(grd.pts)
fullgrid(grd.attr)


###################################################
### code chunk number 20: intro_sp.Rnw:401-405
###################################################
fullgrid(grd.attr) = FALSE
grd.attr[1:5, "z1"]
fullgrid(grd.attr) = TRUE
grd.attr[1:2,-2, c("z2","z1")]


###################################################
### code chunk number 21: intro_sp.Rnw:416-427
###################################################
l1 = cbind(c(1,2,3),c(3,2,2))
l1a = cbind(l1[,1]+.05,l1[,2]+.05)
l2 = cbind(c(1,2,3),c(1,1.5,1))
Sl1 = Line(l1)
Sl1a = Line(l1a)
Sl2 = Line(l2)
S1 = Lines(list(Sl1, Sl1a), ID="a")
S2 = Lines(list(Sl2), ID="b")
Sl = SpatialLines(list(S1,S2))
summary(Sl)
plot(Sl, col = c("red", "blue"))


###################################################
### code chunk number 22: intro_sp.Rnw:435-438
###################################################
df = data.frame(z = c(1,2), row.names=sapply(slot(Sl, "lines"), function(x) slot(x, "ID")))
Sldf = SpatialLinesDataFrame(Sl, data = df)
summary(Sldf)


###################################################
### code chunk number 23: intro_sp.Rnw:450-461
###################################################
Sr1 = Polygon(cbind(c(2,4,4,1,2),c(2,3,5,4,2)))
Sr2 = Polygon(cbind(c(5,4,2,5),c(2,3,2,2)))
Sr3 = Polygon(cbind(c(4,4,5,10,4),c(5,3,2,5,5)))
Sr4 = Polygon(cbind(c(5,6,6,5,5),c(4,4,3,3,4)), hole = TRUE)

Srs1 = Polygons(list(Sr1), "s1")
Srs2 = Polygons(list(Sr2), "s2")
Srs3 = Polygons(list(Sr3, Sr4), "s3/4")
SpP = SpatialPolygons(list(Srs1,Srs2,Srs3), 1:3)
plot(SpP, col = 1:3, pbg="white")
# plot(SpP)


###################################################
### code chunk number 24: intro_sp.Rnw:468-472
###################################################
attr = data.frame(a=1:3, b=3:1, row.names=c("s3/4", "s2", "s1"))
SrDf = SpatialPolygonsDataFrame(SpP, attr)
as(SrDf, "data.frame")
spplot(SrDf)


###################################################
### code chunk number 25: intro_sp.Rnw:475-476
###################################################
print(spplot(SrDf))


###################################################
### code chunk number 26: intro_sp.Rnw:480-482
###################################################
SrDf = attr
polygons(SrDf) = SpP


