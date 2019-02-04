package io.pleo.antaeus.rest

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.services.InvoiceService
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class AntaeusRest (
    private val invoiceService: InvoiceService
) : Runnable {

    override fun run() {
        app.start(7000)
    }

    private val app = Javalin
        .create()
        .apply {
            exception(InvoiceNotFoundException::class.java) { _, ctx ->
                ctx.status(404)
            }
            exception(Exception::class.java) { e, _ ->
                logger.error(e) { "Internal server error" }
            }
            error(404) { ctx -> ctx.json("not found") }
        }

    init {
        app.routes {
           path("rest") {
               // Route to check whether the app is running
               get("health") {
                   it.json("ok")
               }

               // V1
               path("v1") {
                   path("invoices") {
                       get {
                           it.json(invoiceService.fetchAll())
                       }

                       get(":id") {
                          it.json(invoiceService.fetch(it.pathParam("id")))
                       }
                   }
               }
           }
        }
    }
}