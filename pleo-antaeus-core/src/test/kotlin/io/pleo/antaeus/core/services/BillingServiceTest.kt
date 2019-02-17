package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.data.AntaeusDal
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.helpers.DateTimeProvider
import io.pleo.antaeus.core.helpers.Logger
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.BillingService.EscalationType
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import kotlin.random.Random

class BillingServiceTest {

    class TestModel {

        fun getRandomStatus(): InvoiceStatus {
            if (Random.nextInt() == 0)
                return InvoiceStatus.PENDING
            else
                return InvoiceStatus.PAID
        }

        val mockCustomer = Customer(
                id = 1,
                currency = Currency.values()[Random.nextInt(0, Currency.values().size)]
        )

        val pendingInvoices = listOf(
                Invoice(
                        id = 1,
                        amount = Money(
                                value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                                currency = mockCustomer.currency
                        ), customerId = mockCustomer.id, status = InvoiceStatus.PENDING, valid = true
                ),
                Invoice(
                        id = 2,
                        amount = Money(
                                value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                                currency = mockCustomer.currency
                        ), customerId = mockCustomer.id, status = InvoiceStatus.PENDING, valid = true
                )
        )

        val paidInvoices = listOf(
                Invoice(
                        id = 3,
                        amount = Money(
                                value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                                currency = mockCustomer.currency
                        ), customerId = mockCustomer.id, status = InvoiceStatus.PAID, valid = true
                ),
                Invoice(
                        id = 4,
                        amount = Money(
                                value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                                currency = mockCustomer.currency
                        ), customerId = mockCustomer.id, status = InvoiceStatus.PAID, valid = true
                )
        )

        var invoiceService = mockk<InvoiceService> {}
        var customerService = mockk<CustomerService> {}
        var paymentProvider = mockk<PaymentProvider> {}
        var dateTimeProvider = mockk<DateTimeProvider> {
            every { isFirstDayOfMonth() } returns true
        }
        val logger = mockk<Logger> {
            every { info(any()) } returns Unit
            every { warn(any()) } returns Unit
            every { error(any()) } returns Unit
            every { debug(any()) } returns Unit
        }
    }

    @Test
    fun `test that the escalate function updates invoice validity when customer is not found`() {

        val model = TestModel()

        model.invoiceService = mockk {
            every { updateInvoiceValidity(any(), any()) } returns Unit
        }

        model.customerService = mockk {
            every { fetch(1) } returns model.mockCustomer
        }

        model.paymentProvider = mockk {
            every { charge(any()) } returns true
        }

        val billingService = BillingService(
                invoiceService = model.invoiceService,
                customerService = model.customerService,
                paymentProvider = model.paymentProvider,
                dateTimeProvider = model.dateTimeProvider,
                logger = model.logger)

        val invoiceOne = model.pendingInvoices[0]
        billingService.escalate(invoiceOne, EscalationType.CUSTOMER_NOT_FOUND)
        verify { model.invoiceService.updateInvoiceValidity(invoiceOne.id, false) }
    }

    @Test
    fun `test that the escalate function handles currency mismatch appropriately`() {
        val model = TestModel()

        model.invoiceService = mockk {
            every { updateInvoiceValidity(any(), any()) } returns Unit
            every { updateInvoiceCurrencyWithCustomer(any(), any()) } returns Unit
        }

        model.customerService = mockk {
            every { fetch(1) } returns model.mockCustomer
        }

        model.paymentProvider = mockk {
            every { charge(any()) } returns true
        }

        val billingService = BillingService(
                invoiceService = model.invoiceService,
                customerService = model.customerService,
                paymentProvider = model.paymentProvider,
                dateTimeProvider = model.dateTimeProvider,
                logger = model.logger)

        val invoiceOne = model.pendingInvoices[0]
        billingService.escalate(invoiceOne, EscalationType.CURRENCY_MISMATCH)
        verify { model.invoiceService.updateInvoiceCurrencyWithCustomer(invoiceOne.id, model.mockCustomer) }
    }

