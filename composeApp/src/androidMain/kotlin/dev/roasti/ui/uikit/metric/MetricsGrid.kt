package dev.roasti.ui.uikit.metric

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.roasti.ui.theme.Spacing

private const val SMALL_GRID_COLUMNS = 3

@Composable
fun MetricsGrid(
    main: List<MetricEntry>,
    others: List<MetricEntry>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        if (main.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                main.forEach { entry ->
                    MetricItem(
                        entry = entry,
                        size = MetricSize.Large,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        if (others.isNotEmpty()) {
            others.chunked(SMALL_GRID_COLUMNS).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    row.forEach { entry ->
                        MetricItem(
                            entry = entry,
                            size = MetricSize.Small,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(SMALL_GRID_COLUMNS - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
