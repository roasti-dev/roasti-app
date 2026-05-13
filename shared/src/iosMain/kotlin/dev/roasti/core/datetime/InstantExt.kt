package dev.roasti.core.datetime

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import platform.Foundation.NSCalendar
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
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
            val components = NSDateComponents().apply {
                setYear(local.year.toLong())
                setMonth(local.monthNumber.toLong())
                setDay(local.dayOfMonth.toLong())
            }
            val date = NSCalendar.currentCalendar
                .dateFromComponents(components) ?: return ""
            NSDateFormatter().apply {
                locale = NSLocale.currentLocale
                dateFormat = if (sameYear) "d MMMM" else "dd/MM/yy"
            }.stringFromDate(date)
        }
    }
}
