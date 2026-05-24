package dev.roasti.ui.features.recipesteps.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.roasti.R
import dev.roasti.ui.theme.Spacing

@Composable
internal fun AutoAdvanceToggle(
    autoAdvance: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (autoAdvance) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(durationMillis = 240),
        label = "auto_toggle_container",
    )
    val contentColor by animateColorAsState(
        targetValue = if (autoAdvance) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 240),
        label = "auto_toggle_content",
    )

    Surface(
        onClick = { onToggle(!autoAdvance) },
        modifier = modifier,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Spacing.md,
                vertical = Spacing.xs,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Crossfade(
                targetState = autoAdvance,
                animationSpec = tween(220),
                label = "auto_toggle_icon",
            ) { auto ->
                Icon(
                    painter = painterResource(
                        if (auto) R.drawable.ic_play_arrow else R.drawable.ic_pause,
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            }
            AnimatedContent(
                targetState = autoAdvance,
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 3 })
                        .togetherWith(
                            fadeOut(tween(140)) + slideOutVertically(tween(140)) { -it / 3 },
                        )
                },
                label = "auto_toggle_label",
            ) { auto ->
                Text(
                    text = stringResource(
                        if (auto) R.string.steps_auto_label else R.string.steps_manual_label,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
