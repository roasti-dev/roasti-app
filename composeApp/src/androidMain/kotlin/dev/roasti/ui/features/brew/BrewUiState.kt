package dev.roasti.ui.features.brew

import dev.roasti.ui.features.recipesteps.SessionUiState

/**
 * UI-состояние Brew-экрана. BREWING рендерится переиспользуемым session-рендером (как старый
 * RecipeSteps), WAITING — отдельным экраном ожидания с обратным отсчётом.
 */
internal sealed interface BrewUiState {

    data class Brewing(
        val session: SessionUiState,
        val canBackgroundCurrentStep: Boolean,   // длинный шаг (>5 мин) → показать «Уведомить когда готово»
        val currentStepDurationSeconds: Int,     // префилл picker'а времени фона
    ) : BrewUiState

    data class Waiting(
        val recipeTitle: String,
        val stepTitle: String,
        val waitUntil: Long,                     // epochMillis; готовность считается по wall-clock в UI
    ) : BrewUiState
}

internal sealed interface BrewNavEvent {
    data object NavigateBack : BrewNavEvent
}
