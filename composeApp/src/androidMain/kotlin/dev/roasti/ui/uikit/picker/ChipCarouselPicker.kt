package dev.roasti.ui.uikit.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.roasti.R

private val ChipHeight = 40.dp

data class ChipOption<T>(
    val value: T,
    val label: String,
    val fillTint: Color? = null,
)

@Composable
fun <T> ChipCarouselPicker(
    options: List<ChipOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberLazyListState()
    val selectedIdx = options.indexOfFirst { it.value == selected }.coerceAtLeast(0)

    LaunchedEffect(selectedIdx) {
        if (selectedIdx >= 0) state.animateScrollToItem(selectedIdx)
    }

    LazyRow(
        modifier = modifier,
        state = state,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options.size) { index ->
            ChipItem(
                option = options[index],
                isSelected = options[index].value == selected,
                onClick = { onSelect(options[index].value) },
            )
        }
    }
}

@Composable
private fun <T> ChipItem(
    option: ChipOption<T>,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val tint = option.fillTint

    val background = when {
        tint != null -> tint
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = when {
        tint != null -> autoContrastOn(tint)
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .height(ChipHeight)
            .clip(shape)
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (isSelected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = option.label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = contentColor,
        )
    }
}

private fun autoContrastOn(bg: Color): Color =
    if (bg.luminance() < 0.5f) Color.White else Color(0xFF1C1917)