    @Test
    fun `test that exec method processes the two pending invoices`() {

        val model = TestModel()

        model.invoiceService = mockk {
            every { updateInvoiceValidity(any(), any()) } returns Unit
            every { updateInvoiceCurrencyWithCustomer(any(), any()) } returns Unit
            every { updateInvoiceStatus(any(), any()) } returns true
            every { fetchAllDueInvoices() } returns model.pendingInvoices
        }

        model.customerService = mockk {
            every { fetch(1) } returns model.mockCustomer
        }

        model.paymentProvider = mockk {
            every { charge(any()) } returns true
        }

        val billingService = BillingService(
                invoiceService = model.invoiceService,
                customerService = model.customerService,
                paymentProvider = model.paymentProvider,
                dateTimeProvider = model.dateTimeProvider,
                logger = model.logger)

        runBlocking {
            billingService.exec()
        }

        verify { model.paymentProvider.charge(model.pendingInvoices[0]) }
        verify { model.paymentProvider.charge(model.pendingInvoices[1]) }
    }

    @Test
    fun `test that exec method correctly handles network outage`() {

        val model = TestModel()

        model.invoiceService = mockk {
            every { updateInvoiceValidity(any(), any()) } returns Unit
            every { updateInvoiceCurrencyWithCustomer(any(), any()) } returns Unit
            every { updateInvoiceStatus(any(), any()) } returns true
            every { fetchAllDueInvoices() } returns model.pendingInvoices
        }

        model.customerService = mockk {
            every { fetch(1) } returns model.mockCustomer
        }

        model.paymentProvider = mockk {
            every { charge(any()) } throws NetworkException() andThen true
        }

        val billingService = BillingService(
                invoiceService = model.invoiceService,
                customerService = model.customerService,
                paymentProvider = model.paymentProvider,
                dateTimeProvider = model.dateTimeProvider,
                logger = model.logger)

        runBlocking {
            billingService.exec()
        }

        val slot = slot<String>()
        verify(atLeast = 1) { model.logger.debug(msg = capture(slot)) }
        assert(slot.captured.contains(model.pendingInvoices[0].id.toString()))

        verify(exactly = 2) { model.paymentProvider.charge(any())}
    }

    @Test
    fun `test that exec method correctly handles customer not found exception`() {

        val model = TestModel()

        model.invoiceService = mockk {
            every { updateInvoiceValidity(any(), any()) } returns Unit
            every { updateInvoiceCurrencyWithCustomer(any(), model.mockCustomer) } returns Unit
            every { updateInvoiceStatus(any(), any()) } returns true
            every { fetchAllDueInvoices() } returns model.pendingInvoices
        }

        model.customerService = mockk {
            every { fetch(1) } returns model.mockCustomer
        }

        model.paymentProvider = mockk {
            every { charge(any()) } throws CustomerNotFoundException(model.pendingInvoices.first().customerId) andThen true
        }

        val billingService = BillingService(
                invoiceService = model.invoiceService,
                customerService = model.customerService,
                paymentProvider = model.paymentProvider,
                dateTimeProvider = model.dateTimeProvider,
                logger = model.logger)

        runBlocking {
            billingService.exec()
        }

        verify { model.paymentProvider.charge(model.pendingInvoices[0]) }
        verify { model.paymentProvider.charge(model.pendingInvoices[1]) }
        verify { billingService.escalate(model.pendingInvoices[0],EscalationType.CUSTOMER_NOT_FOUND) }
        verify { billingService.handleCustomerNotFoundException(model.pendingInvoices[0]) }

    }

    @Test
    fun `test that exec method correctly handles currency mismatch exception`() {

        val model = TestModel()

        model.invoiceService = mockk {
            every { updateInvoiceValidity(any(), any()) } returns Unit
            every { updateInvoiceCurrencyWithCustomer(1 , model.mockCustomer) } returns Unit
            every { updateInvoiceCurrencyWithCustomer(2 , model.mockCustomer) } returns Unit
            every { updateInvoiceStatus(any(), any()) } returns true
            every { fetchAllDueInvoices() } returns model.pendingInvoices
        }

        model.customerService = mockk {
            every { fetch(1) } returns model.mockCustomer
        }

        model.paymentProvider = mockk {
            every { charge(any()) } throws CurrencyMismatchException(model.pendingInvoices[0].id, model.pendingInvoices[0].customerId) andThen true
        }

        val billingService = BillingService(
                invoiceService = model.invoiceService,
                customerService = model.customerService,
                paymentProvider = model.paymentProvider,
                dateTimeProvider = model.dateTimeProvider,
                logger = model.logger)

        runBlocking {
            billingService.exec()
        }

        val slot = slot<String>()
        verify(atLeast = 1) { model.logger.warn(msg = capture(slot)) }
        assert(slot.captured.contains(model.pendingInvoices[0].id.toString()))

        verify { model.paymentProvider.charge(model.pendingInvoices[0]) }
    }
}
