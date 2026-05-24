package dev.roasti.ui.features.recipesteps.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.roasti.R
import dev.roasti.ui.features.recipesteps.StepRowKind

private const val ACTIVATION_DELAY_MILLIS = 400

@Composable
internal fun StepIndicatorBadge(
    kind: StepRowKind,
    number: Int,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
) {
    val tertiary = MaterialTheme.colorScheme.tertiary
    val onTertiary = MaterialTheme.colorScheme.onTertiary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val activationDelay = if (kind == StepRowKind.Active) ACTIVATION_DELAY_MILLIS else 0
    val containerColor by animateColorAsState(
        targetValue = if (kind == StepRowKind.Upcoming) Color.Transparent else tertiary,
        animationSpec = tween(durationMillis = 320, delayMillis = activationDelay),
        label = "badge_container",
    )
    val contentColor by animateColorAsState(
        targetValue = if (kind == StepRowKind.Upcoming) onSurfaceVariant else onTertiary,
        animationSpec = tween(durationMillis = 320, delayMillis = activationDelay),
        label = "badge_content",
    )
    val borderColor by animateColorAsState(
        targetValue = if (kind == StepRowKind.Upcoming) outline else Color.Transparent,
        animationSpec = tween(durationMillis = 320, delayMillis = activationDelay),
        label = "badge_border",
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        if (kind == StepRowKind.Active) {
            ActivePulseRing(color = tertiary, modifier = Modifier.fillMaxSize())
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(containerColor)
                .border(width = 1.dp, color = borderColor, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = kind == StepRowKind.Done,
                transitionSpec = {
                    (scaleIn(spring(dampingRatio = 0.55f, stiffness = 380f)) + fadeIn(tween(180)))
                        .togetherWith(fadeOut(tween(120)))
                },
                label = "badge_content_swap",
            ) { isDone ->
                if (isDone) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(size * 0.55f),
                    )
                } else {
                    Text(
                        text = number.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivePulseRing(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "badge_pulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "badge_pulse_scale",
    )
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "badge_pulse_alpha",
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(CircleShape)
            .background(color),
    )
}
