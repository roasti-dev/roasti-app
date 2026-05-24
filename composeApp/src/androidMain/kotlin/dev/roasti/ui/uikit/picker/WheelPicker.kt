package dev.roasti.ui.uikit.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private val DefaultItemHeight = 36.dp
private const val DefaultVisibleItems = 5

@Composable
fun <T> WheelPicker(
    items: List<T>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    itemHeight: Dp = DefaultItemHeight,
    visibleItems: Int = DefaultVisibleItems,
) {
    if (items.isEmpty()) return

    val safeSelected = selectedIndex.coerceIn(0, items.lastIndex)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeSelected)
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val centerIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && centerIndex != safeSelected) {
            onSelectedIndexChange(centerIndex)
        }
    }

    LaunchedEffect(safeSelected) {
        if (safeSelected != listState.firstVisibleItemIndex && !listState.isScrollInProgress) {
            listState.animateScrollToItem(safeSelected)
        }
    }

    val viewportHeight = itemHeight * visibleItems
    val verticalPadding = itemHeight * (visibleItems / 2)

    val nestedScrollConsumer = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset = Offset(0f, available.y)

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity = Velocity(0f, available.y)
        }
    }

    Box(
        modifier = modifier
            .height(viewportHeight)
            .nestedScroll(nestedScrollConsumer),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
        )

        LazyColumn(
            state = listState,
            flingBehavior = snapBehavior,
            contentPadding = PaddingValues(vertical = verticalPadding),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(items.size) { index ->
                val distance = abs(index - centerIndex)
                val alpha = when (distance) {
                    0 -> 1f
                    1 -> 0.55f
                    2 -> 0.28f
                    else -> 0.12f
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label(items[index]),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (distance == 0) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        color = if (distance == 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                        },
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
