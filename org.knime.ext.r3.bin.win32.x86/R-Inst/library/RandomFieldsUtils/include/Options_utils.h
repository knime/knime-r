

/*
 Authors 
 Martin Schlather, schlather@math.uni-mannheim.de


 Copyright (C) 2015 Martin Schlather

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
 
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.  
*/



#ifndef rfutils_options_H
#define rfutils_options_H 1

#include <R.h>
#include <Rdefines.h>
#include "Basic_utils.h"
#include "Solve.h"


#define R_PRINTLEVEL 1
#define C_PRINTLEVEL 1
extern int PL;


#define LEN_OPTIONNAME 201

#define basicN 7
// IMPORTANT: all names of basic must be at least 3 letters long !!!
extern const char *basic[basicN];
typedef struct basic_param {
  bool 
   skipchecks,
     asList;
  int 
  Rprintlevel,
    Cprintlevel,
    seed, cores;
} basic_param;
#define basic_START \
  { false,  true, 						\
      R_PRINTLEVEL, C_PRINTLEVEL, NA_INTEGER, 1			\
      }


#define nr_InversionMethods ((int) Diagonal + 1)
#define nr_user_InversionMethods ((int) NoInversionMethod + 1)
extern const char * InversionNames[nr_InversionMethods];

#define PIVOT_NONE 0
#define PIVOT_MMD 1
#define PIVOT_RCM 2
#define SOLVE_SVD_TOL 3
#define solveN 12
typedef struct solve_param {
  usr_bool sparse;
  double spam_tol, spam_min_p, svd_tol, eigen2zero;
  InversionMethod Methods[SOLVE_METHODS];
  int spam_min_n, spam_sample_n, spam_factor,
    pivot, max_chol, max_svd;
  //  bool tmp_delete;
} solve_param;
#ifdef SCHLATHERS_MACHINE
#define svd_tol_start 1e-08
#else
#define svd_tol_start 0
#endif
#define solve_START				\
  { Nan, DBL_EPSILON,	0.8, svd_tol_start, 1e-12,	\
      {NoInversionMethod, NoInversionMethod},		\
      400, 500, 4294967, PIVOT_MMD, 16384, 10000}
extern const char * solve[solveN];


typedef struct utilsparam{
  basic_param basic;
  solve_param solve;
} utilsparam;



typedef void (*setparameterfct) (int, int, SEXP, char[200], bool);
typedef void (*getparameterfct) (SEXP*);
typedef void (*finalsetparameterfct) ();
#define ADD(ELT) SET_VECTOR_ELT(sublist[i], k++, ELT);
#define ADDCHAR(ELT) x[0] = ELT; ADD(ScalarString(mkChar(x)));


#endif
