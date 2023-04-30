package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class BillingService (private val invoiceService: InvoiceService, private val customerService: CustomerService) : PaymentProvider {

    /*
       Here we override the charge method from the PaymentProvider interface, but for this challenge we will just return
       true to simulate calls to an external service.We can catch specific exceptions here and handle them individually.
       I have included a comment to briefly describe what I would do in each exeption in a real implementation
     */
    override fun charge(invoice: Invoice): Boolean {
        try {
            return true
        } catch (exception: CustomerNotFoundException) {
            // trigger alert/ticket for manual intervention/investigation to resolve invoice with invalid customerId
            return false
        } catch (exception: CurrencyMismatchException) {
            // call additional logic to update invoice currency to match what the customer uses
            return false
        } catch (exception: InvoiceNotFoundException) {
            // trigger alert/ticket for manual intervention/investigation to resolve by creating missing invoice record
            return false
        }
    }

    /*
        Will run billing for all pending invoices for every customer. This could be kicked off by a cron job or some
        other scheduling service, but for the challenge we manually trigger this with a rest call
     */
    fun runScheduledBillingCycle() {
        val customerInvoices = invoiceService.getAllPendingInvoicesGroupedByCustomerId()
        println(customerInvoices.size.toString() + " customers have been charged during this months billing cycle.")

        for(invoices in customerInvoices) {
            for (invoice in invoices) {
                println("Starting billing for cutomer: id=" + invoice.customerId)

                updateInvoiceCurrenciesToMatchCustomer(invoice)

                if(charge(invoice)) {
                    println("Invoice " + invoice.id + " charge success!")
                    updateInvoiceStatusToPaid(invoice.id)
                }else {
                    println("Invoice " + invoice.id + " charge failure!")
                }
            }
        }
    }

    /*
     Additional function for a scenario where we might want to bill a single customer.
     There will be a rest endpoint to call this manually for demo purposes for this challenge
    */
    fun billIndividualCustomer(customerId: Int) {
        var customerInvoices = invoiceService.getAllPendingInvoicesForIndividualCustomer(customerId)
        println("Customer " + customerId + " will be billed for " + customerInvoices.size + " invoices")
        for(invoice in customerInvoices) {
            updateInvoiceCurrenciesToMatchCustomer(invoice)

            if(charge(invoice)) {
                println("Invoice " + invoice.id + " charge success!")
                updateInvoiceStatusToPaid(invoice.id)
            }else {
                println("Invoice " + invoice.id + " charge failure!")
            }
        }
    }

    fun updateInvoiceCurrenciesToMatchCustomer(invoice: Invoice) {
        val customerCurrency = customerService.fetch(invoice.customerId).currency
        // we do this check to avoid redundant db calls
        if(customerCurrency != invoice.amount.currency) {
            invoiceService.updateCurrency(invoice.id, customerCurrency.toString())
        }
    }

    fun updateInvoiceStatusToPaid(invoiceId: Int) {
        return invoiceService.updateStatus(invoiceId, InvoiceStatus.PAID.toString())
    }
}
