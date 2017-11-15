## ----include = FALSE-----------------------------------------------------
library(ggformula)
library(dplyr)
library(ggplot2)
library(mosaicData)
knitr::opts_chunk$set(
  fig.show = "hold",
  out.width = "45%"
)
theme_set(theme_light())

## ---- eval = FALSE-------------------------------------------------------
#  gf_plottype(formula, data = mydata)

## ------------------------------------------------------------------------
library(ggformula)
gf_point(mpg ~ hp, data = mtcars)

## ------------------------------------------------------------------------
gf_point(mpg ~ hp, color = ~ cyl, size = ~carb, alpha = 0.50, data = mtcars) 

## ------------------------------------------------------------------------
# set alpha using a function argument instead of in the formula
gf_point(mpg ~ hp, color = ~cyl, size = ~carb, alpha = 0.50, data = mtcars) 

## ------------------------------------------------------------------------
gf_point(mpg ~ hp,  color = ~factor(cyl), size = ~carb, alpha = 0.75, data = mtcars)
gf_point(mpg ~ hp,  color = ~cylinders, size = ~carb, alpha = 0.75, 
         data = mtcars %>% mutate(cylinders = factor(cyl)))

## ----fig.show = "hold", out.width = "30%", warning=FALSE-----------------
data(Runners, package = "statisticalModeling")
Runners <- Runners %>% filter( ! is.na(net))
gf_density( ~ net, data = Runners)
gf_density( ~ net,  fill = ~sex,  alpha = 0.5, data = Runners)
# gf_dens() is similar, but there is no line at bottom/sides, and it is not "fillable"
gf_dens( ~ net, color = ~sex, alpha = 0.7, data = Runners)    

## ------------------------------------------------------------------------
# less smoothing
gf_dens( ~ net, color = ~sex, alpha = 0.7, data = Runners, adjust = 0.25)  
# more smoothing
gf_dens( ~ net, color = ~sex, alpha = 0.7, data = Runners, adjust = 4)     

## ----fig.show = "hold", warning=FALSE------------------------------------
gf_density( ~ net, fill = ~sex, color = NA, position = "stack", data = Runners)
gf_density( ~ net, fill = ~sex, color = NA, position = "fill", data = Runners, adjust = 2)

## ------------------------------------------------------------------------
gf_point(age ~ sex, alpha = 0.05, data = Runners)
gf_jitter(age ~ sex, alpha = 0.05, data = Runners)

## ----fig.show = "hold", warning = FALSE----------------------------------
gf_boxplot(net ~ sex, color = "red", data = Runners)
gf_boxplot(net ~ sex, color = ~start_position, data = Runners)

## ------------------------------------------------------------------------
gf_boxplot(net ~ year, data = Runners)

## ------------------------------------------------------------------------
gf_boxplot(net ~ year, group = ~ year, data = Runners)

## ------------------------------------------------------------------------
# add a new variable to the data
Runners$the_year <- as.character(Runners$year)               # in base R
Runners <- Runners %>% mutate(the_year = as.character(year)) # in dplyr
gf_boxplot(net ~ the_year, color = ~sex, data = Runners)
# or do it on the fly
gf_boxplot(net ~ factor(year), color = ~sex, data = Runners)

## ----fig.show = "hold"---------------------------------------------------
gf_density_2d(net ~ age, data = Runners)
gf_hex(net ~ age, data = Runners)

## ----fig.show = "hold", out.width = "30%"--------------------------------
# create a categorical variable
mtcars <- mtcars %>% mutate(n_cylinders = as.character(cyl)) 
gf_line(mpg ~ hp, data = mtcars)
gf_path(mpg ~ hp, data = mtcars)
gf_line(mpg ~ hp, color = ~n_cylinders, data = mtcars)

## ------------------------------------------------------------------------
library(mosaicData)
gf_point(births ~ date, data = Births78)

## ------------------------------------------------------------------------
library(mosaicData)
gf_point(births ~ date, color = ~wday, data = Births78)

## ------------------------------------------------------------------------
gf_line(births ~ date, color = ~wday, data = Births78)

## ------------------------------------------------------------------------
require(weatherData)
Temps <- NewYork2013 %>%
  mutate(date = lubridate::date(Time),
         month = lubridate::month(Time)) %>% 
  filter(month <= 4) %>%
  group_by(date) %>%
  summarise(
    hi = max(Temperature, na.rm = TRUE),
    lo = min(Temperature, na.rm = TRUE)
  )   
