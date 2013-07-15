\name{NEWS}
\title{NEWS file for the coxme package}
\section{Changes in version 2.2-3}{
  \itemize{
    \item A serious bug in lmekin was pointed out by Paola Sebastiani. It occurs
    whenever the variance matrix of the random effects is not diagonal:
    in one location I had an upper diagonal matrix U when I needed to
    have the lower diagonal form U'.  This affects all the genetics
    models. A test case with non-diagonal variance has been added.  
    Unfortunately, all of the prior test cases were designed to validate
    agains lme and thus had diagonal penalty matrices.  Also address a 
    slowness problem by speeding up the conversion between bdsmatrix and
    matrix objects.

    \item Change the default in coxmeMlist to rescale=FALSE.  For
    kinship matrices, which have a diagonal of 1/2, automatic rescaling
    was a handy feature.  For other types of matrices, e.g., a smoothing
    spline example currently in development, it causes much confusion.

    \item Add an anova method.
}}

\section{Changes in version 2.2-2}{
  \itemize{
    \item Add a genetic example to the lmekin vignette.
    
    \item Add dimnames to the result of vcov, per the suggestion of
    Joshua Wiley

    \item Fix an indexing error, when there were multiple random terms
    and the first of them had only sparse intercept terms, then
    entries for the second were incorrectly placed in the combined matrix.
    This is a serious bug, I can only assume that people were not using
    multiple random terms enough to have encountered it.

    \item Allow for different integration methods in the refine.n code.
    This is a research question, and the option may disappear once I
    answer it.
}}

\section{Changes in version 2.2-1}{
  \itemize{
    \item Further refinement to the laplace and coxme vignettes.
    \item Remove a Makefile from the vignettes directory that was
    causing errors at CRAN.  Why it didn't lead to errors here is
    unknown.
}}
\section{Changes in version 2.2}{
  \itemize{
    \item Create vignettes for the laplace approximation and lmekin.
    In the process changed the way in which the refine.n option works.
    It is now more robust.
    
    \item Fix two bugs in the return values, caused by incorrect mapping
    of the b coefficients into the final output, when there were
    multiple random terms in the model.  (The fit was correct.  One bug
    the returned ranef(fit) to be incorrectly ordered, the other led to NA
    in the linear.predictor.)
    
    \item Fix a bug noted by Lawrence Hunsicker: if a model had no fixed
    covariates or only an offset, the gridsearch for starting values
    would always declare failure.  One variable was mistakenly NA.

    \item Add a new version of lmekin to the package.  This also marks
    the start of a conversion to use of the Matrix library.

    \item Change the definition of ranef and add VarCorr so as to match
    the usage in lme and lmer.  This will break some old code, but will
    save much heartache in the long term.  Add a vcov method as well.
    }}
\section{Changes in version 2.1-3}{
  \itemize{
    \item Further interaction with Gabor Mezaros and his huge data set
    showed that I didn't get the iteration counts quite right (see
    second bullet for 2.1-0).  As a positive, we now have cross checks and
    agreement with the Survival Kit software for a substantial example,
    a major increase in our assurance of the code's correctness.
    The change in iteration paths causes small changes in some tests,
    all statistically inconsequential.
}}
    
\section{Changes in version 2.1-2}{
  \itemize{
    \item Fix a bug pointed out by Goran Brostrom: using (start, stop)
    data the program could give a memory fault.  A 1 character typo was
    using the wrong ending variable in a for loop, if the model had
    random slopes.
    Added two more tests to the suite to catch this.

    \item Updates to the vignette per suggestions from Marianne
    (co-author).
}}

\section{Changes in version 2.1-1}{
  \itemize{
    \item Add further checks on user-written coxme variance
    functions. The goal was to give a more informative error message
    for common oversights.

  \item Update the manual pages for ranef and fixef to remove a warning
  message from the package checks.
  }}

\section{Changes in version 2.1-0}{
  \itemize{
    \item Add a small vignette that shows the use of user-constructed
    variance methods.
    
    \item Printout change: the chisq, AIC, BIC printed when thinking of
    the model as a penalized calculation should use the full,
    unpenalized value of the likelihood.  I had been using loglik - penalty.

    \item The default number of iterations was made more intelligent.
      This was driven by a query from Gabor Meszaros; his large model
      required 10 iterations for an ordinary coxph fit, so the default
      inner.iter value of 4 was totally inadequate leading to a nonsense
      iteration path.  Now the program uses 1 + the number of iterations
      used in the no-frailty model or 4, whichever is greater.  A large
      fraction of coxph models require 3 iterations, so there will be no
      change for most runs.

      \item Per a bug report from Sophie Armitage, there was an indexing
      error in the code when using multiple random effects terms, in the
      map for the X matrix.  This has prompted a new release.

      \item Allow for multiple starting estimates.  It became clear that
      a single default starting element that would always work was not
      realistic.  The default grid is now (.02, .1, .4, .8)^2 for
      variances and (0, .3) for correlations; all elements in the grid
      are tested and the best is used as a starting value for optim.
      This changed the iteration path for several of the tests, of course.
    }
}

\section{Changes in version 2.0-1}{
    \itemize{
    \item Change the default starting value of variances from .2 to .1.
    The final answer will be closer to .1 the majority of the time, and
    I had a data set
    where .2 led to wrong iteration.  This area needs more investigation.

    \item Fix error in the fitting routine; if there were 3 or more
    crossed terms
    the indexing was wrong, leading to no penalty for terms after the second.

    \item Allow for NULL as an initial value.
    This occured for a model with 3 terms
    (x1|grp) + (x2|grp) + (x3|grp), and I wanted to give values for x2 and x3
    only.  You can't use names since they all are the same, instead I now use
    vfixed=list(NULL, .1, .1).
  }
} 
 
