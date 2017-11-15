## ----setup,echo=FALSE,message=FALSE,include=FALSE------------------------
#source('setup.R')
require(mosaic)
require(parallel)
require(ggformula)
options(digits=4)
theme_set(theme_bw())
trellis.par.set(theme=col.mosaic())
set.seed(123)
#knit_hooks$set(inline = function(x) {
#	if (is.numeric(x)) return(knitr:::format_sci(x, 'latex'))
#	x = as.character(x)
#	h = knitr:::hilight_source(x, 'latex', list(prompt=FALSE, size='normalsize'))
#	h = gsub("([_#$%&])", "\\\\\\1", h)
#	h = gsub('(["\'])', '\\1{}', h)
#	gsub('^\\\\begin\\{alltt\\}\\s*|\\\\end\\{alltt\\}\\s*$', '', h)
#})
knitr::opts_chunk$set(
	dev="pdf",
	eval=FALSE,
	tidy=FALSE,
	fig.align='center',
	fig.show='hold',
	message=FALSE
	)

## ------------------------------------------------------------------------
#  apropos()
#  ?
#  ??
#  example()

## ------------------------------------------------------------------------
#  # basic ops: + - * / ^ ( )
#  log(); exp(); sqrt()

## ----highlight=FALSE-----------------------------------------------------
#  log10(); abs(); choose()

## ------------------------------------------------------------------------
#  goal(y ~ x | z, data = mydata, ...)

## ------------------------------------------------------------------------
#  favstats()   # mosaic
#  tally()      # mosaic
#  mean()       # mosaic augmented
#  median()     # mosaic augmented
#  sd()         # mosaic augmented
#  var()        # mosaic augmented
#  diffmean()   # mosaic

## ----highlight=FALSE-----------------------------------------------------
#  quantile()   # mosaic augmented
#  prop()       # mosaic
#  perc()       # mosaic
#  rank()
#  IQR()        # mosaic augmented
#  min(); max() # mosaic augmented

## ------------------------------------------------------------------------
#  gf_boxplot()      # ggformula
#  gf_point()        # ggformula
#  gf_histogram()    # ggformula
#  gf_density()      # ggformula
#  gf_dens()         # ggformula
#  gf_freqpol()      # ggformula
#  gf_qq()           # ggformula
#  gf_fun()          # ggformula
#  makeFun()         # mosaic

## ----highlight=FALSE-----------------------------------------------------
#  gf_dot()          # ggformula
#  gf_bar()          # ggformula
#  gf_col()          # ggformula

## ----eval=FALSE----------------------------------------------------------
#  mplot(HELPrct)

## ------------------------------------------------------------------------
#  rflip()     # mosaic
#  do()        # mosaic
#  sample()    # mosaic augmented
#  resample()  # with replacement
#  shuffle()   # mosaic

## ----highlight=FALSE-----------------------------------------------------
#  rbinom()
#  rnorm()     # etc, if needed

## ------------------------------------------------------------------------
#  gf_dist()    # ggformula
#  # plain
#  pbinom(); pnorm();
#  # mosaic augmented
#  xpnorm(); xpchisq(); xpt()
#  xqbinom(); xqnorm();
#  xqchisq(); xqt()

## ------------------------------------------------------------------------
#  t.test()       # mosaic augmented
#  binom.test()   # mosaic augmented
#  prop.test()    # mosaic augmented
#  xchisq.test()            # mosaic
#  fisher.test()
#  pval()                   # mosaic
#  model <- lm()     # linear models
#  summary(model)
#  coef(model)
#  confint(model) # mosaic augmented
#  anova(model)
#  makeFun(model)           # mosaic
#  resid(model); fitted(model)
#  gf_model(model)       # ggformula

## ----highlight=FALSE-----------------------------------------------------
#  mplot(TukeyHSD(model))
#  model <- glm() # logistic reg.

## ------------------------------------------------------------------------
#  nrow(); ncol(); dim()
#  inspect()            # mosaic
#  names()
#  head(); tail()
#  factor()

## ----highlight=FALSE-----------------------------------------------------
#  read.file()          # mosaic
#  with()
#  summary()
#  glimpse()            # dplyr
#  ntiles()             # mosaic
#  cut()
#  c()
#  cbind(); rbind()
#  colnames()
#  rownames()
#  relevel()
#  reorder()

## ----highlight=FALSE-----------------------------------------------------
#  rep()
#  seq()
#  sort()
#  rank()

## ----highlight=FALSE-----------------------------------------------------
#  select()             # dplyr
#  mutate()             # dplyr
#  filter()             # dplyr
#  arrange()            # dplyr
#  summarise()          # dplyr
#  group_by()           # dplyr
#  left_join()          # dplyr
#  inner_join()         # dplyr

## ----more-hooks,eval=TRUE,echo=FALSE-------------------------------------
knitr::opts_chunk$set(
	eval=TRUE, 
  size='small',
	fig.width=4,
	fig.height=1.9,
	fig.align="center",
	out.width=".25\\textwidth",
	out.height=".125\\textwidth",
	tidy=TRUE,
	comment=NA
)

## ----echo=FALSE-----------------------
options(width=40)
options(show.signif.stars=FALSE)

## ----coins,fig.keep="last"------------
rflip(6)
do(2) * rflip(6)
coins <- do(1000)* rflip(6)
tally(~ heads, data=coins)

## -------------------------------------
tally(~ heads, data=coins, format="perc")
tally(~ (heads>=5 | heads<=1) , data=coins)

## ----coins-hist,fig.keep="last"-------
gf_histogram(~ heads, data = coins, binwidth = 1,
            fill = ~ (heads >=5 | heads <= 1))

## ----tally----------------------------
tally(sex ~ substance, data=HELPrct)
mean(age ~ sex, data = HELPrct)
diffmean(age ~ sex, data = HELPrct)
favstats(age ~ sex, data = HELPrct)

## ----densityplot,fig.height=2.4, tidy = FALSE----
gf_dens(~ age | sex, data = HELPrct,
                color = ~ substance) 

## ----bwplot---------------------------
gf_boxplot(age ~ substance | sex, data = HELPrct)

## ----message=FALSE--------------------
pval(binom.test(~ sex, data = HELPrct))
confint(t.test(~ age, data = HELPrct))

## ----tidy=FALSE-----------------------
model <- 
  lm(age ~ sex + substance, data = HELPrct) 
anova(model)

## ----tidy=FALSE-----------------------
gf_point(Sepal.Length ~ Sepal.Width, 
        color = ~Species, data = iris) 

## ----fig.keep="last", tidy=FALSE, fig.height=2.3----
model <- 
  lm(length ~ width + sex, data = KidsFeet)
l <- makeFun(model)
l(width = 8.25, sex = "B")
gf_point(length ~ width, data = KidsFeet,
         color = ~ sex) %>%
  gf_fun(l(w, sex = "B") ~ w, color = ~"B") %>%
  gf_fun(l(w, sex = "G") ~ w, color = ~"G")

## ----fig.height=1.75------------------
gf_dist("chisq", df=4)

## ----include=FALSE--------------------
tally(homeless ~ sex, data = HELPrct)

## ----include=FALSE--------------------
chisq.test(tally(homeless ~ sex, data = HELPrct))
prop.test(homeless ~ sex, data = HELPrct)

