package dev.roasti.brew

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.roasti.feature.brew.domain.BrewRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * После перезагрузки телефона запланированные будильники теряются. Перепланируем будущие
 * WAITING-брю; прошедшие не трогаем — wall-clock покажет «готово» при открытии (§13.4).
 * Koin доступен: RoastiApplication.onCreate отрабатывает до onReceive.
 */
class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val brewRepository: BrewRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                brewRepository.rescheduleActiveAlarms()
            } finally {
                pending.finish()
            }
        }
    }
}
