package dev.roasti.ui.features.recipesteps.mapper

import dev.roasti.feature.recipe.domain.session.BrewingEngineState
import dev.roasti.ui.features.recipesteps.AutoAdvanceCountdownUiState
import dev.roasti.ui.features.recipesteps.BrewingStepRowUiState
import dev.roasti.ui.features.recipesteps.SessionUiState
import dev.roasti.ui.features.recipesteps.StepRowKind
import dev.roasti.ui.features.recipesteps.TimerUiState

internal fun BrewingEngineState.toUiState(): SessionUiState {
    val rows = steps.mapIndexed { index, step ->
        val kind = when {
            isFinished -> StepRowKind.Done
            index < currentStepIndex -> StepRowKind.Done
            index == currentStepIndex -> StepRowKind.Active
            else -> StepRowKind.Upcoming
        }
        BrewingStepRowUiState(
            index = index,
            displayNumber = index + 1,
            title = step.title,
            durationLabel = step.durationSeconds?.let(::formatDuration),
            kind = kind,
            isExpanded = expandedStepIndex == index,
        )
    }

    val timerUi = if (hasTimer && !isFinished) {
        TimerUiState(
            remainingLabel = formatDuration(timer.remainingSeconds),
            progress = timer.progress,
            isRunning = timer.isRunning,
            isCompleted = timer.isCompleted,
        )
    } else null

    return SessionUiState(
        recipeTitle = recipeTitle,
        rows = rows,
        currentStepIndex = currentStepIndex,
        totalSteps = totalSteps,
        isFirstStep = isFirstStep,
        isLastStep = isLastStep,
        isFinished = isFinished,
        hasTimer = hasTimer,
        timer = timerUi,
        autoAdvance = autoAdvance,
        autoAdvanceCountdown = pendingAutoAdvance?.let {
            AutoAdvanceCountdownUiState(
                targetStepIndex = it.targetStepIndex,
                totalMillis = it.totalMillis,
            )
        },
    )
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
