package dev.roasti.feature.brew.data.alarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dev.roasti.feature.brew.domain.BrewAlarmScheduler

/**
 * Android-реализация планировщика будильников. Использует **inexact**
 * [AlarmManager.setAndAllowWhileIdle] — работает в Doze, НЕ требует SCHEDULE_EXACT_ALARM
 * (targetSdk 36 → exact под спец-разрешением). Для ожидания в часы/дни погрешность в минуты
 * допустима (см. PRODUCT §5).
 */
class BrewAlarmSchedulerImpl(
    private val context: Context,
) : BrewAlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun schedule(
        brewId: String,
        triggerAtEpochMillis: Long,
        recipeId: String,
        recipeTitle: String,
        nextStepIndex: Int,
    ) {
        val intent = Intent(BrewAlarmKeys.ACTION_BREW_READY).apply {
            setPackage(context.packageName)
            putExtra(BrewAlarmKeys.EXTRA_BREW_ID, brewId)
            putExtra(BrewAlarmKeys.EXTRA_RECIPE_ID, recipeId)
            putExtra(BrewAlarmKeys.EXTRA_RECIPE_TITLE, recipeTitle)
            putExtra(BrewAlarmKeys.EXTRA_NEXT_STEP, nextStepIndex)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            BrewAlarmKeys.requestCode(brewId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtEpochMillis, pendingIntent)
    }

    override fun cancel(brewId: String) {
        val intent = Intent(BrewAlarmKeys.ACTION_BREW_READY).apply { setPackage(context.packageName) }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            BrewAlarmKeys.requestCode(brewId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        // снять уже показанное «готово»-уведомление, если оно было
        notificationManager.cancel(BrewAlarmKeys.requestCode(brewId))
    }
}
