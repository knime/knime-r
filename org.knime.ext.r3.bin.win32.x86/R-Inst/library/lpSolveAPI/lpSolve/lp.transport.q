lp.transport <- function(cost.mat, direction = c("min", "max"), row.signs,
    row.rhs, col.signs, col.rhs, presolve = 0, compute.sens = 0,
    integers = "all")
{
  # lp.transport: solve the transportation problem.
  # This is a linear program with an ixj matrix of decision variables,
  # and constraints on the row and column sums (and no others)
  #
  # Arguments:
  #     cost.mat: matrix or data.frame of costs
  #          dir: direction ("min" or "max")
  #    row.signs: signs for row constraints
  #      row.rhs: values for row constraints
  #    col.signs: signs for column constraints
  #      col.rhs: values for column constraints
  #     presolve: numeric: should we presolve? Default 0 (no); non-0
  #                        values means "yes." Currently mostly ignored.
  # compute.sens: numeric: compute sensitivities? Default 0 (no);
  #                        non-zero value means "yes."
  #              integers: indicator of integer variables: default, all.
  #
  # Return value:
  #   a list from lpsolve, including objective and optimal values.

  row.signs[row.signs == "<"] <- "<="
  row.signs[row.signs == ">"] <- ">="
  row.signs[row.signs == "=="] <- "="

  col.signs[col.signs == "<"] <- "<="
  col.signs[col.signs == ">"] <- ">="
  col.signs[col.signs == "=="] <- "="

  cost.mat <- as.matrix(cost.mat)

  nr <- nrow(cost.mat)
  nc <- ncol(cost.mat)
  nvar <- nr * nc

  direction <- match.arg(direction)

  if(is.null(integers))
    integers <- integer(0)
  if(length(integers) && integers[1] == "all")
    integers <- 1:nvar

  lprec <- make.lp(nr + nc, nvar)

  x <- as.double(rep(1.0, 2))
  for(k in 1:nvar) {
    index <- c(((k - 1) %% nr) + 1, nr + ceiling(k / nr))
    set.column(lprec, k, x, index)
  }

  set.objfn(lprec, cost.mat)
  set.constr.type(lprec, c(row.signs, col.signs))
  set.rhs(lprec, c(row.rhs, col.rhs))
  set.type(lprec, integers, "integer")

  if(presolve > 0)
    control <- lp.control(lprec, presolve = "sensduals")

  status <- solve(lprec)

  if(compute.sens) {
    sens.obj <- get.sensitivity.obj(lprec)
    sens.coef.from = matrix(sens.obj$objfrom, nr, nc)
    sens.coef.to = matrix(sens.obj$objtill, nr, nc)
  }
  else {
    sens.coef.from = NA
    sens.coef.to = NA
  }

  ans <- list(direction = ifelse(direction == "min", 0, 1),
              costs = cost.mat,
              rsigns = row.signs,
              rrhs = row.rhs,
              csigns = col.signs,
              crhs = col.rhs,
              objval = get.objective(lprec),
              integers = integers,
              solution = matrix(get.variables(lprec), nr, nc),
              presolve = presolve,
              compute.sens = compute.sens,
              sens.coef.from = sens.coef.from,
              sens.coef.to = sens.coef.to,
              status = status)

  oldClass(ans) <- "lp"
  ans
}

