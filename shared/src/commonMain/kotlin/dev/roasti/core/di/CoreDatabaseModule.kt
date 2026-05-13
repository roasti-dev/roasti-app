package dev.roasti.core.di

import org.koin.dsl.module
import dev.roasti.RoastiDatabaseCache
import dev.roasti.core.database.createDatabase

val coreDatabaseModule = module {
    single<RoastiDatabaseCache> { createDatabase(get()) }
}
