package dev.roasti.feature.brew.domain

/**
 * Планирование/отмена локального ОС-будильника для фонового шага. Интерфейс в commonMain
 * (его дёргает [dev.roasti.feature.brew.data.BrewRepositoryImpl]); Android-impl — в androidMain
 * через AlarmManager. На iOS будет своя реализация (UNUserNotificationCenter), пока не нужна.
 *
 * Параметры (recipeId/recipeTitle/nextStepIndex) передаются в payload будильника, чтобы
 * ресивер собрал уведомление и deep-link без обращения к БД.
 */
interface BrewAlarmScheduler {
    fun schedule(
        brewId: String,
        triggerAtEpochMillis: Long,
        recipeId: String,
        recipeTitle: String,
        nextStepIndex: Int,
    )

    fun cancel(brewId: String)
}
