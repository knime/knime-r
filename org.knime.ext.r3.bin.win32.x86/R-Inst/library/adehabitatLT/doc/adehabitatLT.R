### R code from vignette source 'adehabitatLT.Rnw'

###################################################
### code chunk number 1: adehabitatLT.Rnw:29-36
###################################################
owidth <- getOption("width")
options("width"=80)
ow <- getOption("warn")
options("warn"=-1)
.PngNo <- 0
wi <- 600
pt <- 15


###################################################
### code chunk number 2: afig (eval = FALSE)
###################################################
## .PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
##           .PngNo, ".png", sep="")
## png(file=file, width = wi, height = wi, pointsize = pt)


###################################################
### code chunk number 3: zfig (eval = FALSE)
###################################################
## dev.null <- dev.off()
## cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 4: zfigg (eval = FALSE)
###################################################
## dev.null <- dev.off()
## cat("\\includegraphics[height=14cm,width=14cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 5: adehabitatLT.Rnw:105-106
###################################################
library(adehabitatLT)


###################################################
### code chunk number 6: adehabitatLT.Rnw:109-110
###################################################
set.seed(13431)


###################################################
### code chunk number 7: ploltraj (eval = FALSE)
###################################################
## par(mar=c(0.1,0.1,0.1,0.1))
## plot(c(0,1), c(0,1), ty="n", axes=FALSE)
## x <- c(0.2, 0.3, 0.5, 0.7, 0.4)
## y <- c(0.5, 0.3, 0.35, 0.6, 0.9)
## points(x,y, pch=16, cex=2)
## points(x[1],y[1], pch=16, col="green")
## lines(x,y, lty=1)
## arrows(0.4, 0.9, 0.1, 0.8, lty=2, length=0.1)
## lines(c(0.5, 0.7),c(0.35, 0.6), lwd=6)
## lines(c(0.5, 0.7),c(0.35, 0.6), lwd=2, col="red")
## 
## ## dx
## arrows(0.5, 0.32, 0.7, 0.32, code=3, length=0.15)
## text(0.6, 0.3, "dx")
## 
## ## dy
## arrows(0.75, 0.35, 0.75, 0.6, code=3, length=0.15)
## text(0.77, 0.5, "dy")
## 
## ## abs.angle
## ang <- atan2(0.25, 0.2)
## ang <- seq(0,ang, length=10)
## xa <- 0.05*cos(ang) + 0.5
## ya <- 0.05*sin(ang) + 0.35
## lines(c(0.5, 0.7),c(0.35, 0.35), lty=3, col="red")
## lines(xa, ya, col="red", lwd=2)
## text(0.56, 0.38, expression(alpha), col="red")
## 
## ## rel.angle
## lines(c(0.3, 0.5),c(0.3, 0.35), col="blue", lwd=2)
## lines(c(0.5, 0.7),c(0.35, 0.4), lty=3, col="blue")
## 
## ang1 <- atan2(0.25, 0.2)
## ang0 <- atan2(0.05, 0.2)
## ang <- ang0+seq(0, ang1-ang0, length=10)
## xa <- 0.1*cos(ang) + 0.5
## ya <- 0.1*sin(ang) + 0.35
## lines(xa, ya, col="blue", lwd=2)
## xa <- 0.107*cos(ang) + 0.5
## ya <- 0.107*sin(ang) + 0.35
## lines(xa, ya, col="blue", lwd=2)
## text(0.61, 0.43,expression(beta), col="blue")
## 
## ## dist
## arrows(0.48, 0.37, 0.68, 0.62, code=3, length=0.1)
## text(0.53, 0.5, "dist, dt")
## 
## ## R2n
## arrows(0.21, 0.5, 0.49, 0.35, col="darkgreen", code=3, length=0.1)
## text(0.35, 0.49, expression(R[n]^2))
## 
## ## The legend:
## text(0.2, 0.2, expression(paste(alpha, ": abs.angle")), col="red")
## text(0.2, 0.17, expression(paste(beta, ": rel.angle")), col="blue")
## 
## ## x0, y0, t0
## text(0.14, 0.5, expression(paste(x[0], ", ", y[0], ", ", t[0])),
##      col="darkgreen")
## 
## box()


###################################################
### code chunk number 8: adehabitatLT.Rnw:238-241
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
par(mar=c(0.1,0.1,0.1,0.1))
plot(c(0,1), c(0,1), ty="n", axes=FALSE)
x <- c(0.2, 0.3, 0.5, 0.7, 0.4)
y <- c(0.5, 0.3, 0.35, 0.6, 0.9)
points(x,y, pch=16, cex=2)
points(x[1],y[1], pch=16, col="green")
lines(x,y, lty=1)
arrows(0.4, 0.9, 0.1, 0.8, lty=2, length=0.1)
lines(c(0.5, 0.7),c(0.35, 0.6), lwd=6)
lines(c(0.5, 0.7),c(0.35, 0.6), lwd=2, col="red")

## dx
arrows(0.5, 0.32, 0.7, 0.32, code=3, length=0.15)
text(0.6, 0.3, "dx")

## dy
arrows(0.75, 0.35, 0.75, 0.6, code=3, length=0.15)
text(0.77, 0.5, "dy")

## abs.angle
ang <- atan2(0.25, 0.2)
ang <- seq(0,ang, length=10)
xa <- 0.05*cos(ang) + 0.5
ya <- 0.05*sin(ang) + 0.35
lines(c(0.5, 0.7),c(0.35, 0.35), lty=3, col="red")
lines(xa, ya, col="red", lwd=2)
text(0.56, 0.38, expression(alpha), col="red")

## rel.angle
lines(c(0.3, 0.5),c(0.3, 0.35), col="blue", lwd=2)
lines(c(0.5, 0.7),c(0.35, 0.4), lty=3, col="blue")

