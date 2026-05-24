package dev.roasti.feature.recipe.domain.session

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal class FakeBrewingClock(
    private val timeProvider: () -> Long,
) : BrewingClock {
    private val tickFlow = MutableSharedFlow<Long>(extraBufferCapacity = 64)

    override fun nowMillis(): Long = timeProvider()

    override fun ticker(periodMillis: Long): Flow<Long> = tickFlow.asSharedFlow()

    fun emitTick() {
        tickFlow.tryEmit(timeProvider())
    }
}
