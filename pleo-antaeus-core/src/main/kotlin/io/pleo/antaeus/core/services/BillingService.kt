package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class BillingService (private val invoiceService: InvoiceService) : PaymentProvider {

    override fun charge(invoice: Invoice): Boolean {
        TODO("Not yet implemented")
    }

    /*
        Will run billing for all pending invoices for every customer. This could be kicked off by a cron job or some
        other scheduling service, but for the challenge we manually trigger this with a rest call
     */
    fun runScheduledBillingCycle() {

    }

    /*
        Will be called by runScheduledBillingCycle as it calls each individual group of invoices. There will also be a
        rest endpoint to call this manually for demo purposes for this challenge
     */
    fun billIndividualCustomer(customerId: Int): List<Invoice> {
        return invoiceService.fetchByStatus(InvoiceStatus.PENDING.toString())
            .filter { invoice: Invoice -> invoice.customerId.equals(customerId) }
    }

    fun updateInvoiceStatusToPaid(invoiceId: Int) {
        return invoiceService.updateStatus(invoiceId, InvoiceStatus.PENDING.toString())
    }







// TODO - Add code e.g. here
    /*  getting billing can be done 1 of 2 ways:
        1. Go through each customer and search invoice by customerId AND status=PENDING
        2. Filter list of all invoices with status=PENDING and group by customerId
     */

    // fetch all PENDING invoices

    // bundle all PENDING invoices to specific customers
    // - create exception to demo handling of CustomerNotFoundException?

    // update invoice currencies to match customer currency
    // - create exception to demo handling of CurrencyMismatchException

    // create new invoices
    // - create exception to demo handling of CustomerNotFoundException(create new customer)
}
