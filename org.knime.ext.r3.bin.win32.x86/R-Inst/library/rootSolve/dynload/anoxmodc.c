/* compile within R with system("R CMD SHLIB anoxmodc.c") */

#include <R.h> /* gives F77_CALL through R_ext/RS.h */

static double parms[9];

/* A trick to keep up with the parameters */
#define D    parms[0]
#define Flux parms[1]
#define r    parms[2]
#define rox  parms[3]
#define ks   parms[4]
#define ks2  parms[5]
#define BO2  parms[6]
#define BSO4 parms[7]
#define BHS  parms[8]

/* initialisation subroutine: initialises the common block with parameter
   values */

void initanox (void (* steadyparms)(int *, double *))
{
   int N = 9;
   steadyparms(&N, parms);
}

/*  subroutine calculating the rate of change */

void anoxmod (int *neq, double *t, double *y, double *ydot, 
              double *yout, int*ip)
{
    double OM, O2, SO4, HS;
    double Min, oxicmin, anoxicmin;
    
    if (ip[0] <1) error("nout should be at least 1");

    OM  = y[0];
    O2  = y[1];
    SO4 = y[2];
    HS  = y[3];
         
    Min       = r*OM;
    oxicmin   = Min*(O2/(O2+ks));
    anoxicmin = Min*(1-O2/(O2+ks))* SO4/(SO4+ks2);

    ydot[0]  = Flux - oxicmin - anoxicmin;
    ydot[1]  = -oxicmin      -2*rox*HS*(O2/(O2+ks)) + D*(BO2-O2);
    ydot[2]  = -0.5*anoxicmin  +rox*HS*(O2/(O2+ks)) + D*(BSO4-SO4);
    ydot[3]  =  0.5*anoxicmin  -rox*HS*(O2/(O2+ks)) + D*(BHS-HS);
  
    yout[0] = SO4+HS;
         
}

