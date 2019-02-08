package io.pleo.antaeus.models

data class Invoice(
    val id: Int,
    val amount: Money,
    val status: InvoiceStatus
)
