package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.helpers.DateTimeProvider
import io.pleo.antaeus.core.helpers.Logger
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KLogging
import kotlinx.coroutines.*
import java.time.Duration

class BillingService(
        private val invoiceService: InvoiceService,
        private val customerService: CustomerService,
        private val paymentProvider: PaymentProvider,
        private val dateTimeProvider: DateTimeProvider,
        private val logger: Logger) {

    private var job : Job? = null

    // this mimics a message queue temporarily
    val retryInvoiceList = mutableListOf<Invoice>()

    fun exec() {

        logger.info("BillingService starting... will begin watching for due invoices")

        job = GlobalScope.launch {
            if (!dateTimeProvider.isFirstDayOfMonth()) {
                val nextRunTimeInSeconds = getDurationUntilNextRun()
                logger.info ("Suspending job to charge invoices for $nextRunTimeInSeconds seconds which is the next first day of the month")
                delay(nextRunTimeInSeconds)
            } else {
                chargeInvoices()
            }
        }
    }

    protected open suspend fun chargeInvoices() {

        chargeInvoicesInList()

        if (retryInvoiceList.count() > 0) {
            val retryDelayTimeInSec = 180
            delay(retryDelayTimeInSec * 1000L)

            // try previously failed charges just once with a delay of some seconds
            this.logger.info("Retrying failed invoice charges for $${retryInvoiceList.count()} items")
            retryFailedInvoices()
        }
    }

    suspend fun chargeInvoicesInList(retry: Boolean = false) {

        var dueInvoiceList = invoiceService.fetchAllDueInvoices()

        if (retry) dueInvoiceList = retryInvoiceList

        dueInvoiceList.forEach { invoice ->

            this.logger.info("starting charging process for invoice: ${invoice.id} ")

            try {
                val isChargeSuccessful = paymentProvider.charge(invoice)
                if (isChargeSuccessful) invoiceService.updateInvoiceStatus(invoice.id, InvoiceStatus.PAID)
            } catch (e: CustomerNotFoundException) {
                this.logger.debug("Failed to charge invoice ${invoice.id} as the customer ${invoice.customerId} wasn't found")
                escalate(invoice, EscalationType.CUSTOMER_NOT_FOUND)
            } catch (e: CurrencyMismatchException) {
                val customer = customerService.fetch(invoice.customerId)
                this.logger.debug("Failed to charge invoice ${invoice.id} as there's a currency mismatch with customer currency ${customer?.currency}" +
                        " and invoice currency ${invoice.amount.currency}")
                escalate(invoice, EscalationType.CURRENCY_MISMATCH)
            } catch (e: NetworkException) {
                this.logger.debug("Failed to charge invoice ${invoice.id}due to Network error, enqueuing for a retry.")
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
                handleCustomerNotFoundException(invoice)
            }
            EscalationType.CURRENCY_MISMATCH -> {
                handleCurrencyMismatchException(invoice)
            }
        }
    }

    /**
     * It will be better for a human to investigate the cause of this but tentatively,
     * update the `valid` column on the invoice table
     */
    fun handleCustomerNotFoundException(invoice: Invoice) {
        this.logger.warn("Customer not found for the invoice: ${invoice.id}, marking the invoice as invalid.")
        invoiceService.updateInvoiceValidity(invoice.id, false)
    }

    /**
     * This should be open to an in-house check to investigate the cause of this.
     * update the currency of the invoice to the one that exists on the customer table.
     */
    fun handleCurrencyMismatchException(invoice: Invoice) {
        this.logger.warn("Currency mismatch for the invoice: ${invoice.id}, setting the invoice currency to that of the customer table.")
        val customer = customerService.fetch(invoice.customerId)
        invoiceService.updateInvoiceCurrencyWithCustomer(invoice.id, customer)
        addInvoiceToRetryList(invoice)
    }

    fun addInvoiceToRetryList(invoice: Invoice) {
        retryInvoiceList.add(invoice)
    }

    /**
     * retry failed invoices just once which which addresses currency mismatch and network errors
     */
    protected open suspend fun retryFailedInvoices() {
        chargeInvoicesInList(true)
    }

    /**
     * calculates the time in seconds until the next first day of the month
     */
    private fun getDurationUntilNextRun(): Long {
        val nextFirstDayOfMonth = this.dateTimeProvider.nextFirstDayOfMonth()
        return Duration.between(dateTimeProvider.now(), nextFirstDayOfMonth).toMillis() / 1000L
    }

    fun stop() {
        this.logger.info("terminating billing service")

        job?.cancel()
        runBlocking {
            job?.join()
        }

        this.logger.info("billing service successfully stopped")
    }

    enum class EscalationType {
        CUSTOMER_NOT_FOUND, CURRENCY_MISMATCH
    }

}