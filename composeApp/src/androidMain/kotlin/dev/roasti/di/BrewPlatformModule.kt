package dev.roasti.di

import dev.roasti.feature.brew.data.alarm.BrewAlarmSchedulerImpl
import dev.roasti.feature.brew.domain.BrewAlarmScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Android-платформенное DI фичи Brew. Живёт в composeApp (нужен androidContext() — koin-android
 * подключён здесь, а не в shared/androidMain). Scheduler-impl — из shared/androidMain.
 * Регистрировать перед brewModule.
 */
val brewPlatformModule = module {
    single<BrewAlarmScheduler> { BrewAlarmSchedulerImpl(androidContext()) }
}
