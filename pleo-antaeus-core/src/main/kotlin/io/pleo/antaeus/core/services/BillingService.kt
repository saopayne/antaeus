/*
    This is the billing service. It is a "mock" of an external service that you can pretend runs on another system.
    With this API you can ask customers to pay an invoice.

    This mock will always succeed, however the documentation lays out scenarios in which paying an invoice could fail.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.models.Invoice

class BillingService {
    /*
        Charge a customer's account the amount from the invoice.

        Returns:
          `True` when the customer account was successfully charged the given amount.
          `False` when the customer account balance did not allow the charge.

        Throws:
          `CustomerNotFoundException`: when no customer has the given id.
          `CurrencyMismatchException`: when the currency does not match the customer account.
     */
    fun charge(invoice: Invoice): Boolean {
        // Fake implementation of internal API.
        return true
    }
}
