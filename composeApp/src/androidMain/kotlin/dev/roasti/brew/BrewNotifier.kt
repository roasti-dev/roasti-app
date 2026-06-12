package dev.roasti.brew

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.roasti.R
import dev.roasti.feature.brew.data.alarm.BrewAlarmKeys

/**
 * Постит локальное «готово»-уведомление. Живёт в composeApp/androidMain (доступ к R.string,
 * R.drawable, MainActivity). Тап → запуск приложения с extras brewId/nextStep (deep-link
 * консьюмится в MainActivity — шаг навигации §7.3).
 */
class BrewNotifier(
    private val context: Context,
) {

    fun notifyReady(
        brewId: String,
        recipeId: String,
        recipeTitle: String,
        nextStepIndex: Int,
    ) {
        ensureChannel()
        val manager = NotificationManagerCompat.from(context)
        // POST_NOTIFICATIONS не выдан / выключены — тихо выходим, статус покажет wall-clock при открытии
        if (!manager.areNotificationsEnabled()) return

        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply {
                putExtra(BrewAlarmKeys.EXTRA_BREW_ID, brewId)
                putExtra(BrewAlarmKeys.EXTRA_RECIPE_ID, recipeId)
                putExtra(BrewAlarmKeys.EXTRA_NEXT_STEP, nextStepIndex)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        val contentIntent = PendingIntent.getActivity(
            context,
            BrewAlarmKeys.requestCode(brewId),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            // TODO: заменить на монохромную иконку уведомления (пока — иконка приложения)
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle(context.getString(R.string.brew_ready_title, recipeTitle))
            .setContentText(context.getString(R.string.brew_ready_text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(BrewAlarmKeys.requestCode(brewId), notification)
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.brew_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.brew_notification_channel_description)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "brew_ready"
    }
}
