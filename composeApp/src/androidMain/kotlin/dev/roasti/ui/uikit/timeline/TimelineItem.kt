package dev.roasti.ui.uikit.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.roasti.ui.theme.Spacing

@Composable
fun TimelineItem(
    isLast: Boolean,
    modifier: Modifier = Modifier,
    circleSize: Dp = 28.dp,
    connectorColor: Color = MaterialTheme.colorScheme.outlineVariant,
    leading: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(modifier = modifier.height(IntrinsicSize.Min)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(circleSize),
        ) {
            Box(modifier = Modifier.size(circleSize), contentAlignment = Alignment.Center) {
                leading()
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .weight(1f)
                        .background(connectorColor),
                )
            }
        }
        Box(modifier = Modifier.width(Spacing.md))
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}