gf_linerange(lo + hi  ~ date, color = ~hi, data = Temps)
gf_ribbon(lo + hi ~ date, data = Temps, color = "navy", alpha = 0.3)

## ------------------------------------------------------------------------
gf_point(length ~ sex, color = ~ domhand, data = KidsFeet,
         position = position_jitterdodge(jitter.width = 0.2, dodge.width = 0.4))

## ------------------------------------------------------------------------
gf_bar( ~ age, data = HELPrct, stat = "bin")

## ------------------------------------------------------------------------
gf_jitter(length ~ sex, color = ~domhand, data = KidsFeet,
          width = 0.1, height = 0) %>%
  gf_line(length ~ sex, color = ~domhand, data = KidsFeet,
          group = ~domhand, stat="summary")

## ------------------------------------------------------------------------
gf_jitter(length ~ sex, color = ~domhand, data = KidsFeet,
          width = 0.1, height = 0, alpha = 0.3) %>%
  gf_pointrange(length + ..ymin.. + ..ymax.. ~ sex, 
                color = ~domhand, data = KidsFeet, 
                group = ~domhand, stat="summary")

## ------------------------------------------------------------------------
gf_point(length ~ sex, color = ~domhand, data = KidsFeet,
          width = 0.1, height = 0, alpha = 0.5,
          position = position_jitterdodge(jitter.width = 0.2, dodge.width = 0.3)) %>%
  gf_pointrange(length + ..ymin.. + ..ymax.. ~ sex, 
                color = ~domhand, data = KidsFeet, 
                group = ~domhand, stat="summary",
                fun.y = median, fun.ymin = min, fun.ymax = max,
                position = position_dodge(width = 0.6))

## ------------------------------------------------------------------------
gf_histogram( ~ age, data = Runners, alpha = 0.3, fill = "navy") %>%
  gf_freqpoly( ~ age)

## ------------------------------------------------------------------------
gf_density_2d(net ~ age, data = Runners) %>%
  gf_point(net ~ age, alpha = 0.01) 

## ----fig.show = "hold", warning=FALSE------------------------------------
gf_density_2d(net ~ age, data = Runners) %>% gf_facet_grid( ~ sex)
# the dot here is a bit strange, but required to make a valid formula
gf_density_2d(net ~ age, data = Runners) %>% gf_facet_grid( sex ~ .)
gf_density_2d(net ~ age, data = Runners) %>% gf_facet_wrap( ~ the_year)
gf_density_2d(net ~ age, data = Runners) %>% gf_facet_grid(start_position ~ sex)

## ------------------------------------------------------------------------
require(weatherData)
Temps <- NewYork2013 %>% mutate(city = "NYC") %>%
  bind_rows(Mumbai2013 %>% mutate(city = "Mumbai")) %>%
  bind_rows(London2013 %>% mutate(city = "London")) %>%
  mutate(date = lubridate::date(Time),
         month = lubridate::month(Time)) %>% 
  group_by(city, date) %>%
  summarise(
    hi = max(Temperature, na.rm = TRUE),
    lo = min(Temperature, na.rm = TRUE),
    mid = (hi + lo)/2
  )   
gf_ribbon(lo + hi ~ date, data = Temps, alpha = 0.3) %>%
  gf_facet_grid(city ~ .)

gf_linerange(lo + hi ~ date, color = ~ mid, data = Temps) %>%
  gf_facet_grid(city ~ .) %>%
  gf_refine(scale_colour_gradientn(colors = rev(rainbow(5))))

## ------------------------------------------------------------------------
gf_histogram( ~ age, data = Runners, alpha = 0.2, fill = "navy",
              binwidth = 5) %>%
  gf_freqpoly( ~ age, binwidth = 5) %>%
  gf_labs(x = "age (years)", title = "Age of runners") %>%
  gf_lims(x = c(20, 80)) %>%
  gf_theme(theme = theme_minimal())

gf_histogram( ~ age, data = Runners, alpha = 0.5, fill = "white",
              binwidth = 5) %>%
  gf_freqpoly( ~ age, color = "skyblue", binwidth = 5, size = 1.5) %>%
  gf_labs(x = "age (years)", title = "Age of runners") %>%
  gf_lims(x = c(20, 80)) %>%
  gf_theme(theme = theme_dark())

