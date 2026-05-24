package dev.roasti.feature.recipe.domain.session

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext
import kotlin.time.TimeSource

interface BrewingClock {
    fun nowMillis(): Long
    fun ticker(periodMillis: Long): Flow<Long>
}

class BrewingClockImpl : BrewingClock {
    private val originMark = TimeSource.Monotonic.markNow()

    override fun nowMillis(): Long = originMark.elapsedNow().inWholeMilliseconds

    override fun ticker(periodMillis: Long): Flow<Long> = flow {
        emit(nowMillis())
        while (coroutineContext.isActive) {
            delay(periodMillis)
            emit(nowMillis())
        }
    }
}