ang1 <- atan2(0.25, 0.2)
ang0 <- atan2(0.05, 0.2)
ang <- ang0+seq(0, ang1-ang0, length=10)
xa <- 0.1*cos(ang) + 0.5
ya <- 0.1*sin(ang) + 0.35
lines(xa, ya, col="blue", lwd=2)
xa <- 0.107*cos(ang) + 0.5
ya <- 0.107*sin(ang) + 0.35
lines(xa, ya, col="blue", lwd=2)
text(0.61, 0.43,expression(beta), col="blue")

## dist
arrows(0.48, 0.37, 0.68, 0.62, code=3, length=0.1)
text(0.53, 0.5, "dist, dt")

## R2n
arrows(0.21, 0.5, 0.49, 0.35, col="darkgreen", code=3, length=0.1)
text(0.35, 0.49, expression(R[n]^2))

## The legend:
text(0.2, 0.2, expression(paste(alpha, ": abs.angle")), col="red")
text(0.2, 0.17, expression(paste(beta, ": rel.angle")), col="blue")

## x0, y0, t0
text(0.14, 0.5, expression(paste(x[0], ", ", y[0], ", ", t[0])),
     col="darkgreen")

box()
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 9: adehabitatLT.Rnw:290-294
###################################################
data(puechabonsp)
locs <- puechabonsp$relocs
locs <- as.data.frame(locs)
head(locs)


###################################################
### code chunk number 10: adehabitatLT.Rnw:323-326
###################################################
da <- as.character(locs$Date)
head(da)
da <- as.POSIXct(strptime(as.character(locs$Date),"%y%m%d", tz="Europe/Paris"))


###################################################
### code chunk number 11: adehabitatLT.Rnw:332-334
###################################################
puech <- as.ltraj(xy = locs[,c("X","Y")], date = da, id = locs$Name)
puech


###################################################
### code chunk number 12: adehabitatLT.Rnw:345-346
###################################################
head(puech[[1]])


###################################################
### code chunk number 13: plottrajjj (eval = FALSE)
###################################################
## plot(puech)


###################################################
### code chunk number 14: adehabitatLT.Rnw:361-362 (eval = FALSE)
###################################################
## plot(puech)


###################################################
### code chunk number 15: adehabitatLT.Rnw:366-369
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
plot(puech)
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 16: adehabitatLT.Rnw:402-403
###################################################
head(puech[[1]])


###################################################
### code chunk number 17: adehabitatLT.Rnw:409-412
###################################################
puech2 <- puech
puech2[[1]][2,1] <- 700146
head(puech2[[1]])


###################################################
### code chunk number 18: adehabitatLT.Rnw:419-420
###################################################
head(rec(puech2)[[1]])


###################################################
### code chunk number 19: adehabitatLT.Rnw:438-440
###################################################
puech2 <- ld(puech)
head(puech2)


###################################################
### code chunk number 20: adehabitatLT.Rnw:448-449
###################################################
dl(puech2)


###################################################
### code chunk number 21: adehabitatLT.Rnw:465-466
###################################################
is.regular(puech)


###################################################
### code chunk number 22: plotdtltr (eval = FALSE)
###################################################
## plotltr(puech, "dt/3600/24")


###################################################
### code chunk number 23: adehabitatLT.Rnw:484-485 (eval = FALSE)
###################################################
## plotltr(puech, "dt/3600/24")


###################################################
### code chunk number 24: adehabitatLT.Rnw:489-492
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
plotltr(puech, "dt/3600/24")
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 25: adehabitatLT.Rnw:503-506
###################################################
foo <- function(dt) {
    return(dt> (100*3600*24))
}


###################################################
### code chunk number 26: adehabitatLT.Rnw:514-516
###################################################
puech2 <- cutltraj(puech, "foo(dt)", nextr = TRUE)
puech2


###################################################
### code chunk number 27: adehabitatLT.Rnw:524-526
###################################################
burst(puech2)[3:4] <- c("Chou.1992", "Chou.1993")
puech2


###################################################
### code chunk number 28: adehabitatLT.Rnw:538-539
###################################################
puech2


###################################################
### code chunk number 29: adehabitatLT.Rnw:546-548
###################################################
puech2b <- puech2[c(1,2,5)]
puech2b


###################################################
### code chunk number 30: adehabitatLT.Rnw:554-556
###################################################
puech2c <- c(puech2b, puech2[4])
puech2c


###################################################
### code chunk number 31: adehabitatLT.Rnw:568-570
###################################################
bu <- which.ltraj(puech2, "dist>2000")
bu


###################################################
### code chunk number 32: adehabitatLT.Rnw:577-578
###################################################
puech2[burst(puech2)%in%bu$burst]


###################################################
### code chunk number 33: plotltr2bu (eval = FALSE)
###################################################
## plotltr(puech2, "dt/3600/24")


###################################################
### code chunk number 34: adehabitatLT.Rnw:591-592 (eval = FALSE)
###################################################
## plotltr(puech2, "dt/3600/24")


###################################################
### code chunk number 35: adehabitatLT.Rnw:596-599
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
plotltr(puech2, "dt/3600/24")
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 36: adehabitatLT.Rnw:609-611
###################################################
refda <- strptime("00:00", "%H:%M", tz="Europe/Paris")
refda


###################################################
### code chunk number 37: adehabitatLT.Rnw:620-622
###################################################
puech3 <- setNA(puech2, refda, 1, units = "day")
puech3


###################################################
### code chunk number 38: adehabitatLT.Rnw:639-641
###################################################
data(ibexraw)
ibexraw


###################################################
### code chunk number 39: plotibex (eval = FALSE)
###################################################
## plotltr(ibexraw, "dt/3600")


