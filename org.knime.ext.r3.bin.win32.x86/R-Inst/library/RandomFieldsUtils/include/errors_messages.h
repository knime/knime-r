

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


// Datei wi

#ifndef rfutils_error_H
#define rfutils_error_H 1


#define NOERROR 0                 
#define ERRORMEMORYALLOCATION 1 
#define ERRORFAILED 2      /* method didn't work for the specified parameters */
#define ERRORM 3           /* a single error message */
#define ERRORNOTPROGRAMMEDYET 4

 

#ifdef SCHLATHERS_MACHINE
#define ERRLINE PRINTF("(ERROR in %s, line %d)\n", __FILE__, __LINE__);
#else
#define ERRLINE 
#endif


#define LENMSG 250
#define MAXERRORSTRING 1000
#define nErrorLoc 1000
#define LENERRMSG 2000
typedef char errorstring_type[MAXERRORSTRING];
typedef char errorloc_type[nErrorLoc];
extern char ERRMSG[LENERRMSG], // used by Error_utils.h. Never use elsewhere
  MSG[LENERRMSG], // used by RandomFields in intermediate steps
  BUG_MSG[LENMSG],// not much used
  MSG2[LENERRMSG];// used at the same time with MSG and ERR()
extern errorstring_type ERRORSTRING; // used by ERRORM in RandomFields 
extern errorloc_type ERROR_LOC;

#define ERRMSG(X) if (PL>=PL_ERRORS){errorMSG(X,MSG); PRINTF("error: %s%s\n",ERROR_LOC,MSG);}


#define RFERROR error
#define ERR(X) {ERRLINE;SPRINTF(ERRMSG, "%s %s", ERROR_LOC, X); RFERROR(ERRMSG);}
#define ERR1(X, Y) {ERRLINE;SPRINTF(ERRMSG, "%s %s", ERROR_LOC, X); \
    SPRINTF(MSG2, ERRMSG, Y);					 \
    RFERROR(MSG2);}
#define ERR2(X, Y, Z) {ERRLINE;SPRINTF(ERRMSG, "%s %s", ERROR_LOC, X);\
    SPRINTF(MSG2, ERRMSG, Y, Z);					\
    RFERROR(MSG2);}
#define ERR3(X, Y, Z, A) {ERRLINE;SPRINTF(ERRMSG, "%s %s", ERROR_LOC, X); \
    SPRINTF(MSG2, ERRMSG, Y, Z, A);					\
    RFERROR(MSG2);}
#define ERR4(X, Y, Z, A, B) {ERRLINE;SPRINTF(ERRMSG, "%s %s", ERROR_LOC, X); \
    SPRINTF(MSG2, ERRMSG, Y, Z, A, B);					\
    RFERROR(MSG2);}
#define ERR5(X, Y, Z, A, B, C) {ERRLINE;SPRINTF(ERRMSG, "%s %s", ERROR_LOC,X); \
    SPRINTF(MSG2, ERRMSG, Y, Z, A, B, C);				\
    RFERROR(MSG2);}
#define ERR6(X, Y, Z, A, B,C,D) {ERRLINE;SPRINTF(ERRMSG, "%s %s",ERROR_LOC,X); \
    SPRINTF(MSG2, ERRMSG, Y, Z, A, B, C, D);				\
    RFERROR(MSG2);}
#define ERR7(X, Y, Z,A,B,C,D,E) {ERRLINE;SPRINTF(ERRMSG, "%s %s",ERROR_LOC,X); \
    SPRINTF(MSG2, ERRMSG, Y, Z, A, B, C, D, E);				\
    RFERROR(MSG2);}
#define ERR8(X,Y,Z,A,B,C,D,E,F) {ERRLINE;SPRINTF(ERRMSG, "%s %s",ERROR_LOC,X); \
    SPRINTF(MSG2, ERRMSG, Y, Z, A, B, C, D, E, F);			\
    RFERROR(MSG2);}
