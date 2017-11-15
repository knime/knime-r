## ----setup, include = FALSE----------------------------------------------
library(dplyr)
library(lattice)
library(ggformula)
theme_set(theme_bw())
knitr::opts_chunk$set(
  fig.show = "hold",
  fig.width = 3.0,
  fig.height = 2.3
)

## ----results = "hide", fig.keep = "none"---------------------------------
# diamonds2 was called `recoded' in Nick's post
library(dplyr)
diamonds2 <- diamonds %>% 
  filter(color == "D" | color == "J") %>%
  mutate(col = as.character(color))

  mean(price ~ col, data = diamonds2)  
bwplot(price ~ col, data = diamonds2)
t.test(price ~ col, data = diamonds2)
    lm(price ~ col, data = diamonds2)

## ------------------------------------------------------------------------
gf_boxplot(price ~ col, data = diamonds2)
gf_violin(price ~ col, data = diamonds2)

## ------------------------------------------------------------------------
gf_boxplot(price ~ col, data = diamonds2, alpha = 0.05) %>%
gf_violin(price ~ col, data = diamonds2, alpha = 0.3, fill = "navy", col = NA)

## ------------------------------------------------------------------------
gf_boxplot(price ~ col, data = diamonds2, alpha = 0.05) %>%
  gf_violin(price ~ col, data = diamonds2, alpha = 0.3, fill = ~ col)

## ------------------------------------------------------------------------
xyplot(price ~ carat, groups = col, data = diamonds2,
       auto.key = TRUE, type = c("p", "r"), alpha = 0.5) 
gf_point(price ~ carat, color = ~ col, data = diamonds2, alpha = 0.5) %>%
   gf_lm(price ~ carat, color = ~ col, data = diamonds2, alpha = 0.5) 

## ------------------------------------------------------------------------
gf_violin()

## ---- fig.width = 6------------------------------------------------------
gf_violin(price ~ col, data = diamonds2,
          color = ~ col, fill = ~col, alpha = 0.2,
          scale = "count",
          draw_quantiles = c(0.25, 0.5, 0.75), size = 0.8,
          adjust = 1/2)

## ----fig.width = 7-------------------------------------------------------
gf_point(price ~ carat | col, data = diamonds2, alpha = 0.2) %>%
  gf_lm()
gf_point(price ~ carat | col ~ clarity, data = diamonds2, alpha = 0.2) %>%
  gf_lm()

## ----fig.keep = "none"---------------------------------------------------
gf_point(price ~ carat, data = diamonds2, alpha = 0.2, size = 0.5) %>%
  gf_lm(alpha = 0.5) %>%
  gf_facet_wrap( ~ color)

gf_point(price ~ carat, data = diamonds2, alpha = 0.2, size = 0.5) %>%
  gf_lm(alpha = 0.5) %>%
  gf_facet_grid( color ~ clarity)

## ---- fig.width = 5------------------------------------------------------
gf_point(price ~ carat, data = diamonds2, 
         color = ~ col, alpha = 0.2, size = 0.5) %>%
  gf_lm(alpha = 0.5) %>%
  gf_facet_wrap( ~ color) %>%
  gf_labs(title = "Price vs Size", subtitle = "(2 colors of diamonds)",
          caption = "source: ggplot2::diamonds",
          x = "size (carat)", y = "price (US dollars)"
  ) %>%
  gf_refine(scale_color_manual(values = c("red", "navy"), guide = "none")) %>%
  gf_theme(theme_minimal())

## ------------------------------------------------------------------------
diamonds %>%
  filter(color %in% c("D", "G", "J")) %>%
  mutate(col = as.character(color)) %>%
  gf_bar( ~ col)

