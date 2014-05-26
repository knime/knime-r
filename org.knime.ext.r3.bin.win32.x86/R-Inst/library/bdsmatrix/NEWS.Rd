\name{NEWS}
\title{NEWS file for the bdsmatrix package}
\section{Changes in version 1.3.1}{
  \itemize{
    \item Avoid a "not initialized" compiler warning in gchol_bds.c
}}

\section{Changes in version 1.3}{
  \itemize{
    \item Make backsolve an S4 generic instead of S3.  The documentation
    file for chol in the Matrix package was a big help in sorting out
    how to make the CMD check process content.
    }}
\section{Changes in version 1.2}{
  \itemize{
    \item Add the backsolve method for ghcol and gchol.bdsmatrix
    objects.
}}
