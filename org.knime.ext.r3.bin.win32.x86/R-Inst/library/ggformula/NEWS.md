# ggformula 0.6.0

Mostly minor changes:

  * Improved documentation of gf_ functions.
  * na.warn() is now re-exported.
  * Bug fix in gf_violin().
  * Reformatted quick help messages.


# ggformula 0.5.0

For version 0.5, the internals of `ggformula` have been largely redesigned to allow 
implementation of some new features.  The new version relies much less on string parsing.

  * Some important changes to formula parsing include
    * `attribute:value` and `attribute::expression` are no longer supported within the main formula.
    * In exchange, things like `gf_point(1:10 ~ 1:10)` work, making it simpler to create on the fly plots
      without having to build a data frame first.
    * `y ~ 1` is equivalent to `~ y` in functions that allow the `~ y` formula shape.  Example: `gf_histogram(age ~ 1)` and `gf_histogram( ~ age)` are equivalent
    
  * Some new functions have been added
    * `gf_dist()` can plot distributions
    * `gf_dhistogram()` plots density histograms by default
    * `gf_ash()` creates ASH plots
    
  * `df_stats()` has been improved to handle one-sided formulas better.
  
  * Secondary layers are now able to inherit both data and formula-defined attributes from the primary layer.  Use 
  `inherit = FALSE` if you don't want inheritance.  (A few functions have `inherit = FALSE` as their default because
  it seems unlikely that inheriting will be desireable.)
  
  
  
  
# ggformula 0.4.0

Version 0.4.0 constitutes a stable beta release.  Changes to the API are still possible, but more 
likely future changes will focus on expansion of the suite of functions supplied, changes to
the internal implementation, and improved documentation.

  * Separated `ggformula` from `statisticalModeling`.
  * Added support for many more geoms.
  * Improved parsing of formulas.  This is still a bit clunky since the order of operations in
  R formulas does not match what we would prefer in this package.
  * `data` may now be an expression (like `data = KidsFeet %>% filter(sex == "G")`)
  * Added support for geoms that have different required aesthetics.
  * Added support for functions that allow more than one formula shape.  Example: `gf_histogram()`
  accepts formulas with shape ` ~ x` or `y ~ x`.  This makes it possible to create density 
  histograms with `gf_histogram()`.
  * Parntheses now halt parsing of formulas.  This allows for on-the-fly computations in formulas.  Typically these computed expressions must be within parentheses to avoid formula expansion.
  * Use `::` to indicate mapping aesthetics.  (`:` will autodetect, but only if the value
is the name of a variable in the data set.)  This should be considered experimental.
  * Added wrappers `gf_lims()`, `gf_labs()`, `gf_theme()`, `gf_facet_grid()`, `gf_facet_wrap()`
  * Added `gf_refine()` which can be used to pass by chaining anything that would have been "added" in ``ggplot2`
  * Expanded and improved vignette describing use of the package.
  * Added two tutorials.
  * Added "quick help" for plotting functions.
  * Added `gf_lm()`, which is `gf_smooth()` with `method = "lm"`
  * Added `gf_dens()` which is `gf_line()` with `stat = "density"`.



