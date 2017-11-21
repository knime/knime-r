\name{NEWS}
\title{News for Package \pkg{vcd}}
\newcommand{\cpkg}{\href{https://CRAN.R-project.org/package=#1}{\pkg{#1}}}

\section{Changes in version 1.4-3}{
  \itemize{
    \item Remove outdated inst/doc directory.
  }
}

\section{Changes in version 1.4-2}{
  \itemize{
    \item add \code{lines()} method for \code{loddsratio} objects.
    \item add \code{gpar()} argment to control confidence intervals in \code{distplot()}.
  }
}

\section{Changes in version 1.4-1}{
  \itemize{
    \item add imports for functions in recommended packages to NAMESPACE
    \item \code{shading_hcl} now also uses solid line type for
    \code{abs(residuals) < eps} like \code{shading_hsv()}.
  }
}

\section{Changes in version 1.4-0}{
  \itemize{
    \item Add \code{shading_Marimekko} and \code{shading_diagonal}.
    \item Add residual-based shading to \code{rootogram()}.
    \item Add \code{residuals()} method for \code{"goodfit"} objects.
    \item Add \code{grid_abline()} for convenience.
    \item Add P-values to the output print.Kappa() produces.
    \item Fix legend of \code{distplot()} in case of leveled plots.
    \item \code{cotab_agreementplot} panel function for plotting
    conditional (stratified) agreement charts added.
    \item \code{loddsratio} added as an extension of \code{oddsratio} for
    conditioned generalized odds ratios. The plot method is greatly
    enhanced, and structural zeros (missing values) are also
    accepted. \code{oddsratio} is now just an alias for
    \code{loddsratio}.
    \item Bug fix in \code{pairs.table()}: for
    \code{"conditional"} type, tables are now reordered so that the
    conditioning variables come \emph{first}.
    
    \item \code{cotab_loddsratio} panel function for plotting
    conditional log-odds ratios plots added.
    \item \code{assocstats} now handles strata (all but the first two
    dimensions).
    \item \code{sieve} now accepts a \code{scale=} argument.
    \item \code{binreg_plot()} added for (conditioned) binary regression plots
    \item Bug fix in \code{mosaic()}: using \code{highlighting=} and/or
    \code{condvars=} now not only reorders the table, but also the split
    information, so that both remain consistent.
    \item All plot functions now have an option to return the produced
    plot as a grid object for further use (e.g., in \code{mplot()}).
    \item \code{mplot()} added for combining multiple grid plots in a
    multi-panel-layout.
    \item \code{legend_grid} now allows more options for positioning,
    and an \code{inset} argument has been added for relative adjustments.
  }  
}

\section{Changes in version 1.3-2}{
  \itemize{
    \item \code{Ord_plot()} gets \code{lwd}, \code{lty} and \code{col} arguments
    to control plotting of the OLS and WLS lines.
    \item \code{distplot()} gets an \code{lwd} argument.
    \item Changed default line types for \code{sieve} so that positive residuals are
    shown with solid lines, as per Friendly specifications.
    \item fixed problems with \code{pairs_strucplot()} type argument, particularly for
    \code{type="conditional"}
    \item Fixed error in \code{CoalMiners} data (missing group, labels
    switched)
    \item Change default area type for sieve plots to "area"
  }
}

\section{Changes in version 1.3-1}{
  \itemize{
    \item several namespace issues fixed
  }
}

\section{Changes in version 1.3-0}{
  \itemize{
    \item Bug fixed in \code{assoc()}
    \item \code{grid_legend()} extended for better finetuning of graphical parameters
    \item \code{legend_resbased()} better handles spacing for labels.
    \item \code{legend_resbased()} and \code{legend_fixed()}
    now allow changing the font family.
    \item default diagonal panel in \code{pairs()} is now set to
    \code{pairs_diagonal_mosaic()}, with alternating labels and
    frequencies shown on the bars.
    \item labeling is more customizable in \code{fourfold()}
  }
}

\section{Changes in version 1.2-13}{
  \itemize{
    \item \code{agreementplot()} now allows to add marginals to the plot
    \item \code{abbreviate} argument of \code{labeling_border()} renamed to
    \code{abbreviate_labs} to prevent name clash with
    \code{abbreviate_foo} args in \code{labeling_cells()}
    \item Several partial matches fixed in code
  }
}
\section{Changes in version 1.2-12}{
  \itemize{
    \item Bug fix: \code{assoc()} would not plot tables with 0 residuals
    \item Bug fix: \code{structable()} adds dimnames and dimname names if none are specified
    \item Bug fix: print error message when subsetting/selecting of
    structable objects using more than 2 indices
    \item \file{NEWS} file changed to .Rd format
  }
}
\section{Changes in version 1.2-11}{
  \itemize{
    \item Bug fix: \code{gamma} argument removed from \code{hcl2hex()}
  }
}
\section{Changes in version 1.2-10}{
  \itemize{
    \item Add aperm method for structable objects
    \item For use with \code{shading_Friendly()}, \code{shading_hsv()}
  now sets the line type of borders corresponding to \code{abs(residual)
    < eps} to \code{lty[1]} in addition to setting \code{color}
  to \code{line_color}.
  
    \item In \code{fourfold()}, modified default \code{colors[3:4]} for
    non-significant log odds ratios to be more visually distinct from
    the fully saturated \code{colors[5:6]} for significant ones.
    
    \item In \code{fourfold()}, allow the function to work with
    tables with more than 3 dimensions,
    by restructuring all strata dimensions into
    a single combined 3rd dimension.
    
    \item In \code{fourfold()}, modified defaults for \code{mfrow}/\code{mfcol} to give landscape display, \eqn{nr <= nc}, 
      rather than \eqn{nr >= nc}. If \code{length(dim(x)) > 3}, set \code{nr=dim(x)[3]}.
  }
}
\section{Changes in version 1.2-9}{
  \itemize{
    \item Fixed \code{Ord_plot()} for devices where the default filling is
      \code{"white"} and not \code{"transparent"} by explicitly setting it to
      the latter.
    \item Bug fix in \code{as.table.structable()}, returning the table in a different order
      than defined in the structable object, confusing in particular
      \code{plot.structable()}.
    \item add parameter to \code{ternaryplot()} to control the positioning of the
      plot labels.
  }
}
\section{Changes in version 1.2-8}{
  \itemize{
    \item Small bug fixes in handling of some graphical parameters.
  }
}
\section{Changes in version 1.2-7}{
  \itemize{
    \item Corrected df handling in \code{goodfit()} with ML estimation:
      only non-zero cells are used. This is backward compatible
      with versions \eqn{<=} 1.2-4.
    \item Fixed bug in \code{goodfit()} for binomial distribution with
      specified \code{"size"} parameter (introduced in 1.2-5).
  }
}
\section{Changes in version 1.2-6}{
  \itemize{
    \item Small typo in doc of \code{co_table()}.
  }
}
\section{Changes in version 1.2-5}{
  \itemize{
    \item Bundesliga data set has been augmented with the results of the
      seasons 2006/7, 2007/8, 2008/9 (thanks to Torsten Hothorn).
      \item \code{goodfit()} was modified to treat zero cells better:
      \sQuote{Internal}
      zero cells (i.e., counts below the maximal observed count that
      did not occur in the sample) are retained (and not dropped as
      before). \sQuote{Trailing} zero cells (i.e., counts above the maximal
      observed count) are still not considered. The documentation now
      points out the problems with the minimum-chi-squared method in
      the latter situation.
    \item \code{sieve()} now accepts a \code{gp_tile} argument to control the appearance of
      the cells (apart from the sieve color)
  }
}
\section{Changes in version 1.2-4}{
  \itemize{
    \item Bug fix: labeling arguments were incorrectly handled when the options
      were not provided as named vector
    \item \code{ternaryplot()} now makes use of the \code{cex} argument also for 
      the rendering of optional labels, if any
  }
}
\section{Changes in version 1.2-3}{
  \itemize{
    \item \file{hcl-colors.pdf} removed from source ball (vignette now in
      \cpkg{colorspace})
  }
}
\section{Changes in version 1.2-2}{
  \itemize{
    \item \code{strucplot()} now accepts a \code{df} argument that is
      passed to the shading functions. Also, expected values are no
      longer computed if residuals are given.
  }
}
\section{Changes in version 1.2-1}{
  \itemize{
    \item Fixed a bug in labeling (incorrect handling of some parameters)
  }
}
\section{Changes in version 1.2-0}{
  \itemize{
    \item Moved color palettes from \cpkg{vcd} to \cpkg{colorspace}, including
    \code{vignette("hcl-colors")}.
    Package \cpkg{colorspace} is (as before) loaded
      automatically with \cpkg{vcd}.
  }
}
