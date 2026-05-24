package dev.roasti.ui.features.recipesteps.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.roasti.ui.features.recipesteps.StepRowKind
import dev.roasti.ui.theme.RoastiShapes
import dev.roasti.ui.theme.Spacing

@Composable
internal fun CollapsedStepContent(
    title: String,
    durationLabel: String?,
    kind: StepRowKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleColor = when (kind) {
        StepRowKind.Done -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        StepRowKind.Active -> MaterialTheme.colorScheme.onSurface
        StepRowKind.Upcoming -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoastiShapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Spacing.sm,
                vertical = Spacing.sm,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                modifier = Modifier.weight(1f),
            )
            if (durationLabel != null) {
                DurationChip(
                    label = durationLabel,
                    kind = kind,
                )
            }
        }
    }
}

@Composable
private fun DurationChip(
    label: String,
    kind: StepRowKind,
) {
    val chipBg = when (kind) {
        StepRowKind.Active -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val chipFg = when (kind) {
        StepRowKind.Active -> MaterialTheme.colorScheme.tertiary
        StepRowKind.Done -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        StepRowKind.Upcoming -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(RoastiShapes.small)
            .background(chipBg)
            .padding(horizontal = Spacing.sm, vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = chipFg,
        )
    }
}
