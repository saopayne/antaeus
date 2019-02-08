package io.pleo.antaeus.core.exceptions

import io.pleo.antaeus.models.Currency

class CurrencyMismatchException(currency: Currency) :  Exception("Currency '$currency' was not found")
