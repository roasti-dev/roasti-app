package dev.roasti.ui.features.recipesteps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.roasti.feature.preferences.domain.BrewingPreferencesRepository
import dev.roasti.feature.recipe.domain.RecipeRepository
import dev.roasti.feature.recipe.domain.session.BrewingClock
import dev.roasti.feature.recipe.domain.session.BrewingEffect
import dev.roasti.feature.recipe.domain.session.BrewingEngine
import dev.roasti.ui.features.recipesteps.mapper.toUiState
import dev.roasti.ui.uikit.state.ContentUiState
import dev.roasti.ui.uikit.state.UiError
import dev.roasti.ui.uikit.state.UiEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
internal class RecipeStepsViewModel(
    private val recipeId: String,
    private val startStep: Int,
    private val repository: RecipeRepository,
    private val preferences: BrewingPreferencesRepository,
    private val clock: BrewingClock,
) : ViewModel() {

    private val engineFlow = MutableStateFlow<BrewingEngine?>(null)
    private val loadError = MutableStateFlow(false)

    val state: StateFlow<ContentUiState<SessionUiState>> = engineFlow
        .flatMapLatest { engine ->
            when {
                loadError.value -> flowOf(ContentUiState.FullscreenError(UiError.Generic))
                engine == null -> flowOf(ContentUiState.Loading)
                else -> engine.state.map { ContentUiState.Content(it.toUiState()) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ContentUiState.Loading)

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private val _navEvents = MutableSharedFlow<RecipeStepsNavEvent>(extraBufferCapacity = 1)
    val navEvents: SharedFlow<RecipeStepsNavEvent> = _navEvents.asSharedFlow()

    val brewEffects: SharedFlow<BrewingEffect> = engineFlow
        .filterNotNull()
        .flatMapLatest { it.effects }
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 0)

    init {
        retry()
        observeAutoAdvancePreference()
    }

    fun retry() {
        loadError.value = false
        engineFlow.value = null
        viewModelScope.launch {
            repository.getById(recipeId)
                .onSuccess { recipe ->
                    engineFlow.value?.dispose()
                    engineFlow.value = BrewingEngine.fromRecipe(
                        recipe = recipe,
                        startStep = startStep,
                        autoAdvance = preferences.preferences.value.autoAdvance,
                        scope = viewModelScope,
                        clock = clock,
                    )
                }
                .onFailure { loadError.value = true }
        }
    }

    fun nextStep() {
        engineFlow.value?.next()
    }

    fun previousStep() {
        engineFlow.value?.previous()
    }

    fun pauseTimer() {
        engineFlow.value?.pause()
    }

    fun resumeTimer() {
        engineFlow.value?.resume()
    }

    fun seekTo(index: Int) {
        engineFlow.value?.seekTo(index)
    }

    fun toggleExpand(index: Int) {
        engineFlow.value?.toggleExpand(index)
    }

    fun cancelAutoAdvance() {
        engineFlow.value?.cancelAutoAdvance()
    }

    fun onAutoAdvanceToggle(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setAutoAdvance(enabled)
        }
    }

    fun finish() {
        _navEvents.tryEmit(RecipeStepsNavEvent.NavigateBack)
    }

    override fun onCleared() {
        engineFlow.value?.dispose()
        super.onCleared()
    }

    private fun observeAutoAdvancePreference() {
        viewModelScope.launch {
            preferences.preferences
                .map { it.autoAdvance }
                .distinctUntilChanged()
                .collect { autoAdvance ->
                    engineFlow.value?.setAutoAdvance(autoAdvance)
                }
        }
    }
}

internal sealed interface RecipeStepsNavEvent {
    data object NavigateBack : RecipeStepsNavEvent
}
