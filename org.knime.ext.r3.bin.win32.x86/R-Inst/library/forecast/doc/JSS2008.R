## ----load packages, echo=FALSE, message=FALSE----------------------------
library('forecast')
library('expsmooth')

## ----ets-forecast, fig.height=7, fig.width=9, echo=FALSE, cache=TRUE-----
par(mfrow = c(2,2))
mod1 <- ets(bonds)
mod2 <- ets(usnetelec)
mod3 <- ets(ukcars)
mod4 <- ets(visitors)

plot(forecast(mod1), main="(a) US 10-year bonds yield", xlab="Year", ylab="Percentage per annum")
plot(forecast(mod2), main="(b) US net electricity generation", xlab="Year", ylab="Billion kwh")
plot(forecast(mod3), main="(c) UK passenger motor vehicle production", xlab="Year", ylab="Thousands of cars")
plot(forecast(mod4), main="(d) Overseas visitors to Australia", xlab="Year", ylab="Thousands of people")

## ----etsnames, echo=FALSE------------------------------------------------
etsnames <- c(mod1$method, mod2$method, mod3$method, mod4$method)
etsnames <- gsub("Ad","A\\\\damped",etsnames)

## ----ets-usnetelec, echo=TRUE, cache=TRUE--------------------------------
etsfit <- ets(usnetelec)

## ----ets-usnetelec-print,echo=TRUE, cache=TRUE---------------------------
etsfit

## ----ets-usnetelec-accuracy,eval=TRUE,echo=TRUE, cache=TRUE--------------
accuracy(etsfit)

## ----ets-usnetelec-fcast, fig.height=5, fig.width=8, message=FALSE, warning=FALSE, cache=TRUE, include=FALSE, output=FALSE----
fcast <- forecast(etsfit)
plot(fcast)

## ----ets-usnetelec-fcast-print,eval=TRUE,echo=TRUE, cache=TRUE-----------
fcast

## ----ets-usnetelec-newdata,eval=FALSE,echo=TRUE, cache=TRUE--------------
#  fit <- ets(usnetelec[1:45])
#  test <- ets(usnetelec[46:55], model = fit)
#  accuracy(test)

## ----ets-usnetelec-fcast-accuracy,eval=FALSE,echo=TRUE, cache=TRUE-------
#  accuracy(forecast(fit,10), usnetelec[46:55])

## ----arima-forecast-plots, fig.height=7, fig.width=9, echo=FALSE, cache=TRUE----
mod1 <- auto.arima(bonds, seasonal=FALSE, approximation=FALSE)
mod2 <- auto.arima(usnetelec)
mod3 <- auto.arima(ukcars)
mod4 <- auto.arima(visitors)
par(mfrow = c(2,2))
plot(forecast(mod1), main="(a) US 10-year bonds yield", xlab="Year", ylab="Percentage per annum")
plot(forecast(mod2), main="(b) US net electricity generation", xlab="Year", ylab="Billion kwh")
plot(forecast(mod3), main="(c) UK passenger motor vehicle production", xlab="Year", ylab="Thousands of cars")
plot(forecast(mod4), main="(d) Overseas visitors to Australia", xlab="Year", ylab="Thousands of people")

## ----arima-auto-fcast,eval=TRUE,echo=TRUE,fig.show="hide", cache=TRUE----
arimafit <- auto.arima(usnetelec)
fcast <- forecast(arimafit)
plot(fcast)

## ----arimanames, echo=FALSE----------------------------------------------
# Convert character strings to latex
arimanames <- c(as.character(mod1),
  as.character(mod2),
  as.character(mod3),
  as.character(mod4))
arimanames <-
    gsub("\\[([0-9]*)\\]", "$_{\\1}$", arimanames)

## ----arimafcastsummary, echo=TRUE, message=FALSE, warning=FALSE, as.is=TRUE----
summary(fcast)

