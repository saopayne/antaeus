package io.pleo.antaeus.core.exceptions

class InvoiceNotFoundException(id: String) : EntityNotFoundException("Invoice", id)