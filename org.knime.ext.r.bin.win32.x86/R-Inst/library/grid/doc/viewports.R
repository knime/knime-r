### R code from vignette source 'viewports.Rnw'

###################################################
### code chunk number 1: viewports.Rnw:21-26
###################################################
library(grDevices)
library(stats) # for runif()
library(grid)
ps.options(pointsize = 12)
options(width = 60)


###################################################
### code chunk number 2: viewports.Rnw:45-48
###################################################
pushViewport(viewport())
upViewport()
pushViewport(viewport())


###################################################
### code chunk number 3: viewports.Rnw:63-65
###################################################
grid.newpage()



###################################################
### code chunk number 4: viewports.Rnw:66-71
###################################################
pushViewport(viewport(name = "A"))
upViewport()
pushViewport(viewport(name = "B"))
upViewport()
downViewport("A")


###################################################
### code chunk number 5: viewports.Rnw:80-81
###################################################
seekViewport("B")


###################################################
### code chunk number 6: viewports.Rnw:86-88
###################################################
current.vpTree()



###################################################
### code chunk number 7: vpstackguts
###################################################
vp <- viewport(width = 0.5, height = 0.5)
grid.rect(vp = vpStack(vp, vp))


###################################################
### code chunk number 8: viewports.Rnw:109-111
###################################################
grid.rect(gp = gpar(col = "grey"))
vp <- viewport(width = 0.5, height = 0.5)
grid.rect(vp = vpStack(vp, vp))


###################################################
### code chunk number 9: viewports.Rnw:135-137
###################################################
grid.newpage()



###################################################
### code chunk number 10: viewports.Rnw:138-141
###################################################
pushViewport(viewport(name = "A"))
pushViewport(viewport(name = "B"))
pushViewport(viewport(name = "A"))


###################################################
### code chunk number 11: viewports.Rnw:147-149
###################################################
seekViewport("A")
current.vpTree(FALSE)


###################################################
### code chunk number 12: viewports.Rnw:154-156
###################################################
seekViewport(vpPath("B", "A"))
current.vpTree(FALSE)


###################################################
### code chunk number 13: viewports.Rnw:161-162
###################################################
vpPath("A", "B")


###################################################
### code chunk number 14: viewports.Rnw:169-171 (eval = FALSE)
###################################################
## seekViewport(vpPath("A", "B"))
## seekViewport("A::B")


###################################################
### code chunk number 15: viewports.Rnw:180-182
###################################################
x <- runif(10)
y <- runif(10)


###################################################
### code chunk number 16: viewports.Rnw:187-189
###################################################
xscale <- extendrange(x)
yscale <- extendrange(y)


###################################################
### code chunk number 17: viewports.Rnw:198-203
###################################################
top.vp <-
    viewport(layout=grid.layout(3, 3,
             widths=unit(c(5, 1, 2), c("lines", "null", "lines")),
             heights=unit(c(5, 1, 4), c("lines", "null", "lines"))))



###################################################
### code chunk number 18: viewports.Rnw:204-205
###################################################
grid.show.layout(viewport.layout(top.vp))


###################################################
### code chunk number 19: viewports.Rnw:212-222
###################################################
margin1 <- viewport(layout.pos.col = 2, layout.pos.row = 3,
                    name = "margin1")
margin2 <- viewport(layout.pos.col = 1, layout.pos.row = 2,
                    name = "margin2")
margin3 <- viewport(layout.pos.col = 2, layout.pos.row = 1,
                    name = "margin3")
margin4 <- viewport(layout.pos.col = 3, layout.pos.row = 2,
                    name = "margin4")
plot <- viewport(layout.pos.col = 2, layout.pos.row = 2,
                 name = "plot", xscale = xscale, yscale = yscale)


###################################################
### code chunk number 20: viewports.Rnw:229-230
###################################################
splot <- vpTree(top.vp, vpList(margin1, margin2, margin3, margin4, plot))


###################################################
### code chunk number 21: viewports
###################################################
pushViewport(splot)


