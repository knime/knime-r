lp <- function(direction = c("min", "max"), objective.in, const.mat, const.dir,
               const.rhs, transpose.constraints = TRUE, int.vec, presolve = 0,
               compute.sens = 0)
{
  # lp: solve a general linear program
  #
  # Arguments:
  #     direction: Character: direction of optimization: "min" (default) or
  #                "max."
  #  objective.in: Numeric vector (or one-column data frame) of coefficients
  #                of objective function
  #     const.mat: Matrix of numeric constraint coefficients, one row  per
  #                constraint, one column per variable (unless
  #                transpose.constraints =  FALSE; see below).
  #     const.dir: Vector of character strings giving the direction of the
  #                constraints: each value should be one of "<=", "=" or ">=."
  #     const.rhs: Vector of numeric values for the right-hand sides of  the
  #                constraints.
  # transpose.constraints: By default each constraint occupies a row  of
  #                const.mat, and that matrix needs to be transposed before
  #                being passed  to the optimizing code.  For very large
  #                constraint matrices it may be wiser  to construct the
  #                constraints in a matrix column-by-column. In that case set
  #                transpose.constraints to FALSE.
  #       int.vec: Numeric vector giving the indices of variables that are
  #                required to be integer. The length of this vector will
  #                therefore be the  number of integer variables.
  #  presolve: Numeric: Should presolve be done (in lp_solve)? Default: 0 (no).
  #                A non-zero value means "yes." Currently mostly ignored.
  #  compute.sens: Numeric: compute sensitivities? Default 0 (no). Any non-zero
  #                value means "yes."

  const.dir[const.dir == "<"] <- "<="
  const.dir[const.dir == ">"] <- ">="
  const.dir[const.dir == "=="] <- "="

  if(!transpose.constraints)
    const.mat <- t(const.mat)

  direction <- match.arg(direction)

  m <- dim(const.mat)[1]
  n <- dim(const.mat)[2]

  # Basic usage checks

  if(length(objective.in) != n)
    stop(sQuote("objective.in"), " must have the same number of elements as ",
         "there are columns in ", sQuote("const.mat"))

  if(length(const.dir) != m)
    stop(sQuote("const.dir"), " must have the same number of elements as ",
         "there are rows in ", sQuote("const.mat"))

  if(length(const.rhs) != m)
    stop(sQuote("const.rhs"), " must have the same number of elements as ",
         "there are rows in ", sQuote("const.mat"))

  # Build the model

  lprec <- make.lp(m, n)
  control <- lp.control(lprec, sense = direction)
  for(j in 1:n)
    set.column(lprec, j, const.mat[, j])
  set.rhs(lprec, const.rhs)
  set.constr.type(lprec, const.dir)
  set.objfn(lprec, objective.in)

  if(!missing(int.vec))
    set.type(lprec, int.vec, "integer")
  else
    int.vec <- integer(0)

  if(compute.sens > 0)
    control <- lp.control(lprec, presolve = "sensduals")

  # Solve the model

  status <- solve(lprec)

  lp.out <- list(direction = ifelse(direction == "min", 0, 1),
                 x.count = n,
                 objective = objective.in,
                 const.count = m,
                 int.count = length(int.vec),
                 int.vec = int.vec,
                 objval = get.objective(lprec),
                 solution = get.variables(lprec),
                 presolve = 0,
                 compute.sens = compute.sens,
                 sens.coef.from = NA,
                 sens.coef.to = NA,
                 duals = NA,
                 duals.from = NA,
                 duals.to = NA,
                 status = status)

  if(compute.sens > 0) {
    sens.obj <- get.sensitivity.obj(lprec)
    sens.rhs <- get.sensitivity.rhs(lprec)
    lp.out$sens.coef.from = as.double(sens.obj$objfrom)
    lp.out$sens.coef.to = as.double(sens.obj$objtill)
    lp.out$duals = as.double(sens.rhs$duals)
    lp.out$duals.from = as.double(sens.rhs$dualsfrom)
    lp.out$duals.to = as.double(sens.rhs$dualstill)
  }

  oldClass(lp.out) <- "lp"
  lp.out
}

