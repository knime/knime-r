## ---- setup, include = FALSE---------------------------------------------
library(mosaic)   # Load additional packages here
library(ggformula)

# Some customization.  You can alter or delete as desired (if you know what you are doing).
trellis.par.set(theme = theme.mosaic()) # change default color scheme for lattice
theme_set(theme_bw())                   # change default theme for ggformula
knitr::opts_chunk$set(
  tidy = FALSE,     # display code as typed
  size = "small")   # slightly smaller font for code

## ------------------------------------------------------------------------
library(mosaic)   # loads ggformula as well
gf_histogram(~ age, data = HELPrct)

## ------------------------------------------------------------------------
gf_histogram(~ age, data = HELPrct,
             binwidth = 5)

## ------------------------------------------------------------------------
library(mosaic)   # loads lattice too
histogram(~ age, data = HELPrct)

## ------------------------------------------------------------------------
histogram(~ age, width = 5, data = HELPrct)

## ---- message = FALSE----------------------------------------------------
gf_dens(~ age, data = HELPrct)

## ---- message = FALSE----------------------------------------------------
gf_dens(~ age, data = HELPrct,
        color = ~ sex)

## ---- message = FALSE----------------------------------------------------
densityplot(~ age, data = HELPrct)

## ------------------------------------------------------------------------
densityplot(~ age, data = HELPrct,
            groups = sex,  auto.key = TRUE)

## ---- message = FALSE----------------------------------------------------
gf_boxplot(age ~ sex, data = HELPrct)

## ---- message = FALSE----------------------------------------------------
gf_boxplot(age ~ sex | homeless,
  data = HELPrct)

## ---- message = FALSE----------------------------------------------------
bwplot(age ~ sex, data = HELPrct)

## ---- message = FALSE----------------------------------------------------
bwplot(age ~ sex | homeless,
       data = HELPrct)

## ---- message = FALSE----------------------------------------------------
gf_point(cesd ~ age, data = HELPrct)

## ---- message = FALSE----------------------------------------------------
gf_point(cesd ~ age, data = HELPrct,
         color = ~ sex) %>%
  gf_lm()

## ---- message = FALSE----------------------------------------------------
xyplot(cesd ~ age, data = HELPrct)

## ---- message = FALSE----------------------------------------------------
xyplot(cesd ~ age,  data = HELPrct,
       groups = sex,
       type = c("p", "r"),
       auto.key = TRUE)

## ---- message = FALSE----------------------------------------------------
gf_point(cesd ~ age | sex,
         data = HELPrct) %>%
  gf_smooth()

## ------------------------------------------------------------------------
gf_point(cesd ~ age, data = HELPrct,
         color = ~ sex) %>%
  gf_lm() %>%
  gf_theme(legend.position = "top") %>%
  gf_labs(title = "This is my plot",
    x = "age (in years)",
    y = "CES-D measure of
depressive symptoms")

## ---- message = FALSE----------------------------------------------------
xyplot(cesd ~ age | sex,  data = HELPrct,
       type = c("p", "smooth"),
       auto.key = TRUE)

## ---- message = FALSE----------------------------------------------------
xyplot(cesd ~ age, groups = sex,
       type = c("p", "r"),
       auto.key = TRUE,
       main = "This is my plot",
       xlab = "age (in years)",
       ylab = "CES-D measure of
depressive symptoms",
       data = HELPrct)

