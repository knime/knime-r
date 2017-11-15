\name{foo}
\alias{foo}
\alias{bar}
\title{Test functions}
\description{
  These functions test the interface to the C routines of package
  \pkg{expint}.
}
\usage{
foo(x)
bar(a, x)
}
%- maybe also 'usage' for other objects documented here.
\arguments{
  \item{x, a}{vectors of real numbers}
}
\details{
  \code{foo} is an interface to \code{expint_E1}.

  \code{bar} is an interface to \code{gamma_inc}.

  Both functions are vectorized.
}
\value{
  ...
}
\author{ ~~who you are~~ }
\seealso{ ~~objects to See Also as \code{\link{help}}, ~~~ }
\examples{
## Compare results with functions expint() and gammainc()
## of package expint
foo(1.275)

x <- c(0.2, 2.5, 5, 8, 10)
a <- 1.2
bar(a, x)
}
\keyword{math}
