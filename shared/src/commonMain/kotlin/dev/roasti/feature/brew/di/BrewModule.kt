package dev.roasti.feature.brew.di

import dev.roasti.core.datetime.SystemWallClock
import dev.roasti.core.datetime.WallClock
import dev.roasti.feature.brew.data.BrewRepositoryImpl
import dev.roasti.feature.brew.domain.BrewRepository
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * commonMain DI фичи Brew. BrewAlarmScheduler приходит из brewPlatformModule (androidMain).
 * Регистрировать в RoastiApplication перед viewModelsModule, после brewPlatformModule.
 */
val brewModule = module {
    single<WallClock> { SystemWallClock() }
    single { BrewRepositoryImpl(get(), get(), get()) } bind BrewRepository::class
}
