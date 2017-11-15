\name{NEWS}
\title{NEWS file for the compare package}
\encoding{UTF-8}

\section{Changes in version 0.2-6}{
  \itemize{
    \item Fixed bug when allowing rounding (when comparing two numerics)
    so that rounding will occur regardless of whether comparison object
    needs rounding (because model might benefit from rounding anyway).

    Reported by Thomas Guillet (with patch!).
  }
}
\section{Changes in version 0.2-5}{
  \itemize{
    \item Fixed bug when model is data frame or list with zero columns
    or components (and comparison is NOT identical).

    Reported by Joshua Ulrich (with patch!).
  }
}
