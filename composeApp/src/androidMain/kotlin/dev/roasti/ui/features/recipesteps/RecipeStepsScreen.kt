package dev.roasti.ui.features.recipesteps

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.roasti.R
import dev.roasti.feature.recipe.domain.session.BrewingEffect
import dev.roasti.ui.features.recipesteps.components.AutoAdvanceToggle
import dev.roasti.ui.features.recipesteps.components.BrewingActiveCard
import dev.roasti.ui.features.recipesteps.components.BrewingCompletionContent
import dev.roasti.ui.features.recipesteps.components.BrewingControlsDock
import dev.roasti.ui.features.recipesteps.components.CollapsedStepContent
import dev.roasti.ui.features.recipesteps.components.StepIndicatorBadge
import dev.roasti.ui.theme.Spacing
import dev.roasti.ui.uikit.state.ContentScaffold
import dev.roasti.ui.uikit.timeline.TimelineColumn
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val NoOpClick: () -> Unit = {}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun RecipeStepsRoute(
    id: String,
    startStep: Int = 0,
    onBackClick: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") sharedTransitionScope: SharedTransitionScope? = null,
    @Suppress("UNUSED_PARAMETER") animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val viewModel: RecipeStepsViewModel = koinViewModel { parametersOf(id, startStep) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val view = LocalView.current

    LaunchedEffect(Unit) {
        viewModel.navEvents.collectLatest { event ->
            when (event) {
                RecipeStepsNavEvent.NavigateBack -> onBackClick()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.brewEffects.collect { effect ->
            val constant = when (effect) {
                is BrewingEffect.StepCompleted -> HapticFeedbackConstants.LONG_PRESS
                is BrewingEffect.AutoAdvanceArmed -> HapticFeedbackConstants.CLOCK_TICK
                is BrewingEffect.AutoAdvanceFired -> HapticFeedbackConstants.VIRTUAL_KEY
                BrewingEffect.SessionFinished -> HapticFeedbackConstants.LONG_PRESS
                is BrewingEffect.StepChanged,
                BrewingEffect.AutoAdvanceCancelled -> null
            }
            if (constant != null) {
                view.performHapticFeedback(constant)
            }
        }
    }

    ContentScaffold(
        state = state,
        onRetry = viewModel::retry,
        events = viewModel.events,
    ) { session ->
        RecipeStepsScreen(
            session = session,
            onBackClick = onBackClick,
            onPreviousStep = viewModel::previousStep,
            onNextStep = viewModel::nextStep,
            onPauseTimer = viewModel::pauseTimer,
            onResumeTimer = viewModel::resumeTimer,
            onToggleExpand = viewModel::toggleExpand,
            onCancelAutoAdvance = viewModel::cancelAutoAdvance,
            onAutoAdvanceToggle = viewModel::onAutoAdvanceToggle,
            onFinish = viewModel::finish,
        )
    }
}

@Composable
private fun RecipeStepsScreen(
    session: SessionUiState,
    onBackClick: () -> Unit,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onToggleExpand: (Int) -> Unit,
    onCancelAutoAdvance: () -> Unit,
    onAutoAdvanceToggle: (Boolean) -> Unit,
    onFinish: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        AnimatedContent(
            targetState = session.isFinished,
            transitionSpec = {
                fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.92f) togetherWith
                        fadeOut(tween(200))
            },
            label = "completion_swap",
        ) { finished ->
            if (finished) {
                BrewingCompletionContent(
                    onFinish = onFinish,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                ActiveSessionContent(
                    session = session,
                    onBackClick = onBackClick,
                    onPreviousStep = onPreviousStep,
                    onNextStep = onNextStep,
                    onPauseTimer = onPauseTimer,
                    onResumeTimer = onResumeTimer,
                    onToggleExpand = onToggleExpand,
                    onCancelAutoAdvance = onCancelAutoAdvance,
                    onAutoAdvanceToggle = onAutoAdvanceToggle,
                )
            }
        }
    }
}

@Composable
private fun ActiveSessionContent(
    session: SessionUiState,
    onBackClick: () -> Unit,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onToggleExpand: (Int) -> Unit,
    onCancelAutoAdvance: () -> Unit,
    onAutoAdvanceToggle: (Boolean) -> Unit,
) {
    val expandedIndex = session.rows.firstOrNull { it.isExpanded }?.index

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            RecipeStepsTopBar(
                recipeTitle = session.recipeTitle,
                currentStepDisplay = session.currentStepIndex + 1,
                totalSteps = session.totalSteps,
                autoAdvance = session.autoAdvance,
                onBackClick = onBackClick,
                onAutoAdvanceToggle = onAutoAdvanceToggle,
            )
        },
        bottomBar = {
            BottomBar(
                session = session,
                onPreviousStep = onPreviousStep,
                onNextStep = onNextStep,
                onPauseTimer = onPauseTimer,
                onResumeTimer = onResumeTimer,
            )
        },
    ) { innerPadding ->
        TimelineColumn(
            activeIndex = session.currentStepIndex,
            expandedIndex = expandedIndex,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(
                start = Spacing.lg,
                end = Spacing.lg,
                bottom = innerPadding.calculateBottomPadding() + Spacing.xl,
                top = Spacing.sm,
            ),
            badge = { _, index, size ->
                val row = session.rows[index]
                StepIndicatorBadge(
                    kind = row.kind,
                    number = row.displayNumber,
                    size = size,
                )
            },
        ) {
            items(
                items = session.rows,
                key = { it.index },
                collapsed = { row ->
                    val onClick: () -> Unit =
                        if (row.kind == StepRowKind.Active) {
                            { onToggleExpand(row.index) }
                        } else NoOpClick
                    CollapsedStepContent(
                        title = row.title,
                        durationLabel = activeRowRemainingOrDuration(row, session),
                        kind = row.kind,
                        onClick = onClick,
                    )
                },
                expanded = { row ->
                    BrewingActiveCard(
                        title = row.title,
                        timer = session.timer,
                        autoAdvanceCountdown = session.autoAdvanceCountdown,
                        onClick = { onToggleExpand(row.index) },
                        onCancelAutoAdvance = onCancelAutoAdvance,
                    )
                },
            )
        }
    }
}

@Composable
private fun BottomBar(
    session: SessionUiState,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        BrewingControlsDock(
            isFirstStep = session.isFirstStep,
            isLastStep = session.isLastStep,
            hasTimer = session.hasTimer,
            isTimerRunning = session.timer?.isRunning == true,
            onPrevious = onPreviousStep,
            onPauseResume = {
                val isRunning = session.timer?.isRunning == true
                if (isRunning) onPauseTimer() else onResumeTimer()
            },
            onNext = onNextStep,
        )
    }
}

@Composable
private fun RecipeStepsTopBar(
    recipeTitle: String,
    currentStepDisplay: Int,
    totalSteps: Int,
    autoAdvance: Boolean,
    onBackClick: () -> Unit,
    onAutoAdvanceToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = stringResource(R.string.back_label),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Spacing.sm),
        ) {
            Text(
                text = recipeTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.steps_step_counter, currentStepDisplay, totalSteps),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AutoAdvanceToggle(
            autoAdvance = autoAdvance,
            onToggle = onAutoAdvanceToggle,
            modifier = Modifier.padding(end = Spacing.sm),
        )
    }
}

private fun activeRowRemainingOrDuration(
    row: BrewingStepRowUiState,
    session: SessionUiState,
): String? {
    if (row.kind != StepRowKind.Active) return row.durationLabel
    if (session.timer == null) return null
    return session.timer.remainingLabel
}
