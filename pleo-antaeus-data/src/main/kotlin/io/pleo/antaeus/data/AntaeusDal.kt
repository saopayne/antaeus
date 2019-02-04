package io.pleo.antaeus.data

import io.pleo.antaeus.models.Invoice
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class AntaeusDal(private val db: Database) {
    fun fetchInvoice(id: String): Invoice? {
        return transaction(db) {
            Invoices
                .select { Invoices.id.eq(id) }
                .firstOrNull()
                ?.toInvoice()
        }
    }

    fun fetchInvoices(): List<Invoice> {
        return transaction(db) {
            Invoices
                .selectAll()
                .map { it.toInvoice() }
        }
    }
}