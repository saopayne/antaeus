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

    val retryInvoiceList = mutableListOf<Invoice>()

    fun chargeInvoices() {
        val dueInvoiceList = dal.fetchDueInvoices()
        chargeInvoicesInList(dueInvoiceList)
    }

    fun chargeInvoicesInList(invoiceList: List<Invoice>, retry = false) {
        invoiceList.forEach { invoice ->
            // ideally, each charge should run in a sequential manner for a start but coroutines can be explored
            try {
                val isChargeSuccessful = paymentProvider.charge(invoice)
                if (isChargeSuccessful) dal.updateInvoiceStatus(invoice.id, InvoiceStatus.PAID)
            } catch (nfException: CustomerNotFoundException) {
                escalate(invoice, EscalationType.CUSTOMER_NOT_FOUND)
            } catch (mismatchException: CurrencyMismatchException) {
                escalate(invoice, EscalationType.CURRENCY_MISMATCH)
            } catch (networkException: NetworkException) {
                logger.debug("Network error while charging invoice: ${invoice.id}, enqueuing for a retry.")
                if (!retry) addInvoiceToRetryList(invoice)
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
            EscalationType.CUSTOMER_NOT_FOUND -> {
                // update the `valid` column on the invoice table
                dal.updateInvoiceValidity(invoice.id, false)
            }
            EscalationType.CURRENCY_MISMATCH -> {
                logger.debug("Currency mismatch for the invoice: ${invoice.id}, " +
                        "setting the invoice currency to that on the customer table.")
                handleCurrencyMismatch(invoice)
            }
        }
    }

    fun handleCurrencyMismatch(invoice: Invoice) {
        dal.updateInvoiceCurrency(invoice.id)
        addInvoiceToRetryList(invoice)
    }

    fun addInvoiceToRetryList(invoice: Invoice) {
        retryInvoiceList.add(invoice)
    }

    // retry failed just once which w
    fun retryFailedInvoices() {
        chargeInvoicesInList(retryInvoiceList, true)
    }

    enum class EscalationType {
        CUSTOMER_NOT_FOUND, CURRENCY_MISMATCH
    }

}