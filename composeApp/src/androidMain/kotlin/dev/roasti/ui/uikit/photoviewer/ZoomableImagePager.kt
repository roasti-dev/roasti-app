package dev.roasti.ui.uikit.photoviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * Reusable photo pager with pinch-to-zoom (per page) and vertical drag-to-dismiss.
 *
 * - Each page is a [ZoomableImage] with its own [ZoomableImageState].
 * - Pager horizontal swipe is disabled while the current image is zoomed (`scale > 1`).
 * - Vertical drag on the root translates + scales the entire pager;
 *   if the drag distance crosses [DismissThresholdDp] the [onClose] callback fires.
 *   Background is solid black and does not fade with drag.
 */
@Composable
fun ZoomableImagePager(
    images: List<String>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    pagerState: PagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { images.size },
    ),
    onSingleTap: () -> Unit = {},
    pageContentModifier: @Composable (url: String) -> Modifier = { Modifier },
) {
    val scope = rememberCoroutineScope()

    val states = remember(images) {
        mutableStateMapOf<Int, ZoomableImageState>().also { map ->
            images.indices.forEach { idx -> map[idx] = ZoomableImageState() }
        }
    }

    val currentState = states[pagerState.currentPage]
    val isCurrentZoomed = currentState?.isZoomed == true

    val dragOffsetY = remember { androidx.compose.animation.core.Animatable(0f) }
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { DismissThresholdDp.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isCurrentZoomed) {
                if (isCurrentZoomed) return@pointerInput
                detectVerticalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (abs(dragOffsetY.value) > dismissThresholdPx) {
                                onClose()
                            } else {
                                dragOffsetY.animateTo(0f)
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch { dragOffsetY.animateTo(0f) }
                    },
                    onVerticalDrag = { _, dy ->
                        scope.launch { dragOffsetY.snapTo(dragOffsetY.value + dy) }
                    },
                )
            }
            .graphicsLayer {
                translationY = dragOffsetY.value
                val dragScale = (1f - abs(dragOffsetY.value) / 2000f).coerceAtLeast(0.85f)
                scaleX = dragScale
                scaleY = dragScale
            },
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isCurrentZoomed,
            beyondViewportPageCount = 1,
        ) { page ->
            val pageState = states.getOrPut(page) { ZoomableImageState() }
            val url = images[page]
            ZoomableImage(
                url = url,
                state = pageState,
                onSingleTap = onSingleTap,
                modifier = Modifier.fillMaxSize(),
                contentModifier = pageContentModifier(url),
            )
        }
    }
}

private const val DismissThresholdDp = 140
