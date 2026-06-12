package dev.roasti.core.datetime

import kotlinx.datetime.Clock

/**
 * Абсолютное (wall-clock) время в epochMillis. В отличие от
 * [dev.roasti.feature.recipe.domain.session.BrewingClock] (monotonic, foreground-only),
 * сравнимо между запусками процесса и переживает рестарт/перезагрузку.
 * Используется для фонового ожидания (`waitUntil`). Тестируется фейком.
 */
interface WallClock {
    fun nowMillis(): Long
}

class SystemWallClock : WallClock {
    override fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
