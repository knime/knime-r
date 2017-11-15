/*  == expint: Exponential Integral and Incomplete Gamma Function ==
 *
 *  Support for exported functions at the C level.
 *
 *  This is derived from code in package zoo.
 *
 *  Copyright (C) 2016 Vincent Goulet
 *  Copyright (C) 2010 Jeffrey A. Ryan jeff.a.ryan @ gmail.com
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

#include <Rinternals.h>
#include <R_ext/Rdynload.h>
#include <R_ext/Visibility.h>

#ifdef  __cplusplus
extern "C" {
#endif

double expint_E1(double x, int scale);
double expint_E2(double x, int scale);
double expint_En(double x, int order, int scale);
double gamma_inc(double a, double x);

#ifdef  __cplusplus
}
#endif
