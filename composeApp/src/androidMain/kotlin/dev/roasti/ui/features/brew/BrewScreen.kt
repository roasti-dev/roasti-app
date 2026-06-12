package dev.roasti.ui.features.brew

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.roasti.R
import dev.roasti.feature.recipe.domain.session.BrewingEffect
import dev.roasti.ui.features.recipesteps.BrewingStepRowUiState
import dev.roasti.ui.features.recipesteps.SessionUiState
import dev.roasti.ui.features.recipesteps.StepRowKind
import dev.roasti.ui.features.recipesteps.components.AutoAdvanceToggle
import dev.roasti.ui.features.recipesteps.components.BrewingActiveCard
import dev.roasti.ui.features.recipesteps.components.BrewingCompletionContent
import dev.roasti.ui.features.recipesteps.components.BrewingControlsDock
import dev.roasti.ui.features.recipesteps.components.CollapsedStepContent
import dev.roasti.ui.features.recipesteps.components.StepIndicatorBadge
import dev.roasti.ui.theme.Spacing
import dev.roasti.ui.uikit.ActionButtonPrimary
import dev.roasti.ui.uikit.RoastiBottomSheet
import dev.roasti.ui.uikit.picker.DurationWheelPicker
import dev.roasti.ui.uikit.state.ContentUiState
import dev.roasti.ui.uikit.timeline.TimelineColumn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val NoOpClick: () -> Unit = {}

