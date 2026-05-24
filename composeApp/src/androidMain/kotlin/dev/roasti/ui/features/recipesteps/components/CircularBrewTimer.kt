package dev.roasti.ui.features.recipesteps.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.roasti.R

@Composable
internal fun CircularBrewTimer(
    progress: Float,
    remainingLabel: String,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(100, easing = LinearEasing),
        label = "timer_progress",
    )
    val arcColor by animateColorAsState(
        targetValue = if (isRunning) MaterialTheme.colorScheme.tertiary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(250),
        label = "timer_arc_color",
    )
    val digitAlpha by animateFloatAsState(
        targetValue = if (isRunning) 1f else 0.6f,
        animationSpec = tween(250),
        label = "timer_digit_alpha",
    )
    val trackColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = remainingLabel,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = digitAlpha),
            )
            Text(
                text = stringResource(R.string.steps_remaining_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
