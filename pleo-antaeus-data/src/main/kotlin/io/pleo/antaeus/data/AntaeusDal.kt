/*
    Implements the data access layer (DAL).
    This file implements the database queries used to fetch and insert rows in our database tables.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class AntaeusDal(private val db: Database) {
    fun fetchInvoice(id: Int): Invoice? {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first invoice with matching id.
            InvoiceTable
                .select { InvoiceTable.id.eq(id) }
                .firstOrNull()
                ?.toInvoice()
        }
    }

    fun fetchInvoices(): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                .selectAll()
                .map { it.toInvoice() }
        }
    }

    fun createInvoice(amount: Money, customer: Customer): Invoice? {
        val id = transaction(db) {
            // Insert the invoice and returns its new id.
            InvoiceTable
                .insert {
                    it[value] = amount.value
                    it[currency] = amount.currency.toString()
                    it[status] = InvoiceStatus.PENDING.toString()
                    it[customerId] = customer.id
                } get InvoiceTable.id
        }

        return fetchInvoice(id!!)
    }

    fun fetchCustomer(id: Int): Customer? {
        return transaction(db) {
            CustomerTable
                .select { CustomerTable.id.eq(id) }
                .firstOrNull()
                ?.toCustomer()
        }
    }

    fun fetchCustomers(): List<Customer> {
        return transaction(db) {
            CustomerTable
                .selectAll()
                .map { it.toCustomer() }
        }
    }

    fun createCustomer(accountBalance: Money): Customer? {
        val id = transaction(db) {
            // Insert the customer and return its new id.
            CustomerTable.insert {
                it[currency] = accountBalance.currency.toString()
                it[balance] = accountBalance.value
            } get CustomerTable.id
        }

        return fetchCustomer(id!!)
    }

    fun updateInvoiceStatus(invoice: Invoice, newStatus: InvoiceStatus) {
        transaction(db) {

            InvoiceTable.update({InvoiceTable.id eq invoice.id}) {
                it[status] = newStatus.toString()
            }
        }
    }

    fun fetchFirstInvoiceByStatus(status: InvoiceStatus): Invoice? {
        return transaction(db) {
            InvoiceTable.select {InvoiceTable.status eq status.toString()}
                    .firstOrNull()
                    ?.toInvoice()
        }
    }
}
