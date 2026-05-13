package dev.roasti.core.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.roasti.RoastiDatabaseCache

actual class SqlDelightDriverFactory(private val context: Context) {
    actual fun createDriver() : SqlDriver {
        return AndroidSqliteDriver(RoastiDatabaseCache.Schema, context, "roasti_v3.db")
    }
}
