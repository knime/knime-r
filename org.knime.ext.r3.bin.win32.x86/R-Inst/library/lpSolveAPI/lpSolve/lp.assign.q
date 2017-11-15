lp.assign <- function(cost.mat, direction = c("min", "max"), presolve = 0,
                      compute.sens = 0)
{
  # lp.assign: solves the assignment problem. This
  # is a linear program with an ixj matrix of decision variables,
  # and i+j constraints: that the rows and columns all add up to one.
  #
  # Arguments:
  #      cost.mat: matrix or data.frame of costs
  #     direction: "min" (default) or "max"
  #      presolve: numeric. Presolve? Default 0. Currently ignored.
  #  compute.sens: numeric. Compute sensitivities? Default 0 (no).
  #                Any non-zero number means "yes" and, in that
  #                case, presolving is attempted.
  #
  # Return value:
  #   a list from lpsolve, including objective and assignments.

  direction <- match.arg(direction)

  nr <- nrow(cost.mat)
  nc <- ncol(cost.mat)
  rnum.signs <- rep("=", nr)
  row.rhs <- rep(1, nr)
  cnum.signs <- rep("=", nc)
  col.rhs <- rep(1, nc)

  lp.transport(cost.mat, direction = direction, row.signs = rnum.signs,
               row.rhs = row.rhs, col.signs = cnum.signs, col.rhs = col.rhs,
               presolve = presolve, compute.sens = compute.sens)
}


