/*
    Defines the main() entry point of the app.
    Configures the database and sets up the REST web service.
 */

@file:JvmName("AntaeusApp")

package io.pleo.antaeus.app

import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.data.CustomerTable
import io.pleo.antaeus.data.InvoiceTable
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Money
import io.pleo.antaeus.rest.AntaeusRest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.sql.Connection

fun main() {
    // The tables to create in the database.
    val tables = arrayOf(InvoiceTable, CustomerTable)

    // Connect to the database and create the needed tables. Drop any existing data.
    val db = Database
        .connect("jdbc:sqlite:/tmp/data.db", "org.sqlite.JDBC")
        .also {
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            transaction(it) {
                addLogger(StdOutSqlLogger)
                // Drop all existing tables to ensure a clean slate on each run
                SchemaUtils.drop(*tables)
                // Create all tables
                SchemaUtils.create(*tables)
            }
        }

    // Set up data access layer.
    val dal = AntaeusDal(db = db)

    // Insert example data in the database.
    setupExampleEntities(dal = dal)

    // Create REST web services.
    val invoiceService = InvoiceService(dal = dal)
    val customerService = CustomerService(dal = dal)

    AntaeusRest(
        invoiceService = invoiceService,
        customerService = customerService
    ).run()
}

// Setup initial data
private fun setupExampleEntities(dal: AntaeusDal) {
    // A customer with an account balance of 42 DKK.
    val customer = dal.createCustomer(
       accountBalance = Money(
           value = BigDecimal(42),
           currency = Currency.DKK
       )
    )

    // Customer needs to pay an invoice of 10 DKK.
    val invoice = dal.createInvoice(
        amount = Money(
            value = BigDecimal.TEN,
            currency = Currency.DKK
        ),
        customer = customer!!
    )
}
