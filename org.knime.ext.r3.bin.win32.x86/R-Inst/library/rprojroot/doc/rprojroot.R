## ------------------------------------------------------------------------
basename(getwd())

## ------------------------------------------------------------------------
rprojroot::is_r_package

## ------------------------------------------------------------------------
rprojroot::is_rstudio_project

## ------------------------------------------------------------------------
rprojroot::has_file(".git/index")

## ------------------------------------------------------------------------
root <- rprojroot::is_r_package

## ------------------------------------------------------------------------
readLines(root$find_file("DESCRIPTION"), 3)

## ------------------------------------------------------------------------
root_file <- root$make_fix_file()

## ------------------------------------------------------------------------
withr::with_dir(
  "../..",
  readLines(root_file("DESCRIPTION"), 3)
)

## ------------------------------------------------------------------------
library(rprojroot)

# List all files and directories below the root
dir(find_root(has_file("DESCRIPTION")))

# Find a file relative to the root
file.exists(find_root_file("R", "root.R", criterion = has_file("DESCRIPTION")))

## ------------------------------------------------------------------------
has_file("DESCRIPTION")

## ------------------------------------------------------------------------
as.root_criterion("DESCRIPTION")

## ------------------------------------------------------------------------
criteria

## ------------------------------------------------------------------------
has_license <- has_file("LICENSE")
has_license

is_projecttemplate_project <- has_file("config/global.dcf", "^version: ")
is_projecttemplate_project

## ------------------------------------------------------------------------
is_r_package | is_rstudio_project

## ------------------------------------------------------------------------
# Print first lines of the source for this document
head(readLines(find_package_root_file("vignettes", "rprojroot.Rmd")))

## ------------------------------------------------------------------------
P <- find_package_root_file

# Use a shorter alias
file.exists(P("vignettes", "rprojroot.Rmd"))

## ----error = TRUE--------------------------------------------------------
# Use the has_license criterion to find the root
R <- has_license$find_file
R

# Our package does not have a LICENSE file, trying to find the root results in an error
R()

## ------------------------------------------------------------------------
# Define a function that computes file paths below the current root
F <- is_r_package$make_fix_file()
F

# Show contents of the NAMESPACE file in our project
readLines(F("NAMESPACE"))

## ------------------------------------------------------------------------
# Print the size of the namespace file, working directory outside the project
withr::with_dir(
  "../..",
  file.size(F("NAMESPACE"))
)

## ------------------------------------------------------------------------
is_testthat

## ------------------------------------------------------------------------
dir(is_testthat$find_file("hierarchy", path = is_r_package$find_file()))

