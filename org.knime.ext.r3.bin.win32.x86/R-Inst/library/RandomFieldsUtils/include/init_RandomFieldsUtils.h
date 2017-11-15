


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

#ifndef rfutils_init_H
#define rfutils_init_H 1

#include "Options_utils.h"
#include "errors_messages.h"



#ifdef HAVE_VISIBILITY_ATTRIBUTE
  # define attribute_hidden __attribute__ ((visibility ("hidden")))
#else
  # define attribute_hidden
#endif

#ifdef __cplusplus
extern "C" {
#endif

#define RF_UTILS "RandomFieldsUtils"
  //#define FCT_PREFIX RU_
#define CALL0(V, N)							\
  V attribute_hidden RU_##N() {						\
    static V(*fun)(AV) = NULL;						\
    if (fun == NULL) fun = (V (*) ()) R_GetCCallable(RF_UTILS, #N);	\
    return fun(); }
#define DECLARE0(V, N)							\
  typedef V (*N##_type)();						\
  /* extern N##_type Ext_##N; */					\
  V attribute_hidden RU_##N();						\
  V N();

#define CALL1(V, N, AV, AN)						\
  /* N##_type Ext_##N = NULL; */					\
  V attribute_hidden RU_##N(AV AN) {					\
  static N##_type fun = NULL;						\
  if (fun == NULL) fun = (N##_type) R_GetCCallable(RF_UTILS, #N);	\
  return fun(AN); }						
#define DECLARE1(V, N, AV, AN)						\
  typedef V (*N##_type)(AV AN);						\
  /* extern N##_type Ext_##N; */					\
  V attribute_hidden RU_##N(AV AN);					\
  V N(AV AN);
  
#define CALL2(V, N, AV, AN, BV, BN)					\
  /* N##_type Ext_##N = NULL; */					\
  V attribute_hidden RU_##N(AV AN, BV BN) {			\
  static N##_type fun = NULL;						\
  if (fun == NULL) fun = (N##_type) R_GetCCallable(RF_UTILS, #N);	\
  return fun(AN, BN); }					       
#define DECLARE2(V, N, AV, AN, BV, BN)		\
  typedef V (*N##_type)(AV AN, BV BN);	\
  /* extern N##_type Ext_##N; */		\
  V attribute_hidden RU_##N(AV AN, BV BN);	\
  V N(AV AN, BV BN);
  
#define CALL3(V, N, AV, AN, BV, BN, CV, CN)				\
  /* N##_type Ext_##N = NULL; */					\
  V attribute_hidden RU_##N(AV AN, BV BN, CV CN) {		\
  static N##_type fun = NULL;						\
  if (fun == NULL) fun = (N##_type) R_GetCCallable(RF_UTILS, #N);	\
  return fun(AN, BN, CN); }						
#define DECLARE3(V, N, AV, AN, BV, BN, CV, CN)				\
  typedef V (*N##_type)(AV AN, BV BN, CV CN);				\
  /* extern N##_type Ext_##N; */					\
  V attribute_hidden RU_##N(AV AN, BV BN, CV CN);			\
  V N(AV AN, BV BN, CV CN);
  
#define CALL4(V, N, AV, AN, BV, BN, CV, CN, DV, DN)			\
  /* N##_type Ext_##N = NULL; */					\
  V attribute_hidden RU_##N(AV AN, BV BN, CV CN, DV DN) {	\
  static N##_type fun = NULL;						\
  if (fun == NULL) fun = (N##_type) R_GetCCallable(RF_UTILS, #N);	\
  return fun(AN, BN, CN, DN); }					
#define DECLARE4(V, N, AV, AN, BV, BN, CV, CN, DV, DN)			\
  typedef V (*N##_type)(AV AN, BV BN, CV CN, DV DN);			\
  /* extern N##_type Ext_##N; */					\
  V attribute_hidden RU_##N(AV AN, BV BN, CV CN, DV DN);		\
  V N(AV AN, BV BN, CV CN, DV DN);
  
#define CALL5(V, N, AV, AN, BV, BN, CV, CN, DV, DN, EV, EN)		\
  /* N##_type Ext_##N = NULL; */					\
  V attribute_hidden RU_##N(AV AN, BV BN, CV CN, DV DN, EV EN) {	\
  static N##_type fun = NULL;						\
  if (fun == NULL) fun = (N##_type) R_GetCCallable(RF_UTILS, #N);	\
  return fun(AN, BN, CN, DN, EN); }					
#define DECLARE5(V, N, AV, AN, BV, BN, CV, CN, DV, DN, EV, EN)		\
  typedef V (*N##_type)(AV AN, BV BN, CV CN, DV DN, EV EN);		\
  /* extern N##_type Ext_##N; */					\
  V attribute_hidden RU_##N(AV AN, BV BN, CV CN, DV DN, EV EN);		\
  V N(AV AN, BV BN, CV CN, DV DN, EV EN);
  
#define CALL6(V, N, AV, AN, BV, BN, CV, CN, DV, DN, EV, EN, FV, FN)	\
  /* N##_type Ext_##N = NULL; */					\
  V attribute_hidden RU_##N(AV AN, BV BN, CV CN, DV DN, EV EN, FV FN) { \
    static N##_type fun = NULL;						\
      if (fun == NULL) fun = (N##_type) R_GetCCallable(RF_UTILS, #N);	\
      return fun(AN, BN, CN, DN, EN, FN); }				
#define DECLARE6(V, N, AV, AN, BV, BN, CV, CN, DV, DN, EV, EN, FV, FN)	\
  typedef V (*N##_type)(AV AN, BV BN, CV CN, DV DN, EV EN, FV FN);	\
  /* extern N##_type Ext_##N; */					\
  V attribute_hidden RU_##N(AV AN, BV BN, CV CN, DV DN, EV EN, FV FN);	\
  V N(AV AN, BV BN, CV CN, DV DN, EV EN, FV FN);
  
#define CALL7(V, N, AV, AN, BV, BN, CV, CN, DV, DN, EV, EN, FV, FN, GV, GN) \
  /* N##_type Ext_##N = NULL; */					\
  V attribute_hidden RU_##N(AV AN, BV BN, CV CN, DV DN, EV EN, FV FN, GV GN) { \
    static N##_type fun = NULL;						\
      if (fun == NULL) fun = (N##_type) R_GetCCallable(RF_UTILS, #N);	\
      return fun(AN, BN, CN, DN, EN, FN, GN); }			       
#define DECLARE7(V, N, AV, AN, BV, BN, CV, CN, DV, DN, EV, EN, FV, FN, GV, GN) \
  typedef V (*N##_type)(AV AN, BV BN, CV CN, DV DN, EV EN, FV FN, GV GN); \
  /* extern N##_type Ext_##N; */					\
  V attribute_hidden RU_##N(AV AN, BV BN, CV CN, DV DN, EV EN, FV FN, GV GN); \
  V N(AV AN, BV BN, CV CN, DV DN, EV EN, FV FN, GV GN);
  
#define CALL8(V, N, AV, AN, BV, BN, CV, CN, DV, DN, EV, EN, FV, FN, GV, GN, HV, HN) \
  /* N##_type Ext_##N = NULL; */					\
  V attribute_hidden RU_##N(AV AN, BV BN, CV CN, DV DN, EV EN, FV FN, GV GN, HV HN) { \
  static N##_type fun = NULL;						\
  if (fun == NULL) fun = (N##_type) R_GetCCallable(RF_UTILS, #N);	\
  return fun(AN, BN, CN, DN, EN, FN, GN, HN); }		      
#define DECLARE8(V, N, AV, AN, BV, BN, CV, CN, DV, DN, EV, EN, FV, FN, GV, GN, HV, HN) \
  typedef V (*N##_type)(AV AN, BV BN, CV CN, DV DN, EV EN, FV FN, GV GN, HV HN); \
  /* extern N##_type Ext_##N; */					\
  V attribute_hidden RU_##N(AV AN, BV BN, CV CN, DV DN, EV EN, FV FN, GV GN, HV HN); \
  V N(AV AN, BV BN, CV CN, DV DN, EV EN, FV FN, GV GN, HV HN);


  DECLARE1(void, solve_DELETE, solve_storage**, S)
  DECLARE1(void, solve_NULL, solve_storage*, x)
  DECLARE7(int, solvePosDef, double*, M, int, size, bool, posdef, 
	   double *, rhs, int, rhs_cols, double *, logdet, solve_storage *, PT)
  DECLARE8(int, solvePosDefResult, double*, M, int, size, bool, posdef, 
	   double *, rhs, int, rhs_cols, double *, result, double*, logdet, 
	   solve_storage*, PT)
  DECLARE3(int, sqrtPosDef, double *, M, int, size, solve_storage *, pt)
  DECLARE3(int, sqrtPosDefFree, double *, M, int, size, solve_storage *, pt)
  DECLARE3(int, sqrtRHS, solve_storage *, pt, double*, RHS, double *, res)
  DECLARE2(int, invertMatrix, double *, M, int, size)
  DECLARE2(double, StruveH, double, x, double, nu)
  DECLARE3(double, StruveL, double, x, double, nu, bool, expScaled)
  DECLARE1(double, I0mL0, double, x)
  DECLARE3(double, WM, double, x, double, nu, double, factor)
  DECLARE3(double, DWM, double, x, double, nu, double, factor)
  DECLARE3(double, DDWM, double, x, double, nu, double, factor)
  DECLARE3(double, D3WM, double, x, double, nu, double, factor)
  DECLARE3(double, D4WM, double, x, double, nu, double, factor)
  DECLARE4(double, logWM, double, x, double, nu1, double, nu2, double, factor)
  DECLARE1(double, Gauss, double, x)
  DECLARE1(double, DGauss, double, x)
  DECLARE1(double, DDGauss, double, x)
  DECLARE1(double, D3Gauss, double, x)
  DECLARE1(double, D4Gauss, double, x)
  DECLARE1(double, logGauss, double, x)

  DECLARE1(void, getErrorString, errorstring_type, errorstring)
  DECLARE1(void, setErrorLoc, errorloc_type, errorloc)
  DECLARE1(void, getUtilsParam, utilsparam **, up)
  DECLARE7(void, attachRFoptions, const char **, prefixlist, int, N, 
	   const char ***, all, int *, allN, setparameterfct, set, 
	   finalsetparameterfct, final, getparameterfct, get)
  DECLARE2(void, detachRFoptions, const char **, prefixlist, int, N)
  DECLARE1(void, relaxUnknownRFoption, bool, relax)

  DECLARE3(void, sorting, double*, data, int, len, usr_bool, NAlast)
  DECLARE3(void, sortingInt, int*, data, int, len, usr_bool, NAlast)
  DECLARE4(void, ordering, double*, data, int, len, int, dim, int *, pos)
  DECLARE4(void, orderingInt, int*, data, int, len, int, dim, int *, pos)


  
  /*

    See in R package RandomFields, /src/userinterfaces.cc 
          CALL#(...)
    at the beginning for how to make the functions available
    in a calling package

   */
#ifdef __cplusplus
}
#endif


#endif

      
