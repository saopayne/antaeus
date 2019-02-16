package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.helpers.DateTimeProvider
import io.pleo.antaeus.core.helpers.Logger
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KLogging
import kotlinx.coroutines.*
import java.time.Duration

class BillingService(
        private val paymentProvider: PaymentProvider,
        private val dal: AntaeusDal,
        private val dateTimeProvider: DateTimeProvider,
        private val logger: Logger) {

    private var job : Job? = null
    val retryInvoiceList = mutableListOf<Invoice>() // this mimics a message queue temporarily

    fun exec() {

        logger.info("BillingService starting... will begin watching for due invoices")

        job = GlobalScope.launch {
            if (!dateTimeProvider.isFirstDayOfMonth()) {
                val nextRunTimeInSeconds = getDurationUntilNextRun()
                logger.info ("Suspending job to charge invoices for $nextRunTimeInSeconds seconds until the next first of the month")
                delay(nextRunTimeInSeconds)
            } else {
                chargeInvoices()
            }
        }
    }

    fun chargeInvoices() {

        val dueInvoiceList = dal.fetchDueInvoices()
        chargeInvoicesInList(dueInvoiceList)

        if (retryInvoiceList.count() > 0) {
            // try previously failed charges just once
            this.logger.info("Retrying failed invoice charges for $${retryInvoiceList.count()} items")
            retryFailedInvoices()
        }
    }

    fun chargeInvoicesInList(invoiceList: List<Invoice>, retry: Boolean = false) {
        invoiceList.forEach { invoice ->

            this.logger.info("starting charging process for invoice: ${invoice.id} ")

            try {
                val isChargeSuccessful = paymentProvider.charge(invoice)
                if (isChargeSuccessful) dal.updateInvoiceStatus(invoice.id, InvoiceStatus.PAID)
            } catch (e: CustomerNotFoundException) {
                logger.debug("Failed to charge invoice ${invoice.id} as the customer ${invoice.customerId} wasn't found")
                escalate(invoice, EscalationType.CUSTOMER_NOT_FOUND)
            } catch (e: CurrencyMismatchException) {
                val customer = dal.fetchCustomer(invoice.customerId)
                logger.debug("Failed to charge invoice ${invoice.id} as there's a currency mismatch with customer currency ${customer?.currency}" +
                        " and invoice currency ${invoice.amount.currency}")
                escalate(invoice, EscalationType.CURRENCY_MISMATCH)
            } catch (e: NetworkException) {
                logger.debug("Failed to charge invoice ${invoice.id}due to Network error, enqueuing for a retry.")
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
                logger.warn("Currency mismatch for the invoice: ${invoice.id}, " +
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

    // retry failed invoices just once which which addresses currency mismatch and network errors
    fun retryFailedInvoices() {
        chargeInvoicesInList(retryInvoiceList, true)
    }

    /**
     * calculates the time in seconds until the next first day of the month
     */
    private fun getDurationUntilNextRun(): Long {
        val nextFirstDayOfMonth = this.dateTimeProvider.nextFirstDayOfMonth()
        return Duration.between(nextFirstDayOfMonth, dateTimeProvider.now()).toMillis() * 1000L
    }

    enum class EscalationType {
        CUSTOMER_NOT_FOUND, CURRENCY_MISMATCH
    }

}