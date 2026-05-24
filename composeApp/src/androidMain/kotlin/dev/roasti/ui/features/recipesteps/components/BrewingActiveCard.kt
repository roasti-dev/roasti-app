package dev.roasti.ui.features.recipesteps.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.roasti.ui.features.recipesteps.AutoAdvanceCountdownUiState
import dev.roasti.ui.features.recipesteps.TimerUiState
import dev.roasti.ui.theme.ShapeXxl
import dev.roasti.ui.theme.Spacing

@Composable
internal fun BrewingActiveCard(
    title: String,
    timer: TimerUiState?,
    autoAdvanceCountdown: AutoAdvanceCountdownUiState?,
    onClick: () -> Unit,
    onCancelAutoAdvance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = ShapeXxl,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            AnimatedVisibility(
                visible = timer != null,
                enter = fadeIn(tween(durationMillis = 220, delayMillis = 180)) +
                        scaleIn(
                            initialScale = 0.86f,
                            animationSpec = tween(durationMillis = 260, delayMillis = 180),
                        ),
                exit = fadeOut(tween(durationMillis = 120)) +
                        shrinkVertically(tween(durationMillis = 160)),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                timer?.let {
                    CircularBrewTimer(
                        progress = it.progress,
                        remainingLabel = it.remainingLabel,
                        isRunning = it.isRunning,
                        modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.sm),
                    )
                }
            }
            AnimatedVisibility(
                visible = autoAdvanceCountdown != null,
                enter = fadeIn(tween(durationMillis = 200)) +
                        expandVertically(tween(durationMillis = 240)),
                exit = fadeOut(tween(durationMillis = 120)) +
                        shrinkVertically(tween(durationMillis = 160)),
            ) {
                autoAdvanceCountdown?.let {
                    AutoAdvanceCountdownInline(
                        totalMillis = it.totalMillis,
                        onCancel = onCancelAutoAdvance,
                    )
                }
            }
        }
    }
}
