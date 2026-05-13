package dev.roasti.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.roasti.RoastiDatabaseCache

actual class SqlDelightDriverFactory {
    actual fun createDriver() : SqlDriver {
        return NativeSqliteDriver(RoastiDatabaseCache.Schema, "roasti_v3.db")
    }
}
