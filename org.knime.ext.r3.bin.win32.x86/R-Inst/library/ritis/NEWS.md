ritis 0.7.0
===========

### NEW FEATURES

* Now using new version of `solrium` package - users shouldn't
see any differences (#9)


ritis 0.6.0
===========

### NEW FEATURES

* Now using `crul` as underlying HTTP client (#5)

### BUG FIXES

* Base URL change for Solr service from `http` to `https` (#8)
* Fixed JSON parsing problem (#6)


ritis 0.5.4
===========

### BUG FIXES

* Base URL changed from `http` to `https`, was causing problems in some
requests, but not others. Changed to `https` (#4)


ritis 0.5.0
===========

### NEW FEATURES

* Re-released to CRAN
* Complete overhaul of the package API, simplifying all function
interfaces, using JSON by default, shorter names, reduce code reuse.
* Added functions for interacting with ITIS's new Solr
interface via use of `solrium`


ritis 0.0.3
===========

### BUG FIXES

* Removed dependency on plyr - moved from laply to lapply across functions.


ritis 0.0.2
===========

### BUG FIXES

* Temporarily removed all tests until they can be fixed and updated, and so that package passes checks.


ritis 0.0.1
===========

### NEW FEATURES

* released to CRAN
