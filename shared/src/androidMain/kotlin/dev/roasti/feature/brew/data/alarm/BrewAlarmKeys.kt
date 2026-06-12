package dev.roasti.feature.brew.data.alarm

/**
 * Контракт между [BrewAlarmSchedulerImpl] (shared/androidMain) и ресивером/нотифаером
 * (composeApp/androidMain). Scheduler не ссылается на класс ресивера напрямую — broadcast
 * адресуется по action + setPackage(ownPackage) (package-scoped → разрешён manifest-ресиверам).
 */
object BrewAlarmKeys {
    const val ACTION_BREW_READY = "dev.roasti.action.BREW_READY"

    const val EXTRA_BREW_ID = "brew_id"
    const val EXTRA_RECIPE_ID = "recipe_id"
    const val EXTRA_RECIPE_TITLE = "recipe_title"
    const val EXTRA_NEXT_STEP = "next_step_index"

    /** Стабильный код для PendingIntent и id уведомления (один Brew → один слот). */
    fun requestCode(brewId: String): Int = brewId.hashCode()
}
