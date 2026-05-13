package dev.roasti.ui.features.recipesteps

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import dev.roasti.R
import dev.roasti.ui.theme.ShapeXxl
import dev.roasti.ui.theme.Spacing
import dev.roasti.ui.uikit.state.ContentScaffold

private const val TimerAnimationDurationMillis = 100

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun RecipeStepsRoute(
    id: String,
    startStep: Int = 0,
    onBackClick: () -> Unit = {},
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val viewModel: RecipeStepsViewModel = koinViewModel { parametersOf(id, startStep) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                RecipeStepsNavEvent.NavigateBack -> onBackClick()
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
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            onBackClick = onBackClick,
            onNextStep = viewModel::nextStep,
            onPreviousStep = viewModel::previousStep,
            onPauseTimer = viewModel::pauseTimer,
            onResumeTimer = viewModel::resumeTimer,
            onFinish = viewModel::finish,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun RecipeStepsScreen(
    session: SessionState,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    onBackClick: () -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onFinish: () -> Unit,
) {
    val sharedElementModifier =
        if (sharedTransitionScope != null && animatedVisibilityScope != null) {
            with(sharedTransitionScope) {
                Modifier.sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "brew_step_${session.currentStepIndex}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }
        } else Modifier

    Box(
        modifier = sharedElementModifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        AnimatedContent(
            targetState = session.isFinished,
            transitionSpec = {
                fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.92f) togetherWith
                        fadeOut(tween(200))
            },
            label = "brewing_completion",
        ) { finished ->
            if (finished) {
                CompletionContent(
                    modifier = Modifier.fillMaxSize(),
                    onFinish = onFinish,
                )
            } else {
                StepContent(
                    session = session,
                    onBackClick = onBackClick,
                    onNextStep = onNextStep,
                    onPreviousStep = onPreviousStep,
                    onPauseTimer = onPauseTimer,
                    onResumeTimer = onResumeTimer,
                )
            }
        }
    }
}

@Composable
private fun StepContent(
    session: SessionState,
    onBackClick: () -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = "navigate back button",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = stringResource(
                    R.string.steps_step_counter,
                    session.currentStepIndex + 1,
                    session.totalSteps,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val animatedStepProgress by animateFloatAsState(
            targetValue = session.stepProgress,
            animationSpec = tween(400),
            label = "step_progress",
        )
        LinearProgressIndicator(
            progress = { animatedStepProgress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg),
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = session.currentStepIndex,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn(tween(300)) togetherWith
                                slideOutHorizontally { -it } + fadeOut(tween(200))
                    } else {
                        slideInHorizontally { -it } + fadeIn(tween(300)) togetherWith
                                slideOutHorizontally { it } + fadeOut(tween(200))
                    }
                },
                label = "step_content",
            ) { stepIndex ->
                val step = session.steps[stepIndex]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.xxxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        if (session.hasTimer) {
            CircularTimer(
                timerProgress = session.timerProgress,
                remainingSeconds = session.remainingSeconds,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(Spacing.xl),
            )
        } else {
            Spacer(modifier = Modifier.height(Spacing.xl))
        }

        BottomControls(
            isFirstStep = session.isFirstStep,
            hasTimer = session.hasTimer,
            isTimerRunning = session.isTimerRunning,
            onPreviousStep = onPreviousStep,
            onPauseResume = {
                if (session.isTimerRunning) onPauseTimer() else onResumeTimer()
            },
            onNextStep = onNextStep,
        )
    }
}

@Composable
private fun CircularTimer(
    timerProgress: Float,
    remainingSeconds: Int,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = timerProgress,
        animationSpec = tween(durationMillis = TimerAnimationDurationMillis, easing = LinearEasing),
        label = "timer_arc",
    )
    val arcColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier.size(160.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
            )
            drawArc(
                color = arcColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = stroke,
            )
        }
        Text(
            text = formatSeconds(remainingSeconds),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun BottomControls(
    isFirstStep: Boolean,
    hasTimer: Boolean,
    isTimerRunning: Boolean,
    onPreviousStep: () -> Unit,
    onPauseResume: () -> Unit,
    onNextStep: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xl, vertical = Spacing.xl),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onPreviousStep,
            enabled = !isFirstStep,
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = "previous step button",
                tint = if (isFirstStep) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }

        if (hasTimer) {
            FilledIconButton(
                onClick = onPauseResume,
                modifier = Modifier.size(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                AnimatedContent(
                    targetState = isTimerRunning,
                    transitionSpec = {
                        fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                    },
                    label = "pause_resume_icon",
                ) { running ->

                    if(running) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pause),
                            contentDescription = "pause button",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_play_arrow),
                            contentDescription = "resume button",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.size(72.dp))
        }

        IconButton(
            onClick = onNextStep,
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = "previous step button",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun CompletionContent(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(Spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_coffee),
            contentDescription = "coffee cup icon",
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(Spacing.xl))
        Text(
            text = stringResource(R.string.steps_brew_ready),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.xxxl))
        Button(
            onClick = onFinish,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = ShapeXxl,
        ) {
            Text(
                text = stringResource(R.string.steps_finish),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

private fun formatSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
