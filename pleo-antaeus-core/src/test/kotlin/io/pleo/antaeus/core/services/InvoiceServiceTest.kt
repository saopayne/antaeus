package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class InvoiceServiceTest {

    private val mockInvoice1 = Invoice(1,11, Money((BigDecimal(100.23)), Currency.EUR), InvoiceStatus.PAID)
    private val mockInvoice2 = Invoice(2,12, Money((BigDecimal(100.23)), Currency.GBP), InvoiceStatus.PENDING)
    private val mockInvoice3 = Invoice(3,13, Money((BigDecimal(100.23)), Currency.USD), InvoiceStatus.PAID)
    private val mockInvoice4 = Invoice(4,14, Money((BigDecimal(100.23)), Currency.DKK), InvoiceStatus.PENDING)

    private val mockInvoiceList = arrayListOf<Invoice>(mockInvoice1, mockInvoice2, mockInvoice3, mockInvoice4)

    private val dal = mockk<AntaeusDal> {
        every { fetchInvoice(404) } returns null
        every { fetchInvoice(1) } returns mockInvoice1
        every { fetchInvoice(2) } returns mockInvoice2
        every { fetchInvoices() } returns mockInvoiceList
    }

    private val invoiceService = InvoiceService(dal = dal)

    @Test
    fun `will throw if invoice is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
    }

    @Test
    fun `will return expected invoice`() {
        assertEquals(1, invoiceService.fetch(1).id)
        assertEquals(2, invoiceService.fetch(2).id)
    }

    @Test
    fun `will return all invoices`() {
        assertEquals(4, invoiceService.fetchAll().size)
    }

    @Test
    fun `will return all pending invoices`() {
        // take list of both pending and paid invoices, return only pending
    }

    @Test
    fun `will return collection of invoices grouped by customerId`() {
        // take a list of pening invoices, return a collection of invoice lists grouped by customerId
    }

}
