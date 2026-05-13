package dev.roasti.ui.features.recipesteps

import kotlin.math.ceil

internal data class StepTimerState(
    val totalMillis: Long,
    val remainingMillis: Long,
    val isRunning: Boolean,
    val startedAtMillis: Long? = null,
) {
    val progress: Float
        get() = if (totalMillis > 0) remainingMillis / totalMillis.toFloat() else 1f

    fun advance(nowMillis: Long): StepTimerState {
        if (!isRunning || startedAtMillis == null) return this

        val updatedRemaining = (remainingMillis - (nowMillis - startedAtMillis)).coerceAtLeast(0L)
        return copy(
            remainingMillis = updatedRemaining,
            startedAtMillis = nowMillis,
        )
    }

    fun pause(nowMillis: Long): StepTimerState {
        val updated = advance(nowMillis)
        return updated.copy(
            isRunning = false,
            startedAtMillis = null,
        )
    }

    fun resume(nowMillis: Long): StepTimerState {
        if (isRunning || remainingMillis <= 0L) return this
        return copy(
            isRunning = true,
            startedAtMillis = nowMillis,
        )
    }

    fun complete(): StepTimerState = copy(
        remainingMillis = 0L,
        isRunning = false,
        startedAtMillis = null,
    )

    companion object {
        fun forStep(durationSeconds: Int?, isRunning: Boolean, nowMillis: Long): StepTimerState {
            val totalMillis = (durationSeconds ?: 0) * MILLIS_IN_SECOND
            val shouldRun = isRunning && totalMillis > 0L
            return StepTimerState(
                totalMillis = totalMillis,
                remainingMillis = totalMillis,
                isRunning = shouldRun,
                startedAtMillis = nowMillis.takeIf { shouldRun },
            )
        }

        private const val MILLIS_IN_SECOND = 1000L
    }
}

internal data class BrewingStepUiModel(
    val order: Int,
    val title: String,
    val description: String,
    val durationSeconds: Int?,
)

internal data class SessionState(
    val steps: List<BrewingStepUiModel>,
    val currentStepIndex: Int,
    val totalSteps: Int,
    val isFirstStep: Boolean,
    val isLastStep: Boolean,
    val isFinished: Boolean,
    val stepProgress: Float,
    val timer: StepTimerState,
) {
    val currentStep: BrewingStepUiModel get() = steps[currentStepIndex]
    val hasTimer: Boolean get() = (currentStep.durationSeconds ?: 0) > 0
    val isTimerRunning: Boolean get() = timer.isRunning
    val remainingSeconds: Int get() = ceil(timer.remainingMillis / 1000f).toInt()
    val timerProgress: Float get() = timer.progress
}
