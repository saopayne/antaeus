/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class InvoiceService(private val dal: AntaeusDal) {
    fun fetchAll(): List<Invoice> {
        return dal.fetchInvoices()
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun fetchByStatus(status: String): List<Invoice> {
        return dal.fetchInvoices().filter { invoice: Invoice -> invoice.status.toString().equals(status)  }
    }

    fun updateStatus(id: Int, status: String) {
        return dal.updateInvoiceStatus(id, status)
    }

    fun updateCurrency(id: Int, currency: String) {
        return dal.updateInvoiceCurrency(id, currency)
    }

    fun getAllPendingInvoicesGroupedByCustomerId() : List<List<Invoice>> {
        return fetchByStatus(InvoiceStatus.PENDING.toString())
            .groupBy { it.customerId }.map { it.value }
    }

    fun getAllPendingInvoicesForIndividualCustomer(customerId: Int) : List<Invoice> {
        return fetchByStatus(InvoiceStatus.PENDING.toString())
            .filter { invoice: Invoice -> invoice.customerId.equals(customerId) }
    }

    fun getAllInvoicesGroupedByCustomerId() : List<List<Invoice>> {
        return fetchAll().groupBy { it.customerId }.map { it.value }
    }

    fun getAllInvoicesForIndividualCustomer(customerId: Int) : List<Invoice> {
        return fetchAll().filter { invoice: Invoice -> invoice.customerId.equals(customerId) }
    }
}
