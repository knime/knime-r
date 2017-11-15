## ----echo=FALSE----------------------------------------------------------
knitr::opts_chunk$set(
  comment = "#>",
  collapse = TRUE,
  warning = FALSE,
  message = FALSE
)

## ----eval=FALSE----------------------------------------------------------
#  install.packages("wikitaxa")

## ----eval=FALSE----------------------------------------------------------
#  devtools::install_github("ropensci/wikitaxa")

## ------------------------------------------------------------------------
library("wikitaxa")

## ----eval=FALSE----------------------------------------------------------
#  wt_data("Poa annua")

## ------------------------------------------------------------------------
wt_data_id("Mimulus foliatus")

## ------------------------------------------------------------------------
pg <- wt_wiki_page("https://en.wikipedia.org/wiki/Malus_domestica")
res <- wt_wiki_page_parse(pg)
res$iwlinks

## ------------------------------------------------------------------------
res <- wt_wikipedia("Malus domestica")
res$common_names
res$classification

## ----eval=FALSE----------------------------------------------------------
#  # French
#  wt_wikipedia(name = "Malus domestica", wiki = "fr")
#  # Slovak
#  wt_wikipedia(name = "Malus domestica", wiki = "sk")
#  # Vietnamese
#  wt_wikipedia(name = "Malus domestica", wiki = "vi")

## ------------------------------------------------------------------------
wt_wikipedia_search(query = "Pinus")

## ----eval=FALSE----------------------------------------------------------
#  wt_wikipedia_search(query = "Pinus", wiki = "fr")

## ------------------------------------------------------------------------
pg <- wt_wiki_page("https://commons.wikimedia.org/wiki/Abelmoschus")
res <- wt_wikicommons_parse(pg)
res$common_names[1:3]

## ------------------------------------------------------------------------
res <- wt_wikicommons("Abelmoschus")
res$classification
res$common_names

## ------------------------------------------------------------------------
wt_wikicommons_search(query = "Pinus")

## ------------------------------------------------------------------------
pg <- wt_wiki_page("https://species.wikimedia.org/wiki/Malus_domestica")
res <- wt_wikispecies_parse(pg, types = "common_names")
res$common_names[1:3]

## ------------------------------------------------------------------------
res <- wt_wikispecies("Malus domestica")
res$classification
res$common_names

## ------------------------------------------------------------------------
wt_wikispecies_search(query = "Pinus")

