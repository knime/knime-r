/*
 *  Example of a routine making use of the interface defined in
 *  init.c. The routine will iterate on both arguments passed from R,
 *  thereby making the R function vectorized.
 *
 *  The code is derived from package actuar and base R.
 *
 *  Copyright (C) 2016 Vincent Goulet
 *  Copyright (C) 1995--1997 Robert Gentleman and Ross Ihaka
 *  Copyright (C) 1998--2016 The R Core Team.
 *  Copyright (C) 2003--2016 The R Foundation
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 *
 *  AUTHOR: Vincent Goulet <vincent.goulet@act.ulaval.ca>
 */

#include <R.h>
#include <Rinternals.h>
#include "locale.h"
#include "pkg.h"

SEXP pkg_do_foo(SEXP args)
{
    SEXP sx, sy;
    int i, nx;
    double xi, *x, *y;
    Rboolean naflag = FALSE;

    if (!isNumeric(CADR(args)))
        error(_("invalid arguments"));

    nx = LENGTH(CADR(args));
    if (nx == 0)
        return(allocVector(REALSXP, 0));

    PROTECT(sx = coerceVector(CADR(args), REALSXP));
    PROTECT(sy = allocVector(REALSXP, nx));
    x = REAL(sx);
    y = REAL(sy);

    for (i = 0; i < nx; i++)
    {
        xi = x[i];
	if (ISNA(xi))
	    y[i] = NA_REAL;			\
        else if (ISNAN(xi))
	    y[i] = R_NaN;
        else
        {
	    /* this is where the expint routine is used */
            y[i] = pkg_expint_E1(xi, 0);
            if (ISNAN(y[i])) naflag = TRUE;
        }
    }

    if (naflag)
        warning(R_MSG_NA);

    SET_ATTRIB(sy, duplicate(ATTRIB(sx)));
    SET_OBJECT(sy, OBJECT(sx));
    UNPROTECT(2);

    return sy;
}
