## ----init,echo=FALSE-----------------------------------------------------
knitr::opts_chunk$set(warning=FALSE, message=FALSE, cache=FALSE, 
               comment=NA, verbose=TRUE, fig.width=5, fig.height=5,dev='jpeg',dev.args=list(quality=50))

## ------------------------------------------------------------------------
im <- load.example('parrots') %>% grayscale
px <- im > .6 #Select pixels with high luminance
px
plot(px)

## ------------------------------------------------------------------------
str(px)

## ------------------------------------------------------------------------
all(dim(px) == dim(im))

## ------------------------------------------------------------------------
sum(px) #Number of pixels in set
mean(px) #Proportion

## ------------------------------------------------------------------------
as.cimg(px)
##same thing: automatic conversion to a numeric type
px + 0

## ------------------------------------------------------------------------
mean(im[px])
mean(im[!px])
which(px) %>% head

## ------------------------------------------------------------------------
plot(im)
px <- (isoblur(im,4)  > .5 )
highlight(px)

## ------------------------------------------------------------------------
plot(px)

## ------------------------------------------------------------------------
where(px) %>% head

## ------------------------------------------------------------------------
where(px) %>% dplyr::summarise(mx=mean(x),my=mean(y))

## ------------------------------------------------------------------------
plot(im)
#Start the fill at location (180,274). sigma sets the tolerance
px.flood(im,180,274,sigma=.21) %>% highlight

## ------------------------------------------------------------------------
sp <- split_connected(px) #returns an imlist 
plot(sp[1:4])
sp

## ------------------------------------------------------------------------
is.connected <- function(px) length(split_connected(px)) == 1
sapply(sp,is.connected)
is.connected(px)

## ------------------------------------------------------------------------
boundary(px) %>% plot
##Make your own highlight function:
plot(im)
boundary(px) %>% where %$% { points(x,y,cex=.1,col="red") }

## ------------------------------------------------------------------------
plot(im)
highlight(px)
#Grow by 5 pixels
grow(px,5) %>% highlight(col="green")
#Shrink by 5 pixels
shrink(px,5) %>% highlight(col="blue")

#Compute bounding box
bbox(px) %>% highlight(col="yellow")


## ------------------------------------------------------------------------
px.none(im) #No pixels
px.all(im) #All of them

plot(im)
#Image borders at depth 10
px.borders(im,10) %>% highlight
#Left-hand border (5 pixels), see also px.top, px.bottom, etc.
px.left(im,5) %>% highlight(col="green")




## ------------------------------------------------------------------------
#Split pixset in two along x
imsplit(px,"x",2) %>% plot(layout="row")
#Splitting pixsets results into a list of pixsets
imsplit(px,"x",2) %>% str

#Cut along y, append along x
imsplit(px,"y",2) %>% imappend("x") %>% plot()

## ------------------------------------------------------------------------
px <- boats > .8
px
where(px) %>% head

## ------------------------------------------------------------------------
plot(px)

## ------------------------------------------------------------------------
imsplit(px,"c") %>% plot

## ------------------------------------------------------------------------
#parall stands for "parallel-all", and works similarly to parmax, parmin, etc.
imsplit(px,"c") %>% parall %>% where %>% head

#at each location, test if any channel is in px
imsplit(px,"c") %>% parany %>% where %>% head

#highlight the set (unsurprisingly, it's mostly white pixels)
plot(boats)
imsplit(px,"c") %>% parany %>% highlight


## ------------------------------------------------------------------------
im <- load.example("coins")
plot(im)

## ------------------------------------------------------------------------
threshold(im) %>% plot

## ------------------------------------------------------------------------
library(dplyr)
d <- as.data.frame(im)
##Subsamble, fit a linear model
m <- sample_n(d,1e4) %>% lm(value ~ x*y,data=.) 
##Correct by removing the trend
im.c <- im-predict(m,d)
out <- threshold(im.c)
plot(out)

## ------------------------------------------------------------------------
out <- clean(out,3) %>% imager::fill(7)
plot(im)
highlight(out)

## ----fig.width=8---------------------------------------------------------
bg <- (!threshold(im.c,"10%"))
fg <- (threshold(im.c,"90%"))
imlist(fg,bg) %>% plot(layout="row")
#Build a seed image where fg pixels have value 2, bg 1, and the rest are 0
seed <- bg+2*fg
plot(seed)

## ------------------------------------------------------------------------
edges <- imgradient(im,"xy") %>% enorm
p <- 1/(1+edges)
plot(p)

## ------------------------------------------------------------------------
ws <- (watershed(seed,p)==1)
plot(ws)

## ------------------------------------------------------------------------
ws <- bucketfill(ws,1,1,color=2) %>% {!( . == 2) }
plot(ws)

## ------------------------------------------------------------------------
clean(ws,5) %>% plot

## ------------------------------------------------------------------------
split_connected(ws) %>% purrr::discard(~ sum(.) < 100) %>%
    parany %>% plot

## ----fig.width=8---------------------------------------------------------
layout(t(1:2))
plot(im,main="Thresholding")
highlight(out)

plot(im,main="Watershed")
out2 <- clean(ws,5)
highlight(out2,col="green")