###################################################
### code chunk number 40: adehabitatLT.Rnw:651-652 (eval = FALSE)
###################################################
## plotltr(ibexraw, "dt/3600")


###################################################
### code chunk number 41: adehabitatLT.Rnw:656-659
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
plotltr(ibexraw, "dt/3600")
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 42: adehabitatLT.Rnw:668-671
###################################################
refda <- strptime("2003-06-01 00:00", "%Y-%m-%d %H:%M", tz="Europe/Paris")
ib2 <- setNA(ibexraw, refda, 4, units = "hour")
ib2


###################################################
### code chunk number 43: plotltrib2 (eval = FALSE)
###################################################
## plotltr(ib2, "dt/3600")


###################################################
### code chunk number 44: adehabitatLT.Rnw:682-683 (eval = FALSE)
###################################################
## plotltr(ib2, "dt/3600")


###################################################
### code chunk number 45: adehabitatLT.Rnw:687-690
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
plotltr(ib2, "dt/3600")
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 46: adehabitatLT.Rnw:698-700
###################################################
ib3 <- sett0(ib2, refda, 4, units = "hour")
ib3


###################################################
### code chunk number 47: adehabitatLT.Rnw:723-724
###################################################
is.sd(ib3)


###################################################
### code chunk number 48: adehabitatLT.Rnw:732-733
###################################################
ib3


###################################################
### code chunk number 49: adehabitatLT.Rnw:743-747
###################################################
ib4 <- set.limits(ib3, begin = "2003-06-01 00:00",
                  dur = 14, units = "day", pattern = "%Y-%m-%d %H:%M",
                  tz="Europe/Paris")
ib4


###################################################
### code chunk number 50: adehabitatLT.Rnw:752-753
###################################################
is.sd(ib4)


###################################################
### code chunk number 51: adehabitatLT.Rnw:776-778
###################################################
di <- sd2df(ib4, "dist")
head(di)


###################################################
### code chunk number 52: adehabitatLT.Rnw:801-803
###################################################
data(capreochiz)
head(capreochiz)


###################################################
### code chunk number 53: adehabitatLT.Rnw:815-819
###################################################
capreo <- as.ltraj(xy = capreochiz[,c("x","y")], date = capreochiz$date,
                   id = "Roe.Deer",
                   infolocs = capreochiz[,4:8])
capreo


###################################################
### code chunk number 54: adehabitatLT.Rnw:826-828
###################################################
inf <- infolocs(capreo)
head(inf[[1]])


###################################################
### code chunk number 55: plotdop (eval = FALSE)
###################################################
## plotltr(capreo, "log(Dop)")


###################################################
### code chunk number 56: adehabitatLT.Rnw:856-857 (eval = FALSE)
###################################################
## plotltr(capreo, "log(Dop)")


###################################################
### code chunk number 57: adehabitatLT.Rnw:861-864
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
plotltr(capreo, "log(Dop)")
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 58: NAtest (eval = FALSE)
###################################################
## runsNAltraj(ib4)


###################################################
### code chunk number 59: adehabitatLT.Rnw:897-898 (eval = FALSE)
###################################################
## runsNAltraj(ib4)


###################################################
### code chunk number 60: adehabitatLT.Rnw:903-906
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
runsNAltraj(ib4)
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 61: adehabitatLT.Rnw:932-934
###################################################
data(bear)
bear


###################################################
### code chunk number 62: runsNAbear (eval = FALSE)
###################################################
## runsNAltraj(bear)


###################################################
### code chunk number 63: adehabitatLT.Rnw:946-947 (eval = FALSE)
###################################################
## runsNAltraj(bear)


###################################################
### code chunk number 64: adehabitatLT.Rnw:951-954
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
runsNAltraj(bear)
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 65: plotNAbear (eval = FALSE)
###################################################
## plotNAltraj(bear)


###################################################
### code chunk number 66: adehabitatLT.Rnw:965-966 (eval = FALSE)
###################################################
## plotNAltraj(bear)


###################################################
### code chunk number 67: adehabitatLT.Rnw:970-973
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
plotNAltraj(bear)
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 68: adehabitatLT.Rnw:982-984
###################################################
missval <- as.numeric(is.na(bear[[1]]$x))
head(missval)


###################################################
### code chunk number 69: adehabitatLT.Rnw:1010-1012
###################################################
bearI <- typeII2typeI(bear)
bearI


###################################################
### code chunk number 70: trajetours (eval = FALSE)
###################################################
## plot(bearI)


###################################################
### code chunk number 71: adehabitatLT.Rnw:1034-1035 (eval = FALSE)
###################################################
## plot(bearI)


###################################################
### code chunk number 72: adehabitatLT.Rnw:1039-1042
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
plot(bearI)
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 73: adehabitatLT.Rnw:1049-1051
###################################################
bearIr <- redisltraj(bearI, 500)
bearIr


###################################################
### code chunk number 74: plotltrajredisbear (eval = FALSE)
###################################################
## plot(bearIr)


###################################################
### code chunk number 75: adehabitatLT.Rnw:1061-1062 (eval = FALSE)
###################################################
## plot(bearIr)


###################################################
### code chunk number 76: adehabitatLT.Rnw:1066-1069
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
plot(bearIr)
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 77: sliwinltr (eval = FALSE)
###################################################
## sliwinltr(bearIr, function(x) mean(cos(x$rel.angle)), type="locs", step=30)


###################################################
### code chunk number 78: adehabitatLT.Rnw:1082-1083 (eval = FALSE)
###################################################
## sliwinltr(bearIr, function(x) mean(cos(x$rel.angle)), type="locs", step=30)


