/*
    Implements the data access layer (DAL).
    This file implements the database queries used to fetch and insert rows in our database tables.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.selectAll
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

    fun fetchInvoices(status: InvoiceStatus): List<Invoice> {
        val invoiceStatus = status.toString()
        // collect a list of invoices with specified status
        return transaction(db) {
            InvoiceTable
                    .select{ InvoiceTable.status.eq(invoiceStatus) }
                    .map { it.toInvoice() }
        }
    }

    fun fetchDueInvoices(): List<Invoice> {
        val invoiceStatus = InvoiceStatus.PENDING.toString()
        // collect a list of valid invoices with pending status
        return transaction(db) {
            InvoiceTable
                .select{
                    InvoiceTable.status.eq(invoiceStatus)
                    InvoiceTable.valid.eq(true)
                }.map { it.toInvoice() }
        }
    }

    fun createInvoice(amount: Money, customer: Customer, status: InvoiceStatus = InvoiceStatus.PENDING): Invoice? {
        val id = transaction(db) {
            // Insert the invoice and returns its new id.
            InvoiceTable
                .insert {
                    it[this.value] = amount.value
                    it[this.currency] = amount.currency.toString()
                    it[this.status] = status.toString()
                    it[this.customerId] = customer.id
                    it[this.valid] = true
                } get InvoiceTable.id
        }

        return fetchInvoice(id!!)
    }

    fun updateInvoiceStatus(id: Int, status: InvoiceStatus): Boolean {
        val invoice = fetchInvoice(id)
        if (invoice != null) {
            val id = transaction(db) {
                InvoiceTable
                    .update({InvoiceTable.id.eq(id)}) {
                        it[this.status] = status.toString()
                    }
            }
            return true
        }

        return false
    }

    fun updateInvoiceValidity(id: Int, valid: Boolean): Boolean {
        val invoice = fetchInvoice(id)
        if (invoice != null) {
            val id = transaction(db) {
                InvoiceTable
                        .update({InvoiceTable.id.eq(id)}) {
                            it[this.valid] = valid
                        }
            }
            return true
        }

        return false
    }

    fun updateInvoiceCurrency(id: Int) {
        var invoice = fetchInvoice(id)
        var customer = fetchCustomer(invoice?.customerId!!)

        if (invoice != null && customer != null) {
            val id = transaction(db) {
                InvoiceTable
                        .update({InvoiceTable.id.eq(id)}) {
                            it[this.value] = invoice.amount.value
                            it[this.currency] = customer.currency.toString()
                        }
            }
        }
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

    fun createCustomer(currency: Currency): Customer? {
        val id = transaction(db) {
            // Insert the customer and return its new id.
            CustomerTable.insert {
                it[this.currency] = currency.toString()
            } get CustomerTable.id
        }

        return fetchCustomer(id!!)
    }
}
