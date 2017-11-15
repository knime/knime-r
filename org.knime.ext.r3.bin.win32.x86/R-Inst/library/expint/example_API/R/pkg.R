foo <- function(x)
    .External("pkg_do_foo", x)

bar <- function(a, x)
    .External("pkg_do_bar", a, x)
