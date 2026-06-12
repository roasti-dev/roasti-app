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

/**
 * Picker длительности для фонового шага: дни / часы / минуты. Построен на том же
 * [WheelPicker]-примитиве, что и [TimeWheelPicker] (минуты:секунды у edit-step), но покрывает
 * длинные ожидания (cold brew 1–4 дня). Значение — в секундах, чтобы стыковаться с durationSeconds.
 */
@Composable
fun DurationWheelPicker(
    totalSeconds: Int,
    onTotalSecondsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    dayRange: IntRange = 0..7,
    hourRange: IntRange = 0..23,
    minuteRange: IntRange = 0..59,
    minuteStep: Int = 5,
) {
    val dayOptions = remember(dayRange) { dayRange.toList() }
    val hourOptions = remember(hourRange) { hourRange.toList() }
    val minuteOptions = remember(minuteRange, minuteStep) { minuteRange.step(minuteStep).toList() }

    val totalMinutes = totalSeconds / 60
    val currentDay = (totalMinutes / (24 * 60)).coerceIn(dayRange)
    val currentHour = ((totalMinutes % (24 * 60)) / 60).coerceIn(hourRange)
    val currentMinute = (totalMinutes % 60).coerceIn(minuteRange)

    val dayIndex = remember(currentDay, dayOptions) { nearestIndex(dayOptions, currentDay) }
    val hourIndex = remember(currentHour, hourOptions) { nearestIndex(hourOptions, currentHour) }
    val minuteIndex = remember(currentMinute, minuteOptions) { nearestIndex(minuteOptions, currentMinute) }

    fun emit(days: Int, hours: Int, minutes: Int) =
        onTotalSecondsChange(((days * 24 + hours) * 60 + minutes) * 60)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = Spacing.md),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DurationColumn(
            options = dayOptions,
            selectedIndex = dayIndex,
            unitLabel = stringResource(R.string.brew_duration_days),
            onSelect = { idx -> emit(dayOptions[idx], hourOptions[hourIndex], minuteOptions[minuteIndex]) },
        )
        DurationColumn(
            options = hourOptions,
            selectedIndex = hourIndex,
            unitLabel = stringResource(R.string.brew_duration_hours),
            pad = true,
            onSelect = { idx -> emit(dayOptions[dayIndex], hourOptions[idx], minuteOptions[minuteIndex]) },
        )
        DurationColumn(
            options = minuteOptions,
            selectedIndex = minuteIndex,
            unitLabel = stringResource(R.string.brew_duration_minutes),
            pad = true,
            onSelect = { idx -> emit(dayOptions[dayIndex], hourOptions[hourIndex], minuteOptions[idx]) },
        )
    }
}

@Composable
private fun DurationColumn(
    options: List<Int>,
    selectedIndex: Int,
    unitLabel: String,
    onSelect: (Int) -> Unit,
    pad: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier.padding(horizontal = Spacing.sm),
    ) {
        WheelPicker(
            items = options,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = onSelect,
            label = { value -> if (pad) value.toString().padStart(2, '0') else value.toString() },
            modifier = Modifier.size(width = 72.dp, height = 180.dp),
        )
        Text(
            text = unitLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
