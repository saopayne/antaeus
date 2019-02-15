package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.BillingService.EscalationType
import java.math.BigDecimal
import kotlin.random.Random

class BillingServiceTest {

    fun getRandomStatus(): InvoiceStatus {
        if (Random.nextInt() == 0)
            return InvoiceStatus.PENDING
        else
            return InvoiceStatus.PAID
    }

    val mockCustomer = Customer (
            id = 1,
            currency = Currency.values()[Random.nextInt(0, Currency.values().size)]
    )

    val mockInvoiceList = mutableListOf(
            Invoice(
                    id = 1,
                    amount = Money(
                            value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                            currency = mockCustomer.currency
                    ),
                    customerId = mockCustomer.id,
                    status = InvoiceStatus.PENDING,
                    valid = true
            )
    )

    private val dal = mockk<AntaeusDal> {
        every { fetchCustomer(404) } returns null
        every { updateInvoiceValidity(1, true) } returns true
        every { fetchDueInvoices() } returns mockInvoiceList
        every { updateInvoiceCurrency(1) } returns Unit
        every{ updateInvoiceStatus(1, InvoiceStatus.PAID) } returns true
        every { updateInvoiceValidity(1, false) } returns true
    }

    private val invoice = mockk<Invoice>{}

    private val paymentProvider = mockk<PaymentProvider> {
        every { charge(any())} returns true
    }

    private val billingService = BillingService(paymentProvider = paymentProvider, dal = dal)

    @Test
    fun `test that the escalate function updates invoice validity when customer is not found`() {
        val invoiceOne = mockInvoiceList[0]
        billingService.escalate(invoiceOne, EscalationType.CUSTOMER_NOT_FOUND)
        verify{dal.updateInvoiceValidity(invoiceOne.id, false)}
    }

    @Test
    fun `test that the escalate function handles currency mismatch appropriately`() {
        val invoiceOne = mockInvoiceList[0]
        billingService.escalate(invoiceOne, EscalationType.CURRENCY_MISMATCH)
        verify{ dal.updateInvoiceCurrency(invoiceOne.id)}
    }

}