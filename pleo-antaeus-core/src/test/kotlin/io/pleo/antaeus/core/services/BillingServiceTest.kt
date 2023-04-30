package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BillingServiceTest {

    private val mockInvoiceList = arrayListOf<Invoice>(
        Invoice(1,11, Money((BigDecimal(100.23)), Currency.EUR), InvoiceStatus.PENDING),
        Invoice(2,12, Money((BigDecimal(100.23)), Currency.GBP), InvoiceStatus.PENDING),
        Invoice(3,12, Money((BigDecimal(100.23)), Currency.USD), InvoiceStatus.PENDING),
        Invoice(4,12, Money((BigDecimal(100.23)), Currency.DKK), InvoiceStatus.PENDING))

    private val dal = mockk<AntaeusDal> {
        every { fetchInvoices() } returns mockInvoiceList
    }

    private val invoiceService = mockk<InvoiceService> {
        every { fetchAll() } returns mockInvoiceList
        every { fetchByStatus(any()) } returns mockInvoiceList
    }

    private val billingService = BillingService(invoiceService)

    @Test
    fun `will throw if customer and invoice currencies do not match`() {
        // throw currency mismatch exception
    }

    @Test
    fun `will update invoice currency to match customer currency`() {
        // lookup customer matching invoice, update invoice recored to match customer currency
    }

    @Test
    fun `will return pending invoices for indivual customer`() {
        assertEquals(1,billingService.billIndividualCustomer(11).size)
        assertEquals(3,billingService.billIndividualCustomer(12).size)
    }

    @Test
    fun `will return list of invoices grouped by customer`() {
        assertEquals(2, billingService.getAllPendingInvoicesGroupedByCustomerId().size)
    }
}