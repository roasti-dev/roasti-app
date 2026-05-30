package dev.roasti.di

import org.koin.dsl.bind
import org.koin.dsl.module
import dev.roasti.core.database.SqlDelightDriverFactory
import dev.roasti.core.session.storage.KeychainTokenStorage
import dev.roasti.core.session.storage.TokenStorage

val iosPlatformModule = module {
    single { KeychainTokenStorage() } bind TokenStorage::class
    single { SqlDelightDriverFactory() }
}
