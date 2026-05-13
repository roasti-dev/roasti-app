package dev.roasti.core.database

import app.cash.sqldelight.db.SqlDriver
import dev.roasti.Recipe
import dev.roasti.RoastiDatabaseCache

expect class SqlDelightDriverFactory {
    fun createDriver() : SqlDriver
}

fun createDatabase(driverFactory: SqlDelightDriverFactory): RoastiDatabaseCache {
    val driver = driverFactory.createDriver()
    return RoastiDatabaseCache(
        driver = driver,
        RecipeAdapter = Recipe.Adapter(
            brew_methodAdapter = brewMethodColumnAdapter,
            difficultyAdapter = difficultyColumnAdapter,
            roast_levelAdapter = roastLevelColumnAdapter,
        ),
    )
}
