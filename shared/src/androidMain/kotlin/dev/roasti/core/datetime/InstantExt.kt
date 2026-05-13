package dev.roasti.core.datetime

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration.Companion.hours

actual fun Instant.formatRelative(): String {
    val now = Clock.System.now()
    val diff = now - this
    val tz = TimeZone.currentSystemDefault()
    return when {
        diff < 1.hours -> "${diff.inWholeMinutes.coerceAtLeast(1)}m"
        diff < 24.hours -> "${diff.inWholeHours}h"
        else -> {
            val local = toLocalDateTime(tz).date
            val sameYear = local.year == now.toLocalDateTime(tz).year
            val pattern = if (sameYear) "d MMMM" else "dd/MM/yy"
            DateTimeFormatter
                .ofPattern(pattern, Locale.getDefault())
                .format(java.time.LocalDate.of(local.year, local.monthNumber, local.dayOfMonth))
        }
    }
}