@Composable
internal fun BrewRoute(
    brewId: String,
    autoResume: Boolean = false,
    onBackClick: () -> Unit = {},
) {
    val viewModel: BrewViewModel = koinViewModel { parametersOf(brewId, autoResume) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val view = LocalView.current

    LaunchedEffect(Unit) {
        viewModel.navEvents.collectLatest { event ->
            when (event) {
                BrewNavEvent.NavigateBack -> onBackClick()
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
            if (constant != null) view.performHapticFeedback(constant)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        when (val s = state) {
            ContentUiState.Loading -> Unit // короткий мост между состояниями — фон surface
            is ContentUiState.FullscreenError -> Unit
            is ContentUiState.Content -> when (val ui = s.data) {
                is BrewUiState.Brewing -> BrewingScreen(
                    brewing = ui,
                    onBackClick = onBackClick,
                    onPreviousStep = viewModel::previousStep,
                    onNextStep = viewModel::nextStep,
                    onPauseTimer = viewModel::pauseTimer,
                    onResumeTimer = viewModel::resumeTimer,
                    onToggleExpand = viewModel::toggleExpand,
                    onCancelAutoAdvance = viewModel::cancelAutoAdvance,
                    onAutoAdvanceToggle = viewModel::onAutoAdvanceToggle,
                    onBackgroundStep = viewModel::backgroundCurrentStep,
                    onFinish = viewModel::finish,
                )

                is BrewUiState.Waiting -> BrewWaitingScreen(
                    waiting = ui,
                    onBackClick = onBackClick,
                    onContinue = viewModel::resumeWait,
                    onCancel = viewModel::cancelBrew,
                )
            }
        }
    }
}

@Composable
private fun BrewingScreen(
    brewing: BrewUiState.Brewing,
    onBackClick: () -> Unit,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onToggleExpand: (Int) -> Unit,
    onCancelAutoAdvance: () -> Unit,
    onAutoAdvanceToggle: (Boolean) -> Unit,
    onBackgroundStep: (durationSeconds: Int) -> Unit,
    onFinish: (note: String?) -> Unit,
) {
    var showBackgroundSheet by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }

    if (showBackgroundSheet) {
        BackgroundDurationSheet(
            initialSeconds = brewing.currentStepDurationSeconds,
            onConfirm = { seconds ->
                showBackgroundSheet = false
                onBackgroundStep(seconds)
            },
            onDismiss = { showBackgroundSheet = false },
        )
    }

    AnimatedContent(
        targetState = brewing.session.isFinished,
        transitionSpec = {
            fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.92f) togetherWith fadeOut(tween(200))
        },
        label = "brew_completion_swap",
    ) { finished ->
        if (finished) {
            BrewingCompletionContent(
                onFinish = { onFinish(note) },
                modifier = Modifier.fillMaxSize(),
                note = note,
                onNoteChange = { note = it },
            )
        } else {
            ActiveSessionContent(
                session = brewing.session,
                canBackgroundCurrentStep = brewing.canBackgroundCurrentStep,
                onBackClick = onBackClick,
                onPreviousStep = onPreviousStep,
                onNextStep = onNextStep,
                onPauseTimer = onPauseTimer,
                onResumeTimer = onResumeTimer,
                onToggleExpand = onToggleExpand,
                onCancelAutoAdvance = onCancelAutoAdvance,
                onAutoAdvanceToggle = onAutoAdvanceToggle,
                onBackgroundStepClick = { showBackgroundSheet = true },
            )
        }
    }
}

@Composable
private fun ActiveSessionContent(
    session: SessionUiState,
    canBackgroundCurrentStep: Boolean,
    onBackClick: () -> Unit,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onToggleExpand: (Int) -> Unit,
    onCancelAutoAdvance: () -> Unit,
    onAutoAdvanceToggle: (Boolean) -> Unit,
    onBackgroundStepClick: () -> Unit,
) {
    val expandedIndex = session.rows.firstOrNull { it.isExpanded }?.index

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            BrewTopBar(
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
                canBackgroundCurrentStep = canBackgroundCurrentStep,
                onPreviousStep = onPreviousStep,
                onNextStep = onNextStep,
                onPauseTimer = onPauseTimer,
                onResumeTimer = onResumeTimer,
                onBackgroundStepClick = onBackgroundStepClick,
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
                StepIndicatorBadge(kind = row.kind, number = row.displayNumber, size = size)
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
    canBackgroundCurrentStep: Boolean,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onBackgroundStepClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (canBackgroundCurrentStep) {
            ActionButtonPrimary(
                onClick = onBackgroundStepClick,
                text = stringResource(R.string.brew_notify_when_done),
                modifier = Modifier.fillMaxWidth(),
            )
        }
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
private fun BrewTopBar(
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

@Composable
private fun BrewWaitingScreen(
    waiting: BrewUiState.Waiting,
    onBackClick: () -> Unit,
    onContinue: () -> Unit,
    onCancel: () -> Unit,
) {
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }
    val remainingSeconds = ((waiting.waitUntil - now) / 1000L).coerceAtLeast(0L)
    val isReady = remainingSeconds <= 0L
    val remainingLabel = formatRemaining(
        totalSeconds = remainingSeconds,
        days = stringResource(R.string.brew_duration_days),
        hours = stringResource(R.string.brew_duration_hours),
        minutes = stringResource(R.string.brew_duration_minutes),
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_left),
                        contentDescription = stringResource(R.string.back_label),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = waiting.recipeTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = Spacing.sm),
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (isReady) {
                    stringResource(R.string.brew_waiting_ready)
                } else {
                    stringResource(R.string.brew_waiting_steeping)
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = waiting.stepTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.sm),
            )
            if (!isReady) {
                Text(
                    text = stringResource(R.string.brew_waiting_remaining, remainingLabel),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = Spacing.lg),
                )
            }
            ActionButtonPrimary(
                onClick = onContinue,
                text = if (isReady) {
                    stringResource(R.string.brew_continue)
                } else {
                    stringResource(R.string.brew_finish_wait_early)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xl),
            )
            TextButton(
                onClick = onCancel,
                modifier = Modifier.padding(top = Spacing.sm),
            ) {
                Text(text = stringResource(R.string.brew_cancel))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackgroundDurationSheet(
    initialSeconds: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var seconds by remember { mutableIntStateOf(initialSeconds.coerceAtLeast(0)) }

    RoastiBottomSheet(
        onDismiss = onDismiss,
        title = stringResource(R.string.brew_background_sheet_title),
    ) {
        DurationWheelPicker(
            totalSeconds = seconds,
            onTotalSecondsChange = { seconds = it },
            modifier = Modifier.padding(horizontal = Spacing.lg),
        )
        ActionButtonPrimary(
            onClick = { onConfirm(seconds) },
            text = stringResource(R.string.brew_background_confirm),
            enabled = seconds > 0,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
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

private fun formatRemaining(
    totalSeconds: Long,
    days: String,
    hours: String,
    minutes: String,
): String {
    val d = totalSeconds / 86_400
    val h = (totalSeconds % 86_400) / 3_600
    val m = (totalSeconds % 3_600) / 60
    return when {
        d > 0 -> "$d$days $h$hours"
        h > 0 -> "$h$hours $m$minutes"
        else -> "${m + 1}$minutes"
    }
}
