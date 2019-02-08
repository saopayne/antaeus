package io.pleo.antaeus.models

import java.math.BigDecimal

data class Invoice(
    val id: Int,
    val amount: Money
)

data class Customer(
    val id: Int,
    val accountBalance: Money
)

data class Money(
    val value: BigDecimal,
    val currency: Currency
)

enum class Currency {
    EUR,
    USD,
    DKK,
    SEK,
    GBP
}