package io.pleo.antaeus.core.helpers

import java.time.LocalDateTime
import java.time.ZoneId

class DateTimeProvider {

    fun now() : LocalDateTime {
        return LocalDateTime.now(ZoneId.of("UTC"))
    }

    fun isFirstDayOfMonth() : Boolean {
        return now().dayOfMonth == 1
    }

    fun nextFirstDayOfMonth() : LocalDateTime {
        val now = now()
        return when {
            now.monthValue != 12 -> LocalDateTime.of(now.year, now.monthValue + 1, 1, 0, 0)
            else -> LocalDateTime.of(now.year + 1, 1, 1, 0, 0)
        }
    }

}