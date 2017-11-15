## CHANGES in phylobase VERSION 0.8.4

*  CRAN maintenance release

## CHANGES in phylobase VERSION 0.8.2

*  Fix typo in examples of phylo4d methods.

##  CHANGES IN phylobase VERSION 0.8.0

### New features

*  Initial basic support for converting RNeXML objects in phylo4 and phylo4d
format.

*  New methods: `internalEdges()`, `terminalEdges()`

* `descendants()` has now a `"ALL"` argument to include self in results

* New method: `nodeHeight()`  provides a consistent and comprehensive way of
  calculating the distance between a node and either the root or the tips. (fix
  #3)

* The replacement methods for `labels`, `tipLabels`, `nodeLabels`, `edgeLabels`
  now accept `NA` or `NULL` to remove labels (fix #2)

### Major changes

* `readNexus` and `readNewick` now internally use the package `rncl` to parse
  files. They also use a different approach to reconstruct the edge
  matrix. These changes make file parsing faster. Objects created with this new
  approach may not exactly be identical to those created in previous versions as
  node numbering might differ, they should however be fully compatible with each
  others.

* `readNexus` and `readNewick` can now parse tree files with trees containing a
  subset of the taxa listed in the TAXA Block.

* Source code for the package is hosted on GitHub at https://github.com/fmichonneau/phylobase

### Minor changes

*  All tests done with testthat

*  `rootNode` returns the rootNode using the same format as `getNode()`.

*  All documentation is in Roxygen format

*  `hasPoly`, `hasRetic`, `hasSingle` are now methods instead of functions.

### Deprecated functions

* `nodeDepth` and `depthTips` are now deprecated and are replaced by `nodeHeight`

### Bug fixes

*  Fix bug: `NA` in labels were considered duplicated by `checkPhylo4()`.

*  Fix bug #605 (R-forge) -- treePlot subsets numeric data for plotting.

*  Fix bug #4: `descendants()` behave like `ancestors()` when provided with a
   vector of nodes and is consistent across all arguments.


##  CHANGES IN phylobase VERSION 0.6.8

*   Not many user-visible changes, most are related to improving speeds
during test of object validation (most tests done in C++) and to getNode
that is used by many functions.

*  Changes to package structure to make it compatible with devtools
(switching testing to testthat -- partial at this stage) and docs to
roxygen format (partial at this stage).

*  Changes to package structure to comply with new Rcpp standards

## CHANGES IN phylobase VERSION 0.6.5

*  Updates from cout/cerr to Rcpp::Rcout/Rcerr

*  Comments in Nexus tree strings are being removed before being processed by
readNCL

## CHANGES IN phylobase VERSION 0.6.3

*   Fixed bugs in getNode in cases where labels included regexpr
metacharacters and when a tip was labelled 0

*   New methods: depthTips, nodeDepth and isUltrametric


## CHANGES IN phylobase VERSION 0.6.2

*   Improve handling of errors returned by NCL (NxsException)

*   Fix bug in case state labels are missing from the NEXUS file

*   Upgrade to NCL 2.1.14

## CHANGES IN phylobase VERSION 0.6.1

*   Fix bugs that prevented building on Windows 64-bit systems


## CHANGES IN phylobase VERSION 0.6

### MAJOR CHANGES

*   Updated to the Nexus Class Library (NCL) 2.1.12.

*   Changed the way NCL is built during the installation process.

*   Complete rewrite of the function readNexus which brings many new
functionalities.

*   Nodes labels do not have to be unique.


### NEW FEATURES

*   In readNexus, the option return.labels gives the state labels of the
characters.

*   It is now possible to import several types of  data blocks in a single
NEXUS file with readNexus.

*   The function phylobase.options() provides global options to control the
behavior of the phylo4/phylo4d validator.

*   The new method hasDuplicatedLabels() indicates whether any node labels
are duplicated.

*   The new method nData() returns the number of datasets associated with
a tree.

*   The column that contains the labels can now be specified by its name in
the function formatData()

### MINOR CHANGES

*   The function getNode() has been modified to allow node matching in the
case of non-unique labels.

*   Many new unit tests.

### BUG FIXES

*   Far too many to document.  See the SVN log for details.

### KNOWN ISSUES

*   Unrooted trees are not supported by all functions, e.g. plot() and
reorder().

*   Factors are not supported by the default plotting method.


## CHANGES IN phylobase VERSION 0.5

### MAJOR CHANGES

*   A var-cov matrix tree class, phylo4vcov, and methods for converting to
and from other classes now exists.

*   Replaced separate the tip.label and node.label slots with a unified
label slot in the phylo4 class definition.

*   Replaced separate the tip.data and node.data into a single data slot in
the phylo4d class definition.

*   The phylo4 class grew a annotate slot.

*   The phylo4d class grew a metadata slot.

*   Added an order slot to the phylo4 class definition and updated as()
methods to assign the proper order (if possible) when converting
between ape and phylobase tree formats.

*   The Nnode slot was removed from the phylo4 class definition.

*   An explicit root edge has been added to the edge matrix with 0 as the
ancestor and nTips(phy) + 1 as the rood node.

*   The edgeLabels() and edgeLength() accessors now return vectors with
named elements in the same order as rows of the edge matrix, even when
some/all values are missing.

*   The labels() accessor and nodeID() methods now always return labels in
ascending order of node ID

*   Many function and argument names and defaults have been changed to make
them more consistent most functions follow the getNode() pattern.

*   The plotting functions have been replaced (see below).

*   Now, data are matched against node numbers instead of node labels.

*   Tip and internal node labels have now internal names that are character
strings of the node number they correspond to. Thus it is possible to
store labels in any order and assignment of labels more robust.

*   We now use the RUnit package (not required for normal use) for adding
unit tests.  Adding unit tests to inst/unitTests/ is now preferred over
the tests/ directory.

*   Numerous changes to pruning and tree subsetting code.  It is
considerably more robust and no longer relies on calls to APE.

### NEW FEATURES

*   Added a function nodeType() for identifying whether a node is root,
tip or internal.

*   Changed nodeNumbers to nodeId() and extended it abilities.

*   Added method reorder() for converting edge matrices into preorder or
postorder.

*   Added the edgeOrder accessor to get the order of a phylobase object.

*   Added a package help file accessible from ?phylobase.

*   Added labels()<- for assigning labels.

*   Added edgeLength()<- for assigning edgeLengths.

*   Added a phylo4() method for importing APE phylo objects.

*   Added a hasTipData() method.

*   Added a edgeId() method.

*   Created the addData() method for adding data to phylo4 objects.

*   Added tipData and nodeData getter/setter methods

*   If all node.labels are numerical values, they are automatically
converted as data. Useful when importing consensus tree from MrBayes.

*   It is now possible to print tree objects in edge order using the
edgeOrder argument in printphylo4().

*   reorder(), descendants(), ancestors(), and portions of the plotting code
have been recoded in C to improve performance.

*   Added a developer vignette to document and guide development of the
phylobase package.

*   The previous plotting functions, based on base graphics, have been
replaced with function based on the grid graphics device.

*   A S4 generic plot() function, calling treePlot() has been added it
dispatches a plotting function based on object class and arguments.

*   Plots using grid based code can be inserted at the tree tips using the
tip.plot.fun argument in plot()

*   The getNode() method has been enhanced to allow matching against
specific node types, and if the requested node is missing, all nodes of
specified type are returned.

*   Changed getEdge() to allow no node argument, which returns all edges
appropriate for the given type.

### CHANGES

*   Node labels are, if not supplied, a vector of NA.

*   printphylo() is now deprecated, print() and summary() now alsow work on
empty objects.

*   phylo4() is now and S4 generic with signature "matrix".

*   phylobase now uses a NAMESPACE file.

*   Legacy plotting code (0.4) can be found in the SVN repo tags directory.

*   The tdata default "type" argument changed to 'all'.

*   Row names now stored internally as numeric, not character.

### BUG FIXES

*   Far too many to document.  See the SVN log for details.

### KNOWN ISSUES

*   Unrooted trees are not supported by all functions, e.g. plot() and
reorder().

*   Factors are not supported by the default plotting method.

*   The Nexus Class Library is build for the system default ARCH which may
or may not be the architecture that R and the rest of the package is
built with.  If this occurs the package will fail to load.

*   Unique labels are required for internal nodes, this behavior will be
changed in the future.
