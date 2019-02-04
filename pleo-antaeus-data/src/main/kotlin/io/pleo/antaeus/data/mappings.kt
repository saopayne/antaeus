package io.pleo.antaeus.data

import io.pleo.antaeus.models.Invoice
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toInvoice(): Invoice = Invoice(
    id = this[Invoices.id]
)