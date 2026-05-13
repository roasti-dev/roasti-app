package dev.roasti.core.database

import app.cash.sqldelight.db.SqlDriver

actual class SqlDelightDriverFactory {
    actual fun createDriver(): SqlDriver = throw UnsupportedOperationException("SQLDelight not supported on JVM server")
}