#define FERR(X) strcpy(ERRORSTRING, X); DEBUGINFOERR
#define SERR(X) { FERR(X); return ERRORM;}
#define CERR(X) { FERR(X); err=ERRORM; continue;}
#define FERR1(X,Y) SPRINTF(ERRORSTRING, X, Y); DEBUGINFOERR
#define SERR1(X,Y) { FERR1(X, Y); return ERRORM;}
#define CERR1(X,Y) { FERR1(X, Y); err=ERRORM; continue; }
#define FERR2(X,Y,Z) SPRINTF(ERRORSTRING, X, Y, Z); DEBUGINFOERR
#define SERR2(X, Y, Z) { FERR2(X, Y, Z); return ERRORM;}
#define CERR2(X, Y, Z) { FERR2(X, Y, Z);  err=ERRORM; continue;}
#define FERR3(X,Y,Z,A) SPRINTF(ERRORSTRING, X, Y, Z, A); DEBUGINFOERR
#define SERR3(X, Y, Z, A) { FERR3(X, Y, Z, A); return ERRORM;}
#define CERR3(X, Y, Z, A) { FERR3(X, Y, Z, A); err=ERRORM; continue;}
#define FERR4(X,Y,Z,A,B) SPRINTF(ERRORSTRING, X, Y, Z, A, B); DEBUGINFOERR 
#define SERR4(X, Y, Z, A, B) {  FERR4(X, Y, Z, A, B); return ERRORM;}
#define FERR5(X,Y,Z,A,B,C) SPRINTF(ERRORSTRING,X,Y,Z,A,B,C); DEBUGINFOERR 
#define SERR5(X, Y, Z, A, B, C) {FERR5(X, Y, Z, A, B, C); return ERRORM;}
#define FERR6(X,Y,Z,A,B,C,D) SPRINTF(ERRORSTRING,X,Y,Z,A,B,C,D); DEBUGINFOERR 
#define SERR6(X, Y, Z, A, B, C, D) {FERR6(X, Y, Z, A, B, C,D);  return ERRORM;}
#define FERR7(X,Y,Z,A,B,C,D,E) SPRINTF(ERRORSTRING,X,Y,Z,A,B,C,D,E);DEBUGINFOERR
#define SERR7(X, Y, Z, A, B, C, D, E) {FERR7(X,Y,Z,A,B,C,D,E);  return ERRORM;}
#define GERR(X) {FERR(X); err = ERRORM; goto ErrorHandling;}
#define GERR1(X,Y) {FERR1(X,Y);err = ERRORM; goto ErrorHandling;}
#define GERR2(X,Y,Z) {FERR2(X,Y,Z); err = ERRORM; goto ErrorHandling;}
#define GERR3(X,Y,Z,A) {FERR3(X,Y,Z,A);  err = ERRORM; goto ErrorHandling;}
#define GERR4(X,Y,Z,A,B) {FERR4(X,Y,Z,A,B); err = ERRORM; goto ErrorHandling;}
#define GERR5(X,Y,Z,A,B,C) {FERR5(X,Y,Z,A,B,C); err=ERRORM; goto ErrorHandling;}
#define GERR6(X,Y,Z,A,B,C,D) {FERR6(X,Y,Z,A,B,C,D); err=ERRORM; goto ErrorHandling;}

#define RFWARNING warning
#define warn(X) {ERRLINE;SPRINTF(ERRMSG, "%s %s", ERROR_LOC, X); RFWARNING(ERRMSG);}
#define WARN1(X, Y) {ERRLINE;SPRINTF(ERRMSG, "%s %s", ERROR_LOC, X); \
    SPRINTF(MSG2, ERRMSG, Y);					 \
    RFWARNING(MSG2);}
#define WARN2(X, Y, Z) {ERRLINE;SPRINTF(ERRMSG, "%s %s", ERROR_LOC, X);\
    SPRINTF(MSG2, ERRMSG, Y, Z);					\
    RFWARNING(MSG2);}
#define WARN3(X, Y, Z, A) {ERRLINE;SPRINTF(ERRMSG, "%s %s", ERROR_LOC, X); \
    SPRINTF(MSG2, ERRMSG, Y, Z, A);					\
    RFWARNING(MSG2);}
#define WARN4(X, Y, Z, A, B) {ERRLINE;SPRINTF(ERRMSG, "%s %s", ERROR_LOC, X); \
    SPRINTF(MSG2, ERRMSG, Y, Z, A, B);					\
    RFWARNING(MSG2);}
#define WARN5(X, Y, Z, A, B, C) {ERRLINE;SPRINTF(ERRMSG, "%s %s", ERROR_LOC,X); \
    SPRINTF(MSG2, ERRMSG, Y, Z, A, B, C);				\
    RFWARNING(MSG2);}
#define WARN6(X, Y, Z, A, B,C,D) {ERRLINE;SPRINTF(ERRMSG, "%s %s",ERROR_LOC,X); \
    SPRINTF(MSG2, ERRMSG, Y, Z, A, B, C, D);				\
    RFWARNING(MSG2);}
#define WARN7(X, Y, Z,A,B,C,D,E) {ERRLINE;SPRINTF(ERRMSG, "%s %s",ERROR_LOC,X); \
    SPRINTF(MSG2, ERRMSG, Y, Z, A, B, C, D, E);				\
    RFWARNING(MSG2);}


#endif