###################################################
### code chunk number 22: grid (eval = FALSE)
###################################################
## labelvp <- function(name) {
##     seekViewport(name)
##     grid.rect(gp = gpar(col = "grey", lwd = 5))
##     grid.rect(x = 0, y = 1, width = unit(1, "strwidth", name) + unit(2, "mm"),
##               height = unit(1, "lines"), just = c("left", "top"),
##               gp = gpar(fill = "grey", col = NULL))
##     grid.text(name, x = unit(1, "mm"), y = unit(1, "npc") - unit(1, "mm"),
##               just = c("left", "top"), gp = gpar(col = "white"))
## }
## labelvp("plot")
## labelvp("margin1")
## labelvp("margin2")
## labelvp("margin3")
## labelvp("margin4")


###################################################
### code chunk number 23: plot (eval = FALSE)
###################################################
## seekViewport("plot")
## grid.points(x, y)
## grid.xaxis()
## grid.yaxis()
## grid.rect()


###################################################
### code chunk number 24: margin1 (eval = FALSE)
###################################################
## seekViewport("margin1")
## grid.text("Random X", y = unit(1, "lines"))


###################################################
### code chunk number 25: margin2 (eval = FALSE)
###################################################
## seekViewport("margin2")
## grid.text("Random Y", x = unit(1, "lines"), rot = 90)
## 


###################################################
### code chunk number 26: viewports.Rnw:282-288
###################################################
pushViewport(viewport(w = 0.9, h = 0.9))
pushViewport(splot)
labelvp <- function(name) {
    seekViewport(name)
    grid.rect(gp = gpar(col = "grey", lwd = 5))
    grid.rect(x = 0, y = 1, width = unit(1, "strwidth", name) + unit(2, "mm"),
              height = unit(1, "lines"), just = c("left", "top"),
              gp = gpar(fill = "grey", col = NULL))
    grid.text(name, x = unit(1, "mm"), y = unit(1, "npc") - unit(1, "mm"),
              just = c("left", "top"), gp = gpar(col = "white"))
}
labelvp("plot")
labelvp("margin1")
labelvp("margin2")
labelvp("margin3")
labelvp("margin4")
seekViewport("plot")
grid.points(x, y)
grid.xaxis()
grid.yaxis()
grid.rect()
seekViewport("margin1")
grid.text("Random X", y = unit(1, "lines"))
seekViewport("margin2")
grid.text("Random Y", x = unit(1, "lines"), rot = 90)



###################################################
### code chunk number 27: viewports.Rnw:302-303
###################################################
upViewport(0)


###################################################
### code chunk number 28: annguts (eval = FALSE)
###################################################
## seekViewport("margin3")
## grid.text("The user adds a title!", gp = gpar(fontsize = 20))
## 


###################################################
### code chunk number 29: viewports.Rnw:320-328
###################################################
pushViewport(viewport(w = 0.9, h = 0.9))
pushViewport(splot)
labelvp <- function(name) {
    seekViewport(name)
    grid.rect(gp = gpar(col = "grey", lwd = 5))
    grid.rect(x = 0, y = 1, width = unit(1, "strwidth", name) + unit(2, "mm"),
              height = unit(1, "lines"), just = c("left", "top"),
              gp = gpar(fill = "grey", col = NULL))
    grid.text(name, x = unit(1, "mm"), y = unit(1, "npc") - unit(1, "mm"),
              just = c("left", "top"), gp = gpar(col = "white"))
}
labelvp("plot")
labelvp("margin1")
labelvp("margin2")
labelvp("margin3")
labelvp("margin4")
seekViewport("plot")
grid.points(x, y)
grid.xaxis()
grid.yaxis()
grid.rect()
seekViewport("margin1")
grid.text("Random X", y = unit(1, "lines"))
seekViewport("margin2")
grid.text("Random Y", x = unit(1, "lines"), rot = 90)

seekViewport("margin3")
grid.text("The user adds a title!", gp = gpar(fontsize = 20))

popViewport(0)


