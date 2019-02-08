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
    val db = Database
        .connect("jdbc:sqlite:/tmp/data.db", "org.sqlite.JDBC")
        .also {
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            val tables = arrayOf(InvoiceTable, CustomerTable)
            transaction(it) {
                addLogger(StdOutSqlLogger)
                // Drop all existing tables to ensure a clean slate on each run
                SchemaUtils.drop(*tables)
                // Create all tables
                SchemaUtils.create(*tables)
            }
        }

    val dal = AntaeusDal(db = db)

    setupEntities(dal = dal)

    val invoiceService = InvoiceService(dal = dal)
    val customerService = CustomerService(dal = dal)

    AntaeusRest(
        invoiceService = invoiceService,
        customerService = customerService
    ).run()
}

// This will create all schemas and setup initial data
private fun setupEntities(dal: AntaeusDal) {
    val customer = dal.createCustomer(
       accountBalance = Money(
           value = BigDecimal(42),
           currency = Currency.DKK
       )
    )

    val invoice = dal.createInvoice(
        amount = Money(
            value = BigDecimal.TEN,
            currency = Currency.DKK
        ),
        customer = customer!!
    )
}
