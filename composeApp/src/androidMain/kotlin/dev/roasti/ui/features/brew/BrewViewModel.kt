package dev.roasti.ui.features.brew

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.roasti.core.datetime.WallClock
import dev.roasti.feature.brew.domain.BrewRepository
import dev.roasti.feature.brew.domain.model.Brew
import dev.roasti.feature.brew.domain.model.BrewStatus
import dev.roasti.feature.brew.domain.model.isBackgroundStepReady
import dev.roasti.feature.preferences.domain.BrewingPreferencesRepository
import dev.roasti.feature.recipe.domain.model.BrewStep
import dev.roasti.feature.recipe.domain.session.BrewingClock
import dev.roasti.feature.recipe.domain.session.BrewingEffect
import dev.roasti.feature.recipe.domain.session.BrewingEngine
import dev.roasti.feature.recipe.domain.session.BrewingEngineState
import dev.roasti.ui.features.recipesteps.mapper.toUiState
import dev.roasti.ui.uikit.state.ContentUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
internal class BrewViewModel(
    private val brewId: String,
    // true только из deep-link уведомления: при готовности шага сразу продвинуть на следующий
    private val autoResume: Boolean,
    private val brewRepository: BrewRepository,
    private val preferences: BrewingPreferencesRepository,
    private val clock: BrewingClock,        // monotonic — foreground-таймер шага
    private val wallClock: WallClock,       // absolute — обратный отсчёт ожидания
) : ViewModel() {

    private val engineFlow = MutableStateFlow<BrewingEngine?>(null)

    // отслеживаем предыдущий статус, чтобы пересоздавать движок только при входе в BREWING (старт/резюм)
    private var sessionStatus: BrewStatus? = null

    private val _navEvents = MutableSharedFlow<BrewNavEvent>(extraBufferCapacity = 1)
    val navEvents: SharedFlow<BrewNavEvent> = _navEvents.asSharedFlow()

    private val brewFlow: StateFlow<Brew?> = brewRepository.observeById(brewId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val engineStateFlow: Flow<BrewingEngineState?> = engineFlow
        .flatMapLatest { engine -> engine?.state ?: flowOf(null) }

    val state: StateFlow<ContentUiState<BrewUiState>> =
        combine(brewFlow, engineStateFlow) { brew, engineState ->
            toContentState(brew, engineState)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, ContentUiState.Loading)

    val brewEffects: SharedFlow<BrewingEffect> = engineFlow
        .filterNotNull()
        .flatMapLatest { it.effects }
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 0)

    init {
        maybeAutoResume()
        observeBrewForEngineLifecycle()
        persistAdvances()
        observeAutoAdvancePreference()
    }

    /**
     * Deep-link из уведомления о готовности: если шаг действительно готов по wall-clock
     * (источник правды, не факт доставки нотификации) — продвинуть на следующий шаг, чтобы
     * пользователь попал сразу на него, а не на waiting-экран того же шага. Однократно.
     */
    private fun maybeAutoResume() {
        if (!autoResume) return
        viewModelScope.launch {
            val brew = brewFlow.filterNotNull().first()
            if (brew.status == BrewStatus.WAITING && brew.isBackgroundStepReady(wallClock.nowMillis())) {
                brewRepository.resumeFromWait(brewId)
            }
        }
    }

    // ---- actions: step controls (делегируются движку) ----

    fun nextStep() = run { engineFlow.value?.next(); Unit }
    fun previousStep() = run { engineFlow.value?.previous(); Unit }
    fun pauseTimer() = run { engineFlow.value?.pause(); Unit }
    fun resumeTimer() = run { engineFlow.value?.resume(); Unit }
    fun toggleExpand(index: Int) = run { engineFlow.value?.toggleExpand(index); Unit }
    fun cancelAutoAdvance() = run { engineFlow.value?.cancelAutoAdvance(); Unit }

    fun onAutoAdvanceToggle(enabled: Boolean) {
        viewModelScope.launch { preferences.setAutoAdvance(enabled) }
    }

    // ---- actions: brew lifecycle ----

    /** Отпустить текущий длинный шаг в фон на выбранную длительность. */
    fun backgroundCurrentStep(durationSeconds: Int) {
        val engine = engineFlow.value ?: return
        val stepIndex = engine.state.value.currentStepIndex
        viewModelScope.launch {
            brewRepository.backgroundStep(brewId, stepIndex, durationSeconds * 1000L)
        }
    }

    /** Выйти из ожидания (по готовности или досрочно) — к следующему шагу. */
    fun resumeWait() {
        viewModelScope.launch { brewRepository.resumeFromWait(brewId) }
    }

    /** Финал заваривания + опциональная заметка. NavigateBack — по эмиссии COMPLETED. */
    fun finish(note: String?) {
        viewModelScope.launch {
            brewRepository.finishBrew(brewId, note?.takeIf { it.isNotBlank() })
        }
    }

    fun cancelBrew() {
        viewModelScope.launch { brewRepository.cancelBrew(brewId) }
    }

    override fun onCleared() {
        engineFlow.value?.dispose()
        engineFlow.value = null
        super.onCleared()
    }

    // ---- internals ----

    private fun toContentState(
        brew: Brew?,
        engineState: BrewingEngineState?,
    ): ContentUiState<BrewUiState> {
        if (brew == null) return ContentUiState.Loading
        return when (brew.status) {
            BrewStatus.WAITING -> {
                val stepIndex = brew.backgroundStepIndex ?: brew.currentStepIndex
                ContentUiState.Content(
                    BrewUiState.Waiting(
                        recipeTitle = brew.recipeTitle,
                        stepTitle = brew.steps.getOrNull(stepIndex)?.title.orEmpty(),
                        waitUntil = brew.waitUntil ?: wallClock.nowMillis(),
                    ),
                )
            }

            BrewStatus.BREWING -> {
                if (engineState == null) return ContentUiState.Loading
                val durationSeconds =
                    engineState.steps.getOrNull(engineState.currentStepIndex)?.durationSeconds ?: 0
                ContentUiState.Content(
                    BrewUiState.Brewing(
                        session = engineState.toUiState(),
                        canBackgroundCurrentStep = durationSeconds > BACKGROUND_THRESHOLD_SECONDS &&
                            !engineState.isFinished,
                        currentStepDurationSeconds = durationSeconds,
                    ),
                )
            }

            BrewStatus.COMPLETED, BrewStatus.CANCELLED -> ContentUiState.Loading
        }
    }

    private fun observeBrewForEngineLifecycle() {
        viewModelScope.launch {
            brewFlow.collect { brew ->
                if (brew == null) return@collect
                when (brew.status) {
                    BrewStatus.BREWING ->
                        if (sessionStatus != BrewStatus.BREWING) recreateEngine(brew)

                    BrewStatus.WAITING -> disposeEngine()

                    BrewStatus.COMPLETED, BrewStatus.CANCELLED -> {
                        disposeEngine()
                        _navEvents.tryEmit(BrewNavEvent.NavigateBack)
                    }
                }
                sessionStatus = brew.status
            }
        }
    }

    private fun recreateEngine(brew: Brew) {
        engineFlow.value?.dispose()
        val steps = brew.steps.map { snapshot ->
            BrewStep(
                order = snapshot.order,
                title = snapshot.title,
                durationSeconds = snapshot.durationSeconds,
                imageId = snapshot.imageId,
            )
        }
        engineFlow.value = BrewingEngine.fromSteps(
            steps = steps,
            recipeId = brew.recipeId,
            recipeTitle = brew.recipeTitle,
            startStep = brew.currentStepIndex,
            autoAdvance = preferences.preferences.value.autoAdvance,
            scope = viewModelScope,
            clock = clock,
        )
    }

    private fun disposeEngine() {
        engineFlow.value?.dispose()
        engineFlow.value = null
    }

    /** Персистим позицию в Brew при каждом переходе движка по шагам. */
    private fun persistAdvances() {
        viewModelScope.launch {
            engineFlow
                .filterNotNull()
                .flatMapLatest { it.effects }
                .collect { effect ->
                    if (effect is BrewingEffect.StepChanged) {
                        brewRepository.advanceToStep(brewId, effect.toIndex)
                    }
                }
        }
    }

    private fun observeAutoAdvancePreference() {
        viewModelScope.launch {
            preferences.preferences
                .map { it.autoAdvance }
                .distinctUntilChanged()
                .collect { autoAdvance -> engineFlow.value?.setAutoAdvance(autoAdvance) }
        }
    }

    private companion object {
        const val BACKGROUND_THRESHOLD_SECONDS = 300   // длинный шаг = ожидание > 5 мин
    }
}