###################################################
### code chunk number 79: adehabitatLT.Rnw:1087-1090
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
sliwinltr(bearIr, function(x) mean(cos(x$rel.angle)), type="locs", step=30)
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 80: adehabitatLT.Rnw:1100-1102
###################################################
cosrelangle <- cos(bearIr[[1]]$rel.angle)
head(cosrelangle)


###################################################
### code chunk number 81: adehabitatLT.Rnw:1132-1134
###################################################
data(porpoise)
porpoise


###################################################
### code chunk number 82: adehabitatLT.Rnw:1144-1145
###################################################
(porpoise2 <- redisltraj(na.omit(porpoise[1:3]), 86400, type="time"))


###################################################
### code chunk number 83: adehabitatLT.Rnw:1162-1163 (eval = FALSE)
###################################################
## trajdyn(ib4)


###################################################
### code chunk number 84: adehabitatLT.Rnw:1201-1202
###################################################
wawotest(bear)


###################################################
### code chunk number 85: plotltrbeardist (eval = FALSE)
###################################################
## plotltr(bear, "dist")


###################################################
### code chunk number 86: adehabitatLT.Rnw:1217-1218 (eval = FALSE)
###################################################
## plotltr(bear, "dist")


###################################################
### code chunk number 87: adehabitatLT.Rnw:1222-1225
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
plotltr(bear, "dist")
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 88: adehabitatLT.Rnw:1232-1234 (eval = FALSE)
###################################################
## sliwinltr(bear, function(x) mean(na.omit(x$dist)),
##           5*48, type="locs")


###################################################
### code chunk number 89: acfdistltra (eval = FALSE)
###################################################
## acfdist.ltraj(bear, lag=5, which="dist")


###################################################
### code chunk number 90: adehabitatLT.Rnw:1262-1263 (eval = FALSE)
###################################################
## acfdist.ltraj(bear, lag=5, which="dist")


###################################################
### code chunk number 91: adehabitatLT.Rnw:1267-1270
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
acfdist.ltraj(bear, lag=5, which="dist")
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 92: critereangle (eval = FALSE)
###################################################
## opar <- par(mar = c(0,0,4,0))
## plot(0,0, asp=1, xlim=c(-1, 1), ylim=c(-1, 1), ty="n", axes=FALSE,
##      main="Criteria f for the measure of independence between successive
##      angles at time i-1 and i")
## box()
## symbols(0,0,circle=1, inches=FALSE, lwd=2, add=TRUE)
## abline(h=0, v=0)
## x <- c( cos(pi/3), cos(pi/2 + pi/4))
## y <- c( sin(pi/3), sin(pi/2 + pi/4))
## arrows(c(0,0), c(0,0), x, y)
## lines(x,y, lwd=2, col="red")
## text(0, 0.9, expression(f^2 == 2*sum((1 - cos(alpha[i]-alpha[i-1])),
##     i==1, n-1)), col="red")
## foo <- function(t, alpha)
## {
##     xa <- sapply(seq(0, alpha, length=20), function(x) t*cos(x))
##     ya <- sapply(seq(0, alpha, length=20), function(x) t*sin(x))
##     lines(xa, ya)
## }
## foo(0.3, pi/3)
## foo(0.1, pi/2 + pi/4)
## foo(0.11, pi/2 + pi/4)
## text(0.34,0.18,expression(alpha[i]), cex=1.5)
## text(0.15,0.11,expression(alpha[i-1]), cex=1.5)
## par(opar)


