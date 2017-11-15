

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



#ifndef rfutils_H
#define rfutils_H 1

#include <R.h>
#include <Rinternals.h>
#include <Rmath.h>
#include <string.h>
#include "Basic_utils.h"
#include "errors_messages.h"
#include "kleinkram.h"
#include "Solve.h"



#define DOPRINTF if (DOPRINT) Rprintf
#define PRINTF Rprintf
#define print PRINTF /* // */

#ifdef HIDE_UNUSED_VARIABLE
#define VARIABLE_IS_NOT_USED __attribute__ ((unused))
#else
#ifdef __GNUC__
#define VARIABLE_IS_NOT_USED __attribute__ ((unused))
#else
#define VARIABLE_IS_NOT_USED
#endif
#endif




// not SCHLATHERS_MACHINE
#ifndef SCHLATHERS_MACHINE
#define INTERNAL SERR("Sorry. This functionality does not exist currently. There is work in progress at the moment by the maintainer.")
#define assert(X) {}
#define BUG {								\
    SPRINTF(BUG_MSG, "Severe error occured in function '%s' (file '%s', line %d). Please contact maintainer martin.schlather@math.uni-mannheim.de .", \
	    __FUNCTION__, __FILE__, __LINE__);				\
    RFERROR(BUG_MSG);							\
  }									
#define DO_TESTS false
//#define MEMCOPY(A,B,C) {memcpy(A,B,C); printf("memcpy %s %d\n", __FILE__, __LINE__);}
#define MEMCOPY(A,B,C) MEMCOPYX(A,B,C)
#define MALLOC MALLOCX
#define CALLOC CALLOCX
#define FREE(X) if ((X) != NULL) {FREEX(X); (X)=NULL;}
#define UNCONDFREE(X) {FREEX(X); (X)=NULL;}
#endif // not SCHLATHERS_MACHINE



// SCHLATHERS_MACHINE
#ifdef SCHLATHERS_MACHINE 
#define MAXALLOC 1e9

// __extension__ unterdrueckt Fehlermeldung wegen geklammerter Argumente
#define INTERNAL  \
  SPRINTF(BUG_MSG, \
	  "made to be an internal function '%s' ('%s', line %d).", /* // */ \
	  __FUNCTION__, __FILE__, __LINE__);				\
  /* warning(BUG_MSG) */						\
  SERR(BUG_MSG)
 
#define assert(X) if (!__extension__ (X)) {				\
    SPRINTF(BUG_MSG,"'assert(%s)' failed in function '%s'.",#X,__FUNCTION__); \
    ERR(BUG_MSG);							\
  }
#define SHOW_ADDRESSES 1
#define BUG { PRINTF("BUG in '%s'.",  __FUNCTION__);  ERR(BUG_MSG); }
#define DO_TESTS true

#define MEMCOPY(A,B,C) __extension__ ({ assert((A)!=NULL && (B)!=NULL); MEMCOPYX(A,B,C); })
//#define MEMCOPY(A,B,C) memory_copy(A, B, C)
#define MALLOC(X) __extension__ ({assert((X)>0 && (X)<=MAXALLOC); MALLOCX(X);})
#define CALLOC(X, Y) __extension__({assert((X)>0 && (X)<=MAXALLOC && (Y)>0 && (Y)<=64); CALLOCX(X,Y);})
#define FREE(X) { if ((X) != NULL) {if (showfree) DOPRINTF("(free in %s, line %d)\n", __FILE__, __LINE__); FREEX(X); (X)=NULL;}}
#define UNCONDFREE(X) { if (showfree) DOPRINTF("(free in %s, line %d)\n", __FILE__, __LINE__); FREEX(X); (X)=NULL;}
#endif // SCHLATHERS_MACHINE




#ifdef RANDOMFIELDS_DEBUGGING
#undef MALLOC
#define MALLOC(X) __extension__({DOPRINTF("(MALL %s, line %d)\n", __FILE__, __LINE__);assert((X)>0 && (X)<=3e9); MALLOCX(X);})
//
#undef CALLOC
#define CALLOC(X, Y) __extension__({DOPRINTF("(CALL %s, line %d)\n",__FILE__, __LINE__);assert((X)>0 && (X)<MAXALLOC && (Y)>0 && (Y)<=64); CALLOCX(X,Y);})
//#define MALLOC malloc
//#define CALLOC calloc

#define DEBUGINFOERR {						\
    errorstring_type dummy_; strcpy(dummy_, ERRORSTRING);		\
    SPRINTF(ERRORSTRING, "%s (%s, line %d)\n", dummy_, __FILE__, __LINE__); \
  }
#define DEBUGINFO DOPRINTF("(currently at  %s, line %d)\n", __FILE__, __LINE__)

#else
#define DEBUGINFO
#define DEBUGINFOERR if (PL >= PL_ERRORS) PRINTF("error: %s\n", ERRORSTRING);
#endif


#define PL_IMPORTANT 1 
#define PL_BRANCHING 2
#define PL_DETAILSUSER 3
#define PL_RECURSIVE 4
#define PL_STRUCTURE 5 // see also initNerror.ERROROUTOFMETHOD
#define PL_ERRORS  6 // only those that are caught internally

#define PL_FCTN_DETAILS 7  // R
#define PL_FCTN_SUBDETAILS 8

#define PL_COV_STRUCTURE 7 // C
#define PL_DIRECT_SEQU 8
#define PL_DETAILS 9
#define PL_SUBDETAILS 10

#define MATERN_NU_THRES 100


#endif


