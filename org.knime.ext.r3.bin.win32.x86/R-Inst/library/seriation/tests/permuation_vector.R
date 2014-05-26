library(seriation)


context("permutation vector")

p <- sample(1:10)
sp <- ser_permutation_vector(p, method="valid")

expect_identical(length(sp), 10L)
expect_identical(get_order(sp), p)
expect_identical(get_order(rev(sp)), rev(p))


expect_error(ser_permutation_vector(c(1:10, 12L), method="invalid"), "Invalid permutation vector!")
expect_error(ser_permutation_vector(c(1:10, 3L), method="invalid"), "Invalid permutation vector!")


context("permute")

expect_identical(permute(1:10, ser_permutation(1:10)), 1:10)
expect_identical(permute(LETTERS[1:10], ser_permutation(1:10)), LETTERS[1:10])
expect_identical(permute(1:10, ser_permutation(10:1)), 10:1)
expect_identical(permute(LETTERS[1:10], ser_permutation(1:10)), LETTERS[1:10])

expect_error(permute(1:10, ser_permutation(1:11)))

m <- matrix(runif(9), ncol=3)
expect_identical(permute(m, ser_permutation(1:3, 3:1)), m[,3:1])
expect_identical(permute(m, ser_permutation(3:1, 3:1)), m[3:1,3:1])

expect_error(permute(m, ser_permutation(1:10, 1:9)))
expect_error(permute(m, ser_permutation(1:9, 1:11)))

d <- dist(matrix(runif(25), ncol=5))
attr(d, "call") <- NULL   ### permute removes the call attribute
expect_identical(permute(d, ser_permutation(1:5)), d)
### is_equivalent_to ignores attributes
expect_equivalent_to(permute(d, ser_permutation(5:1)), as.dist(as.matrix(d)[5:1,5:1]))

expect_error(permute(d, ser_permutation(1:8)))
