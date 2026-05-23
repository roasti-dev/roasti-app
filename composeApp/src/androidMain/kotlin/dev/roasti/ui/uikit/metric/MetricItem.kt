package dev.roasti.ui.uikit.metric

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.roasti.ui.theme.Spacing

data class MetricEntry(
    val icon: Painter,
    val label: String,
    val value: String,
)

enum class MetricSize { Large, Small }

@Composable
fun MetricItem(
    entry: MetricEntry,
    size: MetricSize,
    modifier: Modifier = Modifier,
) {
    val iconSize = when (size) {
        MetricSize.Large -> 24.dp
        MetricSize.Small -> 20.dp
    }
    val labelStyle = when (size) {
        MetricSize.Large -> MaterialTheme.typography.labelMedium
        MetricSize.Small -> MaterialTheme.typography.labelSmall
    }
    val valueStyle = when (size) {
        MetricSize.Large -> MaterialTheme.typography.titleMedium
        MetricSize.Small -> MaterialTheme.typography.bodyMedium
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = entry.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(iconSize),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = entry.label,
                style = labelStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.value,
                style = valueStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
