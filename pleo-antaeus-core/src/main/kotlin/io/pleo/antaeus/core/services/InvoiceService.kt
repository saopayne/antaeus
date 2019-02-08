/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoicePaymentResult
import io.pleo.antaeus.models.InvoiceStatus
import java.util.*

class InvoiceService(private val dal: AntaeusDal, private val billingService: BillingService) {


    fun fetchAll(): List<Invoice> {
       return dal.fetchInvoices()
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun payNextInvoice(): InvoicePaymentResult {
        return Optional.ofNullable(dal.fetchFirstInvoiceByStatus(InvoiceStatus.PENDING))
                .map {
                    // TODO: change status to avoid rerunning before payment has completed.
                    var charged = billingService.charge(it)

                    if (charged) {
                        dal.updateInvoiceStatus(it, InvoiceStatus.PAID)
                        InvoicePaymentResult.ONE_INVOICE_PAID
                    } else {
                        InvoicePaymentResult.FAILED
                    }
                }.orElse(InvoicePaymentResult.NONE)
    }

}
