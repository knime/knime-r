test.formatLong <- function() {
  df <- read.csv(system.file("csv","frog2001pcru.csv", package = "unmarked"))
  umf <- formatLong(df, type = "unmarkedFrameOccu")
  ## Add some assertions...
}


test.formatMult <- function() {
  test <- expand.grid.df(expand.grid(site = LETTERS[1:4], visit = 1:3), 
                         data.frame(year = 2015:2016))
  test <- test[with(test, order(site, year, visit)), ]
  test <- test[, c("year", "site", "visit")]
  
  set.seed(18939)
  test <- within(test, {
    ocov = round(rnorm(nrow(test)), 2)
    scov = 2 * as.numeric(test$site)
    yscov = 1.3 * as.numeric(interaction(test$site, test$year))
    obsfac = factor(sample(LETTERS[1:2], nrow(test), replace = TRUE))
    sitefac = factor(round(as.numeric(site)/5))
    ysfac = factor(round(as.numeric(interaction(site, year))/10))
    y  = rpois(nrow(test), lambda = 2)
  })

  withfac <- formatMult(test) 
  
  checkEquals(withfac,
              new("unmarkedMultFrame"
                  , numPrimary = 2L
                  , yearlySiteCovs = structure(list(ysfac = structure(c(1L, 1L, 1L, 2L, 1L, 2L, 1L, 
                                                                        2L), .Label = c("0", "1"), class = "factor"), 
                                                    yscov = c(1.3, 6.5, 2.6, 7.8, 3.9, 9.1, 5.2, 10.4)), 
                                               .Names = c("ysfac", "yscov"), row.names = c(NA, -8L), class = "data.frame")
                  , y = structure(c(0L, 1L, 3L, 3L, 2L, 2L, 2L, 1L, 1L, 0L, 3L, 3L, 1L, 
                                    1L, 0L, 0L, 3L, 1L, 2L, 2L, 2L, 1L, 3L, 3L), .Dim = c(4L, 6L), .Dimnames = list(
                                      structure(c("A", "B", "C", "D"), .Names = c("1", "43", "85", "127")), 
                                      structure(c("2015-1", "2015-2", "2015-3", "2016-1", "2016-2", "2016-3"), 
                                                .Names = c("1", "8", "15", "22", "29", "36"))))
                  , obsCovs = structure(list(visit = c(1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3), 
                                             obsfac = structure(c(2L, 1L, 1L, 1L, 2L, 2L, 2L, 1L, 1L, 1L, 2L, 1L, 2L, 2L, 1L, 
                                                                  1L, 2L,2L, 2L, 1L, 2L, 2L, 1L, 1L), .Label = c("A", "B"), class = "factor"), 
                                             ocov = c(0.28, -1.41, -0.31, 0.05, -0.53, 0.84, -0.95, 1.63, 0.87, 1.03, 1.41, 
                                                      1.25, -0.32, 0.11, -0.45, -0.83, 0.17, 0.28, -0.13, -1.86, -1.82, 0.11, 
                                                      1.29, -0.31)), .Names = c("visit", "obsfac", "ocov"), 
                                        class = "data.frame", row.names = c(NA, -24L))
                  , siteCovs = structure(list(sitefac = structure(c(1L, 1L, 2L, 2L), .Label = c("0", "1"), class = "factor"), 
                                              scov = c(2, 4, 6, 8)), .Names = c("sitefac", "scov"), row.names = c(NA, -4L), class = "data.frame")
                  , mapInfo = NULL
                  , obsToY = structure(c(1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 
                                         0, 0, 0, 0, 0, 0, 1), .Dim = c(6L, 6L))
              ))
}