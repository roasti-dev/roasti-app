package dev.roasti.brew

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.roasti.feature.brew.data.alarm.BrewAlarmKeys

/**
 * Срабатывает по ОС-будильнику в момент `waitUntil`. Только постит уведомление — в БД не пишет
 * (статус готовности считается по wall-clock при открытии приложения, §13.3).
 */
class BrewAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BrewAlarmKeys.ACTION_BREW_READY) return
        val brewId = intent.getStringExtra(BrewAlarmKeys.EXTRA_BREW_ID) ?: return
        val recipeId = intent.getStringExtra(BrewAlarmKeys.EXTRA_RECIPE_ID).orEmpty()
        val recipeTitle = intent.getStringExtra(BrewAlarmKeys.EXTRA_RECIPE_TITLE).orEmpty()
        val nextStepIndex = intent.getIntExtra(BrewAlarmKeys.EXTRA_NEXT_STEP, 0)

        BrewNotifier(context.applicationContext).notifyReady(
            brewId = brewId,
            recipeId = recipeId,
            recipeTitle = recipeTitle,
            nextStepIndex = nextStepIndex,
        )
    }
}
