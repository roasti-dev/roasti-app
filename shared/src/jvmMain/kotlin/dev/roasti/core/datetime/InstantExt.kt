package dev.roasti.core.datetime

import kotlinx.datetime.Instant

actual fun Instant.formatRelative(): String = throw UnsupportedOperationException("formatRelative not supported on JVM server")
