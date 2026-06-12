package dev.roasti.ui.features.brew.carousel

data class ActiveBrewCardUiModel(
    val brewId: String,
    val title: String,
    val imageUrl: String?,
    val progress: ActiveBrewProgress,
)

sealed interface ActiveBrewProgress {
    /** Фоновый шаг: обратный отсчёт по wall-clock (тикает в карточке). */
    data class Waiting(val waitUntil: Long) : ActiveBrewProgress

    /** Активное прохождение шагов. */
    data class Brewing(val currentStep: Int, val totalSteps: Int) : ActiveBrewProgress
}
