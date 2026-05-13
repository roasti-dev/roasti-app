package dev.roasti.core.datetime

import kotlinx.datetime.Instant

expect fun Instant.formatRelative(): String
