package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KLogging

class BillingService( private val paymentProvider: PaymentProvider, private val dal: AntaeusDal ) {

    companion object: KLogging()

    fun chargeInvoices() {
        val invoiceList = dal.fetchInvoices(InvoiceStatus.PENDING)
        invoiceList.forEach { invoice ->
            // ideally, each charge should run in a sequential manner for a start but coroutines can be explored
            try {
                var isChargeSuccessful = paymentProvider.charge(invoice)
                if (isChargeSuccessful) dal.updateInvoiceStatus(invoice.id, InvoiceStatus.PAID)
            } catch (nfException: CustomerNotFoundException) {
                escalate(invoice, EscalationType.CUSTOMER_NOT_FOUND)
            } catch (mismatchException: CurrencyMismatchException) {
                escalate(invoice, EscalationType.CURRENCY_MISMATCH)
            } catch (networkException: NetworkException) {
                logger.debug("Network error while charging invoice: ${invoice.id}, enqueuing for a retry.")
                addInvoiceToRetryList()
            }
       }
    }

    /**
     * Escalating the invoice would mean some other action has to be performed
     * -> customer not found: the invoice should be marked as invalid
     * -> currency mismatch: typically, a human has to interfere to resolve whether
     *    the currency on the customer table should be used for invoice or vice-versa.
     *    For this project, I'm re-assigning the currency on the customer to be that of the invoice
     */
    fun escalate(invoice: Invoice, escalationType: EscalationType) {
        when (escalationType) {
            EscalationType.CUSTOMER_NOT_FOUND ->
                // update the `valid` column on the invoice table
                dal.updateInvoiceValidity(invoice.id, false)
            EscalationType.CURRENCY_MISMATCH ->
                logger.debug("Currency mismatch for the invoice: ${invoice.id}, " +
                        "setting the invoice currency to that on the customer table.")
                addInvoiceToRetryList(invoice)
        }
    }

    fun addInvoiceToRetryList(invoice: Invoice) {}

    fun retryFailedInvoices() {}

    enum class EscalationType {
        CUSTOMER_NOT_FOUND, CURRENCY_MISMATCH
    }

}