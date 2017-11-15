#include <Rinternals.h>

/* Error messages */
#define R_MSG_NA        _("NaNs produced")

/* Functions accessed from .External() */
SEXP pkg_do_foo(SEXP args);
SEXP pkg_do_bar(SEXP args);

/* Interfaces to routines from package expint */
double(*pkg_expint_E1)(double,int);
double(*pkg_gamma_inc)(double,double);
