@file:JvmName("AntaeusApp")

package io.pleo.antaeus.app

import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.data.CustomerTable
import io.pleo.antaeus.data.InvoiceTable
import io.pleo.antaeus.rest.AntaeusRest
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.sql.Connection

private val logger = KotlinLogging.logger {}

fun main() {
    val db = Database
        .connect("jdbc:sqlite:/tmp/data.db", "org.sqlite.JDBC")
        .also {
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            setupDB(db = it)
        }

    val dal = AntaeusDal(db = db)

    val invoiceService = InvoiceService(dal = dal)
    val customerService = CustomerService(dal = dal)

    AntaeusRest(
        invoiceService = invoiceService,
        customerService = customerService
    ).run()
}

// This will create all schemas and setup initial data
private fun setupDB(db: Database) {
    logger.info { "Setting up initial database" }

    val tables = arrayOf(InvoiceTable, CustomerTable)

    transaction(db) {
        addLogger(StdOutSqlLogger)

        // Drop all existing tables to ensure a clean slate on each run
        SchemaUtils.drop(*tables)

        // Create all tables
        SchemaUtils.create(*tables)

        // Setup initial state
        val firstCustomerId = CustomerTable.insert {
            it[balance] = BigDecimal(42)
            it[currency] = "DKK"
        } get CustomerTable.id

        InvoiceTable.insert {
            it[value] = BigDecimal.TEN
            it[currency] = "DKK"
            it[customerId] = firstCustomerId!!
        }
    }

    logger.info("Done setting up database")
}