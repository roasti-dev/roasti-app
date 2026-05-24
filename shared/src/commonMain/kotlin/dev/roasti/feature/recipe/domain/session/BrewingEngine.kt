package dev.roasti.feature.recipe.domain.session

import dev.roasti.feature.recipe.domain.model.Recipe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BrewingEngine(
    private val scope: CoroutineScope,
    private val clock: BrewingClock,
    private val config: BrewingEngineConfig = BrewingEngineConfig(),
    initial: BrewingEngineState,
) {
    private val _state = MutableStateFlow(initial)
    val state: StateFlow<BrewingEngineState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<BrewingEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<BrewingEffect> = _effects.asSharedFlow()

    private var tickJob: Job? = null
    private var autoAdvanceJob: Job? = null

    init {
        startTickerIfNeeded()
    }

    fun next() {
        cancelAutoAdvance()
        val current = _state.value
        if (current.isFinished) return
        if (current.isLastStep) {
            finishInternal()
            return
        }
        moveToStep(targetIndex = current.currentStepIndex + 1)
    }

    fun previous() {
        cancelAutoAdvance()
        val current = _state.value
        if (current.isFinished || current.isFirstStep) return
        moveToStep(targetIndex = current.currentStepIndex - 1)
    }

    fun seekTo(index: Int) {
        cancelAutoAdvance()
        val current = _state.value
        if (current.isFinished) return
        val safeIndex = index.coerceIn(0, current.steps.lastIndex)
        if (safeIndex == current.currentStepIndex) return
        moveToStep(targetIndex = safeIndex)
    }

    fun pause() {
        cancelAutoAdvance()
        val nowMillis = clock.nowMillis()
        _state.update { it.copy(timer = it.timer.pause(nowMillis)) }
        stopTicker()
    }

    fun resume() {
        val nowMillis = clock.nowMillis()
        _state.update { it.copy(timer = it.timer.resume(nowMillis)) }
        startTickerIfNeeded()
    }

    fun setAutoAdvance(enabled: Boolean) {
        _state.update { it.copy(autoAdvance = enabled) }
        if (!enabled) cancelAutoAdvance()
    }

    fun cancelAutoAdvance() {
        autoAdvanceJob?.cancel()
        autoAdvanceJob = null
        if (_state.value.pendingAutoAdvance != null) {
            _state.update { it.copy(pendingAutoAdvance = null) }
            _effects.tryEmit(BrewingEffect.AutoAdvanceCancelled)
        }
    }

    fun toggleExpand(index: Int) {
        _state.update { current ->
            val newIndex = if (current.expandedStepIndex == index) null else index
            current.copy(expandedStepIndex = newIndex)
        }
    }

    fun dispose() {
        autoAdvanceJob?.cancel()
        autoAdvanceJob = null
        tickJob?.cancel()
        tickJob = null
    }

    private fun moveToStep(targetIndex: Int) {
        val current = _state.value
        val fromIndex = current.currentStepIndex
        val nowMillis = clock.nowMillis()
        stopTicker()
        val nextStep = current.steps[targetIndex]
        val shouldAutoStart = (nextStep.durationSeconds ?: 0) > 0
        val nextTimer = StepTimerState.forStep(
            durationSeconds = nextStep.durationSeconds,
            isRunning = shouldAutoStart,
            nowMillis = nowMillis,
        )
        _state.update {
            it.copy(
                currentStepIndex = targetIndex,
                expandedStepIndex = targetIndex,
                timer = nextTimer,
                isFinished = false,
            )
        }
        _effects.tryEmit(BrewingEffect.StepChanged(fromIndex = fromIndex, toIndex = targetIndex))
        startTickerIfNeeded()
    }

    private fun finishInternal() {
        stopTicker()
        cancelAutoAdvance()
        _state.update { it.copy(isFinished = true) }
        _effects.tryEmit(BrewingEffect.SessionFinished)
    }

    private fun startTickerIfNeeded() {
        if (tickJob?.isActive == true) return
        val current = _state.value
        if (!current.timer.isRunning || current.isFinished) return
        tickJob = scope.launch {
            clock.ticker(config.tickIntervalMillis).collect {
                onTick(clock.nowMillis())
            }
        }
    }

    private fun stopTicker() {
        tickJob?.cancel()
        tickJob = null
    }

    private fun onTick(nowMillis: Long) {
        val current = _state.value
        if (!current.timer.isRunning) return
        val advanced = current.timer.advance(nowMillis)
        if (advanced.remainingMillis <= 0L && current.timer.totalMillis > 0L) {
            stopTicker()
            val completed = advanced.complete()
            _state.update { it.copy(timer = completed) }
            _effects.tryEmit(BrewingEffect.StepCompleted(current.currentStepIndex))
            armAutoAdvanceIfNeeded()
        } else {
            _state.update { it.copy(timer = advanced) }
        }
    }

    private fun armAutoAdvanceIfNeeded() {
        val current = _state.value
        if (!current.autoAdvance) return
        if (current.isLastStep || current.isFinished) return
        val now = clock.nowMillis()
        val targetIndex = current.currentStepIndex + 1
        val countdown = AutoAdvanceCountdown(
            targetStepIndex = targetIndex,
            startedAtMillis = now,
            totalMillis = config.autoAdvanceDelayMillis,
        )
        _state.update { it.copy(pendingAutoAdvance = countdown) }
        _effects.tryEmit(
            BrewingEffect.AutoAdvanceArmed(nextIndex = targetIndex, delayMillis = config.autoAdvanceDelayMillis),
        )
        autoAdvanceJob = scope.launch {
            delay(config.autoAdvanceDelayMillis)
            val latest = _state.value
            if (latest.pendingAutoAdvance?.targetStepIndex == targetIndex && latest.autoAdvance) {
                _state.update { it.copy(pendingAutoAdvance = null) }
                _effects.tryEmit(BrewingEffect.AutoAdvanceFired(toIndex = targetIndex))
                moveToStep(targetIndex)
            }
        }
    }

    companion object {
        fun fromRecipe(
            recipe: Recipe,
            startStep: Int,
            autoAdvance: Boolean,
            scope: CoroutineScope,
            clock: BrewingClock,
            config: BrewingEngineConfig = BrewingEngineConfig(),
        ): BrewingEngine {
            require(recipe.steps.isNotEmpty()) { "Recipe has no brewing steps" }
            val safeStart = startStep.coerceIn(0, recipe.steps.lastIndex)
            val firstStep = recipe.steps[safeStart]
            val shouldAutoStart = (firstStep.durationSeconds ?: 0) > 0
            val initialTimer = StepTimerState.forStep(
                durationSeconds = firstStep.durationSeconds,
                isRunning = shouldAutoStart,
                nowMillis = clock.nowMillis(),
            )
            val initial = BrewingEngineState(
                recipeId = recipe.id,
                recipeTitle = recipe.title,
                steps = recipe.steps,
                currentStepIndex = safeStart,
                expandedStepIndex = safeStart,
                isFinished = false,
                timer = initialTimer,
                autoAdvance = autoAdvance,
                pendingAutoAdvance = null,
            )
            return BrewingEngine(
                scope = scope,
                clock = clock,
                config = config,
                initial = initial,
            )
        }
    }
}