###################################################
### code chunk number 93: adehabitatLT.Rnw:1316-1319
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
opar <- par(mar = c(0,0,4,0))
plot(0,0, asp=1, xlim=c(-1, 1), ylim=c(-1, 1), ty="n", axes=FALSE,
     main="Criteria f for the measure of independence between successive
     angles at time i-1 and i")
box()
symbols(0,0,circle=1, inches=FALSE, lwd=2, add=TRUE)
abline(h=0, v=0)
x <- c( cos(pi/3), cos(pi/2 + pi/4))
y <- c( sin(pi/3), sin(pi/2 + pi/4))
arrows(c(0,0), c(0,0), x, y)
lines(x,y, lwd=2, col="red")
text(0, 0.9, expression(f^2 == 2*sum((1 - cos(alpha[i]-alpha[i-1])),
    i==1, n-1)), col="red")
foo <- function(t, alpha)
{
    xa <- sapply(seq(0, alpha, length=20), function(x) t*cos(x))
    ya <- sapply(seq(0, alpha, length=20), function(x) t*sin(x))
    lines(xa, ya)
}
foo(0.3, pi/3)
foo(0.1, pi/2 + pi/4)
foo(0.11, pi/2 + pi/4)
text(0.34,0.18,expression(alpha[i]), cex=1.5)
text(0.15,0.11,expression(alpha[i-1]), cex=1.5)
par(opar)
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 94: adehabitatLT.Rnw:1328-1329
###################################################
testang.ltraj(bear, "relative")


###################################################
### code chunk number 95: acfangbear (eval = FALSE)
###################################################
## acfang.ltraj(bear, lag=5)


###################################################
### code chunk number 96: adehabitatLT.Rnw:1343-1344 (eval = FALSE)
###################################################
## acfang.ltraj(bear, lag=5)


###################################################
### code chunk number 97: adehabitatLT.Rnw:1348-1351
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
acfang.ltraj(bear, lag=5)
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 98: adehabitatLT.Rnw:1378-1381
###################################################
data(porpoise)
gus <- porpoise[1]
gus


###################################################
### code chunk number 99: pltgus (eval = FALSE)
###################################################
## plot(gus)


###################################################
### code chunk number 100: adehabitatLT.Rnw:1391-1392 (eval = FALSE)
###################################################
## plot(gus)


###################################################
### code chunk number 101: adehabitatLT.Rnw:1396-1399
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
plot(gus)
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 102: acfgus (eval = FALSE)
###################################################
## acfdist.ltraj(gus, "dist", lag=20)


###################################################
### code chunk number 103: adehabitatLT.Rnw:1416-1417 (eval = FALSE)
###################################################
## acfdist.ltraj(gus, "dist", lag=20)


###################################################
### code chunk number 104: adehabitatLT.Rnw:1421-1424
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
acfdist.ltraj(gus, "dist", lag=20)
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 105: plodisgus (eval = FALSE)
###################################################
## plotltr(gus, "dist")


###################################################
### code chunk number 106: adehabitatLT.Rnw:1436-1437 (eval = FALSE)
###################################################
## plotltr(gus, "dist")


###################################################
### code chunk number 107: adehabitatLT.Rnw:1441-1444
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
plotltr(gus, "dist")
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 108: adehabitatLT.Rnw:1454-1455
###################################################
(tested.means <- round(seq(0, 130000, length = 10), 0))


###################################################
### code chunk number 109: adehabitatLT.Rnw:1463-1466
###################################################
(limod <- as.list(paste("dnorm(dist, mean =",
                        tested.means,
                        ", sd = 5000)")))


###################################################
### code chunk number 110: adehabitatLT.Rnw:1482-1484
###################################################
mod <- modpartltraj(gus, limod)
mod


###################################################
### code chunk number 111: bpmgus (eval = FALSE)
###################################################
## bestpartmod(mod)


###################################################
### code chunk number 112: adehabitatLT.Rnw:1496-1497 (eval = FALSE)
###################################################
## bestpartmod(mod)


###################################################
### code chunk number 113: adehabitatLT.Rnw:1501-1504
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
bestpartmod(mod)
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 114: adehabitatLT.Rnw:1542-1543
###################################################
(pm <- partmod.ltraj(gus, 4, mod))


###################################################
### code chunk number 115: plotpartitiongus (eval = FALSE)
###################################################
## plot(pm)


###################################################
### code chunk number 116: adehabitatLT.Rnw:1553-1554 (eval = FALSE)
###################################################
## plot(pm)


###################################################
### code chunk number 117: adehabitatLT.Rnw:1558-1561
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
plot(pm)
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 118: plotpartltr (eval = FALSE)
###################################################
## ## Shows the segmentation on the distances:
## plotltr(gus, "dist")
## tmp <- lapply(1:length(pm$ltraj), function(i) {
##     coul <- c("red","green","blue")[as.numeric(factor(pm$stats$mod))[i]]
##     lines(pm$ltraj[[i]]$date, rep(tested.means[pm$stats$mod[i]],
##                                   nrow(pm$ltraj[[i]])),
##           col=coul, lwd=2)
## })


###################################################
### code chunk number 119: adehabitatLT.Rnw:1586-1587 (eval = FALSE)
###################################################
## ## Shows the segmentation on the distances:
## plotltr(gus, "dist")
## tmp <- lapply(1:length(pm$ltraj), function(i) {
##     coul <- c("red","green","blue")[as.numeric(factor(pm$stats$mod))[i]]
##     lines(pm$ltraj[[i]]$date, rep(tested.means[pm$stats$mod[i]],
##                                   nrow(pm$ltraj[[i]])),
##           col=coul, lwd=2)
## })


###################################################
### code chunk number 120: adehabitatLT.Rnw:1591-1594
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
## Shows the segmentation on the distances:
plotltr(gus, "dist")
tmp <- lapply(1:length(pm$ltraj), function(i) {
    coul <- c("red","green","blue")[as.numeric(factor(pm$stats$mod))[i]]
    lines(pm$ltraj[[i]]$date, rep(tested.means[pm$stats$mod[i]],
                                  nrow(pm$ltraj[[i]])),
          col=coul, lwd=2)
})
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 121: plotrespart (eval = FALSE)
###################################################
## ## Computes the residuals
## res <- unlist(lapply(1:length(pm$ltraj), function(i) {
##     pm$ltraj[[i]]$dist - rep(tested.means[pm$stats$mod[i]],
##                              nrow(pm$ltraj[[i]]))
## }))
## plot(res, ty = "l")


###################################################
### code chunk number 122: adehabitatLT.Rnw:1612-1613 (eval = FALSE)
###################################################
## ## Computes the residuals
## res <- unlist(lapply(1:length(pm$ltraj), function(i) {
##     pm$ltraj[[i]]$dist - rep(tested.means[pm$stats$mod[i]],
##                              nrow(pm$ltraj[[i]]))
## }))
## plot(res, ty = "l")


###################################################
### code chunk number 123: adehabitatLT.Rnw:1617-1620
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
## Computes the residuals
res <- unlist(lapply(1:length(pm$ltraj), function(i) {
    pm$ltraj[[i]]$dist - rep(tested.means[pm$stats$mod[i]],
                             nrow(pm$ltraj[[i]]))
}))
plot(res, ty = "l")
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 124: adehabitatLT.Rnw:1627-1628
###################################################
wawotest(res)


###################################################
### code chunk number 125: adehabitatLT.Rnw:1733-1756
###################################################
par(mar=c(0,0,0,0))
plot(c(0,1), xlim=c(-0.3,1.3), ylim=c(-0.3,1.3), axes=FALSE)
ii <- seq(0.05, 0.95, by=0.1)
tmp <- lapply(ii, function(i) {
    lapply(ii, function(j) {
        if (j-(1-i)> -0.01) {
            polygon(c(i-0.05, i+0.05, i+0.05, i-0.05),
                    c(j-0.05, j-0.05, j+0.05, j+0.05), col="grey")
        } else {
            polygon(c(i-0.05, i+0.05, i+0.05, i-0.05),
                    c(j-0.05, j-0.05, j+0.05, j+0.05))
        }
    })})
polygon(c(0,1,1,0), c(0,0,1,1), lwd=2)
text(rep(-0.05,10), ii, 10:1)
text(ii, rep(1.05,10), 1:10)
i <- 0.65
j <- 0.85
polygon(c(i-0.05, i+0.05, i+0.05, i-0.05),
        c(j-0.05, j-0.05, j+0.05, j+0.05), col="red")
segments(0.4, 1.18, i, j)
points(i,j, pch=16)
text(0.4, 1.25, "Contrast function measured on the segment\n beginning at observation 2 and ending at observation 7")


###################################################
### code chunk number 126: adehabitatLT.Rnw:1764-1784
###################################################
par(mar=c(0,0,0,0))
plot(c(0,1), xlim=c(-0.3,1.3), ylim=c(-0.3,1.3), axes=FALSE)
ii <- seq(0.05, 0.95, by=0.1)
tmp <- lapply(ii, function(i) {
    lapply(ii, function(j) {
        if (j-(1-i)> -0.01) {
            polygon(c(i-0.05, i+0.05, i+0.05, i-0.05),
                    c(j-0.05, j-0.05, j+0.05, j+0.05), col="grey")
        } else {
            polygon(c(i-0.05, i+0.05, i+0.05, i-0.05),
                    c(j-0.05, j-0.05, j+0.05, j+0.05))
        }
    })})
polygon(c(0,1,1,0), c(0,0,1,1), lwd=2)
text(rep(-0.05,10), ii, 10:1)
text(ii, rep(1.05,10), 1:10)
lines(c(0.00, 0.45, 0.50, 0.75, 0.80, 0.95),
      c(0.95, 0.95, 0.45, 0.45, 0.15, 0.15), col="red", lwd=3)
points(c(0.45, 0.75, 0.95),
       c(0.95, 0.45, 0.15), pch=16, cex=2)


###################################################
### code chunk number 127: figseri1 (eval = FALSE)
###################################################
## set.seed(129)
## seri <- c(rnorm(100), rnorm(100, mean=2),
##           rnorm(100), rnorm(100, mean=-3),
##           rnorm(100), rnorm(100, mean=2))
## plot(seri, ty="l", xlab="time", ylab="Series")


###################################################
### code chunk number 128: adehabitatLT.Rnw:1859-1860 (eval = FALSE)
###################################################
## set.seed(129)
## seri <- c(rnorm(100), rnorm(100, mean=2),
##           rnorm(100), rnorm(100, mean=-3),
##           rnorm(100), rnorm(100, mean=2))
## plot(seri, ty="l", xlab="time", ylab="Series")


###################################################
### code chunk number 129: adehabitatLT.Rnw:1864-1867
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
set.seed(129)
seri <- c(rnorm(100), rnorm(100, mean=2),
          rnorm(100), rnorm(100, mean=-3),
          rnorm(100), rnorm(100, mean=2))
plot(seri, ty="l", xlab="time", ylab="Series")
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 130: adehabitatLT.Rnw:1876-1877
###################################################
(l <- lavielle(seri, Lmin=10, Kmax=20, type="mean"))


###################################################
### code chunk number 131: adehabitatLT.Rnw:1888-1889
###################################################
chooseseg(l)


###################################################
### code chunk number 132: figchoose1 (eval = FALSE)
###################################################
## fp <- findpath(l, 6)


###################################################
### code chunk number 133: adehabitatLT.Rnw:1908-1909 (eval = FALSE)
###################################################
## fp <- findpath(l, 6)


###################################################
### code chunk number 134: adehabitatLT.Rnw:1913-1916
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
fp <- findpath(l, 6)
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 135: adehabitatLT.Rnw:1924-1925
###################################################
fp


###################################################
### code chunk number 136: adehabitatLT.Rnw:1937-1938
###################################################
plotltr(gus, "dist")


###################################################
### code chunk number 137: adehabitatLT.Rnw:1948-1950
###################################################
lav <- lavielle(gus, Lmin=2, Kmax=20)
chooseseg(lav)


###################################################
### code chunk number 138: adehabitatLT.Rnw:1957-1959
###################################################
kk <- findpath(lav, 4)
kk


###################################################
### code chunk number 139: adehabitatLT.Rnw:1966-1967
###################################################
plot(kk)


###################################################
### code chunk number 140: imagerast1 (eval = FALSE)
###################################################
## data(puechcirc)
## data(puechabonsp)
## mimage(puechabonsp$map)


###################################################
### code chunk number 141: adehabitatLT.Rnw:2018-2019 (eval = FALSE)
###################################################
## data(puechcirc)
## data(puechabonsp)
## mimage(puechabonsp$map)


###################################################
### code chunk number 142: adehabitatLT.Rnw:2023-2026
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
data(puechcirc)
data(puechabonsp)
mimage(puechabonsp$map)
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 143: adehabitatLT.Rnw:2032-2033
###################################################
ii <- rasterize.ltraj(puechcirc, puechabonsp$map)


###################################################
### code chunk number 144: adehabitatLT.Rnw:2040-2042
###################################################
tr1 <- ii[[1]]
head(tr1)


###################################################
### code chunk number 145: ksdfjk (eval = FALSE)
###################################################
## plot(tr1)
## points(tr1[tr1[[1]]==3,], col="red")


###################################################
### code chunk number 146: adehabitatLT.Rnw:2054-2055 (eval = FALSE)
###################################################
## plot(tr1)
## points(tr1[tr1[[1]]==3,], col="red")


###################################################
### code chunk number 147: adehabitatLT.Rnw:2059-2062
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
plot(tr1)
points(tr1[tr1[[1]]==3,], col="red")
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 148: jklmmre (eval = FALSE)
###################################################
## ov <- over(tr1, puechabonsp$map)
## mo <- tapply(ov[[1]], tr1[[1]], mean)
## plot(mo, ty="l")


###################################################
### code chunk number 149: adehabitatLT.Rnw:2078-2079 (eval = FALSE)
###################################################
## ov <- over(tr1, puechabonsp$map)
## mo <- tapply(ov[[1]], tr1[[1]], mean)
## plot(mo, ty="l")


###################################################
### code chunk number 150: adehabitatLT.Rnw:2083-2086
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
ov <- over(tr1, puechabonsp$map)
mo <- tapply(ov[[1]], tr1[[1]], mean)
plot(mo, ty="l")
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 151: adehabitatLT.Rnw:2099-2125
###################################################
val <- lapply(1:length(ii), function(i) {

    ## get the rasterized trajectory
    tr <- ii[[i]]

    ## over with the map
    ov <- over(tr, puechabonsp$map)

    ## calculate the mean elevation
    mo <- tapply(ov[[1]], tr[[1]], mean)

    ## prepare the output
    elev <- rep(NA, nrow(puechcirc[[i]]))

    ## place the average values at the right place
    ## names(mo) contains the step number (i.e. relocation
    ## number +1)
    elev[as.numeric(names(mo))+1] <- mo
    df <- data.frame(elevation = elev)

    ## same row.names as in the ltraj
    row.names(df) <- row.names(puechcirc[[i]])

    return(df)
})



###################################################
### code chunk number 152: adehabitatLT.Rnw:2130-2131
###################################################
infolocs(puechcirc) <- val


###################################################
### code chunk number 153: ksdfjeiii (eval = FALSE)
###################################################
## plotltr(puechcirc, "elevation")


###################################################
### code chunk number 154: adehabitatLT.Rnw:2141-2142 (eval = FALSE)
###################################################
## plotltr(puechcirc, "elevation")


###################################################
### code chunk number 155: adehabitatLT.Rnw:2146-2149
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
plotltr(puechcirc, "elevation")
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 156: adehabitatLT.Rnw:2190-2192
###################################################
sim <- simm.crw(1:1000, r=0.95)
sim


###################################################
### code chunk number 157: plotcrwsim (eval = FALSE)
###################################################
## plot(sim, addp=FALSE)


###################################################
### code chunk number 158: adehabitatLT.Rnw:2205-2206 (eval = FALSE)
###################################################
## plot(sim, addp=FALSE)


###################################################
### code chunk number 159: adehabitatLT.Rnw:2210-2213
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
plot(sim, addp=FALSE)
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 160: figoriboar (eval = FALSE)
###################################################
## data(puechcirc)
## data(puechabonsp)
## boar1 <- puechcirc[1]
## xo <- coordinates(puechabonsp$map)
## ## Note that xo is a matrix containing the coordinates of the
## ## pixels of the elevation map (we will use it later to define
## ## the limits of the study area).
## 
## plot(boar1, spixdf=puechabonsp$map, xlim=range(xo[,1]), ylim=range(xo[,2]))


###################################################
### code chunk number 161: adehabitatLT.Rnw:2273-2274 (eval = FALSE)
###################################################
## data(puechcirc)
## data(puechabonsp)
## boar1 <- puechcirc[1]
## xo <- coordinates(puechabonsp$map)
## ## Note that xo is a matrix containing the coordinates of the
## ## pixels of the elevation map (we will use it later to define
## ## the limits of the study area).
## 
## plot(boar1, spixdf=puechabonsp$map, xlim=range(xo[,1]), ylim=range(xo[,2]))


###################################################
### code chunk number 162: adehabitatLT.Rnw:2278-2281
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
data(puechcirc)
data(puechabonsp)
boar1 <- puechcirc[1]
xo <- coordinates(puechabonsp$map)
## Note that xo is a matrix containing the coordinates of the
## pixels of the elevation map (we will use it later to define
## the limits of the study area).

plot(boar1, spixdf=puechabonsp$map, xlim=range(xo[,1]), ylim=range(xo[,2]))
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 163: adehabitatLT.Rnw:2362-2369
###################################################
plotfun <- function(x, par)
{
    image(par)
    points(x[,1:2], pch=16)
    lines(x[,1:2])
    return(x)
}


###################################################
### code chunk number 164: adehabitatLT.Rnw:2377-2382
###################################################
nmo <- NMs.randomShiftRotation(na.omit(boar1), rshift = TRUE, rrot = TRUE,
                               rx = range(xo[,1]), ry = range(xo[,2]),
                               treatment.func = plotfun,
                               treatment.par = puechabonsp$map[,1], nrep=9)
nmo


###################################################
### code chunk number 165: figtestNM (eval = FALSE)
###################################################
## set.seed(90909)
## par(mfrow=c(3,3), mar=c(0,0,0,0))
## resu <- testNM(nmo, count = FALSE)


###################################################
### code chunk number 166: adehabitatLT.Rnw:2403-2404 (eval = FALSE)
###################################################
## set.seed(90909)
## par(mfrow=c(3,3), mar=c(0,0,0,0))
## resu <- testNM(nmo, count = FALSE)


###################################################
### code chunk number 167: adehabitatLT.Rnw:2408-2411
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
set.seed(90909)
par(mfrow=c(3,3), mar=c(0,0,0,0))
resu <- testNM(nmo, count = FALSE)
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 168: adehabitatLT.Rnw:2432-2443
###################################################
confun <- function(x, par)
{
    ## Define a SpatialPointsDataFrame from the trajectory
    coordinates(x) <- x[,1:2]
    ## overlap the relocations x to the elevation map par
    jo <- join(x, par)
    ## checks that there are no missing value
    res <- all(!is.na(jo))
    ## return this check
    return(res)
}


###################################################
### code chunk number 169: adehabitatLT.Rnw:2449-2456
###################################################
nmo2 <- NMs.randomShiftRotation(na.omit(boar1), rshift = TRUE, rrot = TRUE,
                                rx = range(xo[,1]), ry = range(xo[,2]),
                                treatment.func = plotfun,
                                treatment.par = puechabonsp$map[,1],
                                constraint.func = confun,
                                constraint.par = puechabonsp$map[,1],
                                nrep=9)


###################################################
### code chunk number 170: figtestnm2 (eval = FALSE)
###################################################
## set.seed(90909)
## par(mfrow=c(3,3), mar=c(0,0,0,0))
## resu <- testNM(nmo2, count = FALSE)


###################################################
### code chunk number 171: adehabitatLT.Rnw:2468-2469 (eval = FALSE)
###################################################
## set.seed(90909)
## par(mfrow=c(3,3), mar=c(0,0,0,0))
## resu <- testNM(nmo2, count = FALSE)


###################################################
### code chunk number 172: adehabitatLT.Rnw:2473-2476
###################################################
.PngNo <- .PngNo + 1; file <- paste("Fig-bitmap-",
          .PngNo, ".png", sep="")
png(file=file, width = wi, height = wi, pointsize = pt)
set.seed(90909)
par(mfrow=c(3,3), mar=c(0,0,0,0))
resu <- testNM(nmo2, count = FALSE)
dev.null <- dev.off()
cat("\\includegraphics[height=10cm,width=10cm]{", file, "}\n\n", sep="")


###################################################
### code chunk number 173: adehabitatLT.Rnw:2490-2496
###################################################
varel <- function(x, par)
{
    coordinates(x) <- x[,1:2]
    jo <- join(x, par)
    return(var(jo))
}


###################################################
### code chunk number 174: adehabitatLT.Rnw:2505-2512
###################################################
nmo3 <- NMs.randomShiftRotation(na.omit(boar1), rshift = TRUE, rrot = TRUE,
                                rx = range(xo[,1]), ry = range(xo[,2]),
                                treatment.func = varel,
                                treatment.par = puechabonsp$map[,1],
                                constraint.func = confun,
                                constraint.par = puechabonsp$map[,1],
                                nrep=99)


###################################################
### code chunk number 175: adehabitatLT.Rnw:2519-2520 (eval = FALSE)
###################################################
## sim <- testNM(nmo3, count=FALSE)


###################################################
### code chunk number 176: adehabitatLT.Rnw:2523-2524
###################################################
load("sim.Rdata")


###################################################
### code chunk number 177: adehabitatLT.Rnw:2529-2530
###################################################
(obs <- varel(na.omit(boar1)[[1]], puechabonsp$map[,1]))


###################################################
### code chunk number 178: adehabitatLT.Rnw:2537-2539
###################################################
(ran <- as.randtest(unlist(sim[[1]]), obs))
plot(ran)


###################################################
### code chunk number 179: adehabitatLT.Rnw:2551-2553
###################################################
puechcirc
plot(puechcirc)


###################################################
### code chunk number 180: adehabitatLT.Rnw:2560-2561
###################################################
(boar <- bindltraj(puechcirc))


###################################################
### code chunk number 181: adehabitatLT.Rnw:2571-2578
###################################################
nmo4 <- NMs.randomShiftRotation(na.omit(boar), rshift = TRUE, rrot = TRUE,
                                rx = range(xo[,1]), ry = range(xo[,2]),
                                treatment.func = varel,
                                treatment.par = puechabonsp$map[,1],
                                constraint.func = confun,
                                constraint.par = puechabonsp$map[,1],
                                nrep=99)


###################################################
### code chunk number 182: adehabitatLT.Rnw:2583-2584 (eval = FALSE)
###################################################
## sim2 <- testNM(nmo4, count=FALSE)


###################################################
### code chunk number 183: adehabitatLT.Rnw:2587-2588
###################################################
load("sim2.Rdata")


###################################################
### code chunk number 184: adehabitatLT.Rnw:2597-2600
###################################################
(obs <- lapply(na.omit(boar), function(x) {
  varel(x, puechabonsp$map[,1])
}))


###################################################
### code chunk number 185: adehabitatLT.Rnw:2605-2608
###################################################
lapply(1:2, function(i) {
  as.randtest(unlist(sim2[[i]]), obs[[i]])
})


###################################################
### code chunk number 186: adehabitatLT.Rnw:2655-2664
###################################################
meanvarel <- function(x, par)
{
    livar <- lapply(x, function(y) {
        coordinates(y) <- y[,1:2]
        jo <- join(y, par)
        return(var(jo))
    })
    mean(unlist(livar))
}


###################################################
### code chunk number 187: adehabitatLT.Rnw:2671-2673
###################################################
nmo5 <- NMs2NMm(nmo4, treatment.func = meanvarel,
                treatment.par = puechabonsp$map, nrep = 99)


###################################################
### code chunk number 188: adehabitatLT.Rnw:2682-2683 (eval = FALSE)
###################################################
## sim3 <- testNM(nmo5, count=FALSE)


###################################################
### code chunk number 189: adehabitatLT.Rnw:2686-2687
###################################################
load("sim3.Rdata")


###################################################
### code chunk number 190: adehabitatLT.Rnw:2697-2698
###################################################
(obs <- meanvarel(na.omit(boar), puechabonsp$map))


###################################################
### code chunk number 191: adehabitatLT.Rnw:2704-2706
###################################################
(ran <- as.randtest(unlist(sim3), obs))
plot(ran)


