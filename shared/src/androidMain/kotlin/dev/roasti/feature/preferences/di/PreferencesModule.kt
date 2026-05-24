package dev.roasti.feature.preferences.di

import dev.roasti.feature.preferences.data.BrewingPreferencesRepositoryImpl
import dev.roasti.feature.preferences.domain.BrewingPreferencesRepository
import org.koin.dsl.bind
import org.koin.dsl.module

val preferencesModule = module {
    single { BrewingPreferencesRepositoryImpl(get()) } bind BrewingPreferencesRepository::class
}
