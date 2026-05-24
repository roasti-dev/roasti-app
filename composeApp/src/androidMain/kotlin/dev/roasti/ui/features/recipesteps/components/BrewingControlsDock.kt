package dev.roasti.ui.features.recipesteps.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.roasti.R
import dev.roasti.ui.theme.ShapeXxl
import dev.roasti.ui.theme.Spacing

@Composable
internal fun BrewingControlsDock(
    isFirstStep: Boolean,
    isLastStep: Boolean,
    hasTimer: Boolean,
    isTimerRunning: Boolean,
    onPrevious: () -> Unit,
    onPauseResume: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = ShapeXxl,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onPrevious,
                enabled = !isFirstStep,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = stringResource(R.string.steps_previous),
                    tint = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (isFirstStep) 0.3f else 1f,
                    ),
                )
            }
            FilledIconButton(
                onClick = onPauseResume,
                enabled = hasTimer,
                modifier = Modifier.size(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                ),
            ) {
                Crossfade(
                    targetState = isTimerRunning,
                    animationSpec = tween(150),
                    label = "play_pause_icon",
                ) { running ->
                    Icon(
                        painter = painterResource(
                            if (running) R.drawable.ic_pause else R.drawable.ic_play_arrow,
                        ),
                        contentDescription = stringResource(
                            if (running) R.string.steps_pause else R.string.steps_resume,
                        ),
                    )
                }
            }
            IconButton(
                onClick = onNext,
                modifier = Modifier.size(56.dp),
            ) {
                Crossfade(
                    targetState = isLastStep,
                    animationSpec = tween(200),
                    label = "next_finish_icon",
                ) { last ->
                    Icon(
                        painter = painterResource(
                            if (last) R.drawable.ic_check else R.drawable.ic_arrow_right,
                        ),
                        contentDescription = stringResource(R.string.steps_next),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
