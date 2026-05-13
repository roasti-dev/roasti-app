package dev.roasti.testing

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.roasti.Recipe
import dev.roasti.RoastiDatabaseCache
import dev.roasti.feature.recipe.domain.model.BrewMethod
import dev.roasti.feature.recipe.domain.model.Difficulty
import dev.roasti.feature.recipe.domain.model.RoastLevel

fun inMemoryRoastiDatabase(): RoastiDatabaseCache {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    RoastiDatabaseCache.Schema.create(driver)
    return RoastiDatabaseCache(
        driver = driver,
        RecipeAdapter = Recipe.Adapter(
            brew_methodAdapter = enumAdapter(BrewMethod::values, BrewMethod::name),
            difficultyAdapter = enumAdapter(Difficulty::values, Difficulty::name),
            roast_levelAdapter = enumAdapter(RoastLevel::values, RoastLevel::name),
        ),
    )
}

private inline fun <reified E : Enum<E>> enumAdapter(
    crossinline values: () -> Array<E>,
    crossinline name: (E) -> String,
): ColumnAdapter<E, String> = object : ColumnAdapter<E, String> {
    override fun decode(databaseValue: String): E =
        values().first { name(it) == databaseValue }
    override fun encode(value: E): String = name(value)
}
