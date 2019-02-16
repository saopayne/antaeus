package io.pleo.antaeus.core.helpers

import mu.KotlinLogging

class Logger {

    private val kotlinLogger = KotlinLogging.logger {}

    fun info(msg: String) {
        kotlinLogger.info(msg)
    }

    fun error(msg: String) {
        kotlinLogger.error(msg)
    }

    fun warn(msg: String) {
        kotlinLogger.warn(msg)
    }

    fun debug(msg: String) {
        kotlinLogger.debug(msg)
    }
}