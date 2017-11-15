## ------------------------------------------------------------------------
library("dbscan")
data("moons")
plot(moons, pch=20)

## ------------------------------------------------------------------------
  cl <- hdbscan(moons, minPts = 5)
  cl

## ------------------------------------------------------------------------
 plot(moons, col=cl$cluster+1, pch=20)

## ------------------------------------------------------------------------
cl$hc

## ------------------------------------------------------------------------
plot(cl$hc, main="HDBSCAN* Hierarchy")

## ------------------------------------------------------------------------
cl <- hdbscan(moons, minPts = 5)
check <- rep(F, nrow(moons)-1)
core_dist <- kNNdist(moons, k=5-1)[,5-1]

## cutree doesn't distinguish noise as 0, so we make a new method to do it manually 
cut_tree <- function(hcl, eps, core_dist){
  cuts <- unname(cutree(hcl, h=eps))
  cuts[which(core_dist > eps)] <- 0 # Use core distance to distinguish noise
  cuts
}

eps_values <- sort(cl$hc$height, decreasing = T)+.Machine$double.eps ## Machine eps for consistency between cuts 
for (i in 1:length(eps_values)) { 
  cut_cl <- cut_tree(cl$hc, eps_values[i], core_dist)
  dbscan_cl <- dbscan(moons, eps = eps_values[i], minPts = 5, borderPoints = F) # DBSCAN* doesn't include border points
  
  ## Use run length encoding as an ID-independent way to check ordering
  check[i] <- (all.equal(rle(cut_cl)$lengths, rle(dbscan_cl$cluster)$lengths) == "TRUE")
}
print(all(check == T))

## ------------------------------------------------------------------------
 plot(cl)

## ------------------------------------------------------------------------
 plot(cl, gradient = c("yellow", "orange", "red", "blue"))

## ------------------------------------------------------------------------
plot(cl, gradient = c("purple", "blue", "green", "yellow"), scale=1.5)

## ------------------------------------------------------------------------
plot(cl, gradient = c("purple", "blue", "green", "yellow"), show_flat = T)

## ------------------------------------------------------------------------
print(cl$cluster_scores)

## ------------------------------------------------------------------------
  head(cl$membership_prob)

## ------------------------------------------------------------------------
  plot(moons, col=cl$cluster+1, pch=21)
  colors <- mapply(function(col, i) adjustcolor(col, alpha.f = cl$membership_prob[i]), 
                   palette()[cl$cluster+1], seq_along(cl$cluster))
  points(moons, col=colors, pch=20)

## ------------------------------------------------------------------------
  top_outliers <- order(cl$outlier_scores, decreasing = T)[1:10]
  colors <- mapply(function(col, i) adjustcolor(col, alpha.f = cl$outlier_scores[i]), 
                   palette()[cl$cluster+1], seq_along(cl$cluster))
  plot(moons, col=colors, pch=20)
  text(moons[top_outliers, ], labels = top_outliers, pos=3)

## ------------------------------------------------------------------------
data("DS3")
plot(DS3, pch=20, cex=0.25)

## ------------------------------------------------------------------------
cl2 <- hdbscan(DS3, minPts = 25)
cl2

## ------------------------------------------------------------------------
  plot(DS3, col=cl2$cluster+1, 
       pch=ifelse(cl2$cluster == 0, 8, 1), # Mark noise as star
       cex=ifelse(cl2$cluster == 0, 0.5, 0.75), # Decrease size of noise
       xlab=NA, ylab=NA)
  colors <- sapply(1:length(cl2$cluster), 
                   function(i) adjustcolor(palette()[(cl2$cluster+1)[i]], alpha.f = cl2$membership_prob[i]))
  points(DS3, col=colors, pch=20)

## ------------------------------------------------------------------------
  plot(cl2, scale = 3, gradient = c("purple", "orange", "red"), show_flat = T)

