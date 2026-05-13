package dev.roasti.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module
import dev.roasti.core.database.SqlDelightDriverFactory
import dev.roasti.core.session.storage.SharedPreferencesTokenStorage
import dev.roasti.core.session.storage.TokenStorage

val platformModule = module {
    single { SharedPreferencesTokenStorage(get()) } bind TokenStorage::class
    single { SqlDelightDriverFactory(androidContext()) }
}
