package io.pleo.antaeus.data

import org.jetbrains.exposed.sql.Table

object Invoices : Table() {
    val id = varchar("id", 10).primaryKey() // Column<String>
}
