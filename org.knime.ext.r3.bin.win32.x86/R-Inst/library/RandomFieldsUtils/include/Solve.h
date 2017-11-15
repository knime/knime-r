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



#ifndef rfutils_solve_H
#define rfutils_solve_H 1


typedef enum InversionMethod { 
  Cholesky, SVD, Eigen, Sparse,
  NoInversionMethod, // last user available method
  QR, LU, // currently not propagated
  NoFurtherInversionMethod, // local values
  direct_formula, 
  Diagonal // always last one!
} InversionMethod;


#define SOLVE_METHODS 3
typedef struct solve_storage {
  int SICH_n, MM_n, workspaceD_n, workspaceU_n, VT_n, U_n, D_n, 
    iwork_n, work_n, w2_n, ipiv_n, workLU_n, pivot_n,
    xlnz_n, snode_n, xsuper_n, xlindx_n, invp_n, 
    cols_n, rows_n, DD_n, lindx_n, xja_n,
    lnz_n, w3_n, result_n;
  //t_cols_n, t_rows_n, t_DD_n;
  InversionMethod method, newMethods[SOLVE_METHODS];
  int nsuper, nnzlindx, size,
    *iwork, *ipiv,
    *pivot, *xlnz, *snode, *xsuper, *xlindx, 
    *invp, *cols, *rows, *lindx, *xja; //*t_cols, *t_rows;
  double *SICH, *MM, *workspaceD, *workspaceU,
    *VT, *work, *w2, *U, *D, *workLU, 
    *lnz, *DD, *w3, *result,
    *to_be_deleted; //, *t_DD;
} solve_storage;





#endif
