#ifndef DOTCALL64_H
#define DOTCALL64_H

#include <R.h>
#include <Rdefines.h>

// Defines DL_FUNC.
#include <R_ext/Rdynload.h>

// Defines INTSXP and REALSXP to be used in the args_type array.
#include<Rinternals.h>

// Defines int64_t on windows
#include <stdint.h>

/*
 * Because R does not define an int64 type, this pseudo type should be used to
 * indicate an int64_t argument type:
 * Currently, R only uses 4 bits for it's types. Therefore this value will not
 * clash.
 */
#define INT64_TYPE 9999


/*
 * String representing an int64_t argument used in the R-API:
 */
#define INT64_STRING "int64"


/*
 * TODO: Maybe, this should be defined as an enum?
 */
#define INTENT_READ 0x1
#define INTENT_WRITE 0x2
#define INTENT_COPY 0x4
#define INTENT_SPEED 0x8


/*
 * Helpers to read out the bits of the 'intent'.
 */
#define HAS_INTENT_READ(x) (((x) & INTENT_READ ) != 0)
#define HAS_INTENT_WRITE(x) (((x) & INTENT_WRITE) != 0)
#define HAS_INTENT_COPY(x) (((x) & INTENT_COPY) != 0)
#define HAS_INTENT_SPEED(x) (((x) & INTENT_SPEED) != 0)



/*
 * C-API of the dotCall64 package:
 *
 * \param fun            pointer to the function that should be called
 * \param nargs          number of arguments
 * \param args           array of type SEXP containing the 'nargs' arguments.
 * \param args_type      array of int indicating the signature of the function.
 *                       Currently INT64_TYPE, INTSXP and REALSXP are supported.
 * \param args_intent_in array of type int, indicating the intent of each argument.
 *                       The INTENT_* macros defined above have to be used.
 *                       Multiple intents can be combined using the OR operator '|'.
 * \param flag_naok      0: do not accept NAs, 1: accept NAs      
 * \param flag_verbose   0: no warnings, 1: warnings, or 2: diagnostic messages as warnings. 
 *
 * The function returns the result by modifying the 'args' array. All arguments that don't
 * have INTENT_WRITE will be set to R_NilValue. If INTENT_WRITE is set, then the array
 * contains the object containing the value. As usual, any element must be PROTECT'ed
 * against the garbage collector.
 *
 */
void dotCall64(DL_FUNC fun, int nargs, SEXP *args, int *args_type, int *args_intent_in, int flag_naok, int flag_verbose);


#define DOT_CALL64(a,b,c,d,e,f,g) dotCall64(a,b,c,d,e,f,g)



// The maximum number of arguments that a function may have:
#define MAX_ARGS 65

#endif
