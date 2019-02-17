/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.InvoiceStatus

class InvoiceService(private val dal: AntaeusDal) {

    fun fetchAll(): List<Invoice> {
       return dal.fetchInvoices()
    }

    fun fetchAllDueInvoices(): List<Invoice> {
        return dal.fetchDueInvoices()
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun updateInvoiceStatus(id: Int, invoiceStatus: InvoiceStatus): Boolean {
        return dal.updateInvoiceStatus(id, invoiceStatus)
    }

    fun updateInvoiceValidity(id: Int, valid: Boolean) {
        dal.updateInvoiceValidity(id, valid)
    }

    fun updateInvoiceCurrencyWithCustomer(id: Int, customer: Customer) {
        dal.updateInvoiceCurrencyWithCustomer(id, customer)
    }

}

