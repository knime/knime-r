``` r
library("RNeXML")
```

    ## Loading required package: ape

``` r
library("dplyr")
```

    ## 
    ## Attaching package: 'dplyr'
    ## 
    ## The following objects are masked from 'package:stats':
    ## 
    ##     filter, lag
    ## 
    ## The following objects are masked from 'package:base':
    ## 
    ##     intersect, setdiff, setequal, union

``` r
library("geiger")
knitr::opts_chunk$set(message = FALSE, comment = NA)
```

Let's generate a `NeXML` file using the tree and trait data from the `geiger` package's "primates" data:

``` r
data("primates")
add_trees(primates$phy) %>% 
  add_characters(primates$dat, ., append=TRUE) %>% 
  taxize_nexml() -> nex 
```

    Warning in taxize_nexml(.): ID for otu Alouatta_coibensis not found.
    Consider checking the spelling or alternate classification

    Warning in taxize_nexml(.): ID for otu Aotus_hershkovitzi not found.
    Consider checking the spelling or alternate classification

    Warning in taxize_nexml(.): ID for otu Aotus_miconax not found. Consider
    checking the spelling or alternate classification

    Warning in taxize_nexml(.): ID for otu Callicebus_cinerascens not found.
    Consider checking the spelling or alternate classification

    Warning in taxize_nexml(.): ID for otu Callicebus_dubius not found.
    Consider checking the spelling or alternate classification

    Warning in taxize_nexml(.): ID for otu Callicebus_modestus not found.
    Consider checking the spelling or alternate classification

    Warning in taxize_nexml(.): ID for otu Callicebus_oenanthe not found.
    Consider checking the spelling or alternate classification

    Warning in taxize_nexml(.): ID for otu Callicebus_olallae not found.
    Consider checking the spelling or alternate classification

    Warning in taxize_nexml(.): ID for otu Euoticus_pallidus not found.
    Consider checking the spelling or alternate classification

    Warning in taxize_nexml(.): ID for otu Lagothrix_flavicauda not found.
    Consider checking the spelling or alternate classification

    Warning in taxize_nexml(.): ID for otu Leontopithecus_caissara not found.
    Consider checking the spelling or alternate classification

    Warning in taxize_nexml(.): ID for otu Leontopithecus_chrysomela not found.
    Consider checking the spelling or alternate classification

    Warning in taxize_nexml(.): ID for otu Pithecia_aequatorialis not found.
    Consider checking the spelling or alternate classification

    Warning in taxize_nexml(.): ID for otu Pithecia_albicans not found.
    Consider checking the spelling or alternate classification

    Warning in taxize_nexml(.): ID for otu Procolobus_pennantii not found.
    Consider checking the spelling or alternate classification

    Warning in taxize_nexml(.): ID for otu Procolobus_preussi not found.
    Consider checking the spelling or alternate classification

    Warning in taxize_nexml(.): ID for otu Procolobus_rufomitratus not found.
    Consider checking the spelling or alternate classification

    Warning in taxize_nexml(.): ID for otu Tarsius_pumilus not found. Consider
    checking the spelling or alternate classification

(Note that we've used `dplyr`'s cute pipe syntax, but unfortunately our `add_` methods take the `nexml` object as the *second* argument instead of the first, so this isn't as elegant since we need the stupid `.` to show where the piped output should go...)

We now read in the three tables of interest. Note that we tell `get_characters` to give us species labels as there own column, rather than as rownames. The latter is the default only because this plays more nicely with the default format for character matrices that is expected by `geiger` and other phylogenetics packages, but is in general a silly choice for data manipulation.

``` r
otu_meta <- get_metadata(nex, "otus/otu")
taxa <- get_taxa(nex)
char <- get_characters(nex, rownames_as_col = TRUE)
```

We can take a peek at what the tables look like, just to orient ourselves:

``` r
otu_meta
```

    Source: local data frame [215 x 9]

          id property datatype content     xsi.type        rel
       (chr)    (lgl)    (lgl)   (lgl)        (chr)      (chr)
    1     m1       NA       NA      NA ResourceMeta tc:toTaxon
    2     m2       NA       NA      NA ResourceMeta tc:toTaxon
    3     m3       NA       NA      NA ResourceMeta tc:toTaxon
    4     m4       NA       NA      NA ResourceMeta tc:toTaxon
    5     m5       NA       NA      NA ResourceMeta tc:toTaxon
    6     m6       NA       NA      NA ResourceMeta tc:toTaxon
    7     m7       NA       NA      NA ResourceMeta tc:toTaxon
    8     m8       NA       NA      NA ResourceMeta tc:toTaxon
    9     m9       NA       NA      NA ResourceMeta tc:toTaxon
    10   m10       NA       NA      NA ResourceMeta tc:toTaxon
    ..   ...      ...      ...     ...          ...        ...
    Variables not shown: href (chr), otu (chr), otus (chr)

``` r
taxa
```

    Source: local data frame [233 x 5]

          id                       label about xsi.type  otus
       (chr)                       (chr) (chr)    (lgl) (chr)
    1    ou1 Allenopithecus_nigroviridis  #ou1       NA   os1
    2    ou2         Allocebus_trichotis  #ou2       NA   os1
    3    ou3           Alouatta_belzebul  #ou3       NA   os1
    4    ou4             Alouatta_caraya  #ou4       NA   os1
    5    ou5          Alouatta_coibensis  #ou5       NA   os1
    6    ou6              Alouatta_fusca  #ou6       NA   os1
    7    ou7           Alouatta_palliata  #ou7       NA   os1
    8    ou8              Alouatta_pigra  #ou8       NA   os1
    9    ou9               Alouatta_sara  #ou9       NA   os1
    10  ou10          Alouatta_seniculus #ou10       NA   os1
    ..   ...                         ...   ...      ...   ...

``` r
head(char)
```

                             taxa        x
    1 Allenopithecus_nigroviridis 8.465900
    2          Alouatta_seniculus 8.767173
    3               Galago_alleni 5.521461
    4             Galago_gallarum 5.365976
    5            Galago_matschiei 5.267858
    6               Galago_moholi 5.375278

Now that we have nice `data.frame` objects for all our data, it's easy to join them into the desired table with a few obvious `dplyr` commands:

``` r
taxa %>% 
  left_join(char, by = c("label" = "taxa")) %>% 
  left_join(otu_meta, by = c("id" = "otu")) %>%
  select(id, label, x, href)
```

    Warning in left_join_impl(x, y, by$x, by$y): joining factor and character
    vector, coercing into character vector

    Source: local data frame [233 x 4]

          id                       label        x
       (chr)                       (chr)    (dbl)
    1    ou1 Allenopithecus_nigroviridis 8.465900
    2    ou2         Allocebus_trichotis 4.368181
    3    ou3           Alouatta_belzebul 8.729074
    4    ou4             Alouatta_caraya 8.628735
    5    ou5          Alouatta_coibensis 8.764053
    6    ou6              Alouatta_fusca 8.554489
    7    ou7           Alouatta_palliata 8.791790
    8    ou8              Alouatta_pigra 8.881836
    9    ou9               Alouatta_sara 8.796339
    10  ou10          Alouatta_seniculus 8.767173
    ..   ...                         ...      ...
    Variables not shown: href (chr)

Because these are all from the same otus block anyway, we haven't selected that column, but were it of interest it is also available in the taxa table.
