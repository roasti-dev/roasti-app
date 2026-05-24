package dev.roasti.ui.features.recipesteps

internal data class SessionUiState(
    val recipeTitle: String,
    val rows: List<BrewingStepRowUiState>,
    val currentStepIndex: Int,
    val totalSteps: Int,
    val isFirstStep: Boolean,
    val isLastStep: Boolean,
    val isFinished: Boolean,
    val hasTimer: Boolean,
    val timer: TimerUiState?,
    val autoAdvance: Boolean,
    val autoAdvanceCountdown: AutoAdvanceCountdownUiState?,
)

internal enum class StepRowKind { Done, Active, Upcoming }

internal data class BrewingStepRowUiState(
    val index: Int,
    val displayNumber: Int,
    val title: String,
    val durationLabel: String?,
    val kind: StepRowKind,
    val isExpanded: Boolean,
)

internal data class TimerUiState(
    val remainingLabel: String,
    val progress: Float,
    val isRunning: Boolean,
    val isCompleted: Boolean,
)

internal data class AutoAdvanceCountdownUiState(
    val targetStepIndex: Int,
    val totalMillis: Long,
)
