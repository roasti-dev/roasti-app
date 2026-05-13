package dev.roasti.ui.features.recipesteps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dev.roasti.feature.recipe.domain.RecipeRepository
import dev.roasti.feature.recipe.domain.model.Recipe
import dev.roasti.feature.recipe.domain.session.BrewingSession
import dev.roasti.feature.recipe.domain.session.BrewingTimer
import dev.roasti.ui.features.recipesteps.mapper.toUiState
import dev.roasti.ui.uikit.state.ContentUiState
import dev.roasti.ui.uikit.state.UiError
import dev.roasti.ui.uikit.state.UiEvent

internal class RecipeStepsViewModel(
    private val recipeId: String,
    private val startStepIndex: Int,
    private val repository: RecipeRepository,
    private val timer: BrewingTimer,
) : ViewModel() {

    private val _state = MutableStateFlow<ContentUiState<SessionState>>(ContentUiState.Loading)
    val state: StateFlow<ContentUiState<SessionState>> = _state.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private val _navEvents = MutableSharedFlow<RecipeStepsNavEvent>(extraBufferCapacity = 1)
    val navEvents: SharedFlow<RecipeStepsNavEvent> = _navEvents.asSharedFlow()

    private var timerJob: Job? = null
    private var currentBrewingSession: BrewingSession? = null
    private var currentTimerState: StepTimerState? = null

    init {
        retry()
    }

    fun retry() {
        _state.value = ContentUiState.Loading
        viewModelScope.launch {
            repository.getById(recipeId)
                .onSuccess { startSession(it) }
                .onFailure { _state.value = ContentUiState.FullscreenError(UiError.Generic) }
        }
    }

    fun nextStep() {
        val brew = currentBrewingSession ?: return
        val newBrew = brew.nextStep()
        moveToSession(
            newBrew = newBrew,
            shouldAutoStart = shouldAutoStartTimerFor(newBrew),
        )
    }

    fun previousStep() {
        val brew = currentBrewingSession ?: return
        val newBrew = brew.previousStep()
        moveToSession(
            newBrew = newBrew,
            shouldAutoStart = shouldAutoStartTimerFor(newBrew),
        )
    }

    fun pauseTimer() {
        val nowMillis = timer.nowMillis()
        updateTimer { it.pause(nowMillis) }
        stopTicker()
    }

    fun resumeTimer() {
        val nowMillis = timer.nowMillis()
        updateTimer { it.resume(nowMillis) }
        startTicker()
    }

    fun finish() {
        _navEvents.tryEmit(RecipeStepsNavEvent.NavigateBack)
    }

    private fun startSession(recipe: Recipe) {
        val brew = BrewingSession(recipe, currentStepIndex = if (recipe.steps.lastIndex > 0) startStepIndex.coerceIn(0, recipe.steps.lastIndex) else 0)
        startSession(brew)
    }

    private fun startSession(brew: BrewingSession) {
        val nowMillis = timer.nowMillis()
        currentBrewingSession = brew
        currentTimerState = StepTimerState.forStep(
            durationSeconds = brew.stepDurationSeconds,
            isRunning = shouldAutoStartTimerFor(brew),
            nowMillis = nowMillis,
        )
        emitContent()
        startTicker()
    }

    private fun startTicker() {
        stopTicker()
        val session = currentSession() ?: return
        if (!session.isTimerRunning || session.isFinished) return

        timerJob = viewModelScope.launch {
            timer.ticker(TICK_INTERVAL_MILLIS).collect { nowMillis ->
                val current = currentSession() ?: return@collect
                if (!current.isTimerRunning) return@collect

                val updatedTimer = currentTimerState?.advance(nowMillis) ?: return@collect
                if (updatedTimer.remainingMillis <= 0L) {
                    stopTicker()
                    currentTimerState = updatedTimer.complete()
                    emitContent()
                } else {
                    currentTimerState = updatedTimer
                    emitContent()
                }
            }
        }
    }

    private fun stopTicker() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun moveToSession(
        newBrew: BrewingSession,
        shouldAutoStart: Boolean,
    ) {
        stopTicker()

        val nowMillis = timer.nowMillis()
        currentBrewingSession = newBrew
        currentTimerState = StepTimerState.forStep(
            durationSeconds = newBrew.stepDurationSeconds,
            isRunning = shouldAutoStart && !newBrew.isFinished,
            nowMillis = nowMillis,
        )
        emitContent()

        if (!newBrew.isFinished && shouldAutoStart) {
            startTicker()
        }
    }

    private fun shouldAutoStartTimerFor(brew: BrewingSession): Boolean {
        val durationSeconds = brew.stepDurationSeconds ?: return false
        return !brew.isFinished && durationSeconds > 0
    }

    private fun currentSession(): SessionState? =
        (_state.value as? ContentUiState.Content<SessionState>)?.data

    private fun updateTimer(update: (StepTimerState) -> StepTimerState) {
        val timerState = currentTimerState ?: return
        currentTimerState = update(timerState)
        emitContent()
    }

    private fun emitContent() {
        val brew = currentBrewingSession ?: return
        val timerState = currentTimerState ?: return
        _state.value = ContentUiState.Content(
            data = brew.toUiState(timer = timerState),
        )
    }

    override fun onCleared() {
        stopTicker()
        super.onCleared()
    }

    private companion object {
        const val TICK_INTERVAL_MILLIS = 50L
    }
}

internal sealed interface RecipeStepsNavEvent {
    data object NavigateBack : RecipeStepsNavEvent
}
