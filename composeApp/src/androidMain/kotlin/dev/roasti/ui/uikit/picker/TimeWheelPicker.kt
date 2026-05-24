package dev.roasti.ui.uikit.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.roasti.R
import dev.roasti.ui.theme.Spacing
import kotlin.math.abs

@Composable
fun TimeWheelPicker(
    totalSeconds: Int,
    onTotalSecondsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minuteRange: IntRange = 0..59,
    secondRange: IntRange = 0..59,
    minuteStep: Int = 1,
    secondStep: Int = 5,
) {
    val minuteOptions = remember(minuteRange, minuteStep) {
        minuteRange.step(minuteStep).toList()
    }
    val secondOptions = remember(secondRange, secondStep) {
        secondRange.step(secondStep).toList()
    }

    val currentMinutes = (totalSeconds / 60).coerceIn(minuteRange)
    val currentSeconds = (totalSeconds % 60).coerceIn(secondRange)

    val minuteIndex = remember(currentMinutes, minuteOptions) {
        nearestIndex(minuteOptions, currentMinutes)
    }
    val secondIndex = remember(currentSeconds, secondOptions) {
        nearestIndex(secondOptions, currentSeconds)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = Spacing.md),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            WheelPicker(
                items = minuteOptions,
                selectedIndex = minuteIndex,
                onSelectedIndexChange = { idx ->
                    onTotalSecondsChange(minuteOptions[idx] * 60 + secondOptions[secondIndex])
                },
                label = { it.toString().padStart(2, '0') },
                modifier = Modifier.size(width = 80.dp, height = 180.dp),
            )
            Text(
                text = stringResource(R.string.edit_recipe_step_duration_min),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = ":",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = Spacing.md),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            WheelPicker(
                items = secondOptions,
                selectedIndex = secondIndex,
                onSelectedIndexChange = { idx ->
                    onTotalSecondsChange(minuteOptions[minuteIndex] * 60 + secondOptions[idx])
                },
                label = { it.toString().padStart(2, '0') },
                modifier = Modifier.size(width = 80.dp, height = 180.dp),
            )
            Text(
                text = stringResource(R.string.edit_recipe_step_duration_sec),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun nearestIndex(options: List<Int>, value: Int): Int {
    if (options.isEmpty()) return 0
    var best = 0
    var bestDist = Int.MAX_VALUE
    options.forEachIndexed { i, v ->
        val d = abs(v - value)
        if (d < bestDist) {
            bestDist = d
            best = i
        }
    }
    return best
}
